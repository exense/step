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

process.on('message', async ({ type, projectPath, functionName, input, properties, keywordDirectory }) => {
  if (type === 'KEYWORD') {
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
    } finally {
      console.log("[Agent fork] Returning output")
      process.send(outputBuilder.build());
    }
  } else if (type === 'KILL') {
    console.log("[Agent fork] Releasing session...")
    await session.asyncDispose();
    console.log("[Agent fork] Exiting...")
    process.exit(0)
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
      if (kwFile.endsWith('.js')) {
        let kwModule = "file://" + path.resolve(kwDir, kwFile);
        console.log("[Agent fork] Importing keywords from module: " + kwModule)
        let module = await import(kwModule);
        kwModules.push(module);
      }
    }
    return kwModules;
  }

  function searchKeyword(kwModules, keywordName) {
    const kwModule = kwModules.find(m => m[keywordName]);
    return kwModule ? {keywordFunction: kwModule[keywordName], keywordModule: kwModule} : undefined;
  }
});

process.on('unhandledRejection', error => {
  console.log('[Agent fork] Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported', error)
})

process.on('uncaughtException', error => {
  console.log('[Agent fork] Critical: an unhandled error (uncaught exception) occurred and might not have been reported', error)
})

process.on('SIGTERM', () => {
  console.log("[Agent fork] Received SIGTERM. Exiting...")
  process.exit(1);
});
