const js = require('@eslint/js')
const pluginN = require('eslint-plugin-n')
const pluginPromise = require('eslint-plugin-promise')
const pluginImport = require('eslint-plugin-import-x')
const globals = require('globals')

module.exports = [
  {
    ignores: [
      'node_modules/**',
      'filemanager/**',
      'npm-project-workspaces/**',
    ],
  },
  js.configs.recommended,
  pluginN.configs['flat/recommended'],
  pluginPromise.configs['flat/recommended'],
  {
    plugins: {
      import: pluginImport,
    },
    languageOptions: {
      globals: globals.node,
      ecmaVersion: 2022,
      sourceType: 'commonjs',
    },
    rules: {
      'import/no-unresolved': 'error',
      'import/no-extraneous-dependencies': 'warn',
    },
  },
  // process.exit() is legitimate in agent/fork entry-points.
  {
    rules: {
      'n/no-process-exit': 'off',
    },
  },
  // Test files: provide Jest globals and relax rules that don't apply.
  {
    files: ['test/**/*.test.js', 'test/**/*.spec.js'],
    languageOptions: {
      globals: globals.jest,
    },
  },
  // Keyword stubs intentionally declare the full API signature without using every param.
  {
    files: ['test/keywords/**/*.js'],
    rules: {
      'no-unused-vars': ['error', { vars: 'all', args: 'none' }],
    },
  },
  // The config file itself uses devDependencies — allow unpublished requires here.
  {
    files: ['eslint.config.js'],
    rules: {
      'n/no-unpublished-require': 'off',
    },
  },
]