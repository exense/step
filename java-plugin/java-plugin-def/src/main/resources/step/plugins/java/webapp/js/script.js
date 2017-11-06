angular.module('javaPlugin',['step','functionsControllers'])

.run(function(FunctionTypeRegistry) {
  FunctionTypeRegistry.register('step.plugins.java.GeneralScriptFunction','Script (Java, JS, Groovy, etc)','javaplugin/partials/script.html');
})
