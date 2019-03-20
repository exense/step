angular.module('jmeterPlugin',['step','functionsControllers'])

.run(function(FunctionTypeRegistry) {
  FunctionTypeRegistry.register('step.plugins.jmeter.JMeterFunction','JMeter','jmeterplugin/partials/jmeter.html');
})
