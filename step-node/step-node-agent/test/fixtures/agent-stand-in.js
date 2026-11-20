// Stands in for the step-node-agent: forks agent-fork.js the way api/controllers/agent.js does
// (silent: true, so the fork's stdout/stderr are pipes to this process), starts a keyword in it and
// prints the fork's pid. The test then kills this process to simulate the agent dying mid-keyword.
const { fork } = require('child_process')
const path = require('path')

const [agentRoot, functionName, inputJson] = process.argv.slice(2)
const child = fork(path.join(agentRoot, 'api', 'controllers', 'agent-fork.js'), [], { cwd: agentRoot, silent: true })
// Drain the fork's output, as the agent does, so it never blocks on a full pipe.
child.stdout.on('data', () => {})
child.stderr.on('data', () => {})
child.send({ type: 'KEYWORD', projectPath: agentRoot, functionName, input: JSON.parse(inputJson), properties: {}, keywordDirectory: 'test/keywords' })
process.stdout.write(JSON.stringify({ forkPid: child.pid }) + '\n')
