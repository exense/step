angular.module('entities',['step'])

.factory('EntityRegistry', function() {
  var api = {};

  var entities = [];
  api.registerEntity = function(displayName, entityName, entityCollectionName, getUrl, postUrl, tableType, templateUrl, callback, icon){
    entities.push({
      displayName: displayName,
      entityName:entityName,
      entityCollectionName:entityCollectionName,
      getUrl:getUrl,
      postUrl:postUrl,
      tableType: tableType,
      templateUrl: templateUrl,
      callback : callback,
      icon: icon
    });
  };

  api.getEntities = function(){
    return entities;  
  };

  api.getEntityByName = function(name){
    return _.filter(entities,  
                function(item){  
                    return item.entityName === name; 
                })[0]; 
  };

  return api;
})

.factory('EntityScopeResolver', function() {
  var api = {};
  var resolvers = [];
  
  api.registerResolver = function(resolver){
    resolvers.push(resolver);
  };

  api.getScope = function(entity){
    for(i=0;i<resolvers.length;i++) {
      var resolver = resolvers[i];
      var entityScope = resolver(entity);
      if(entityScope != null) {
        return entityScope;
      }
    }
    return null;  
  };

  return api;
})

.directive('entityIcon', function($http) {
  return {
    restrict: 'E',
    scope: {
      entity:'=',
      entityName:'='
    },
    link: function(scope, element, attr) { 
    },
    controller: function($scope, EntityRegistry, EntityScopeResolver){
      var entityScope = EntityScopeResolver.getScope($scope.entity)
      if(entityScope) {
        $scope.cssClass = entityScope.cssClass;
        $scope.tooltip = entityScope.tooltip;
      } else {
        var entityType = EntityRegistry.getEntityByName($scope.entityName);
        if(entityType && entityType.icon) {
          $scope.cssClass = entityType.icon;
          $scope.tooltip = null;
        }
      }
    },
    templateUrl: 'partials/entities/entityIcon.html'
  };
})