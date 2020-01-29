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
angular.module('functionsControllers',['dataTable','step'])

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
  FunctionTypeRegistry.register('step.plugins.functions.types.CompositeFunction','Composite','partials/functions/forms/composite.html');
})

.factory('FunctionDialogs', function ($rootScope, $uibModal, $http, Dialogs, $location) {
  
  function openModal(function_) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
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
        $http.post("rest/interactive/functiontest/"+id+"/start").then(function(response) {
          var result = response.data;
          $rootScope.planEditorInitialState = {
              interactive : true,
              selectedNode : result.callFunctionId
          }
          $location.path('/root/plans/editor/' + result.planId);
        });
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

      $scope.tabledef = {uid:'functions'}
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {
          col.render = function(data, type, row) {
            if(AuthService.hasRight('kw-write')) {
              return '<a href="#" title="Edit keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().openFunctionEditor(\''+row[0]+'\');return false;">' + data + '</a>'
            } else {
              return data
            }
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
              	html+='<button type="button" class="btn btn-default" aria-label="Left Align" title="Configure keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().editFunction(\''+row[0]+'\')">' +
  	            	'<span class="glyphicon glyphicon glyphicon glyphicon-wrench" aria-hidden="true"></span>'+
              	  '</button> ';
            	  html+= '<button type="button" class="btn btn-default" aria-label="Left Align" title="Edit keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().openFunctionEditor(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>'+
                '</button> ';
            	}
            	
            	if(AuthService.hasRight('kw-execute')) {
              	html+= '<button type="button" class="btn btn-default" aria-label="Left Align" title="Execute keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().executeFunction(\''+row[0]+'\')">' +
  	            	'<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>' +
  	            	'</button> ';
            	}
            	
              if(AuthService.hasRight('kw-write')) {
                html+='<button type="button" class="btn btn-default" aria-label="Left Align" title="Copy keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().copyFunction(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon-copy" aria-hidden="true"></span>' +
                '</button> ';
              }
            	
            	if(AuthService.hasRight('kw-delete')) {
              	html+= '<button type="button" class="btn btn-default" aria-label="Left Align" title="Delete keyword" onclick="angular.element(\'#FunctionListCtrl\').scope().deleteFunction(\''+row[0]+'\')">' +
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
  
  $scope.isSchemaEnforced = AuthService.getConf().miscParams.enforceschemas;
  
  $scope.addRoutingCriteria = function() {
    $scope.criteria.push({key:"",value:""});
  }
  
  $scope.removeRoutingCriteria = function(key) {
    delete $scope.function_.tokenSelectionCriteria[key];
    loadTokenSelectionCriteria($scope.function_);
  }
  
  $scope.saveRoutingCriteria = function() {
    var tokenSelectionCriteria = {}
    _.each($scope.criteria, function(entry) {
      tokenSelectionCriteria[entry.key]=entry.value;
    });
    $scope.function_.tokenSelectionCriteria = tokenSelectionCriteria;
  }
  
  $http.get("rest/screens/functionTable").then(function(response){
    $scope.inputs=response.data;
  });
  
  var loadTokenSelectionCriteria = function(function_) {
    $scope.criteria = [];
    if(function_ && function_.tokenSelectionCriteria) {
      _.mapObject(function_.tokenSelectionCriteria,function(val,key) {
        $scope.criteria.push({key:key,value:val});
      });
    }
  }
  
  loadTokenSelectionCriteria(function_);
  
  $scope.loadInitialFunction = function() {
    $http.get("rest/functions/types/"+$scope.function_.type).then(function(response){
      var initialFunction = response.data;
      if($scope.function_) {
        initialFunction.id = $scope.function_.id;
        if($scope.function_.attributes) {
          initialFunction.attributes = $scope.function_.attributes;
        }
        if($scope.function_.schema) {
          initialFunction.schema = $scope.function_.schema;
        }
      }
      
      loadTokenSelectionCriteria(initialFunction);
      
      $scope.function_ = initialFunction;
      $scope.schemaStr = JSON.stringify($scope.function_.schema);
    })  
  }
  
  if(newFunction) {
    $scope.function_ = {type:'step.plugins.java.GeneralScriptFunction'}
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
.controller('CompositeFunctionFormCtrl' , function($scope,PlanDialogs,$location,$http) {  
  $scope.gotoArtefact = function() {
    $scope.save(false);
    $location.path('/root/artefacteditor/' + $scope.function_.artefactId);
  }
  
  $scope.selectPlan = function() {
    PlanDialogs.selectPlan(function(plan) {
      $scope.function_.planId = plan.id;
    })
  }
})
