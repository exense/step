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
const { createLiveReporting } = require("./live-reporting");
const Session = require("./session");
const fs = require("fs");
const path = require('path')
const session = new Session();
let pendingUnhandledRejection = null;
let pendingUncaughtException = null;

// stdout/stderr are pipes to the agent that forked this process (fork(..., { silent: true })).
// When the agent dies, the next write to them fails with an asynchronous 'error' (EPIPE). Without a
// listener it would become an uncaughtException, whose handler logs to the same dead pipe, raising
// another EPIPE: an endless loop that burns CPU and, for keywords that copy their output to a file,
// fills the disk (SED-5045). A failed stream is therefore remembered and never written to by this
// file again, and losing the IPC channel ends the process: nobody is left to receive the result.
const failedStreams = new Set();
for (const stream of [process.stdout, process.stderr]) {
  stream.on('error', () => failedStreams.add(stream));
}

function streamUsable(stream) {
  return !failedStreams.has(stream) && !stream.destroyed && stream.writable;
}

// Logs only through a stream that has not failed. Used by the process-level handlers, which must
// never write to a broken pipe: that write is what would trigger them again.
function safeLog(stream, ...args) {
  if (!streamUsable(stream)) {
    return;
  }
  try {
    (stream === process.stderr ? console.error : console.log)(...args);
  } catch {
    // Logging must never throw from a process-level handler
  }
}

// The session is released at most once, whether the agent asked for it (KILL) or went away
// (disconnect), or both: the agent can die while a KILL is being processed.
// The promise of that single release is kept, so that every caller awaits the same one.
let sessionReleasePromise = null;
function releaseSession() {
  if (!sessionReleasePromise) {
    sessionReleasePromise = session.asyncDispose();
  }
  return sessionReleasePromise;
}

// How long the fork may spend releasing its session once the agent is gone. The agent passes its own
// shutdown timeout (agentForker.shutdownTimeoutMs), the budget a normal close gets for the same work.
const DEFAULT_SHUTDOWN_TIMEOUT_MS = 40000;
function shutdownTimeoutMs() {
  // An empty or blank value falls back to the default too: Number('') would be 0
  const raw = process.env.STEP_AGENT_FORK_SHUTDOWN_TIMEOUT_MS;
  const configured = raw?.trim() ? Number(raw) : NaN;
  return Number.isFinite(configured) && configured >= 0 ? configured : DEFAULT_SHUTDOWN_TIMEOUT_MS;
}

// Nobody is left to receive the keyword's result: release the session, so the resources the keyword
// stored there (browsers, servers, child processes...) don't outlive it, then exit. The release is
// bounded: a close() that never settles must not keep the orphaned fork alive.
process.on('disconnect', async () => {
  const timeoutMs = shutdownTimeoutMs();
  safeLog(process.stderr, `[Agent fork] Disconnected from the agent. Releasing the session (timeout ${timeoutMs}ms) and exiting...`);
  setTimeout(() => {
    safeLog(process.stderr, `[Agent fork] The session could not be released within ${timeoutMs}ms. Exiting...`);
    process.exit(1);
  }, timeoutMs);
  try {
    await releaseSession();
  } catch (e) {
    safeLog(process.stderr, '[Agent fork] Error while releasing session:', e);
  }
  process.exit(1);
});

process.on('message', async ({ type, projectPath, functionName, input, properties, keywordDirectory }) => {
  if (type === 'KEYWORD') {
    // Snapshot any errors that fired between keywords, then clear for this execution.
    const prevUnhandledRejection = pendingUnhandledRejection;
    const prevUncaughtException = pendingUncaughtException;
    pendingUnhandledRejection = null;
    pendingUncaughtException = null;
    console.log("[Agent fork] Calling keyword " + functionName)
    const outputBuilder = new OutputBuilder();
    let liveReporting;
    try {
      // Initialize inside the try so any initialization error is reported via outputBuilder.fail
      // (in the catch below) rather than escaping the message handler.
      liveReporting = createLiveReporting(properties);
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
            await keyword(input, outputBuilder, session, properties, liveReporting);
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
      // Close live reporting: flushes any buffered measures and waits for in-flight uploads.
      // Guarded because initialization above may have failed before liveReporting was assigned.
      try {
        if (liveReporting) {
          await liveReporting.close();
        }
      } catch (e) {
        console.log("[Agent fork] Error while closing live reporting", e);
      }
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
    try {
      await releaseSession();
    } catch (e) {
      // A disposal failure must never prevent the process from exiting, otherwise the fork
      // would stay alive forever and the parent's close() would hang waiting for its exit.
      // The error is logged and reported to the parent via CLOSE_RESULT below.
      console.error('[Agent fork] Error while releasing session:', e);
      closeErrors.push({ message: 'An error occurred while releasing the session: ' + (e?.message || String(e)) });
    }
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
  pendingUnhandledRejection = error;
  safeLog(process.stdout, '[Agent fork] Unhandled promise rejection:', error)
})

process.on('uncaughtException', error => {
  pendingUncaughtException = error;
  safeLog(process.stdout, '[Agent fork] Uncaught exception:', error)
})

process.on('SIGTERM', () => {
  safeLog(process.stdout, "[Agent fork] Received SIGTERM. Exiting...")
  process.exit(1);
});
