const fs = require("fs");
const path = require("path");
const {fork, execSync} = require("child_process");
const Session = require('./session');
module.exports = function Controller (agentContext, fileManager) {
  process.on('unhandledRejection', error => {
    console.log('[Controller] Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported', error)
  })

  process.on('uncaughtException', error => {
    console.log('[Controller] Critical: an unhandled error (uncaught exception) occurred and might not have been reported', error)
  })

  let exports = {}

  const OutputBuilder = require('./output')

  exports.filemanager = fileManager

  exports.isRunning = function (req, res) {
    res.status(200).json('Agent is running')
  }

  exports.reserveToken = function (req, res) {
    exports.reserveToken_(req.params.tokenId)
    res.json({})
  }

  exports.reserveToken_ = function (tokenId) {
    console.log('[Controller] Reserving token: ' + tokenId)
  }

  exports.releaseToken = function (req, res) {
    exports.releaseToken_(req.params.tokenId)
    res.json({})
  }

  exports.releaseToken_ = function (tokenId) {
    console.log('[Controller] Releasing token: ' + tokenId)

    let session = agentContext.tokenSessions[tokenId]
    if (session) {
      // Close the session and all objects it contains
      session[Symbol.dispose]();
      agentContext.tokenSessions[tokenId] = null;
    } else {
      console.log('[Controller] No session founds for token: ' + tokenId)
    }
  }

  exports.interruptExecution = function (req, res) {
    const tokenId = req.params.tokenId
    console.warn('[Controller] Interrupting token: ' + tokenId + ' : not implemented')
  }

  exports.process = function (req, res) {
    const tokenId = req.params.tokenId
    const keywordName = req.body.payload.function
    const argument = req.body.payload.payload
    const properties = req.body.payload.properties

    console.log('[Controller] Using token: ' + tokenId + ' to execute ' + keywordName)

    // add the agent properties
    let agentProperties = agentContext.properties
    Object.entries(agentProperties).forEach(function (element) {
      properties[element[0]] = element[1]
    })

    // add the properties of the tokenGroup
    let additionalProperties = agentContext.tokenProperties[tokenId]
    Object.entries(additionalProperties).forEach(function (element) {
      properties[element[0]] = element[1]
    })

    exports.process_(tokenId, keywordName, argument, properties, function (payload) {
      res.json(payload)
    })
  }

  exports.process_ = function (tokenId, keywordName, argument, properties, callback) {
    const outputBuilder = new OutputBuilder(function (output) {
      console.log(`[Controller] Keyword ${keywordName} successfully executed on token ${tokenId}`)
      callback(output)
    })

    try {
      const filepathPromise = exports.filemanager.loadOrGetKeywordFile(agentContext.controllerUrl + '/grid/file/', properties['$node.js.file.id'], properties['$node.js.file.version'], keywordName)

      filepathPromise.then(function (keywordPackageFile) {
        console.log('[Controller] Executing keyword ' + keywordName + ' using filepath ' + keywordPackageFile)
        exports.executeKeyword(keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder, agentContext)
      }, function (err) {
        console.log('[Controller] Error while attempting to run keyword ' + keywordName + ' :' + err)
        outputBuilder.fail('Error while attempting to run keyword', err)
      })
    } catch (e) {
      outputBuilder.fail(e)
    }
  }

  exports.executeKeyword = async function (keywordName, keywordPackageFile, tokenId, argument, properties, outputBuilder, agentContext) {
    try {
      let npmProjectPath;
      if (keywordPackageFile.toUpperCase().endsWith('ZIP')) {
        if (exports.filemanager.isFirstLevelKeywordFolder(keywordPackageFile)) {
          npmProjectPath = path.resolve(keywordPackageFile);
        } else {
          npmProjectPath = path.resolve(keywordPackageFile, exports.filemanager.getFolderName(keywordPackageFile))
        }
      } else {
        // Local execution with KeywordRunner
        npmProjectPath = path.resolve(keywordPackageFile)
      }

      console.log('[Controller] Executing keyword in project ' + npmProjectPath + ' for token ' + tokenId)

      let session = agentContext.tokenSessions[tokenId]
      if (!session) {
        session = new Session();
        agentContext.tokenSessions[tokenId] = session;
      }

      let forkedAgent = session.get('forkedAgent');

      let project = session.get('npmProjectPath');
      if(!project && forkedAgent) {
        throw new Error("Multiple projects not supported within the same session");
      }

      if(!forkedAgent) {
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

  function createForkedAgent(keywordProjectPath) {
    fs.copyFileSync(path.resolve(__dirname, '../../worker-wrapper.js'), path.join(keywordProjectPath, './worker-wrapper.js'));
    fs.copyFileSync(path.join(__dirname, 'output.js'), path.join(keywordProjectPath, './output.js'));
    fs.copyFileSync(path.join(__dirname, 'session.js'), path.join(keywordProjectPath, './session.js'));
    return fork('./worker-wrapper.js', [], {cwd: keywordProjectPath});
  }

  function runKeywordTask(forkedAgent, keywordProjectPath, functionName, input, properties) {
    return new Promise((resolve, reject) => {
      forkedAgent.send({ projectPath: keywordProjectPath, functionName: functionName, input: input, properties: properties });

      forkedAgent.removeAllListeners('message');
      forkedAgent.on('message', (result) => {
        resolve(result);
      });

      forkedAgent.removeAllListeners('error');
      forkedAgent.on('error', (err) => {
        console.error('Error while calling forked agent:', err);
      });
    });
  }

  return exports
}
