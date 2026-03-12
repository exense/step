const minimist = require('minimist')
const path = require('path')
const YAML = require('yaml')
const logger = require('./api/logger').child({ component: 'Agent' })

let args = minimist(process.argv.slice(2), {
  default: {
    f: path.join(__dirname, 'AgentConf.yaml')
  }
})
logger.info('Using arguments ' + JSON.stringify(args))

const agentConfFile = args.f
logger.info('Reading agent configuration ' + agentConfFile)
const fs = require('fs')
const content = fs.readFileSync(agentConfFile, 'utf8')
const agentConfFileExt = path.extname(agentConfFile)
const agentConf = parseAgentConf();

function parseAgentConf() {
  if (agentConfFileExt === '.yaml') {
    return YAML.parse(content)
  } else if (agentConfFileExt === '.json') {
    return JSON.parse(content)
  } else {
    throw new Error('Unsupported extension ' + agentConfFileExt + " for agent configuration " + content);
  }
}

logger.info('Creating agent context and tokens')
const uuid = require('uuid/v4')
const jwtUtils = require('./utils/jwtUtils')
const agentType = 'node'
const agent = {id: uuid()}
const agentContext = { tokens: [], tokenSessions: [], tokenProperties: [], properties: agentConf.properties, controllerUrl: agentConf.gridHost, gridSecurity: agentConf.gridSecurity }
agentConf.tokenGroups.forEach(function (tokenGroup) {
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
    agentContext.tokenSessions[token.id] = null
    agentContext.tokenProperties[token.id] = additionalProperties
  }
})

logger.info('Starting agent services')
const express = require('express')
const app = express()
const port = agentConf.agentPort || 3000
const timeout = agentConf.agentServerTimeout || 600000
app.use(express.urlencoded({extended: true}))
app.use(express.json())

// Apply JWT authentication middleware
const createJwtAuthMiddleware = require('./middleware/jwtAuth')
const jwtAuthMiddleware = createJwtAuthMiddleware(agentConf.gridSecurity)
app.use(jwtAuthMiddleware)

const routes = require('./api/routes/routes')
routes(app, agentContext)

const server = app.listen(port)
server.setTimeout(timeout)

const startWithAgentUrl = async function(agentUrl) {
  logger.info('Registering agent as ' + agentUrl + ' to grid ' + agentConf.gridHost)
  logger.info('Creating registration timer')
  const registrationPeriod = agentConf.registrationPeriod || 5000
  setInterval(async () => {
    const body = { agentRef: { agentId: agent.id, agentUrl: agentUrl, agentType: agentType }, tokens: agentContext.tokens }

    const headers = { 'Content-Type': 'application/json' }
    // Add bearer token if gridSecurity is configured
    const token = jwtUtils.generateJwtToken(agentConf.gridSecurity, 3600); // 1 hour expiration
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    try {
      const res = await fetch(agentConf.gridHost + '/grid/register', {
        method: 'POST',
        headers,
        body: JSON.stringify(body)
      })
      if (res.status !== 204) {
        const responseBody = await res.text().catch(() => null)
        logger.warn('Failed to register agent: grid responded with status ' + res.status + (responseBody != null ? '. Response body: ' + responseBody : ''))
      }
    } catch (err) {
      logger.error('Error while registering agent to grid: ' + err)
    }
  }, registrationPeriod)

  logger.info('Successfully started on port ' + port)
}

if(agentConf.agentUrl) {
  startWithAgentUrl(agentConf.agentUrl)
} else {
  const getFQDN = require('get-fqdn');
  getFQDN().then(FQDN => {
    startWithAgentUrl('http://' + FQDN + ':' + port)
  }).catch(e => {
    logger.error('Error while getting FQDN: ' + e)
  })
}

const v8 = require('v8');

process.on('SIGUSR2', () => {
  const fileName = `/tmp/heap-${process.pid}-${Date.now()}.heapsnapshot`;
  v8.writeHeapSnapshot(fileName);
  logger.info('Heap dump written to ' + fileName)
});
