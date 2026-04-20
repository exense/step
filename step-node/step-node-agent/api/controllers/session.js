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
  logger = { debug: console.debug.bind(console), info: console.log.bind(console), warn: console.warn.bind(console), error: console.error.bind(console) };
}

class Session extends Map {

  async asyncDispose() {
    logger.info(`Async-disposing Session: Cleaning up ${this.size} resources...`);
    const promises = [];

    for (const [key, resource] of this) {
      try {
        if (resource && typeof resource[Symbol.asyncDispose] === 'function') {
          promises.push(resource[Symbol.asyncDispose]());
        } else if (resource && typeof resource[Symbol.dispose] === 'function') {
          resource[Symbol.dispose]();
        } else if (resource && typeof resource.kill === 'function') {
          resource.kill();
        } else if (resource && typeof resource.close === 'function') {
          const result = resource.close();
          if (result && typeof result.then === 'function') promises.push(result);
        }
        logger.debug(`Successfully closed resource: ${key}`);
      } catch (err) {
        logger.error(`Failed to close resource ${key}:`, err);
      }
    }

    // Clean up Object properties (Added via .dot notation)
    for (const key of Object.keys(this)) {
      const resource = this[key];
      if (resource && typeof resource.close === 'function') {
        try {
          const result = resource.close();
          if (result && typeof result.then === 'function') promises.push(result);
        } catch (err) {
          logger.error(`Failed to close dot-notation resource ${key}:`, err);
        }
      }
    }

    const results = await Promise.allSettled(promises);
    this.clear();
    const failures = results.filter(r => r.status === 'rejected');
    if (failures.length > 0) {
      throw new Error(failures.map(f => f.reason?.message || String(f.reason)).join('; '));
    }
  }
}
module.exports = Session;
