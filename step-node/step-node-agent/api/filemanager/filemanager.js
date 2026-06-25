const fs = require('fs')
const http = require('http')
const https = require('https')
const url = require('url')
const unzip = require('unzip-stream')
const jwtUtils = require('../../utils/jwtUtils')
const logger = require('../logger').child({ component: 'FileManager' })

class FileManager {
  constructor(agentContext) {
    this.agentContext = agentContext;
    const filemanagerPath = agentContext.filemanagerPath || 'filemanager'
    this.workingDir = filemanagerPath + '/work/'
    // Download retry/timeout settings, aligned with the Java agent config
    // (gridMaxRetries / gridRetryDelayMs / gridConnectTimeout / gridReadTimeout)
    this.gridMaxRetries = agentContext.gridMaxRetries ?? 3
    this.gridRetryDelayMs = agentContext.gridRetryDelayMs ?? 1000
    this.gridConnectTimeout = agentContext.gridConnectTimeout ?? 3000
    this.gridReadTimeout = agentContext.gridReadTimeout ?? 3000
    logger.info('Starting file manager using working directory: ' + this.workingDir)

    logger.info('Clearing working dir: ' + this.workingDir)
    fs.rmSync(this.workingDir, { recursive: true, force: true })
    fs.mkdirSync(this.workingDir, { recursive: true })

    this.filemanagerMap = {}
  }

  isFirstLevelKeywordFolder(filePath) {
    return fs.existsSync(filePath + '/keywords')
  }


