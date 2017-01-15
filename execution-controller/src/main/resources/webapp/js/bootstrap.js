$.get("rest/app/plugins").success(function(plugins) {
  var scripts = [];
  var angularModules = [];
  _.each(plugins,function(plugin) {
    angularModules = angularModules.concat(plugin.angularModules);
    scripts = scripts.concat(plugin.scripts);   
  })
  
  $.getScripts(scripts).done(function() {
    _.each(angularModules, function(module) {
      angular.module('tecAdminApp').requires.push(module);      
    })
    angular.bootstrap(document,['tecAdminApp'])
  })
  
})

$.getScripts = function(arr) {
  var _arr = $.map(arr, function(scr) {
    return $.getScript(scr);
  });

  _arr.push($.Deferred(function(deferred) {
    $(deferred.resolve);
  }));

  return $.when.apply($, _arr);
}