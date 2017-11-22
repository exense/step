module.exports = function Controller(agentContext) {

	var exports = {};
	exports.reserveToken = function(req, res) {
		console.log("Reseving token: "+req.params.tokenId);
		res.json({});
	};

	exports.releaseToken = function(req, res) {
		console.log("Releasing token: "+req.params.tokenId);
		res.json({});
	};

	exports.process = function(req, res) {
		var tokenId = req.params.tokenId;
		var keywordName = req.body.function;
		var argument = req.body.argument;
		console.log("Executing " + keywordName + " on token : "+tokenId);
		
		var outputPayload =  {};
		var outputBuilder = {attachments:[]};
		var output = {
			send: function(payload) {
				outputBuilder.payload = payload;
				res.json(outputBuilder);
			},
			fail: function(e) {
				if(e instanceof Error) {
					outputBuilder.error = e.message;
				} else {
					outputBuilder.error = e;
				}
				res.json(outputBuilder);
			},
			attach: function(attachment) {
				outputBuilder.attachments.push(attachment);
			}
		};
		
		try {
			var keywordLibScript = '../../keywords/keywords.js';
			var keywords = require(keywordLibScript);
			var keywordFunction = keywords[keywordName];
			if(keywordFunction) {
				var session = agentContext.tokenSessions[req.params.tokenId];
				keywordFunction(argument, output, session).catch(function(e){
					output.fail(e);
				});
			} else {
				output.fail("Unable to find keyword "+keywordName+" in "+keywordLibScript);
			}

		} catch(e) {
			output.fail(e);
		}
	};

	return  exports;
}
