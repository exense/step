// CJS module that triggers the .default fallback in searchKeyword.
// Node's import() static analysis (cjs-module-lexer) cannot resolve named exports
// when they are assigned to an intermediate variable before being set on module.exports,
// so the entire exports object lands on .default instead of being hoisted as named exports.
var m = {};
m.DefaultInteropKW = async (input, output) => { output.add('ok', true) }
module.exports = m;