const fs = require('fs')
const os = require('os')
const path = require('path')
const Agent = require('../api/controllers/agent')

// The workspace folder is named with a short hash rather than `${fileId}_${fileVersionId}_${tokenId}`: on
// Windows, executables whose path exceeds MAX_PATH (260 chars) cannot be spawned, which the native binaries
// installed in the workspace would otherwise easily do.

describe('npm project workspace', () => {
  const fileId = '6a7c3dd6-6b6e-43e7-8265-c1983cbe038e'
  const fileVersionId = '1791544465364'
  const tokenId = 'ff1906f7-ee7d-4f63-85c8-83fb6224c5e7'
  let workingDir
  let projectPath
  let agent

  beforeEach(() => {
    workingDir = fs.mkdtempSync(path.join(os.tmpdir(), 'step-node-agent-work-'))
    projectPath = fs.mkdtempSync(path.join(os.tmpdir(), 'step-node-agent-project-'))
    fs.writeFileSync(path.join(projectPath, 'package.json'), '{}')
    agent = new Agent({ workingDir, tokens: [], tokenSessions: {}, tokenProperties: {} }, null, 'local')
  })

  afterEach(() => {
    fs.rmSync(workingDir, { recursive: true, force: true })
    fs.rmSync(projectPath, { recursive: true, force: true })
  })

  test('is created in a folder with a short name', async () => {
    const workspacePath = await agent.getOrCreateNpmProjectWorkspace(tokenId, { fileId, fileVersionId, file: projectPath })

    expect(path.dirname(workspacePath)).toBe(path.join(workingDir, 'npm-project-workspaces'))
    expect(path.basename(workspacePath)).toMatch(/^[0-9a-f]{16}$/)
    expect(fs.existsSync(path.join(workspacePath, 'package.json'))).toBe(true)
  })

  test('is reused for the same package version and token, and distinct otherwise', async () => {
    const workspacePath = await agent.getOrCreateNpmProjectWorkspace(tokenId, { fileId, fileVersionId, file: projectPath })

    expect(await agent.getOrCreateNpmProjectWorkspace(tokenId, { fileId, fileVersionId, file: projectPath })).toBe(workspacePath)
    expect(await agent.getOrCreateNpmProjectWorkspace('other-token', { fileId, fileVersionId, file: projectPath })).not.toBe(workspacePath)
    expect(await agent.getOrCreateNpmProjectWorkspace(tokenId, { fileId, fileVersionId: 'other-version', file: projectPath })).not.toBe(workspacePath)
  })
})
