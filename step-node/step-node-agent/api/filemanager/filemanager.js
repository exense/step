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
    const filemanagerPath = agentContext.properties['filemanagerPath'] || 'filemanager'
    this.workingDir = filemanagerPath + '/work/'
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
            cacheEntry.promises.forEach(callback => callback(filePath + '/' + result)) // eslint-disable-line
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

  #getKeywordFile(controllerFileUrl, targetDir) {
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
        const filename = this.#parseName(resp.headers)
        const filepath = targetDir + '/' + filename
        if (this.#isDir(resp.headers) || filename.toUpperCase().endsWith('ZIP')) {
          resp.pipe(unzip.Extract({path: filepath})).on('close', () => resolve(filename))
        } else {
          const myFile = fs.createWriteStream(filepath)
          resp.pipe(myFile).on('finish', () => resolve(filename))
        }
      }).on('error', (err) => {
        logger.error('HTTP request error:', err)
        reject(err)
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
