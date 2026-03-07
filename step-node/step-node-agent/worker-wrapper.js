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

process.on('message', async ({ projectPath, functionName, input, properties }) => {
  console.log("[Agent fork] Calling keyword " + functionName)
  const kwModules = await importAllKeywords(projectPath);

  const outputBuilder = new OutputBuilder();

  try {
    let keyword = searchKeyword(kwModules, functionName);
    if (!keyword) {
      outputBuilder.fail("Unable to find Keyword '" + functionName + "'");
    } else {
      try {
        await keyword(input, outputBuilder, session, properties);
      } catch (e) {
        let onError = searchKeyword(kwModules, 'onError');
        if (onError) {
          if (await onError(e, input, outputBuilder, session, properties)) {
            console.log('[Agent fork] Keyword execution failed and onError hook returned \'true\'')
            outputBuilder.fail(e)
          } else {
            console.log('[Agent fork] Keyword execution failed and onError hook returned \'false\'')
          }
        } else {
          console.log('[Agent fork] Keyword execution failed. No onError hook defined')
          outputBuilder.fail(e)
        }
      }
    }
  } finally {
    console.log("[Agent fork] Returning output")
    process.send(outputBuilder.build());
  }

  async function importAllKeywords(projectPath) {
    const kwModules = [];
    const kwDir = path.resolve(projectPath, "./keywords");
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
    return kwModule ? kwModule[keywordName] : undefined;
  }
});

process.on('exit', () => {
  session[Symbol.dispose]();
})

process.on('unhandledRejection', error => {
  console.log('[Agent fork] Critical: an unhandled error (unhandled promise rejection) occurred and might not have been reported', error)
})

process.on('uncaughtException', error => {
  console.log('[Agent fork] Critical: an unhandled error (uncaught exception) occurred and might not have been reported', error)
})
