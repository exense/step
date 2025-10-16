const jwt = require('jsonwebtoken')

module.exports = {
  generateJwtToken: function(gridSecurity, expirationSeconds) {
    if (!gridSecurity || !gridSecurity.jwtSecretKey) {
      return null;
    }

    const now = Math.floor(Date.now() / 1000);
    const expiration = now + expirationSeconds;

    return jwt.sign(
      {
        iat: now,
        exp: expiration
      },
      gridSecurity.jwtSecretKey,
      { algorithm: 'HS256' }
    );
  }
}