// Keyword of the automation package used by NodeLocalExecutionTest. It only echoes its input: what the test is
// about is that the keyword ran at all, i.e. that it was routed to a real Node.js agent rather than to a local
// token in the CLI's own JVM, which has no handler for Node.js keywords.
exports.NodeEcho = async (input, output) => {
    output.add('echoed', input['message'])
}
