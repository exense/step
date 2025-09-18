const minimist = require('minimist')
const path = require('path')
const YAML = require('yaml')

let args = minimist(process.argv.slice(2), {
  default: {
    f: path.join(__dirname, 'AgentConf.yaml')
  }
})
console.log('[Agent] Using arguments ' + JSON.stringify(args))

const agentConfFile = args.f
console.log('[Agent] Reading agent configuration ' + agentConfFile)
const fs = require('fs')
const content = fs.readFileSync(agentConfFile, 'utf8')
const agentConfFileExt = path.extname(agentConfFile)
var agentConf
if(agentConfFileExt === '.yaml') {
  agentConf = YAML.parse(content)
} else if(agentConfFileExt === '.json') {
  agentConf = JSON.parse(content)
} else {
  throw new Error('Unsupported extension ' + agentConfFileExt + " for agent configuration " + content);
}

console.log('[Agent] Creating agent context and tokens')
const uuid = require('uuid/v4')
const _ = require('underscore')
const jwtUtils = require('./utils/jwtUtils')
const agentType = 'node'
const agent = {id: uuid()}
let agentContext = { tokens: [], tokenSessions: [], tokenProperties: [], properties: agentConf.properties, controllerUrl: agentConf.gridHost, gridSecurity: agentConf.gridSecurity }
_.each(agentConf.tokenGroups, function (tokenGroup) {
  const tokenConf = tokenGroup.tokenConf
  let attributes = tokenConf.attributes
  // Transform the selectionPatterns map <String, String> to <String, Interest>
  let selectionPatterns = tokenConf.selectionPatterns;
  const tokenSelectionPatterns = {};
  if(selectionPatterns) {
    Object.keys(selectionPatterns).forEach((key) => {
      tokenSelectionPatterns[key] = { must: true, selectionPattern: selectionPatterns[key] };
    });
  }
  let additionalProperties = tokenConf.properties
  attributes['$agenttype'] = agentType
  for (let i = 0; i < tokenGroup.capacity; i++) {
    const token = { id: uuid(), agentid: agent.id, attributes: attributes, selectionPatterns: tokenSelectionPatterns }
    agentContext.tokens.push(token)
    agentContext.tokenSessions[token.id] = {}
    agentContext.tokenProperties[token.id] = additionalProperties
  }
})

console.log('[Agent] Starting agent services')
const express = require('express')
const app = express()
const port = agentConf.agentPort || 3000
const timeout = agentConf.agentServerTimeout || 600000
const bodyParser = require('body-parser')

app.use(bodyParser.urlencoded({extended: true}))
app.use(bodyParser.json())

// Apply JWT authentication middleware
const createJwtAuthMiddleware = require('./middleware/jwtAuth')
const jwtAuthMiddleware = createJwtAuthMiddleware(agentConf.gridSecurity)
app.use(jwtAuthMiddleware)

const routes = require('./api/routes/routes')
routes(app, agentContext)

var server = app.listen(port)
server.setTimeout(timeout)

startWithAgentUrl = function(agentUrl) {
  console.log('[Agent] Registering agent as ' + agentUrl + ' to grid ' + agentConf.gridHost)
  console.log('[Agent] Creating registration timer')
  const registrationPeriod = agentConf.registrationPeriod || 5000
  const request = require('request')
  setInterval(function () {
    const requestOptions = {
      uri: agentConf.gridHost + '/grid/register',
      method: 'POST',
      json: true,
      body: { agentRef: { agentId: agent.id, agentUrl: agentUrl, agentType: agentType }, tokens: agentContext.tokens }
    };

    // Add bearer token if gridSecurity is configured
    const token = jwtUtils.generateJwtToken(agentConf.gridSecurity, 300); // 5 minutes expiration
    if (token) {
      requestOptions.headers = {
        'Authorization': 'Bearer ' + token
      };
    }

    request(requestOptions, function (err, res, body) {
      if (err) {
        console.log("[Agent] Error while registering agent to grid")
        console.log(err)
      } else if (res.statusCode !== 204) {
        console.log("[Agent] Failed to register agent: grid responded with status " + res.statusCode + (body != null ? ". Response body: " + JSON.stringify(body) : ""))
      }
    })
  }, registrationPeriod)

  console.log('[Agent] Successfully started on: ' + port)
}

if(agentConf.agentUrl) {
  startWithAgentUrl(agentConf.agentUrl)
} else {
  const getFQDN = require('get-fqdn');
  getFQDN().then(FQDN => {
    startWithAgentUrl('http://' + FQDN + ':' + port)
  }).catch(e => {
    console.log('[Agent] Error while getting FQDN ' + e)
  })
}
