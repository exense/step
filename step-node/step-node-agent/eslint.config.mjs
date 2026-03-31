import js from "@eslint/js";
import globals from "globals";
import { defineConfig } from "eslint/config";

export default defineConfig([
  {
    ignores: [
      "node_modules/**",
      "filemanager/work/**",
      "npm-project-workspaces/**",
    ],
  },
  {
    files: ["**/*.{js,mjs,cjs}"],
    plugins: { js },
    extends: ["js/recommended"],
    languageOptions: { globals: globals.node },
    rules: {
      // Allow intentionally-unused params/vars when prefixed with _
      "no-unused-vars": ["error", { vars: "all", args: "after-used", argsIgnorePattern: "^_", ignoreRestSiblings: true }],
    },
  },
  { files: ["**/*.js"], languageOptions: { sourceType: "commonjs" } },
  // Test files: provide Jest globals
  {
    files: ["test/**/*.test.js", "test/**/*.spec.js"],
    languageOptions: { globals: globals.jest },
  },
  // Keyword stubs intentionally declare the full API signature without using every param
  {
    files: ["test/keywords/**/*.js"],
    rules: { "no-unused-vars": ["error", { vars: "all", args: "none" }] },
  },
]);