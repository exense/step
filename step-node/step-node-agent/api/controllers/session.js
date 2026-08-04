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

/**
 * Closes a single resource using the first disposal method it exposes, awaiting the result so
 * that a resource is fully closed before the next one is touched. Resources without any disposal
 * method are skipped. Returns the error if the close failed, null otherwise.
 *
 * Only one method is called: a resource exposing several of them offers variants of the same
 * teardown, not complementary steps. They are tried from the most explicit and graceful to the
 * most forceful, so close() wins over kill() when a resource has both (a Playwright BrowserServer
 * for instance, where close() shuts the browser down gracefully and kill() terminates its
 * process). A resource that really needs several calls should expose [Symbol.asyncDispose] and
 * perform them there.
 */
async function closeResource(label, resource) {
  if (!resource) return null;
  try {
    if (typeof resource[Symbol.asyncDispose] === 'function') {
      await resource[Symbol.asyncDispose]();
    } else if (typeof resource[Symbol.dispose] === 'function') {
      resource[Symbol.dispose]();
    } else if (typeof resource.close === 'function') {
      await resource.close();
    } else if (typeof resource.kill === 'function') {
      await resource.kill();
    } else {
      return null;
    }
    logger.debug(`Successfully closed resource: ${label}`);
    return null;
  } catch (err) {
    logger.error(`Failed to close resource ${label}:`, err);
    return err;
  }
}

class Session extends Map {

  async asyncDispose() {
    logger.info(`Async-disposing Session: Cleaning up ${this.size} resources...`);
    const failures = [];

    // Resources are closed one after the other, in reverse insertion order: closing them in
    // parallel breaks linked objects (a Playwright page cannot be closed once its browser is
    // gone), and the resource created last is the one depending on the previously created ones.
    for (const [key, resource] of [...this].reverse()) {
      const err = await closeResource(key, resource);
      if (err) failures.push(err);
    }

    // Clean up Object properties (Added via .dot notation)
    for (const key of Object.keys(this).reverse()) {
      const err = await closeResource(`${key} (dot notation)`, this[key]);
      if (err) failures.push(err);
    }

    this.clear();
    if (failures.length > 0) {
      throw new Error(failures.map(err => err?.message || String(err)).join('; '));
    }
  }
}
module.exports = Session;
