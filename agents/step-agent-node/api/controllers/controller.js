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
				console.log(e);
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
			var keywordFunction;
			var keywordLibScripts = ['../../keywords/keywords.js'];
			
			var keywords = agentContext.properties['keywords'];
			if(keywords) {
				var keywordsSplit = keywords.split(';');
				for(i=0;i<keywordsSplit.length;i++) {
					keywordLibScripts.push(process.cwd()+"/"+keywordsSplit[i]);
				}
			}
			
			for(i=0;i<keywordLibScripts.length;i++) {
				var keywordLibScript = keywordLibScripts[i];
				console.log("Searching keyword "+keywordName+" in "+keywordLibScript);
				
				var keywords = require(keywordLibScript);
				
				keywordFunction = keywords[keywordName];
				if(keywordFunction) {
					break;
				}
			}

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
