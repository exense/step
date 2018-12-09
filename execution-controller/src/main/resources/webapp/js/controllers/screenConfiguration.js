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
angular.module('screenConfigurationControllers',['tables','step'])

.run(function(ViewRegistry) {
  ViewRegistry.registerDashlet('admin/controller','Screens','partials/screenconfiguration/screenConfiguration.html');
})


.controller('ScreenConfigurationCtrl', function($rootScope, $scope, $http, stateStorage, Dialogs, InputDialogs, AuthService) {
    stateStorage.push($scope, 'screenconfiguration', {});	
    $scope.authService = AuthService;
    
    $scope.currentScreenId = 'executionParameters'
      
    function reload() {
      $http.get("rest/screens/input/byscreen/"+$scope.currentScreenId).then(function(res) {
        $scope.screenInputs = res.data;
      });
    }
    
    $scope.reload = reload;
    
    reload();
    
    $scope.addInput = function() {
      InputDialogs.editScreenInput(null, $scope.currentScreenId, function() {reload()});
    }

    $scope.editInput = function(id) {
      InputDialogs.editScreenInput(id, null, function() {reload()});
    }
    
    $scope.deleteInput = function(id) {
      Dialogs.showDeleteWarning().then(function() {
        $http.delete("rest/screens/input/"+id).then(function() {
          reload();
        });
      })
    }
    
    $scope.tableHandle = {};
  })

.factory('InputDialogs', function ($uibModal, $http, Dialogs) {
  
  function openModal(id, screenId) {
    var modalInstance = $uibModal.open({
        templateUrl: 'partials/screenconfiguration/editScreenInputDialog.html',
        controller: 'editScreenInputCtrl',
        resolve: {id: function () {return id;}, screenId: function () {return screenId;}}
      });

      return modalInstance.result;
  }
  
  var dialogs = {};
  
  dialogs.editScreenInput = function(id, screenId, callback) {
    openModal(id, screenId).then(function() {
      if(callback){callback()};
    })
  }
  
  return dialogs;
})

.controller('editScreenInputCtrl', function ($scope, $uibModalInstance, $http, AuthService, id, screenId) {
  
  if(id==null) {
    $scope.input = {screenId:screenId, input: {}}
  } else {
    $http.get("rest/screens/input/"+id).then(function(response){
      $scope.input = response.data;
    })      
  }

  $scope.addOption = function() {
    if(!$scope.input.input.options) {
      $scope.input.input.options = [];
    }
    $scope.input.input.options.push({value:"",activationExpression:{script:""}});
  }
  
  $scope.removeOption = function(value) {
    $scope.input.input.options = _.reject($scope.input.input.options, function(option) {return option.value == value});
  }
  
  $scope.save = function () {
    $http.post("rest/screens/input",$scope.input).then(function(response) {
      $uibModalInstance.close();
    })
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})