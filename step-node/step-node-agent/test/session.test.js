const Session = require("../api/controllers/session");

describe('Session auto-disposal', () => {

  test('[Symbol.dispose]() is called on resources stored via session.set()', async () => {
    const session = new Session()
    const disposed = jest.fn()
    session.set('res', {[Symbol.dispose]: disposed})
    await session.asyncDispose()
    expect(disposed).toHaveBeenCalledTimes(1)
  })

  test('.close() is called when the resource has no [Symbol.dispose]', async () => {
    const session = new Session()
    const closed = jest.fn()
    session.set('res', {close: closed})
    await session.asyncDispose()
    expect(closed).toHaveBeenCalledTimes(1)
  })

  test('.kill() is called when the resource has no [Symbol.dispose] or .close()', async () => {
    const session = new Session()
    const killed = jest.fn()
    session.set('res', {kill: killed})
    await session.asyncDispose()
    expect(killed).toHaveBeenCalledTimes(1)
  })

  test('.close() takes precedence over .kill()', async () => {
    const session = new Session()
    const closed = jest.fn()
    const killed = jest.fn()
    session.set('res', {close: closed, kill: killed})
    await session.asyncDispose()
    expect(closed).toHaveBeenCalledTimes(1)
    expect(killed).not.toHaveBeenCalled()
  })

  test('[Symbol.asyncDispose]() is awaited before asyncDispose() resolves', async () => {
    const session = new Session()
    let resolved = false
    session.set('res', {
      [Symbol.asyncDispose]: async () => {
        await new Promise(r => setTimeout(r, 10))
        resolved = true
      }
    })
    await session.asyncDispose()
    expect(resolved).toBe(true)
  })

  test('[Symbol.asyncDispose]() takes precedence over .close() and .kill()', async () => {
    const session = new Session()
    const asyncDisposed = jest.fn().mockResolvedValue(undefined)
    const closed = jest.fn()
    const killed = jest.fn()
    session.set('res', {[Symbol.asyncDispose]: asyncDisposed, close: closed, kill: killed})
    await session.asyncDispose()
    expect(asyncDisposed).toHaveBeenCalledTimes(1)
    expect(closed).not.toHaveBeenCalled()
    expect(killed).not.toHaveBeenCalled()
  })

  test('[Symbol.dispose]() takes precedence over .close() and .kill()', async () => {
    const session = new Session()
    const disposed = jest.fn()
    const closed = jest.fn()
    const killed = jest.fn()
    session.set('res', {[Symbol.dispose]: disposed, close: closed, kill: killed})
    await session.asyncDispose()
    expect(disposed).toHaveBeenCalledTimes(1)
    expect(closed).not.toHaveBeenCalled()
    expect(killed).not.toHaveBeenCalled()
  })

  test('.close() is called on resources stored via dot notation', async () => {
    const session = new Session()
    const closed = jest.fn()
    session.dotResource = {close: closed}
    await session.asyncDispose()
    expect(closed).toHaveBeenCalledTimes(1)
  })

  test('resources stored via dot notation support the same disposal methods as session.set()', async () => {
    const session = new Session()
    const asyncDisposed = jest.fn().mockResolvedValue(undefined)
    const disposed = jest.fn()
    const killed = jest.fn()
    session.asyncRes = {[Symbol.asyncDispose]: asyncDisposed}
    session.syncRes = {[Symbol.dispose]: disposed}
    session.killableRes = {kill: killed}
    await session.asyncDispose()
    expect(asyncDisposed).toHaveBeenCalledTimes(1)
    expect(disposed).toHaveBeenCalledTimes(1)
    expect(killed).toHaveBeenCalledTimes(1)
  })

  test('a failing dot-notation close is reported and does not stop the disposal', async () => {
    const session = new Session()
    const closed = jest.fn()
    session.good = {close: closed}
    session.bad = {close: () => { throw new Error('dot oops') }}
    await expect(session.asyncDispose()).rejects.toThrow('dot oops')
    expect(closed).toHaveBeenCalledTimes(1)
  })

  test('plain values stored via dot notation are skipped', async () => {
    const session = new Session()
    session.count = 42
    session.label = 'hello'
    session.nothing = null
    await expect(session.asyncDispose()).resolves.toBeUndefined()
  })

  test('resources with no disposal method are silently skipped', () => {
    const session = new Session()
    session.set('plain', { value: 42 })
    expect(async () => await session.asyncDispose()).not.toThrow()
  })

  test('all entries are cleared after disposal', async () => {
    const session = new Session()
    session.set('a', {close: jest.fn()})
    session.set('b', {close: jest.fn()})
    await session.asyncDispose()
    expect(session.size).toBe(0)
  })

  test('disposal continues for remaining resources when one throws', async () => {
    const session = new Session()
    session.set('bad', {
      [Symbol.dispose]: () => {
        throw new Error('oops')
      }
    })
    const goodClosed = jest.fn()
    session.set('good', {close: goodClosed})
    await expect(session.asyncDispose()).rejects.toThrow('oops')
    expect(goodClosed).toHaveBeenCalledTimes(1)
  })

  test('resources are closed sequentially, in reverse insertion order', async () => {
    const session = new Session()
    const events = []
    const asyncResource = (name) => ({
      [Symbol.asyncDispose]: async () => {
        events.push(`start-${name}`)
        await new Promise(r => setTimeout(r, 10))
        events.push(`end-${name}`)
      }
    })
    session.set('first', asyncResource('first'))
    session.set('second', asyncResource('second'))
    await session.asyncDispose()
    expect(events).toEqual(['start-second', 'end-second', 'start-first', 'end-first'])
  })

  test('a failing async close does not prevent the remaining resources from being closed', async () => {
    const session = new Session()
    const closed = jest.fn()
    session.set('good', {close: closed})
    session.set('bad', {[Symbol.asyncDispose]: async () => { throw new Error('async oops') }})
    await expect(session.asyncDispose()).rejects.toThrow('async oops')
    expect(closed).toHaveBeenCalledTimes(1)
  })

  test('multiple resources stored via session.set() are all disposed', async () => {
    const session = new Session()
    const fns = [jest.fn(), jest.fn(), jest.fn()]
    fns.forEach((fn, i) => session.set(`res${i}`, {[Symbol.dispose]: fn}))
    await session.asyncDispose()
    fns.forEach(fn => expect(fn).toHaveBeenCalledTimes(1))
  })
})
