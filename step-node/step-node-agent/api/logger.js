const { createLogger, format, transports } = require('winston');

const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: format.combine(
    format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    format.printf(({ timestamp, level, message, component }) => {
      const prefix = component ? `[${component}] ` : ''
      return `${timestamp} [${level.toUpperCase()}] ${prefix}${message}`
    })
  ),
  transports: [new transports.Console()],
});

module.exports = logger;
