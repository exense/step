module.exports = function Controller (agentContext, fileManager) {
  process.on('unhandledRejection', error => {
    console.log('[Controller] Critical: an unhandled error (unhandled promise rejection) occured and might not have been reported', error)
  })

  process.on('uncaughtException', error => {
    console.log('[Controller] Critical: an unhandled error (uncaught exception) occured and might not have been reported', error)
  })

  let exports = {}

  const fs = require('fs')
  const path = require('path')
  const OutputBuilder = require('./output')

  exports.filemanager = fileManager

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
      // call close() for each closeable object in the session:
      Object.entries(session).forEach(function (element) {
        if (typeof element[1]['close'] === 'function') {
          console.log('[Controller] Closing closeable object \'' + element[0] + '\' for token: ' + tokenId)
          element[1].close()
        }
      })
      agentContext.tokenSessions[tokenId] = {}
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
      var kwDir

      if (keywordPackageFile.toUpperCase().endsWith('ZIP')) {
        if (exports.filemanager.isFirstLevelKeywordFolder(keywordPackageFile)) {
          kwDir = path.resolve(keywordPackageFile + '/keywords')
        } else {
          kwDir = path.resolve(keywordPackageFile + '/' + exports.filemanager.getFolderName(keywordPackageFile) + '/keywords')
        }
      } else {
        // Local execution with KeywordRunner
        kwDir = path.resolve(keywordPackageFile + '/keywords')
      }

      console.log('[Controller] Search keyword file in ' + kwDir + ' for token ' + tokenId)

      var keywordFunction = searchAndRequireKeyword(kwDir, keywordName)

      if (keywordFunction) {
        console.log('[Controller] Found keyword for token ' + tokenId)
        let session = agentContext.tokenSessions[tokenId]

        if (!session) session = {}

        console.log('[Controller] Executing keyword ' + keywordName + ' on token ' + tokenId)

        try {
          await keywordFunction(argument, outputBuilder, session, properties)
        } catch (e) {
          var onError = searchAndRequireKeyword(kwDir, 'onError')
          if (onError) {
            if (await onError(e, argument, outputBuilder, session, properties)) {
              console.log('[Controller] Keyword execution marked as failed: onError function returned \'true\' on token ' + tokenId)
              outputBuilder.fail(e)
            } else {
              console.log('[Controller] Keyword execution marked as successful: execution failed but the onError function returned \'false\' on token ' + tokenId)
              outputBuilder.send()
            }
          } else {
            console.log('[Controller] Keyword execution marked as failed: Keyword execution failed and no onError function found on token ' + tokenId)
            outputBuilder.fail(e)
          }
        }
      } else {
        outputBuilder.fail('Unable to find keyword ' + keywordName)
      }
    } catch (e) {
      outputBuilder.fail('An error occured while attempting to execute the keyword ' + keywordName, e)
    }
  }

  function searchAndRequireKeyword (kwDir, keywordName) {
    var keywordFunction
    var kwFiles = fs.readdirSync(kwDir)
    kwFiles.every(function (kwFile) {
      if (kwFile.endsWith('.js')) {
        const kwMod = require(kwDir + '/' + kwFile)
        if (kwMod[keywordName]) {
          keywordFunction = kwMod[keywordName]
          return false
        }
      }
      return true
    })
    return keywordFunction
  }

  return exports
}
