const jwt = require('jsonwebtoken')

module.exports = function createJwtAuthMiddleware(gridSecurity) {
  return function jwtAuthMiddleware(req, res, next) {
    // Skip authentication for /running endpoint
    if (req.path === '/running') {
      return next();
    }

    // Skip authentication if gridSecurity is not configured
    if (!gridSecurity || !gridSecurity.jwtSecretKey) {
      return next();
    }

    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Missing or invalid Authorization header' });
    }

    const token = authHeader.substring(7); // Remove 'Bearer ' prefix

    try {
      const decoded = jwt.verify(token, gridSecurity.jwtSecretKey, { algorithms: ['HS256'] });
      req.user = decoded;
      next();
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        return res.status(401).json({ error: 'Token expired' });
      } else if (err.name === 'JsonWebTokenError') {
        return res.status(401).json({ error: 'Invalid token' });
      } else {
        return res.status(500).json({ error: 'Token verification failed' });
      }
    }
  };
};