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

.factory('ScreenTemplates', function($http, $q) {
  
  var api = {};
  
  api.getScreenInputsByScreenId = function(screenId) {
    return $q(function(resolve, reject) {
      $http.get("rest/screens/"+screenId).then(function(response){
        resolve(response.data)
      })
    })
  }
  
  api.getScreenInputByScreenId = function(screenId, screenInputId) {
    return $q(function(resolve, reject) {
      $http.get("rest/screens/"+screenId+"/"+screenInputId).then(function(response){
        resolve(response.data)
      })
    })
  }
  
  return api;
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
  
  $scope.moveOption = function(value, offset) {
    var options = $scope.input.input.options;
    var position = _.findIndex(options, function(option) {return option && option.value == value});
    var newPosition = position + offset;
    if(newPosition >= 0 && newPosition < options.length) {
      var item = options[position];
      options.splice(position, 1);
      options.splice(newPosition, 0, item);
    }
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
.directive('stCustomForm', function(ScreenTemplates) {
  return {
    restrict: 'E',
    scope: {
      stScreen: '@',
      stModel: '=',
      stOnChange: '&?',
      stDisabled: '=?',
      stInline: '=?',
      stExcludeFields: '=?'
    },
    templateUrl: 'partials/screenconfiguration/customForm.html',
    controller: function($scope) {
      ScreenTemplates.getScreenInputsByScreenId($scope.stScreen).then(function(attributes) {
        $scope.attributes=_.reject(attributes, function(attribute) {
          return $scope.stExcludeFields!=null && $scope.stExcludeFields.includes(attribute.id);
        });
      })
      
      $scope.saveAttributes = function() {
        if($scope.stOnChange) {
          $scope.stOnChange();
        }
      }
    }
  }
})
.directive('stCustomFormInput', function(ScreenTemplates) {
  return {
    restrict: 'E',
    scope: {
      ngModel: '=?',
      stBean: '=?',
      stInput: '=?',
      stScreen: '=?',
      stInputId: '=?',
      ngOnChange: '&?',
      ngDisabled: '=?',
      stInline: '=?'
    },
    templateUrl: 'partials/screenconfiguration/customFormInput.html',
    controller: function ctrl($scope) {
      if(!$scope.stInput) {
        ScreenTemplates.getScreenInputByScreenId($scope.stScreen, $scope.stInputId).then(function(input) {
          $scope.input=input
        })
      } else {
        $scope.input = $scope.stInput
      }
      
      $scope.model = function() {
        return function(value) {
          if($scope.stBean) {
            if(angular.isDefined(value)) {
              eval('$scope.stBean.'+$scope.input.id+'=value');
            } else {
              return eval('$scope.stBean.'+$scope.input.id);
            }
          } else {
            if(angular.isDefined(value)) {
              $scope.ngModel=value;
            } else {
              return $scope.ngModel;
            }
          }
        }
      }
      
      $scope.saveAttributes = function() {
        if($scope.stOnChange) {
          $scope.stOnChange();
        }
      }
    }
  }
})
