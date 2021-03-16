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
angular.module('parametersControllers',['tables','step','screenConfigurationControllers'])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('parameters','partials/parameters/parameterList.html');  
  EntityRegistry.registerEntity('Parameter', 'parameter', 'parameters', 'rest/parameters/', 'rest/parameters/', 'st-table', '/partials/parameters/parameterSelectionTable.html', null, 'glyphicon glyphicon-list-alt');
})

.controller('ParameterListCtrl', function($rootScope, $scope, $http, $uibModal, stateStorage, ExportDialogs, Dialogs, ParameterDialogs, ImportDialogs, AuthService) {
    stateStorage.push($scope, 'parameters', {});	
    $scope.authService = AuthService;
    
    function reload() {
      $scope.tableHandle.reload();
    }
    
    $scope.addParameter = function() {
      ParameterDialogs.editParameter(null, function() {reload()});
    }

    $scope.editParameter = function(id) {
      ParameterDialogs.editParameter(id, function() {reload()});
    }
    
    $scope.deleteParameter = function(id) {
      Dialogs.showDeleteWarning().then(function() {
        $http.delete("rest/parameters/"+id).then(function() {
          reload();
        });
      })
    }

    $scope.copyParameter = function(id) {
      $rootScope.clipboard = {object:"parameter",id:id};
    }
    
    $scope.pasteParameter = function() {
      if($rootScope.clipboard && $rootScope.clipboard.object=="parameter") {
        $http.post("rest/parameters/"+$rootScope.clipboard.id+"/copy")
        .then(function() {
          reload();
        });
      }
    }
    
    $scope.importParameters = function() {
      ImportDialogs.displayImportDialog('Parameters import','parameters',true).then(function () {
        reload();
      });
    }
    
    $scope.exportParameters = function() {
      ExportDialogs.displayExportDialog('Parameters export','parameters', 'allParameters.sta', true).then(function () {})
    }
    
    $scope.$on("parameter.edited",function(evt,data){
      reload();
    });
    
    $scope.tableHandle = {};
  })

.factory('ParameterDialogs', function ($uibModal, $http, Dialogs) {
  
  function openModal(id) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
        templateUrl: 'partials/parameters/editParameterDialog.html',
        controller: 'editParameterCtrl',
        resolve: {id: function () {return id;}}
      });

      return modalInstance.result;
  }
  
  var dialogs = {};
  
  dialogs.editParameter = function(id, callback) {
    openModal(id).then(function() {
      if(callback){callback()};
    })
  }
  
  return dialogs;
})

.factory('ParameterScopeRenderer', function () {
  var api = {};
  
  // Backward compatibility: assuming GLOBAL scope if not set
  api.normalizeScope = function(scope) {
    return scope?scope:'GLOBAL'
  }
  
  api.scopeIcon = function(scope) {
    scope = api.normalizeScope(scope);
    if(scope == 'GLOBAL') {
      return 'glyphicon-unchecked';
    } else if(scope == 'FUNCTION') {
      return 'glyphicon-record';
    } else if(scope == 'APPLICATION') {
      return 'glyphicon-book';
    } 
  }
  
  api.scopeCssClass = function(scope) {
    scope = api.normalizeScope(scope);
    if(scope == 'GLOBAL') {
      return 'parameter-scope-global';
    } else if(scope == 'FUNCTION') {
      return 'parameter-scope-keyword';
    } else if(scope == 'APPLICATION') {
      return 'parameter-scope-application';
    } 
  }
  
  api.scopeSpanCssClass = function(scope) {
    scope = api.normalizeScope(scope);
    return 'parameter-scope '+api.scopeCssClass(scope)
  }
  
  api.label = function(parameter) {
    if(parameter) {
      var scope = api.normalizeScope(parameter.scope);
      if(scope == 'GLOBAL') {
        return 'Global';
      } else {
        return parameter.scopeEntity;
      }
    }
  }
  
  api.scopeLabel = function(scope) {
    if(scope == 'GLOBAL') {
      return 'Global';
    } else if(scope == 'FUNCTION') {
      return 'Keyword';
    } else if(scope == 'APPLICATION') {
      return 'Application';
    } 
  }
  
  return api;
})

.controller('editParameterCtrl', function ($scope, $uibModalInstance, $http, AuthService, id, ParameterScopeRenderer, ScreenTemplates) {
  $scope.AuthService = AuthService;
  
  $scope.scopeLabel = ParameterScopeRenderer.scopeLabel
  $scope.scopeCssClass = ParameterScopeRenderer.scopeCssClass
  $scope.scopeSpanCssClass = ParameterScopeRenderer.scopeSpanCssClass
  $scope.scopeIcon = ParameterScopeRenderer.scopeIcon

  $scope.selectScope = function(scope) {
    $scope.parameter.scopeEntity = '';
    $scope.parameter.scope = scope;
  }
  
  $scope.isApplicationScopeEnabled = false;
  ScreenTemplates.getScreenInputByScreenId('functionTable','attributes.application').then(function(input) {
    if(input) {
      $scope.isApplicationScopeEnabled = true;
    }
  })
  
  if(id==null) {
    $http.get("rest/parameters").then(function(response){
      $scope.parameter = response.data;
    })  
  } else {
    $http.get("rest/parameters/"+id).then(function(response){
      $scope.parameter = response.data;
      if(!$scope.parameter.scope) {
        $scope.parameter.scope = ParameterScopeRenderer.normalizeScope($scope.parameter.scope)
      }
    })      
  }

  $scope.save = function () {
    $http.post("rest/parameters",$scope.parameter).then(function(response) {
      $uibModalInstance.close();
    })
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.directive('parameterKey', function() {
  return {
    restrict: 'E',
    scope: {
      parameter: '=',
      stOptions: '=?'
    },
    templateUrl: 'partials/parameters/parameterKey.html',
    controller: function($scope, AuthService, ParameterDialogs) {
      $scope.authService = AuthService;
      $scope.noLink = $scope.stOptions && $scope.stOptions.includes("noEditorLink")
      $scope.editParameter = function(id) {
        ParameterDialogs.editParameter(id, function() {$scope.$emit("parameter.edited",$scope.parameter)});
      }
    }
  };
})

.directive('parameterScope', function() {
  return {
    restrict: 'E',
    scope: {
      parameter: '='
    },
    template: '<span uib-tooltip="{{scopeLabel}}" ng-class="scopeSpanCssClass">' + 
              '<i style="display:inline-block" ng-class="scopeIcon" class="glyphicon"></i> {{label}}</span>',
    controller: function($scope, ParameterScopeRenderer) {
      $scope.scopeLabel = ParameterScopeRenderer.scopeLabel($scope.parameter.scope)
      $scope.scopeIcon = ParameterScopeRenderer.scopeIcon($scope.parameter.scope)
      $scope.scopeSpanCssClass = ParameterScopeRenderer.scopeSpanCssClass($scope.parameter.scope)
      $scope.label = ParameterScopeRenderer.label($scope.parameter)
    }
  };
})
