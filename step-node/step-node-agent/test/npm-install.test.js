const fs = require('fs')
const os = require('os')
const path = require('path')
const Agent = require('../api/controllers/agent')
const { OutputBuilder } = require('../api/controllers/output')

// npm resolves the project to install from the closest package.json up the directory tree, so running
// `npm install` in a keyword project which has none installs an unrelated project the working directory
// happens to sit under, or fails with ENOENT when there is nothing above it at all.

describe('npm install', () => {
  const projects = []
  const agents = []

  function createProject(withPackageJson) {
    const projectPath = fs.mkdtempSync(path.join(os.tmpdir(), 'step-node-agent-project-'))
    projects.push(projectPath)
    fs.mkdirSync(path.join(projectPath, 'keywords'))
    fs.copyFileSync(path.join(__dirname, 'keywords', 'keywords.js'), path.join(projectPath, 'keywords', 'keywords.js'))
    if (withPackageJson) {
      fs.writeFileSync(path.join(projectPath, 'package.json'),
        JSON.stringify({ name: 'keyword-project', version: '1.0.0', private: true }))
    }
    return projectPath
  }

  // 'local' mode: the keyword project is used as it is, rather than being copied into a workspace per token
  function createAgent() {
    const agent = new Agent({ tokens: [], tokenSessions: {}, tokenProperties: {} }, null, 'local')
    agents.push(agent)
    return agent
  }

  async function executeEcho(agent, projectPath, tokenId) {
    const outputBuilder = new OutputBuilder()
    await agent.executeKeyword('Echo', projectPath, tokenId, { Param1: 'Val1' }, {}, outputBuilder, 30000)
    return outputBuilder.build()
  }

  afterEach(async () => {
    for (const agent of agents.splice(0)) {
      for (const session of Object.values(agent.agentContext.tokenSessions)) {
        const forkedAgent = session && session.get('forkedAgent')
        if (forkedAgent) {
          await forkedAgent.close()
        }
      }
    }
    projects.splice(0).forEach(projectPath => fs.rmSync(projectPath, { recursive: true, force: true }))
  })

  test('is skipped when the keyword project has no package.json', async () => {
    const agent = createAgent()
    const npmInstall = jest.spyOn(agent, 'executeNpmInstall')

    const output = await executeEcho(agent, createProject(false), 'token-without-package-json')

    expect(npmInstall).not.toHaveBeenCalled()
    expect(output.payload.error).toBeUndefined()
    expect(output.payload.payload.Param1).toBe('Val1')
  }, 60000)

  test('runs in a keyword project which has a package.json', async () => {
    const agent = createAgent()
    const npmInstall = jest.spyOn(agent, 'executeNpmInstall')
      .mockResolvedValue({ status: 0, error: null, processOutputAttachment: null })
    const projectPath = createProject(true)

    const output = await executeEcho(agent, projectPath, 'token-with-package-json')

    expect(npmInstall).toHaveBeenCalledWith(projectPath)
    expect(output.payload.error).toBeUndefined()
    expect(output.payload.payload.Param1).toBe('Val1')
  }, 60000)

  test('is skipped when it is turned off, even with a package.json', async () => {
    const agent = createAgent()
    const npmInstall = jest.spyOn(agent, 'executeNpmInstall')
    const outputBuilder = new OutputBuilder()

    await agent.executeKeyword('Echo', createProject(true), 'token-skipping-npm-install', { Param1: 'Val1' },
      { skipNpmInstall: 'true' }, outputBuilder, 30000)

    expect(npmInstall).not.toHaveBeenCalled()
    expect(outputBuilder.build().payload.payload.Param1).toBe('Val1')
  }, 60000)
})
