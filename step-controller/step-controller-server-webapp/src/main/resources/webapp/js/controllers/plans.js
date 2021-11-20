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
angular.module('plans',['tables','step','screenConfigurationControllers'])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('plans','partials/plans/plans.html');
  EntityRegistry.registerEntity('Plan', 'plans', 'plans', 'rest/plans/', 'rest/plans/', 'st-table', '/partials/plans/planSelectionTable.html', null, 'glyphicon glyphicon-file');
})

.factory('PlanTypeRegistry', function() {

  var api = {};

  var registry = {};
  
  api.register = function(type, label, editor) {
    registry[type] = {
      editor: editor,
      label: label,
      type: type
    };
  };
  
  api.getEditorView = function(type){
    return registry[type].editor;
  };
  
  api.getPlanTypes = function() {
    return _.values(registry);
  }
  
  api.getPlanType = function(typeName) {
    return registry[typeName];
  }

  return api;
})

.controller('PlansCtrl', function($rootScope, $scope, stateStorage) {
  stateStorage.push($scope, 'plans', {}); 

  $scope.$watch('$state',function() {
    if($scope.$state!=null) {
      $scope.selectView = $scope.$state
    }
  });
})

.controller('PlanListCtrl', function($rootScope, $scope, $http, $location, $uibModal, stateStorage, Dialogs, PlanDialogs, ImportDialogs, ExportDialogs, AuthService) {
    stateStorage.push($scope, 'list', {});	
    $scope.authService = AuthService;
    
    $scope.tableHandle = {};
    
    function reload() {
      $scope.tableHandle.reload();
    }
    
    $scope.addPlan = function() {
      PlanDialogs.createPlan(function(plan) {
        reload();
      });
    }

    $scope.editPlan = function(id) {
      $location.path('/root/plans/editor/' + id);
    }
    
    $scope.executePlan = function(id) {
      $location.path('/root/repository').search({repositoryId:'local',planid:id});
    }
    
    $scope.copyPlan = function(id) {
      $rootScope.clipboard = {object:"plan",id:id};
    }
    
    $scope.pastePlan = function() {
      if($rootScope.clipboard && $rootScope.clipboard.object=="plan") {
        $http.get("rest/plans/"+$rootScope.clipboard.id+"/clone").then(function(response) {
          var clone = response.data;
          clone.attributes.name = clone.attributes.name + "_Copy" 
          $http.post("rest/plans", clone).then(function(response) {
            reload();
          })
        });
      }
    }
    
    $scope.deletePlan = function(id, name) {
      Dialogs.showDeleteWarning(1, 'Plan "' + name + '"').then(function() {
        $http.delete("rest/plans/"+id).then(function() {
          reload();
        });
      })
    }
    
    $scope.importPlans = function() {
      ImportDialogs.displayImportDialog('Plans import','plans', true, false).then(function () {
        reload();
      });
    }
    
    $scope.exportPlans = function() {
      ExportDialogs.displayExportDialog('Plans export','plans', 'allPlans.sta', true, false).then(function () {})
    }

    $scope.exportPlan = function(id, name) {
      ExportDialogs.displayExportDialog('Plans export','plans/'+id, name+'.sta', true, false).then(function () {})
    }
    
  })
  
.factory('PlanDialogs', function ($uibModal, $http, Dialogs) {
  
  var dialogs = {};
  
  dialogs.createPlan = function(callback) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      templateUrl: 'partials/plans/createPlanDialog.html',
      controller: 'createPlanCtrl',
      resolve: {}
    });

    modalInstance.result.then(function(plan) {
      if(callback){callback(plan)};
    })
  }
  
  dialogs.selectPlan = function(callback) {
    Dialogs.selectEntityOfType('plans', true).then(function(result) {
      var id = result.item;
      $http.get('rest/plans/'+id).then(function(response) {
        var plan = response.data;
        if(callback){callback(plan)};
      }) 
    });
  }
  
  return dialogs;
})

.controller('createPlanCtrl', function ($scope, $uibModalInstance, $location, $http, AuthService, ScreenTemplates, PlanTypeRegistry) {
  $scope.AuthService = AuthService;
  
  $scope.template = 'TestCase';
  $scope.plan = {attributes:{}};

  
  $scope.planTypes = PlanTypeRegistry.getPlanTypes();
  $scope.planType = PlanTypeRegistry.getPlanType('step.core.plans.Plan');
  
  $http.get("rest/plans/artefact/templates").then(function(response){
    $scope.artefactTypes = response.data;
  })
  
  $scope.save = function (editAfterSave) {
    $http.get("rest/plans?type="+$scope.planType.type+'&template='+$scope.template).then(function(response){
      var createdPlan = response.data;
      createdPlan.attributes = $scope.plan.attributes;
      if(createdPlan.root) {
        createdPlan.root.attributes = createdPlan.attributes;
      }
      $http.post("rest/plans", createdPlan).then(function(response) {
        $uibModalInstance.close(response.data);
        if(editAfterSave) {
          $location.path('/root/plans/editor/' + createdPlan.id);
        }
      })
    })
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.directive('planLink', function() {
  return {
    restrict: 'E',
    scope: {
      planRef: '=?',
      planId: '=?',
      description: '=?',
      linkOnly: '=?',
      stOptions: '=?'
    },
    templateUrl: 'partials/components/planLink.html',
    controller: function($scope, $http) {
      $scope.noLink = $scope.stOptions && $scope.stOptions.includes("noEditorLink")
      if($scope.planRef && $scope.planRef.repositoryID=='local') {
        $scope.planId = $scope.planRef.repositoryParameters.planid
      }
    }
  };
})

.directive('planLinkAndName', function() {
  return {
    restrict: 'E',
    scope: {
      planRef: '=?'
    },
    templateUrl: 'partials/components/planLinkAndName.html',
    controller: function($scope, $http) {
      $scope.noLink = $scope.stOptions && $scope.stOptions.includes("noEditorLink")
      if($scope.planRef && $scope.planRef.repositoryID=='local') {
    	  $scope.planId = $scope.planRef.repositoryParameters.planid
    	  $http.get("rest/plans/"+ $scope.planId).then(function(response) {
    		  $scope.planName = response.data.attributes.name;
    	  });
      }
    }
  };
})

.directive('planName', function() {
  return {
    restrict: 'E',
    scope: {
      planRef: '=?'
    },
    templateUrl: 'partials/components/planName.html',
    controller: function($scope, $http) {
      if($scope.planRef && $scope.planRef.repositoryID=='local') {
    	  $scope.planId = $scope.planRef.repositoryParameters.planid
    	  $http.get("rest/plans/"+ $scope.planId).then(function(response) {
    		  $scope.planName = response.data.attributes.name;
    	  });
      }
    }
  };
})
