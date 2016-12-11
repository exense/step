from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPPlugin
from net.grinder.plugin.http import HTTPRequest

request = HTTPRequest()
result = request.GET("http://www.denkbar.io/")
output = result.text;
