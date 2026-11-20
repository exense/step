
function FindProxyForURL(url, host) {
  return "PROXY localhost:1; SOCKS localhost:2; HTTP localhost:3; HTTPS localhost:4; SOCKS4 localhost:5; SOCKS5 localhost:6; DIRECT";
}