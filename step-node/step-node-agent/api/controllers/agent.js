const fs = require("fs");
const path = require("path");
const {fork, spawn} = require("child_process");
const Session = require('./session');
const { OutputBuilder } = require('./output');
const logger = require('../logger').child({ component: 'Agent' });

const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';

process.on('unhandledRejection', error => {
  logger.error('Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported:', error)
})

process.on('uncaughtException', error => {
  logger.error('Critical: an unhandled error (uncaught exception) occurred and might not have been reported:', error)
})

class Agent {
  constructor(agentContext, fileManager, mode) {
    this.agentContext = agentContext;
    this.filemanager = fileManager;
    this.mode = mode;
    this.redirectIO = mode !== 'agent';
    this.npmProjectWorkspaces = new Map(); // cacheKey -> { path, inUse, lastFreeAt }
    this.npmProjectWorkspaceCleanupIdleTimeMs = agentContext.npmProjectWorkspaceCleanupIdleTimeMs ?? 3600000;

    if(mode === 'agent' && this.npmProjectWorkspaceCleanupIdleTimeMs > 0) {
      logger.info(`Scheduling npm project workspace cleanup every ${this.npmProjectWorkspaceCleanupIdleTimeMs}ms`);
      setInterval(() => this.cleanupUnusedWorkspaces(this.npmProjectWorkspaceCleanupIdleTimeMs), this.npmProjectWorkspaceCleanupIdleTimeMs);
    }
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

    let output;
    let token = this.agentContext.tokens.find(value => value.id === tokenId);
    if(token) {
      logger.info('Using token: ' + tokenId + ' to execute ' + keywordName)

      // add the agent properties
      const agentProperties = this.agentContext.properties
      if (agentProperties) {
        Object.entries(agentProperties).forEach(([key, value]) => { properties[key] = value })
      }

      // add the properties of the tokenGroup
      const additionalProperties = this.agentContext.tokenProperties[tokenId]
      if (additionalProperties) {
        Object.entries(additionalProperties).forEach(([key, value]) => { properties[key] = value })
      }

      output = await this.process_(tokenId, keywordName, argument, properties, callTimeoutMs)
    } else {
      const outputBuilder = new OutputBuilder();
      outputBuilder.fail("The token '" + tokenId + " doesn't exist on this agent. This usually means that the agent crashed and restarted.");
      output = outputBuilder.build();
    }
    res.json(output)
  }

  async process_(tokenId, keywordName, argument, properties, callTimeoutMs) {
    const outputBuilder = new OutputBuilder();
    try {
      let fileId = properties['$node.js.file.id'];
      let fileVersionId = properties['$node.js.file.version'];
      const file = await this.filemanager.loadOrGetKeywordFile(
        this.agentContext.controllerUrl + '/grid/file/',
        fileId,
        fileVersionId,
        keywordName
      )

      let npmProjectPath;
      let fileBasename = path.basename(file);
      let wrapperDirectory = path.resolve(file, fileBasename.substring(0, fileBasename.length - 4));

      if (file.toUpperCase().endsWith('.ZIP') && fs.existsSync(wrapperDirectory)) {
        // If the ZIP contains a top-level wrapper folder
        npmProjectPath = path.resolve(file, wrapperDirectory);
      } else {
        npmProjectPath = path.resolve(file)
      }

      let workspacePath;
      if (this.mode === 'agent') {
        // Create a copy of the npm project for each token
        workspacePath = await this.getOrCreateNpmProjectWorkspace(tokenId, {fileId, fileVersionId, file: npmProjectPath});
        this.markWorkspaceInUse(workspacePath);
      } else {
        // When running keywords locally we're working directly in npm project passed by the runner
        workspacePath = npmProjectPath;
      }
      try {
        await this.executeKeyword(keywordName, workspacePath, tokenId, argument, properties, outputBuilder, callTimeoutMs)
      } finally {
        if (this.mode === 'agent') {
          this.markWorkspaceFree(workspacePath);
          if (this.npmProjectWorkspaceCleanupIdleTimeMs === 0) {
            await this.cleanupUnusedWorkspaces(0);
          }
        }
      }
    } catch (e) {
      logger.error('Unexpected error while executing keyword ' + keywordName + ':', e)
      outputBuilder.fail('Unexpected error while executing keyword', e)
    }
    return outputBuilder.build();
  }

