from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPPlugin
from net.grinder.plugin.http import HTTPRequest

request = HTTPRequest()
output.startMeasure("Grinder_Demo_Transaction1")
result = request.GET(input.getString("url"))
output.stopMeasure()
output.add("out",result.text);
