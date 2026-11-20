// Keyword of the automation package used by StreamingAttachmentsLocalExecutionTest. It streams a log while writing
// it, as a keyword following the progress of a long running process would.
const fs = require('fs')
const os = require('os')
const path = require('path')

exports.NodeStreamALog = async (input, output, session, properties, liveReporting) => {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'streamed-'))
    const log = path.join(directory, 'node-keyword.log')
    try {
        fs.writeFileSync(log, '')
        const upload = liveReporting.fileUploads.startTextFileUpload(log)
        fs.writeFileSync(log, 'line 1\nline 2\n')
        await upload.complete()
    } finally {
        fs.rmSync(directory, {recursive: true, force: true})
    }
}
