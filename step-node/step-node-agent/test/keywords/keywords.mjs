// Minimal native ESM keyword module — verifies that .mjs files are loaded by the agent.
export async function EsmKW(input, output) { output.add('ok', true) }