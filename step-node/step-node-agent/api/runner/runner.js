const Session = require('../controllers/session');
module.exports = function (properties = {}) {
  const tokenId = 'local'

  const agentContext = {tokens: [], tokenSessions: {}, properties: properties}
  const tokenSession = new Session();
  agentContext.tokenSessions[tokenId] = tokenSession

  const fileManager = {
    loadOrGetKeywordFile: () => Promise.resolve('.')
  }

  const Controller = require('../controllers/controller')
  const controller = new Controller(agentContext, fileManager)

  const api = {}

  api.run = async function (keywordName, input) {
    const output = await controller.process_(tokenId, keywordName, input, properties)
    return output.payload
  }

  api.close = function () {
    tokenSession[Symbol.dispose]();
  }

  return api
}
