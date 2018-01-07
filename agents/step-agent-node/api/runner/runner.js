module.exports = function(properties) {
	var tokenId = "local";
	
	var agentContext = {tokens:[], tokenSessions:{}, properties:properties};
	agentContext.tokenSessions[tokenId] = {};
	
	var Controller = require('../controllers/controller');
	var controller = new Controller(agentContext);
	
	var api = {};
	
	api.run = function(keywordName, input) {
		return new Promise(resolve => {
			controller.process_(tokenId, keywordName, input, function(output) {resolve(output);});	
		});
	}
	
	return api;
}