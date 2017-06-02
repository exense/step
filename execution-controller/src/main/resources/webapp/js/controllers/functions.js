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
angular.module('functionsControllers',['dataTable','step','schemaForm'])

.factory('FunctionTypeRegistry', function() {
  
  var registry = {};
  
  var api = {};
  
  api.register = function(typeName,label,form) {
    registry[typeName] = {"label":label,"form":form};
  }
  
  api.getForm = function(typeName) {
    return registry[typeName].form;
  }
  
  api.getLabel = function(typeName) {
    return registry[typeName]?registry[typeName].label:"Unknown";
  }
  
  api.getTypes = function() {
    return _.keys(registry);
  }
  
  return api;
})

.run(function(FunctionTypeRegistry) {
  FunctionTypeRegistry.register('step.plugins.functions.types.GeneralScriptFunction','Script (Java, JS, Groovy, etc)','partials/functions/forms/script.html');
  FunctionTypeRegistry.register('step.plugins.functions.types.CompositeFunction','Composite','partials/functions/forms/composite.html');
  FunctionTypeRegistry.register('step.plugins.functions.types.GrinderFunction','Grinder','partials/functions/forms/grinder.html');
})

.factory('FunctionDialogs', function ($rootScope, $uibModal, $http, Dialogs, $location) {
  
  function openModal(function_) {
    var modalInstance = $uibModal.open({
        templateUrl: 'partials/functions/functionConfigurationDialog.html',
        controller: 'newFunctionModalCtrl',
        resolve: {function_: function () {return function_;}}
      });

      return modalInstance.result;
  }
  
  var dialogs = {};
  
  dialogs.editFunction = function(id, callback) {
    $http.get("rest/functions/"+id).then(function(response) {
      openModal(response.data).then(function() {
        if(callback){callback()};
      })
    });
  }
  
  dialogs.addFunction = function(callback) {
    openModal().then(function() {
      if(callback){callback()};
    })
  }
  
  dialogs.openFunctionEditor = function(functionid) {
    $http.get("rest/functions/"+functionid+"/editor").then(function(response){
      var path = response.data
      if(path) {
        $location.path(path);              
      } else {
        Dialogs.showErrorMsg("No editor configured for this function type");
      }
    })
  }
  
  return dialogs;
})

.controller('FunctionListCtrl', [ '$scope', '$rootScope', '$compile', '$http', 'stateStorage', '$interval', '$uibModal', 'Dialogs', 'FunctionDialogs', '$location','AuthService','FunctionTypeRegistry',
    function($scope, $rootScope, $compile, $http, $stateStorage, $interval, $uibModal, Dialogs, FunctionDialogs, $location, AuthService, FunctionTypeRegistry) {
      $stateStorage.push($scope, 'functions', {});	

      $scope.authService = AuthService;
      
      function reload() {
        $scope.table.Datatable.ajax.reload(null, false);
      }

      $scope.editFunction = function(id) {
        FunctionDialogs.editFunction(id, function() {reload()});
      }
      
      $scope.copyFunction = function(id) {
        $rootScope.clipboard = {object:"function",id:id};
      }
      
      $scope.pasteFunction = function() {
        if($rootScope.clipboard && $rootScope.clipboard.object=="function") {
          $http.post("rest/functions/"+$rootScope.clipboard.id+"/copy")
          .then(function() {
            reload();
          });
        }
      }
      
      $scope.openFunctionEditor = function(functionid) {
        $scope.$apply(function() {
          FunctionDialogs.openFunctionEditor(functionid);
        })
      }
      
      $scope.addFunction = function() {
        FunctionDialogs.addFunction(function() {reload()});
      }
      
      $scope.executeFunction = function(id) {
  	  var modalInstance = $uibModal.open({
            animation: $scope.animationsEnabled,
            templateUrl: 'executeFunctionModalContent.html',
            controller: 'executeFunctionModalCtrl',
            resolve: {
              functionId: function () {
                return id;
              }
            }
          });

          modalInstance.result.then(function (argument) {}, function () {});
      }
      
      $scope.deleteFunction = function(id) {
        Dialogs.showDeleteWarning().then(function() {
          $http.delete("rest/functions/"+id).then(function() {
            if($scope.table) {
              reload();
          }
          });
        })
      }
      
      $scope.table = {};

      $scope.tabledef = {}
      $scope.tabledef.actions = [{"label":"Paste","action":function() {$scope.pasteFunction()}}];
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {
          col.render = function(data, type, row) {
            return data
          };
        });
        _.each(_.where(columns, { 'title' : 'Type' }), function(col) {
          col.render = function(data, type, row) {
            return FunctionTypeRegistry.getLabel(data);
          };
        });
        _.each(_.where(columns,{'title':'Actions'}),function(col){
            col.title="Actions";
            col.searchmode="none";
            col.width="200px";
            col.render = function ( data, type, row ) {
            	var html = '<div class="input-group"><div class="btn-group">';
            	
            	if(AuthService.hasRight('kw-write')) {
              	html+='<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().editFunction(\''+row[0]+'\')">' +
  	            	'<span class="glyphicon glyphicon glyphicon glyphicon-wrench" aria-hidden="true"></span>'+
              	  '</button> ';
            	  html+= '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().openFunctionEditor(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>'+
                '</button> ';
            	}
            	
            	if(AuthService.hasRight('kw-execute')) {
              	html+= '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().executeFunction(\''+row[0]+'\')">' +
  	            	'<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>' +
  	            	'</button> ';
            	}
            	
              if(AuthService.hasRight('kw-write')) {
                html+='<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().copyFunction(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon-copy" aria-hidden="true"></span>' +
                '</button> ';
              }
            	
            	if(AuthService.hasRight('kw-delete')) {
              	html+= '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().deleteFunction(\''+row[0]+'\')">' +
  	            	'<span class="glyphicon glyphicon glyphicon glyphicon-trash" aria-hidden="true"></span>' +
  	            	'</button> ';
            	}
            	
            	html+='</div></div>';
            	
            	return html;
            }
           });
        return columns;
      };
    }])

