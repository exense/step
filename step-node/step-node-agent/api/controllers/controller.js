const fs = require("fs");
const path = require("path");
const {fork, spawn, spawnSync} = require("child_process");
const Session = require('./session');
const { OutputBuilder } = require('./output');
const logger = require('../logger').child({ component: 'Controller' });

const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';

process.on('unhandledRejection', error => {
  logger.error('Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported: ' + error)
})

process.on('uncaughtException', error => {
  logger.error('Critical: an unhandled error (uncaught exception) occurred and might not have been reported: ' + error)
})

class Controller {
  constructor(agentContext, fileManager, mode) {
    this.agentContext = agentContext;
    this.filemanager = fileManager;
    this.redirectIO = mode !== 'agent';
  }

  isRunning(req, res) {
    res.status(200).json('Agent is running')
  }

  reserveToken(req, res) {
    this.reserveToken_(req.params.tokenId)
    res.json({})
  }

  reserveToken_(tokenId) {
    logger.info('Reserving token: ' + tokenId)
  }

  releaseToken(req, res) {
    this.releaseToken_(req.params.tokenId)
    res.json({})
  }

  releaseToken_(tokenId) {
    logger.info('Releasing token: ' + tokenId)

    const session = this.agentContext.tokenSessions[tokenId]
    if (session) {
      // Close the session and all objects it contains
      session[Symbol.dispose]();
      this.agentContext.tokenSessions[tokenId] = null;
    } else {
      logger.warn('No session found for token: ' + tokenId)
    }
  }

  interruptExecution(req, res) {
    const tokenId = req.params.tokenId
    logger.warn('Interrupting token: ' + tokenId + ' : not implemented')
  }

  async process(req, res) {
    const tokenId = req.params.tokenId
    let input = req.body.payload;
    const keywordName = input.function
    let offset = 1000;
    const callTimeoutMs = Math.max(offset, input.functionCallTimeout ? input.functionCallTimeout : 180000 - offset);
    const argument = input.payload
    const properties = input.properties

    let token = this.agentContext.tokens.find(value => value.id == tokenId);
    if(token) {
      logger.info('Using token: ' + tokenId + ' to execute ' + keywordName)

      // add the agent properties
      const agentProperties = this.agentContext.properties
      Object.entries(agentProperties).forEach(([key, value]) => { properties[key] = value })

      // add the properties of the tokenGroup
      const additionalProperties = this.agentContext.tokenProperties[tokenId]
      Object.entries(additionalProperties).forEach(([key, value]) => { properties[key] = value })

      const payload = await this.process_(tokenId, keywordName, argument, properties, callTimeoutMs)
      res.json(payload)
    } else {
      const outputBuilder = new OutputBuilder();
      outputBuilder.fail("The token '" + tokenId + " doesn't exist on this agent. This usually means that the agent crashed and restarted.");
    }
  }

  async process_(tokenId, keywordName, argument, properties, callTimeoutMs) {
    const outputBuilder = new OutputBuilder();
    try {
      const keywordPackageFile = await this.filemanager.loadOrGetKeywordFile(
        this.agentContext.controllerUrl + '/grid/file/',
        properties['$node.js.file.id'],
        properties['$node.js.file.version'],
        keywordName
      )

      await this.executeKeyword(keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder, callTimeoutMs)
    } catch (e) {
      logger.error('Unexpected error while executing keyword ' + keywordName + ': ' + e)
      outputBuilder.fail('Unexpected error while executing keyword', e)
    }
    return outputBuilder.build();
  }

  async executeKeyword(keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder, callTimeoutMs) {
    let isDebugEnabled = properties['debug'] === 'true';
    let npmAttachment = null;
    let forkedAgentProcessOutputAttachment = null;
    try {
      let npmProjectPath;
      if (keywordPackageFile.toUpperCase().endsWith('ZIP')) {
        if (this.filemanager.isFirstLevelKeywordFolder(keywordPackageFile)) {
          npmProjectPath = path.resolve(keywordPackageFile);
        } else {
          npmProjectPath = path.resolve(keywordPackageFile, this.filemanager.getFolderName(keywordPackageFile))
        }
      } else {
        // Local execution with KeywordRunner
        npmProjectPath = path.resolve(keywordPackageFile)
      }

      logger.info('Executing keyword in project ' + npmProjectPath + ' for token ' + tokenId)

      let session = this.getOrCreateSession(tokenId);

      const npmProjectPathInSession = session.get('npmProjectPath');
      if (npmProjectPathInSession && npmProjectPathInSession !== npmProjectPath) {
        throw new Error("Multiple npm projects are not supported within the same session");
      } else {
        session.set('npmProjectPath', npmProjectPath);
      }

      let forkedAgent = session.get('forkedAgent');
      if (!forkedAgent) {
        logger.info('Starting agent fork in ' + npmProjectPath + ' for token ' + tokenId)
        forkedAgent = createForkedAgent(npmProjectPath);
        session.set('forkedAgent', forkedAgent);

        logger.info('Running npm install in ' + npmProjectPath + ' for token ' + tokenId)
        const npmInstallResult = await this.executeNpmInstall(npmProjectPath);
        const npmInstallFailed = npmInstallResult.status !== 0 || npmInstallResult.error != null;
        if (npmInstallFailed || isDebugEnabled) {
          npmAttachment = npmInstallResult.processOutputAttachment;
        }

        if (npmInstallFailed) {
          throw npmInstallResult.error || new Error('npm install exited with code ' + npmInstallResult.status);
        }
      }

      logger.info('Executing keyword \'' + keywordName + '\' in ' + npmProjectPath + ' for token ' + tokenId)
      const { result, processOutputAttachment } = await forkedAgent.runKeywordTask(forkedAgent, npmProjectPath, keywordName, argument, properties, callTimeoutMs, this.redirectIO);
      outputBuilder.merge(result.payload)
      if (result.error || isDebugEnabled) {
        forkedAgentProcessOutputAttachment = processOutputAttachment;
      }
    } catch (e) {
      if (e instanceof CategorizedError) {
        logger.error('Error occurred while executing keyword: ' + e.message)
        outputBuilder.fail(e.message)
        forkedAgentProcessOutputAttachment = e.processOutputAttachment;
      } else {
        logger.error('Unexpected error occurred while executing keyword: ' + e)
        outputBuilder.fail('Unexpected error: ' + e.message, e)
      }
    } finally {
      if (npmAttachment) {
        outputBuilder.attach(npmAttachment);
      }
      if (forkedAgentProcessOutputAttachment) {
        outputBuilder.attach(forkedAgentProcessOutputAttachment);
      }
    }
  }

