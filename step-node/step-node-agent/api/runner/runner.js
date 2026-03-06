const Session = require('../controllers/session');
module.exports = function (properties = {}) {
  const tokenId = 'local'

  const agentContext = {tokens: [], tokenSessions: {}, properties: properties}
  let tokenSession = new Session();
  agentContext.tokenSessions[tokenId] = tokenSession

  var fileManager = {
    loadOrGetKeywordFile: function (url, fileId, fileVersion, keywordName) {
      return new Promise(function (resolve, reject) {
        resolve('.')
      })
    }
  }

  const Controller = require('../controllers/controller')
  const controller = new Controller(agentContext, fileManager)

  const api = {}

  api.run = function (keywordName, input) {
    return new Promise(resolve => {
      controller.process_(tokenId, keywordName, input, properties, function (output) { resolve(output.payload) })
    })
  }

  api.close = function () {
    tokenSession[Symbol.dispose]();
  }

  return api
}
