// proxy.conf.js
const PROXY_TARGET = 'http://localhost:8080';

const PROXY_CONFIG = [
  {
    context: ['/api'],
    target: PROXY_TARGET,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
];

module.exports = PROXY_CONFIG;
