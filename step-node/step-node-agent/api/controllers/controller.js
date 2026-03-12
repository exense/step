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
  constructor(agentContext, fileManager) {
    this.agentContext = agentContext;
    this.filemanager = fileManager;
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
    const keywordName = req.body.payload.function
    const argument = req.body.payload.payload
    const properties = req.body.payload.properties

    let token = this.agentContext.tokens.find(value => value.id == tokenId);
    if(token) {
      logger.info('Using token: ' + tokenId + ' to execute ' + keywordName)

      // add the agent properties
      const agentProperties = this.agentContext.properties
      Object.entries(agentProperties).forEach(([key, value]) => { properties[key] = value })

      // add the properties of the tokenGroup
      const additionalProperties = this.agentContext.tokenProperties[tokenId]
      Object.entries(additionalProperties).forEach(([key, value]) => { properties[key] = value })

      const payload = await this.process_(tokenId, keywordName, argument, properties)
      res.json(payload)
    } else {
      const outputBuilder = new OutputBuilder();
      outputBuilder.fail("The token '" + tokenId + " doesn't exist on this agent. This usually means that the agent crashed and restarted.");
    }

  }

  async process_(tokenId, keywordName, argument, properties) {
    const outputBuilder = new OutputBuilder();
    try {
      const keywordPackageFile = await this.filemanager.loadOrGetKeywordFile(
        this.agentContext.controllerUrl + '/grid/file/',
        properties['$node.js.file.id'],
        properties['$node.js.file.version'],
        keywordName
      )

      await this.executeKeyword(keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder)
    } catch (e) {
      logger.error('Unexpected error while executing keyword ' + keywordName + ': ' + e)
      outputBuilder.fail('Unexpected error while executing keyword', e)
    }
    return outputBuilder.build();
  }

  async executeKeyword(keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder) {
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

      let session = this.agentContext.tokenSessions[tokenId]
      if (!session) {
        session = new Session();
        this.agentContext.tokenSessions[tokenId] = session;
      }

      let forkedAgent = session.get('forkedAgent');
      const project = session.get('npmProjectPath');
      if (!project && forkedAgent) {
        throw new Error("Multiple projects not supported within the same session");
      }

      let npmAttachment = null;
      let isDebugEnabled = properties['debug'] === 'true';

      if (!forkedAgent) {
        logger.info('Starting agent fork in ' + npmProjectPath + ' for token ' + tokenId)
        forkedAgent = createForkedAgent(npmProjectPath);

        logger.info('Running npm install in ' + npmProjectPath + ' for token ' + tokenId)
        const npmInstallResult = await this.executeNpmInstall(npmProjectPath);
        const npmInstallOutput = Buffer.concat([npmInstallResult.stdout || Buffer.alloc(0), npmInstallResult.stderr || Buffer.alloc(0)]);
        const npmInstallFailed = npmInstallResult.status !== 0 || npmInstallResult.error != null;
        if (npmInstallFailed || isDebugEnabled) {
          npmAttachment = {
            name: 'npm-install.log',
            isDirectory: false,
            description: 'npm install output',
            hexContent: npmInstallOutput.toString('base64')
          };
        }

        session.set('forkedAgent', forkedAgent);
        session.set('npmProjectPath', npmProjectPath);

        if (npmInstallFailed) {
          outputBuilder.attach(npmAttachment);
          throw npmInstallResult.error || new Error('npm install exited with code ' + npmInstallResult.status);
        }
      }

      logger.info('Executing keyword \'' + keywordName + '\' in ' + npmProjectPath + ' for token ' + tokenId)
      const { result, outputBuffer } = await forkedAgent.runKeywordTask(forkedAgent, npmProjectPath, keywordName, argument, properties);
      outputBuilder.merge(result.payload)

      if (npmAttachment) {
        outputBuilder.attach(npmAttachment);
      }

      if (outputBuffer && (result.payload.error || isDebugEnabled)) {
        const forkAttachment = {
          name: 'keyword-process.log',
          isDirectory: false,
          description: 'Output of the forked keyword process',
          hexContent: outputBuffer.toString('base64'),
        };
        outputBuilder.attach(forkAttachment);
      }
    } catch (e) {
      logger.error('Unexpected error occurred while executing keyword: ' + e)
      outputBuilder.fail('An error occurred while attempting to execute the keyword ' + keywordName, e)
    }
  }

  async executeNpmInstall(npmProjectPath) {
    return await new Promise((resolve) => {
      const child = spawn(npmCommand, ['install'], {cwd: npmProjectPath, shell: false});
      const stdoutChunks = [];
      const stderrChunks = [];

      child.stdout.on('data', (data) => {
        stdoutChunks.push(data);
        process.stdout.write(data);
      });

      child.stderr.on('data', (data) => {
        stderrChunks.push(data);
        process.stderr.write(data);
      });

      child.on('error', (error) => {
        resolve({status: null, error, stdout: Buffer.concat(stdoutChunks), stderr: Buffer.concat(stderrChunks)});
      });

      child.on('close', (code) => {
        resolve({status: code, error: null, stdout: Buffer.concat(stdoutChunks), stderr: Buffer.concat(stderrChunks)});
      });
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
    this.process = process;
  }

  runKeywordTask(forkedAgent, keywordProjectPath, functionName, input, properties) {
    return new Promise((resolve) => {
      try {
        const stdoutChunks = [];
        const stderrChunks = [];

        const stdoutListener = (data) => {
          stdoutChunks.push(data);
          process.stdout.write(data);
        };
        const stderrListener = (data) => {
          stderrChunks.push(data);
          process.stderr.write(data);
        };

        if (this.process.stdout) {
          this.process.stdout.on('data', stdoutListener);
        }

        if (this.process.stderr) {
          this.process.stderr.on('data', stderrListener);
        }

        this.process.removeAllListeners('message');
        this.process.on('message', (result) => {
          if (this.process.stdout) {
            this.process.stdout.removeListener('data', stdoutListener);
          }
          if (this.process.stderr) {
            this.process.stderr.removeListener('data', stderrListener);
          }

          const outputBuffer = Buffer.concat([
            ...stdoutChunks,
            ...stderrChunks,
          ]);

          resolve({ result, outputBuffer });
        });

        this.process.removeAllListeners('error');
        this.process.on('error', (err) => {
          logger.error('Error while calling forked agent: ' + err)
        });

        this.process.send({ type: "KEYWORD", projectPath: keywordProjectPath, functionName, input, properties });
      } catch (e) {
        logger.error('Unexpected error while calling forked agent: ' + e)
      }
    });
  }

  close() {
    try {
      this.process.send({ type: "KILL" });
    } catch (e) {
      this.process.kill();
    }

  }
}
module.exports = Controller;
