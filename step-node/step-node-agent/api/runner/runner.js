const Session = require('../controllers/session');
const logger = require('../logger').child({ component: 'Runner' })
module.exports = function (properties = {}) {
  const tokenId = 'local';
  let throwExceptionOnError = true;

  const agentContext = {tokens: [], tokenSessions: {}, properties: properties}
  const tokenSession = new Session();
  agentContext.tokenSessions[tokenId] = tokenSession

  const fileManager = {
    loadOrGetKeywordFile: () => Promise.resolve('.')
  }

  const Controller = require('../controllers/agent')
  const controller = new Controller(agentContext, fileManager, 'runner')

  const api = {}

  api.setThrowExceptionOnError = function(isThrowExceptionOnError) {
    throwExceptionOnError = isThrowExceptionOnError;
  }

  api.run = async function (keywordName, input) {
    const output = await controller.process_(tokenId, keywordName, input, properties)
    const payload = output.payload;
    if (payload.error) {
      if(throwExceptionOnError) {
        throw new Error('The keyword execution returned an error: ' + JSON.stringify(payload.error))
      } else {
        logger.warn('The keyword execution returned an error: ' + JSON.stringify(payload.error))
      }
    }
    return output.payload
  }

  api.close = async function () {
    return await tokenSession.asyncDispose();
  }

  return api
}
