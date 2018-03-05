angular.module('NodePlugin',['step','functionsControllers'])

.run(function(FunctionTypeRegistry) {
  FunctionTypeRegistry.register('step.plugins.node.NodeFunction','Node','node/partials/node.html');
})
