module.exports = function Controller(agentContext) {

	var exports = {};
	
	exports.reserveToken = function(req, res) {
		exports.reserveToken_(req.params.tokenId);
		res.json({});
	}
	
	exports.reserveToken_ = function(tokenId) {
		console.log("Reserving token: "+tokenId);
	};

	exports.releaseToken = function(req, res) {
		exports.releaseToken_(req.params.tokenId);
		res.json({});
	};
	
	exports.releaseToken_ = function(tokenId) {
		console.log("Releasing token: "+tokenId);
	};

	exports.process = function(req, res) {
		var tokenId = req.params.tokenId;
		var keywordName = req.body.function;
		var argument = req.body.argument;
		exports.process_(tokenId, keywordName, argument, function(output) { 
			res.json(output);
		});
	}
	
	exports.process_ = function(tokenId, keywordName, argument, callback) {
		console.log("Executing " + keywordName + " on token : "+tokenId);
		
		var outputBuilder = {attachments:[]};
		var output = {
			send: function(payload) {
				outputBuilder.payload = payload;
				if(callback) {
					callback(outputBuilder);
				}
			},
			fail: function(e) {
				console.log(e);
				if(e instanceof Error) {
					outputBuilder.error = e.message;
				} else {
					outputBuilder.error = e;
				}
				if(callback) {
					callback(outputBuilder);
				}
			},
			attach: function(attachment) {
				outputBuilder.attachments.push(attachment);
			}
		};
		
		try {
			var keywordFunction;
			var keywordLibScripts = [];
			
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
				var session = agentContext.tokenSessions[tokenId];
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
