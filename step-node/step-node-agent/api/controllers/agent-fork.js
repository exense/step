/*
 * Copyright (C) 2026, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

const { OutputBuilder } = require("./output");
const Session = require("./session");
const fs = require("fs");
const path = require('path')
const session = new Session();
let pendingUnhandledRejection = null;
let pendingUncaughtException = null;

process.on('message', async ({ type, projectPath, functionName, input, properties, keywordDirectory }) => {
  if (type === 'KEYWORD') {
    // Snapshot any errors that fired between keywords, then clear for this execution.
    const prevUnhandledRejection = pendingUnhandledRejection;
    const prevUncaughtException = pendingUncaughtException;
    pendingUnhandledRejection = null;
    pendingUncaughtException = null;
    console.log("[Agent fork] Calling keyword " + functionName)
    const outputBuilder = new OutputBuilder();
    try {
      if (!keywordDirectoryExists(projectPath, keywordDirectory)) {
        outputBuilder.fail("The keyword directory '" + keywordDirectory + "' doesn't exist in " + path.basename(projectPath) + ". Possible cause: If using TypeScript, the keywords may not have been compiled. Fix: Ensure your project is built before deploying to Step or during 'npm install'.")
      } else {
        const kwModules = await importAllKeywords(projectPath, keywordDirectory);
        let keywordSearchResult = searchKeyword(kwModules, functionName);
        if (!keywordSearchResult) {
          console.log('[Agent fork] Unable to find Keyword ' + functionName + "'");
          outputBuilder.fail("Unable to find Keyword '" + functionName + "'");
        } else {
          const module = keywordSearchResult.keywordModule;
          const keyword = keywordSearchResult.keywordFunction;

          try {
            const beforeKeyword = module['beforeKeyword'];
            if(beforeKeyword) {
              await beforeKeyword(functionName);
            }
            await keyword(input, outputBuilder, session, properties);
          } catch (e) {
            console.log("[Agent fork] Keyword execution failed with following error", e)
            const onError = module['onError'];
            if (onError) {
              if (await onError(e, input, outputBuilder, session, properties)) {
                console.log('[Agent fork] onError hook returned \'true\'. Propagating error.')
                outputBuilder.fail(e)
              } else {
                console.log('[Agent fork] onError hook returned \'false\'. Suppressing error.')
              }
            } else {
              console.log('[Agent fork] No onError hook defined.')
              outputBuilder.fail(e)
            }
          } finally {
            let afterKeyword = module['afterKeyword'];
            if (afterKeyword) {
              await afterKeyword(functionName);
            }
          }
        }
      }
    } catch (e) {
      console.log("[Agent fork] Unexpected error occurred while executing keyword", e)
      outputBuilder.fail("An unexpected error occurred while executing keyword: " + (e?.message || String(e)), e)
    } finally {
      // Flush the event loop so unhandledRejection / uncaughtException from the keyword
      // (e.g. fire-and-forget promises, nextTick throws) land before we send the result.
      await new Promise(resolve => setImmediate(resolve));
      // Surface inter-keyword errors first, labelled clearly as coming from a previous keyword.
      if (prevUnhandledRejection) {
        const sep = outputBuilder.hasError() ? '\n' : '';
        outputBuilder.appendError(`${sep}Unhandled promise rejection from a previous keyword: ${prevUnhandledRejection.message || prevUnhandledRejection}`);
      }
      if (prevUncaughtException) {
        const sep = outputBuilder.hasError() ? '\n' : '';
        outputBuilder.appendError(`${sep}Uncaught exception from a previous keyword: ${prevUncaughtException.message || prevUncaughtException}`);
      }
      // Then surface errors from the current keyword execution.
      if (pendingUnhandledRejection) {
        const sep = outputBuilder.hasError() ? '\n' : '';
        outputBuilder.appendError(`${sep}Unhandled promise rejection: ${pendingUnhandledRejection.message || pendingUnhandledRejection}`);
        pendingUnhandledRejection = null;
      }
      if (pendingUncaughtException) {
        const sep = outputBuilder.hasError() ? '\n' : '';
        outputBuilder.appendError(`${sep}Uncaught exception: ${pendingUncaughtException.message || pendingUncaughtException}`);
        pendingUncaughtException = null;
      }
      console.log("[Agent fork] Returning output")
      process.send(outputBuilder.build());
    }
  } else if (type === 'KILL') {
    const closeErrors = [];
    if (pendingUnhandledRejection) {
      console.error('[Agent fork] Unhandled promise rejection occurred after last keyword:', pendingUnhandledRejection);
      closeErrors.push({ message: 'An unhandled promise rejection occurred after the last keyword call: ' + (pendingUnhandledRejection.message || String(pendingUnhandledRejection)) });
    }
    if (pendingUncaughtException) {
      console.error('[Agent fork] Uncaught exception occurred after last keyword:', pendingUncaughtException);
      closeErrors.push({ message: 'An uncaught exception occurred after the last keyword call: ' + (pendingUncaughtException.message || String(pendingUncaughtException)) });
    }
    console.log("[Agent fork] Releasing session...")
    await session.asyncDispose();
    console.log("[Agent fork] Exiting...")
    if (closeErrors.length > 0) {
      // Use the send callback to ensure the message is flushed before we exit.
      // The timeout is a safety net: if the IPC channel closes before the callback
      // fires (e.g. the parent process dies), the fork exits instead of hanging.
      const exitNow = () => process.exit(0);
      const fallback = setTimeout(exitNow, 5000);
      try {
        process.send({ type: 'CLOSE_RESULT', errors: closeErrors }, () => {
          clearTimeout(fallback);
          exitNow();
        });
      } catch {
        clearTimeout(fallback);
        exitNow();
      }
    } else {
      process.exit(0);
    }
  }

  function keywordDirectoryExists(projectPath, keywordDirectory) {
    return fs.existsSync(path.resolve(projectPath, keywordDirectory))
  }

  async function importAllKeywords(projectPath, keywordDirectory) {
    const kwModules = [];
    const kwDir = path.resolve(projectPath, keywordDirectory);
    console.log("[Agent fork] Searching keywords in: " + kwDir)
    const kwFiles = fs.readdirSync(kwDir);
    for (const kwFile of kwFiles) {
      if (kwFile.endsWith('.js') || kwFile.endsWith('.mjs') || kwFile.endsWith('.cjs')) {
        let kwModule = "file://" + path.resolve(kwDir, kwFile);
        console.log("[Agent fork] Importing keywords from module: " + kwModule)
        try {
          let module = await import(kwModule);
          kwModules.push(module);
        } catch (e) {
          throw new Error("Error while importing keyword module " + kwFile + ": " + (e?.message || String(e)), { cause: e })
        }
      }
    }
    return kwModules;
  }

  function searchKeyword(kwModules, keywordName) {
    for (const m of kwModules) {
      if (typeof m[keywordName] === 'function') return { keywordFunction: m[keywordName], keywordModule: m };
      if (typeof m.default?.[keywordName] === 'function') return { keywordFunction: m.default[keywordName], keywordModule: m.default };
    }
    return undefined;
  }
});

process.on('unhandledRejection', error => {
  console.log('[Agent fork] Unhandled promise rejection:', error)
  pendingUnhandledRejection = error;
})

process.on('uncaughtException', error => {
  console.log('[Agent fork] Uncaught exception:', error)
  pendingUncaughtException = error;
})

process.on('SIGTERM', () => {
  console.log("[Agent fork] Received SIGTERM. Exiting...")
  process.exit(1);
});
