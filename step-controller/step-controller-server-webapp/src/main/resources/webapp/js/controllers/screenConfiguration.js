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

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerDashlet('admin/controller','Screens','partials/screenconfiguration/screenConfiguration.html','screens');
  ViewRegistry.registerDashlet('settings','Screens','partials/screenconfiguration/screenConfiguration.html','screens');
  EntityRegistry.registerEntity('Input', 'input', 'screenInputs', 'rest/screens/input/', 'rest/screens/input/', 'st-table', '/partials/screenconfiguration/screenInputSelectionTable.html', null, 'glyphicon glyphicon-modal-window');
})

.factory('ScreenTemplates', function($http, $q) {
  
  var api = {};
  
  var screensCache = {};
  
  api.clearCache = function() {
    screensCache = {};
  }
  
  api.getScreens = function(screenId) {
    return $q(function(resolve, reject) {
      $http.get("rest/screens").then(function(response){
        resolve(response.data)
      })
    })
  }
  
  api.getScreenInputsByScreenId = function(screenId, params) {
    return $q(function(resolve, reject) {
      if(!params && screensCache[screenId]) {
        return resolve(screensCache[screenId]);
      } else {
        $http({url:"rest/screens/"+screenId, method:"POST", data:params}).then(function(response){
          var screenInputs = response.data;
          screensCache[screenId] = screenInputs;
          resolve(screenInputs);
        })
      }
    })
  }
  
  api.getScreenInputByScreenId = function(screenId, screenInputId) {
    return $q(function(resolve, reject) {
      $http.get("rest/screens/"+screenId+"/"+screenInputId).then(function(response){
        resolve(response.data)
      })
    })
  }

  api.getScreenInputModel = function(value, input) {
    var bean = new Bean(value)
    return function(value) {
      if(angular.isDefined(value)) {
        bean.setProperty(input.id,value);
      } else {
        return bean.getProperty(input.id);
      }
    }
  }
  
  return api;
})

.controller('ScreenConfigurationCtrl', function($rootScope, $scope, $http, stateStorage, ScreenTemplates, Dialogs, InputDialogs, AuthService) {
    stateStorage.push($scope, 'screenconfiguration', {});	
    $scope.authService = AuthService;
    
    $scope.currentScreenId = 'executionParameters'
      
    ScreenTemplates.getScreens().then(function(res) {
      $scope.screens = res;
    })
      
    function reload() {
      $http.get("rest/screens/input/byscreen/"+$scope.currentScreenId).then(function(res) {
        $scope.screenInputs = res.data;
        ScreenTemplates.clearCache();
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
    
    $scope.moveInput = function(id, offset) {
      $http.post("rest/screens/input/"+id+"/move",offset).then(function() {
        reload();
      });
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
      backdrop: 'static',
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
      
      function retrieveInputs() {
        var params =  _.clone($scope.stModel);
        ScreenTemplates.getScreenInputsByScreenId($scope.stScreen, params).then(function(attributes) {
          var newAttributes=_.reject(attributes, function(attribute) {
            return $scope.stExcludeFields!=null && $scope.stExcludeFields.indexOf(attribute.id) >= 0;
          });
          if($scope.attributes) {
            // If an input is deactivated due to its "activationExpression" returning false,
            // the corresponding bean property has to be deleted
            _.each($scope.attributes, function(input) {
              if(!_.find(newAttributes, function(i) {
                return i.id == input.id
              })) {
                eval('delete $scope.stModel.'+input.id)
              }
            })
          }
          // Reload attributes only if necessary to avoid focus lost on forms
          if(!angular.equals($scope.attributes, newAttributes)) {
            $scope.attributes = newAttributes;
          }
        })
      }
      
      $scope.$watch('stModel', function(value) {
        if(value) {
          retrieveInputs();
        }
      })

      $scope.saveAttributes = function() {
      	// Retrieve the inputs again to force the reevaluation of the activationExpressions of the inputs
        retrieveInputs();
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
        if($scope.stBean) {
          return ScreenTemplates.getScreenInputModel($scope.stBean, $scope.input);
        } else {
          return function(value) {
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
.directive('stCustomFormInputdropdown', function(ScreenTemplates, $compile, $http) {
  return {
    restrict: 'E',
    scope: {
      options:'=?',action:'=?',initialValue:'=?',handle:'=?',
      stSearchDisable: '=?',
      stBean: '=?',
      stInput: '=?',
    },
    templateUrl: 'partials/screenconfiguration/customFormInputDropdown.html',
    link: function($scope, $element, $attrs, $http) {

    },
    controller: function ctrl($scope, $compile) {
      $scope.input = $scope.stInput
      $scope.model = ScreenTemplates.getScreenInputModel($scope.stBean, $scope.input);
      $scope.actionWrapper = function (value){
        if ($scope.input.searchMapperService && value) {
          $http.post($scope.input.searchMapperService,value).then(function(response) {
            $scope.action(createRegexp(response.data));
          });
        } else {
          $scope.action(value);
        }
        
      }
      
      createRegexp = function(selection) {
        regexp = '';
        if(selection.length>1) {
          regexp = '(';
          _.each(selection,function(value){regexp+=value+'|'});
          regexp=regexp.slice(0, -1)+')';
        } else if (selection.length==1){
          regexp=selection[0];
        } else {
          regexp="no mapping found";
        }
        return regexp;
      }
    }
  }
})
.directive('stCustomFormValue', function(ScreenTemplates, $compile) {
  return {
    restrict: 'E',
    scope: {
      stBean: '=?',
      stInput: '=?',
      stOptions: '=?'
    },
    templateUrl: 'partials/screenconfiguration/customFormValue.html',
    link: function($scope, $element, $attrs) {
      if($scope.input.valueHtmlTemplate) {
        var template = $compile($scope.input.valueHtmlTemplate)($scope)
        $element.replaceWith(template)
      }
    },
    controller: function ctrl($scope, $compile) {
      $scope.input = $scope.stInput
      $scope.model = ScreenTemplates.getScreenInputModel($scope.stBean, $scope.input);
    }
  }
})
.directive('stCustomColumns', function(ScreenTemplates) {
  return {
    restrict: 'E',
    scope: {
      stScreen: '@',
      stExcludeFields: '=?',
      stOptions: '=?',
      stSearchDisable: '=?'
    },
    templateUrl: 'partials/screenconfiguration/customColumns.html',
    controller: function($scope) {
      ScreenTemplates.getScreenInputsByScreenId($scope.stScreen).then(function(attributes) {
        $scope.attributes=_.reject(attributes, function(attribute) {
          return $scope.stExcludeFields!=null && $scope.stExcludeFields.indexOf(attribute.id) >= 0;
        });
      })
    }
  }
})