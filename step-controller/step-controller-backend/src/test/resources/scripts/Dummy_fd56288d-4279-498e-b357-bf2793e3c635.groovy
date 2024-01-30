def slurper = new groovy.json.JsonSlurper();
def input = slurper.parseText(inputJson);

output.add("test","test");