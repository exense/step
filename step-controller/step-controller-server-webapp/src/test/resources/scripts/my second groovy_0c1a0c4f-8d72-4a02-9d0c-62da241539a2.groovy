def slurper = new groovy.json.JsonSlurper()
def input = slurper.parseText(inputJson)

def jsonBuilder = new groovy.json.JsonOutput()
context.setPayloadJson(jsonBuilder.toJson(input));