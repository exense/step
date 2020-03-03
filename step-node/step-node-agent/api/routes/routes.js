'use strict'
module.exports = function (app, agentContext) {
  const Controller = require('../controllers/controller')
  const FileManager = require('../filemanager/filemanager')
  const controller = new Controller(agentContext, new FileManager(agentContext))

  app.route('/token/:tokenId/reserve').get(controller.reserveToken)
  app.route('/token/:tokenId/release').get(controller.releaseToken)
  app.route('/token/:tokenId/process').post(controller.process)
}
