/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
