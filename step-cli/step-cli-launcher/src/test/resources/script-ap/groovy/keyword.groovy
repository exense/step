// Uses groovy.json on purpose: it is a module of its own inside groovy-all, so this also covers the extraction
// picking up more than just the groovy core classes.
def parsed = new groovy.json.JsonSlurper().parseText(inputJson)
output.add('echoed', parsed.message)
