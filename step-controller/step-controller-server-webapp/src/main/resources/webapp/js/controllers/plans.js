/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
  EntityRegistry.registerEntity('Plan', 'artefact', 'artefacts', 'rest/controller/artefact/', 'rest/controller/artefact', 'datatable', '/partials/selection/selectDatatableEntity.html');
})

.controller('PlansCtrl', function($rootScope, $scope, stateStorage) {
  stateStorage.push($scope, 'plans', {}); 

  $scope.$watch('$state',function() {
    if($scope.$state!=null) {
      $scope.selectView = $scope.$state
    }
  });

})

.controller('PlanListCtrl', function($rootScope, $scope, $http, $location, $uibModal, stateStorage, ExportService, Dialogs, PlanDialogs, AuthService) {
    stateStorage.push($scope, 'plans', {});	
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
    
    $scope.deletePlan = function(id) {
      Dialogs.showDeleteWarning().then(function() {
        $http.delete("rest/plans/"+id).then(function() {
          reload();
        });
      })
    }
    
    $scope.importPlans = function() {
      var modalInstance = $uibModal.open({
        backdrop: 'static',
        templateUrl: 'partials/plans/importPlansDialog.html',
        controller: 'importPlansModalCtrl',
        resolve: {}
      });

      modalInstance.result.then(function () {
        reload();
      });
    }
    
    $scope.exportPlans = function() {
      ExportService.get("rest/export/plans")
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
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      templateUrl: 'partials/plans/selectPlanDialog.html',
      controller: 'selectPlanModalCtrl',
      resolve: {}
    });

    modalInstance.result.then(function(plan) {
      if(callback){callback(plan)};
    })
  }
  
  return dialogs;
})

.controller('createPlanCtrl', function ($scope, $uibModalInstance, $location, $http, AuthService, ScreenTemplates) {
  $scope.AuthService = AuthService;
  
  $scope.type = 'Sequence';
  $scope.plan = {attributes:{}};

  $http.get("rest/controller/artefact/types").then(function(response){ 
    $scope.artefactTypes = response.data;
  })
  
  $scope.save = function (editAfterSave) {
    $http.get("rest/plans?type="+$scope.type).then(function(response){
      var createdPlan = response.data;
      createdPlan.attributes = $scope.plan.attributes;
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

.controller('importPlansModalCtrl', function ($scope, $http, $uibModalInstance, Upload, Dialogs) {
  $scope.resourcePath; 
  
  $scope.save = function() {
    if($scope.resourcePath) {
      $http({url:"rest/import/artefact",method:"POST",params:{path:$scope.resourcePath}}).then(function(response) {
        $uibModalInstance.close(response.data);
      })      
    } else {
      Dialogs.showErrorMsg("Upload not completed.");
    }
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.controller('selectPlanModalCtrl', function ($scope, $uibModalInstance, $http) {
  $scope.selectPlan = function(id) {
    $http.get('rest/plans/'+id).then(function(response) {
      var plan = response.data;
      $uibModalInstance.close(plan);
    }) 
  }
  
  $scope.table = {};
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})