.controller('newFunctionModalCtrl', [ '$rootScope', '$scope', '$uibModalInstance', '$http', '$location', 'function_', 'Dialogs', 'AuthService','FunctionTypeRegistry',
function ($rootScope, $scope, $uibModalInstance, $http, $location, function_,Dialogs, AuthService, FunctionTypeRegistry) {
  $scope.functionTypeRegistry = FunctionTypeRegistry;
  
  var newFunction = function_==null;
  $scope.mode = newFunction?"add":"edit";
  
  $scope.isSchemaEnforced = AuthService.getConf().miscParams.enforceSchemas;
  
  $scope.loadInitialFunction = function() {
    $http.get("rest/functions/types/"+$scope.function_.type).then(function(response){
      var initialFunction = response.data;
      if($scope.function_) {
        if($scope.function_.attributes) {
          initialFunction.attributes = $scope.function_.attributes;
        }
        if($scope.function_.schema) {
          initialFunction.schema = $scope.function_.schema;
        }
      }
      $scope.function_ = initialFunction;
      $scope.schemaStr = JSON.stringify($scope.function_.schema);
    })  
  }
  
  if(newFunction) {
    $scope.function_ = {type:'step.plugins.functions.types.GeneralScriptFunction'}
    $scope.loadInitialFunction();
  } else {
    $scope.function_ = function_;
    $scope.schemaStr = JSON.stringify($scope.function_.schema);
  }

  $scope.save = function (editAfterSave) {

	var schemaJson;
	try {
  	schemaJson = JSON.parse($scope.schemaStr);
  	$scope.function_.schema = schemaJson;
  	
  	$http.post("rest/functions",$scope.function_).then(function(response) {
  	  var function_ = response.data;
  	  $uibModalInstance.close(response.data);

  	  if(editAfterSave) {
  	    $http.get("rest/functions/"+function_.id+"/editor").then(function(response){
  	      var path = response.data;
  	      if(path) {
  	        $location.path(path);
  	      } else {
  	        Dialogs.showErrorMsg("No editor configured for this function type");
  	      }
  	    })     
  	  }
  	})

  	}catch(e){
  		Dialogs.showErrorMsg("incorrect schema format (must be Json) : " + e);
  	} 		
   };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
}])

.controller('executeFunctionModalCtrl', function ($scope, $uibModalInstance, $http, functionId) {
  
  $scope.functionId = functionId;
	$scope.handle = {}
  
  $scope.ok = function () {
	  $scope.handle.execute();
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.directive('functionExecutionPanel', function($http) {
  return {
    restrict: 'E',
    scope: {
      functionid: '=',
      handle: '='
    },
    templateUrl: 'partials/functionExecutionPanel.html',
    controller: function($scope,AuthService) {
      $scope.argument = '';
      $scope.running = false;
      
      $scope.setArgument = function(json) {
        $scope.argument = json;
      }
      
      $scope.properties = [];
      
      $scope.addProperty = function() {
        $scope.properties.push({key:"",value:""})
      }
      
      $scope.execute = function () {
        $scope.running=true;
        var properties = {};
        _.each($scope.properties,function(prop){properties[prop.key]=prop.value});
        $http.post("rest/functions/"+$scope.functionid+"/execute",{'properties':properties,'argument':$scope.argument}).then(function(response) {
          var data = response.data;
          $scope.output = data;
          $scope.running=false;
          if(data) {
            var output = data.output;
            if(output.result) {
              $scope.result = JSON.stringify(output.result)       
            }
            $scope.attachments=data.attachments;
            $scope.error = output.error
          }
      },function(error) {
        $scope.running=false;
        $scope.error=error;
      });
      };
      
      if($scope.handle) {
        $scope.handle.execute = $scope.execute;
      }
    }
   }})
   
 .controller('selectFunctionModalCtrl', function ($scope, $uibModalInstance) {
  
  $scope.selectFunction = function(id) {
    $uibModalInstance.close(id);
  }
  
  $scope.table = {};

  $scope.tabledef = {}      
  
  $scope.tabledef.columns = function(columns) {
    _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns,{'title':'Actions'}),function(col){
        col.title="Actions";
        col.searchmode="none";
        col.width="160px";
        col.render = function ( data, type, row ) {
          var html = '<div class="input-group">' +
            '<div class="btn-group">' +
            '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().selectFunction(\''+row[0]+'\')">' +
            '<span class="glyphicon glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>';
          html+='</div></div>';
          return html;
        }
       });
    return columns;
  };


  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})
.controller('CompositeFunctionFormCtrl' , function($scope,$uibModal,$location,$http) {  
  $scope.gotoArtefact = function() {
    $scope.save(false);
    $location.path('/root/artefacteditor/' + $scope.function_.artefactId);
  }
  
  $scope.selectArtefact = function() {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/selectArtefact.html',
      controller: 'selectArtefactModalCtrl',
      resolve: {}
    });

    modalInstance.result.then(function (artefact) {
      $scope.function_.artefactId = artefact.id;
    });
  }
})
