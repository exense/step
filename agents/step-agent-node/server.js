var agentConfFile = "conf/agentConf.json";
console.log("Reading agent configuration "+agentConfFile);
var fs = require("fs");
var content = fs.readFileSync("conf/agentConf.json");
var agentConf = JSON.parse(content);


console.log("Creating agent context and tokens");
const uuidv1 = require('uuid/v1');
const _ = require("underscore");
var agent = {id:uuidv1()}
var agentContext = {tokens:[], tokenSessions:[]};
_.each(agentConf.tokenGroups, function(tokenGroup) {
	var tokenConf = tokenGroup.tokenConf;
	var attributes = tokenConf.attributes;
	attributes['$agenttype'] = 'default';
	for(i=0;i<tokenGroup.capacity;i++) {
		var token = {id:uuidv1(),agentid:agent.id,attributes:attributes, selectionPatterns:{}};
		agentContext.tokens.push(token);
		agentContext.tokenSessions[token.id] = {};
	}
})


console.log("Starting agent services");
var express = require('express'),
  app = express(),
  port = agentConf.agentPort || 3000,
  bodyParser = require('body-parser');

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

var routes = require('./api/routes/routes');
routes(app, agentContext);

app.listen(port);


console.log("Creating registration timer");
const request = require('request');
var os = require("os");
setInterval(function () {
    request({uri:agentConf.gridHost+'/grid/register', 
			 method: 'POST', 
			 json: true, 
			 body: {"agentRef":{"agentId":"test", "agentUrl":"http://"+os.hostname()+":"+port}, "tokens":agentContext.tokens} 
			}, function(err, res, body) {
				if(err) {
					console.log(err);
				} 
			});
}, 2000);

console.log('Agent successfully started on: ' + port);