  getOrCreateSession(tokenId) {
    let session = this.agentContext.tokenSessions[tokenId]
    if (!session) {
      session = new Session();
      this.agentContext.tokenSessions[tokenId] = session;
    }
    return session;
  }

  async executeNpmInstall(npmProjectPath) {
    return await new Promise((resolve) => {
      const child = spawn(npmCommand, ['install'], {cwd: npmProjectPath, shell: false});
      const stdChunks = [];

      child.stdout.on('data', (data) => {
        stdChunks.push(data);
        if (this.redirectIO) {
          process.stdout.write(data)
        }
      });

      child.stderr.on('data', (data) => {
        stdChunks.push(data);
        if (this.redirectIO) {
          process.stderr.write(data)
        }
      });

      child.on('error', (error) => {
        resolve({status: null, error, processOutputAttachment: getNpmInstallProcessOutputAttachment()});
      });

      child.on('close', (code) => {
        resolve({status: code, error: null, processOutputAttachment: getNpmInstallProcessOutputAttachment()});
      });

      function getNpmInstallProcessOutputAttachment() {
        const npmInstallOutput = Buffer.concat(stdChunks);
        return {
          name: 'npm-install.log',
          isDirectory: false,
          description: 'npm install output',
          hexContent: npmInstallOutput.toString('base64')
        };
      }
    });
  }
}

function createForkedAgent(keywordProjectPath) {
  fs.copyFileSync(path.resolve(__dirname, '../../agent-fork.js'), path.join(keywordProjectPath, './agent-fork.js'));
  fs.copyFileSync(path.join(__dirname, 'output.js'), path.join(keywordProjectPath, './output.js'));
  fs.copyFileSync(path.join(__dirname, 'session.js'), path.join(keywordProjectPath, './session.js'));
  return new ForkedAgent(fork('./agent-fork.js', [], {cwd: keywordProjectPath, silent: true}));
}

class ForkedAgent {

  constructor(process) {
    this.forkProcess = process;
  }

  runKeywordTask(forkedAgent, keywordProjectPath, functionName, input, properties, timeoutMs, redirectIO) {
    return new Promise((resolve, reject) => {
      try {
        const stdChunks = [];

        const stdoutListener = (data) => {
          stdChunks.push(data);
          if(redirectIO) {
            process.stdout.write(data);
          }
        };
        const stderrListener = (data) => {
          stdChunks.push(data);
          if(redirectIO) {
            process.stderr.write(data);
          }
        };

        if (this.forkProcess.stdout) {
          this.forkProcess.stdout.on('data', stdoutListener);
        }

        if (this.forkProcess.stderr) {
          this.forkProcess.stderr.on('data', stderrListener);
        }

        const timeoutHandle = timeoutMs != null ? setTimeout(() => {
          cleanup();
          let processOutputAttachment = buildProcessOutputAttachment(stdChunks);
          reject(new CategorizedError(`Keyword execution timed out after ${timeoutMs}ms`, processOutputAttachment));
        }, timeoutMs) : null;

        const cleanup = () => {
          clearTimeout(timeoutHandle);
          if (this.forkProcess.stdout) {
            this.forkProcess.stdout.removeListener('data', stdoutListener);
          }
          if (this.forkProcess.stderr) {
            this.forkProcess.stderr.removeListener('data', stderrListener);
          }
        };

        this.forkProcess.removeAllListeners('message');
        this.forkProcess.on('message', (result) => {
          logger.info(`Keyword '${functionName}' execution completed in forked agent.`)
          cleanup();

          let processOutputAttachment = buildProcessOutputAttachment(stdChunks);
          resolve({ result, processOutputAttachment});
        });

        this.forkProcess.removeAllListeners('error');
        this.forkProcess.on('error', (err) => {
          logger.error('Error while calling forked agent: ' + err)
        });

        this.forkProcess.send({ type: "KEYWORD", projectPath: keywordProjectPath, functionName, input, properties });
      } catch (e) {
        logger.error('Unexpected error while calling forked agent: ' + e)
      }
    });

    function buildProcessOutputAttachment(stdChunks) {
      const outputBuffer = Buffer.concat(stdChunks);
      return {
        name: 'keyword-process.log',
        isDirectory: false,
        description: 'Output of the forked keyword process',
        hexContent: outputBuffer.toString('base64'),
      };
    }
  }

  close() {
    try {
      this.forkProcess.send({ type: "KILL" });
    } catch (e) {
      this.forkProcess.kill();
    }

  }
}

class CategorizedError extends Error {
  processOutputAttachment;
  constructor(message, processOutputAttachment) {
    super(message); // (1)
    this.name = "CategorizedError";
    this.processOutputAttachment = processOutputAttachment;
  }
}

module.exports = Controller;
