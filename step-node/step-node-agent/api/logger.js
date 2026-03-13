const { createLogger, format, transports } = require('winston');

const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: format.combine(
    format.errors({ stack: true }),
    format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    format.printf((info) => {
      const prefix = info.component ? `[${info.component}] ` : '';
      const logMessage = info.stack ? `${info.message} - ${info.stack}` : info.message;
      return `${info.timestamp} [${info.level.toUpperCase()}] ${prefix}${logMessage}`
    })
  ),
  transports: [new transports.Console()],
});

module.exports = logger;
