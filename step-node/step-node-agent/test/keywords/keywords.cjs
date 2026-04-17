// Minimal .cjs keyword module — verifies that .cjs files are loaded by the agent.
exports.CjsKW = async (input, output) => { output.add('ok', true) }