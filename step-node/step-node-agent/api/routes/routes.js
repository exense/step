'use strict'
module.exports = function (app, agentContext) {
  const Controller = require('../controllers/controller')
  const FileManager = require('../filemanager/filemanager')
  const controller = new Controller(agentContext, new FileManager(agentContext))

  app.route('/running').get(controller.isRunning.bind(controller))
  app.route('/token/:tokenId/reserve').get(controller.reserveToken.bind(controller))
  app.route('/token/:tokenId/release').get(controller.releaseToken.bind(controller))
  app.route('/token/:tokenId/process').post(controller.process.bind(controller))
  app.route('/token/:tokenId/interrupt-execution').post(controller.interruptExecution.bind(controller))
}