  async executeKeyword(keywordName, npmProjectPath, tokenId, argument, properties, outputBuilder, callTimeoutMs) {
    let isDebugEnabled = properties['debug'] === 'true';
    let npmAttachment = null;
    let forkedAgentProcessOutputAttachment = null;
    try {
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

        session.set('keywordDirectory', await readStepKeywordDirectory(npmProjectPath));
      }

      const keywordDirectory = session.get('keywordDirectory');
      logger.info('Executing keyword \'' + keywordName + '\' in ' + npmProjectPath + ' for token ' + tokenId)
      const { result, processOutputAttachment } = await forkedAgent.runKeywordTask(npmProjectPath, keywordName, argument, properties, callTimeoutMs, this.redirectIO, keywordDirectory);
      outputBuilder.merge(result.payload)
      if (result.error || isDebugEnabled) {
        forkedAgentProcessOutputAttachment = processOutputAttachment;
      }
    } catch (e) {
      if (e instanceof CategorizedError) {
        logger.error('Error occurred while executing keyword:' + e.message)
        outputBuilder.fail(e.message)
        forkedAgentProcessOutputAttachment = e.processOutputAttachment;
      } else {
        logger.error('Unexpected error occurred while executing keyword:', e)
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

  async getOrCreateNpmProjectWorkspace(tokenId, keywordPackage) {
    const cacheKey = `${keywordPackage.fileId}_${keywordPackage.fileVersionId}_${tokenId}`;
    const workspace = this.npmProjectWorkspaces.get(cacheKey);
    if (workspace) {
      return workspace.path;
    }
    const baseDir = path.resolve((this.agentContext.workingDir ?? '.'), 'npm-project-workspaces');
    const workspacePath = path.join(baseDir, cacheKey);
    if (!fs.existsSync(workspacePath)) {
      logger.info(`Creating npm project workspace at ${workspacePath}`);
      await fs.promises.cp(keywordPackage.file, workspacePath, { recursive: true });
    }
    this.npmProjectWorkspaces.set(cacheKey, { path: workspacePath, inUse: false, lastFreeAt: Date.now() });
    return workspacePath;
  }

  markWorkspaceInUse(workspacePath) {
    for (const workspace of this.npmProjectWorkspaces.values()) {
      if (workspace.path === workspacePath) {
        workspace.inUse = true;
        return;
      }
    }
  }

  markWorkspaceFree(workspacePath) {
    for (const workspace of this.npmProjectWorkspaces.values()) {
      if (workspace.path === workspacePath) {
        workspace.inUse = false;
        workspace.lastFreeAt = Date.now();
        return;
      }
    }
  }

  async cleanupUnusedWorkspaces(idleTimeMs) {
    const now = Date.now();
    for (const [cacheKey, workspace] of this.npmProjectWorkspaces) {
      if (!workspace.inUse && (now - workspace.lastFreeAt) >= idleTimeMs) {
        logger.info(`Deleting npm project workspace unused for ${idleTimeMs}ms: ${workspace.path}`);
        try {
          await fs.promises.rm(workspace.path, { recursive: true, force: true });
        } catch (e) {
          logger.error(`Failed to delete npm project workspace ${workspace.path}:`, e);
          continue;
        }
        this.npmProjectWorkspaces.delete(cacheKey);
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
      const child = spawn(npmCommand, ['install'], {cwd: npmProjectPath, shell: true});
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

async function readStepKeywordDirectory(npmProjectPath) {
  try {
    const content = await fs.promises.readFile(path.join(npmProjectPath, 'package.json'), 'utf8');
    return JSON.parse(content)?.step?.keywords ?? './keywords';
  } catch {
    return './keywords';
  }
}

function createForkedAgent(keywordProjectPath) {
  return new ForkedAgent(keywordProjectPath);
}

class ForkedAgent {

  constructor(keywordProjectPath) {
    const agentForkerLibPath = path.join(keywordProjectPath, 'agent-fork-libs');
    fs.mkdirSync(agentForkerLibPath, { recursive: true });
    fs.copyFileSync(path.resolve(__dirname, 'agent-fork.js'), path.join(agentForkerLibPath, 'agent-fork.js'));
    fs.copyFileSync(path.join(__dirname, 'output.js'), path.join(agentForkerLibPath, 'output.js'));
    fs.copyFileSync(path.join(__dirname, 'session.js'), path.join(agentForkerLibPath, 'session.js'));
    this.agentForkerLibPath = agentForkerLibPath;
    this.forkProcess = fork(path.join(agentForkerLibPath, 'agent-fork.js'), [], {cwd: keywordProjectPath, silent: true});
  }

  runKeywordTask(keywordProjectPath, functionName, input, properties, timeoutMs, redirectIO, keywordDirectory) {
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
          logger.error('Error while calling forked agent:', err)
        });

        this.forkProcess.send({ type: "KEYWORD", projectPath: keywordProjectPath, functionName, input, properties, keywordDirectory });
      } catch (e) {
        logger.error('Unexpected error while calling forked agent:', e)
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
    fs.rmSync(this.agentForkerLibPath, {recursive: true, force: true});
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

module.exports = Agent;
