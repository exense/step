const fs = require("fs");
const path = require("path");
const {fork, execSync} = require("child_process");
const Session = require('./session');
const { OutputBuilder } = require('./output');

process.on('unhandledRejection', error => {
  console.log('[Controller] Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported', error)
})

process.on('uncaughtException', error => {
  console.log('[Controller] Critical: an unhandled error (uncaught exception) occurred and might not have been reported', error)
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
    console.log('[Controller] Reserving token: ' + tokenId)
  }

  releaseToken(req, res) {
    this.releaseToken_(req.params.tokenId)
    res.json({})
  }

  releaseToken_(tokenId) {
    console.log('[Controller] Releasing token: ' + tokenId)

    const session = this.agentContext.tokenSessions[tokenId]
    if (session) {
      // Close the session and all objects it contains
      session[Symbol.dispose]();
      this.agentContext.tokenSessions[tokenId] = null;
    } else {
      console.log('[Controller] No session founds for token: ' + tokenId)
    }
  }

  interruptExecution(req, res) {
    const tokenId = req.params.tokenId
    console.warn('[Controller] Interrupting token: ' + tokenId + ' : not implemented')
  }

  async process(req, res) {
    const tokenId = req.params.tokenId
    const keywordName = req.body.payload.function
    const argument = req.body.payload.payload
    const properties = req.body.payload.properties

    let token = this.agentContext.tokens.find(value => value.id == tokenId);
    if(token) {
      console.log('[Controller] Using token: ' + tokenId + ' to execute ' + keywordName)

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
      console.log('[Controller] Unexpected error while executing keyword ' + keywordName + ' :' + err)
      outputBuilder.fail('Unexpected error while executing keyword', err)
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

      console.log('[Controller] Executing keyword in project ' + npmProjectPath + ' for token ' + tokenId)

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

      if (!forkedAgent) {
        console.log('[Controller] Starting agent fork in ' + npmProjectPath + ' for token ' + tokenId)
        forkedAgent = createForkedAgent(npmProjectPath);
        console.log('[Controller] Running npm install in ' + npmProjectPath + ' for token ' + tokenId)
        execSync('npm install', { cwd: npmProjectPath, stdio: 'inherit' });
        session.set('forkedAgent', forkedAgent);
        session.set('npmProjectPath', npmProjectPath);
      }

      const output = await runKeywordTask(forkedAgent, npmProjectPath, keywordName, argument, properties);
      outputBuilder.merge(output.payload)
    } catch (e) {
      console.log("[Controller] Unexpected error occurred while executing keyword ", e)
      outputBuilder.fail('An error occurred while attempting to execute the keyword ' + keywordName, e)
    }
  }
}

function createForkedAgent(keywordProjectPath) {
  fs.copyFileSync(path.resolve(__dirname, '../../worker-wrapper.js'), path.join(keywordProjectPath, './worker-wrapper.js'));
  fs.copyFileSync(path.join(__dirname, 'output.js'), path.join(keywordProjectPath, './output.js'));
  fs.copyFileSync(path.join(__dirname, 'session.js'), path.join(keywordProjectPath, './session.js'));
  return fork('./worker-wrapper.js', [], {cwd: keywordProjectPath});
}

function runKeywordTask(forkedAgent, keywordProjectPath, functionName, input, properties) {
  return new Promise((resolve) => {
    try {
      forkedAgent.send({ projectPath: keywordProjectPath, functionName, input, properties });

      forkedAgent.removeAllListeners('message');
      forkedAgent.on('message', (result) => {
        resolve(result);
      });

      forkedAgent.removeAllListeners('error');
      forkedAgent.on('error', (err) => {
        console.error('Error while calling forked agent:', err);
      });
    } catch (e) {
      console.log('[Controller] Unexpected error while calling forked agent', e);
    }
  });
}

module.exports = Controller;
