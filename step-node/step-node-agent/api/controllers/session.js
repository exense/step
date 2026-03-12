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
let logger;
try {
  logger = require('../logger').child({ component: 'Session' });
} catch {
  logger = { info: console.log.bind(console), warn: console.warn.bind(console), error: console.error.bind(console) };
}

class Session extends Map {
  [Symbol.dispose]() {
    logger.info(`Disposing Session: Cleaning up ${this.size} resources...`);

    for (const [key, resource] of this) {
      try {
        // 1. Try modern [Symbol.dispose]
        if (resource && typeof resource[Symbol.dispose] === 'function') {
          resource[Symbol.dispose]();
        }
        // 2. Fallback to Node.js child_process .kill()
        else if (resource && typeof resource.kill === 'function') {
          resource.kill();
        }
        // 3. Fallback to generic .close()
        else if (resource && typeof resource.close === 'function') {
          resource.close();
        }

        logger.info(`Successfully closed resource: ${key}`);
      } catch (err) {
        logger.error(`Failed to close resource ${key}: ${err}`);
      }
    }

    // Clean up Object properties (Added via .dot notation) to support keywords written for older versions of the agent exposing the session as simple object
    for (const key of Object.keys(this)) {
      const resource = this[key];
      if (resource && typeof resource.close === 'function') {
        resource.close();
      }
    }

    // Clear the map after disposal so resources aren't held in memory
    this.clear();
  }
}
module.exports = Session;