  async loadOrGetKeywordFile(controllerUrl, fileId, fileVersionId) {
    return new Promise((resolve, reject) => {
      const filePath = this.workingDir + fileId + '/' + fileVersionId

      const cacheEntry = this.#getCacheEntry(fileId, fileVersionId)
      if (cacheEntry) {
        if (!cacheEntry.loading) {
          logger.info('Entry found for fileId ' + fileId + ': ' + cacheEntry.name)
          const fileName = cacheEntry.name

          if (fs.existsSync(filePath + '/' + fileName)) {
            resolve(filePath + '/' + fileName)
          } else {
            reject(new Error('Entry exists but no file found: ' + filePath + '/' + fileName))
          }
        } else {
          logger.info('Waiting for cache entry to be loaded for fileId ' + fileId)
          cacheEntry.promises.push((result) => {
            logger.info('Cache entry loaded for fileId ' + fileId)
            resolve(result)
          })
        }
      } else {
        this.#putCacheEntry(fileId, fileVersionId, { loading: true, promises: [] })

        logger.info('No entry found for fileId ' + fileId + '. Loading...')
        fs.mkdirSync(filePath, { recursive: true })
        logger.info('Created file path: ' + filePath + ' for fileId ' + fileId)

        const fileVersionUrl = controllerUrl + fileId + '/' + fileVersionId
        logger.info('Requesting file from: ' + fileVersionUrl)
        this.#getKeywordFile(fileVersionUrl, filePath).then((result) => {
          logger.info('Transferred file ' + result + ' from ' + fileVersionUrl)

          const cacheEntry = this.#getCacheEntry(fileId, fileVersionId)
          cacheEntry.name = result
          cacheEntry.loading = false

          this.#putCacheEntry(fileId, fileVersionId, cacheEntry)

          if (cacheEntry.promises) {
            cacheEntry.promises.forEach(callback => callback(filePath + '/' + result))
          }
          delete cacheEntry.promises

          resolve(filePath + '/' + result)
        }, (err) => {
          logger.error('Error downloading file:', err)
          reject(err)
        })
      }
    })
  }

  #getCacheEntry(fileId, fileVersion) {
    return this.filemanagerMap[fileId + fileVersion]
  }

  #putCacheEntry(fileId, fileVersion, entry) {
    this.filemanagerMap[fileId + fileVersion] = entry
  }

  // Downloads the keyword file, retrying transient network failures (e.g. the
  // grid temporarily being unreachable, or a connect/read timeout) with a delay
  // between attempts. This mirrors the Java agent (RegistrationClient), which
  // retries downloads on network/timeout errors but not on server-side errors
  // such as a non-200 response or a malformed response.
  async #getKeywordFile(controllerFileUrl, targetDir) {
    let attempt = 0
    while (true) {
      try {
        return await this.#downloadKeywordFile(controllerFileUrl, targetDir)
      } catch (err) {
        if (!err.nonRetryable && attempt < this.gridMaxRetries) {
          attempt++
          logger.warn('Retrying download from ' + controllerFileUrl + ' after a network error (' +
            (err.code || err.message) + '). Attempt ' + attempt + '/' + this.gridMaxRetries)
          await this.#sleep(this.gridRetryDelayMs)
        } else {
          throw err
        }
      }
    }
  }

  #sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  // Builds an error that the retry loop must not retry, used for server-side
  // failures (non-200 responses, malformed responses) where retrying is pointless.
  #nonRetryableError(message) {
    const err = new Error(message)
    err.nonRetryable = true
    return err
  }

  #downloadKeywordFile(controllerFileUrl, targetDir) {
    return new Promise((resolve, reject) => {
      const parsedUrl = url.parse(controllerFileUrl)
      const httpModule = parsedUrl.protocol === 'https:' ? https : http

      const requestOptions = {
        hostname: parsedUrl.hostname,
        port: parsedUrl.port,
        path: parsedUrl.path,
        method: 'GET'
      }

      // Add bearer token if gridSecurity is configured
      const token = jwtUtils.generateJwtToken(this.agentContext.gridSecurity, 300); // 5 minutes expiration
      if (token) {
        requestOptions.headers = { 'Authorization': 'Bearer ' + token };
      }

      const req = httpModule.request(requestOptions, (resp) => {
        // Non-200 responses are server-side errors and are not retried (matching the Java agent)
        if (resp.statusCode !== 200) {
          resp.resume() // drain the response so the socket can be freed
          reject(this.#nonRetryableError('Unexpected server error while downloading ' + controllerFileUrl + ': HTTP status ' + resp.statusCode))
          return
        }

        const filename = this.#parseName(resp.headers)
        if (!filename) {
          resp.resume()
          reject(this.#nonRetryableError('No filename found in content-disposition header of the response from ' + controllerFileUrl))
          return
        }

        // Errors occurring while streaming the body (e.g. a connection reset
        // mid-download) are network errors and are therefore retryable.
        resp.on('error', reject)
        const filepath = targetDir + '/' + filename
        if (this.#isDir(resp.headers) || filename.toUpperCase().endsWith('ZIP')) {
          const extract = unzip.Extract({path: filepath})
          extract.on('error', reject)
          resp.pipe(extract).on('close', () => resolve(filename))
        } else {
          const myFile = fs.createWriteStream(filepath)
          myFile.on('error', reject)
          resp.pipe(myFile).on('finish', () => resolve(filename))
        }
      }).on('error', (err) => {
        // Logged at debug level only: network errors may be retried by the
        // caller, and the final failure is logged by loadOrGetKeywordFile.
        logger.debug('HTTP request error:', err)
        reject(err)
      })

      // Apply the connect/read timeouts. Node uses a single socket inactivity
      // timeout, so we use the larger of the two; a timeout is surfaced as a
      // retryable ETIMEDOUT, matching the Java agent which retries on timeouts.
      const socketTimeout = Math.max(this.gridConnectTimeout, this.gridReadTimeout)
      req.setTimeout(socketTimeout, () => {
        const err = new Error('Request to ' + controllerFileUrl + ' timed out after ' + socketTimeout + 'ms')
        err.code = 'ETIMEDOUT'
        req.destroy(err)
      })

      req.end()
    })
  }

  #parseName(headers) {
    const contentDisposition = headers['content-disposition'] || ''
    const match = contentDisposition.match(/filename\s*=\s*([^;]+)/)
    return match ? match[1].trim() : ''
  }

  #isDir(headers) {
    const contentDisposition = headers['content-disposition'] || ''
    const match = contentDisposition.match(/type\s*=\s*([^;]+)/)
    return match ? match[1].trim().startsWith('dir') : false
  }
}

module.exports = FileManager;
