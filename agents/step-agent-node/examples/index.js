var runner = require("step-agent-node").runner({"keywords":"keywords/keywords.js"});
const assert = require('assert');

(async () => {
	await runner.run("Open_Chrome",{});
	var output = await runner.run("Google_Search",{"search":"djigger"});
	assert.equal(output.payload.result,'OK');
	await runner.run("Google_Search",{"search":"djigger2"});
})();
