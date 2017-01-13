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

.run(function($http, $rootScope) {
  function loadTypes() {
    $http.get("rest/functions/types").success(function(data){
      $rootScope.functionTypes = data;
    });  
  }
  
  loadTypes();
  
  $rootScope.$on('step.login.succeeded',function() {
    loadTypes();    
  })
})

.controller('FunctionListCtrl', [ '$scope', '$rootScope', '$compile', '$http', 'stateStorage', '$interval', '$modal', 'Dialogs', '$location','AuthService',
    function($scope, $rootScope, $compile, $http, $stateStorage, $interval, $modal, Dialogs, $location, AuthService) {
      $stateStorage.push($scope, 'functions', {});	

      $scope.authService = AuthService;

      function openModal(function_) {
    	  var modalInstance = $modal.open({
            templateUrl: 'newFunctionModalContent.html',
            controller: 'newFunctionModalCtrl',
            resolve: {function_: function () {return function_;}}
          });
  
          modalInstance.result.then(function (functionParams) {
            	if($scope.table) {
            	  $scope.table.Datatable.ajax.reload(null, false);
            	}
          }, function () {});
      }
      
      function reload() {
        $scope.table.Datatable.ajax.reload(null, false);
      }

      $scope.editFunction = function(id) {
      	$http.get("rest/functions/"+id).success(function(function_) {
      	  openModal(function_);
      	});
      }
      
      $scope.copyFunction = function(id) {
        $rootScope.clipboard = {object:"function",id:id};
      }
      
      $scope.pasteFunction = function() {
        if($rootScope.clipboard && $rootScope.clipboard.object=="function") {
          $http.post("rest/functions/"+$rootScope.clipboard.id+"/copy")
          .success(function() {
            reload();
          });
        }
      }
      
      $scope.openFunctionEditor = function(functionid) {
        $scope.$apply(function() {
          $http.get("rest/functions/"+functionid+"/editor").success(function(path){
            if(path) {
              $location.path(path);              
            } else {
              Dialogs.showErrorMsg("No editor configured for this function type");
            }
          })
        })
      }
      
      $scope.addFunction = function() {
        openModal();
      }
      
      $scope.executeFunction = function(id) {
  	  var modalInstance = $modal.open({
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
          $http.delete("rest/functions/"+id).success(function() {
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
//        _.each(_.where(columns, { 'title' : 'Type' }), function(col) {
//          col.render = function(data, type, row) {
//        	var function_ = JSON.parse(row[row.length-1]);
//        	if(data.indexOf('class:step.core.tokenhandlers.ArtefactMessageHandler')!=-1) {
//        	  if(function_.handlerProperties) {
//        	    if(AuthService.hasRight('kw-write')) { 
//        	      return '<a href="#/root/artefacteditor/' + function_.configuration.artefactId + '">Composite</a>'        	      
//        	    } else {
//        	      return 'Composite';
//        	    }
//        	  } else {
//        		return 'Unknown';
//        	  }
//        	} else {
//        	  return 'Handler'        	  
//        	}
//          };
//        });
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
    } ])
    
.controller('newFunctionModalCtrl', function ($rootScope, $scope, $modalInstance, $http, $location, function_) {

  var newFunction = function_==null;
  $scope.mode = newFunction?"add":"edit";
  
  $scope.getFunctionAttributes = function() {
  	return _.keys($scope.function_.attributes);
  }
  
  $scope.functionTypes = $rootScope.functionTypes;
  
  if(newFunction) {
  	$scope.function_= {"attributes":{},"type":"script"};
  	$http.get("rest/screens/functionTable").success(function(data){
  	  _.each(data,function(input) {
  	    eval('$scope.function_.'+input.id+"=''");
  	  })
  	});	
  } else {
    $scope.function_=function_;
  } 
  
  $scope.$watch('function_.type',function(functionType,oldFunctionType){
    if(functionType!=oldFunctionType) {
      $http.get("rest/functions/types/"+functionType).success(function(template){
        $scope.function_.configuration = template;
      }) 
    }
  });
  
  $scope.save = function (editAfterSave) {
    $http.post("rest/functions",$scope.function_).success(function(function_) {
      $modalInstance.close(function_);
  	
    	if(editAfterSave) {
    	  $http.get("rest/functions/"+function_.id+"/editor").success(function(path){
    	    if(path) {
    	      $location.path(path);
    	    } else {
    	      Dialogs.showErrorMsg("No editor configured for this function type");
    	    }
        })     
    	}
    })
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
})

.controller('executeFunctionModalCtrl', function ($scope, $modalInstance, $http, functionId) {

  $scope.argument = '';
	$scope.running = false;
  
	$scope.properties = [];
	
	$scope.addProperty = function() {
	  $scope.properties.push({key:"",value:""})
	}
	
  $scope.ok = function () {
    $scope.running=true;
    var properties = {};
    _.each($scope.properties,function(prop){properties[prop.key]=prop.value});
    $http.post("rest/functions/"+functionId+"/execute",{'properties':properties,'argument':$scope.argument}).success(function(data) {
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
	}).error(function(error) {
	  $scope.running=false;
	  $scope.error=error;
	});
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
});