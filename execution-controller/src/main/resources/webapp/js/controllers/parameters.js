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
angular.module('parametersControllers',['tables','step'])

.controller('ParameterListCtrl', function($rootScope, $scope, $http, stateStorage, Dialogs, ParameterDialogs, AuthService) {
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
    
    $scope.tableHandle = {};
  })

.factory('ParameterDialogs', function ($uibModal, $http, Dialogs) {
  
  function openModal(id) {
    var modalInstance = $uibModal.open({
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

.controller('editParameterCtrl', function ($scope, $uibModalInstance, $http, AuthService, id) {
  
  if(id==null) {
    $http.get("rest/parameters").then(function(response){
      $scope.parameter = response.data;
    })  
  } else {
    $http.get("rest/parameters/"+id).then(function(response){
      $scope.parameter = response.data;
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