angular.module('NodePlugin',['step','functionsControllers'])

.run(function(FunctionTypeRegistry) {
  FunctionTypeRegistry.register('step.plugins.node.NodeFunction','Node.js','node/partials/node.html');
})
