angular.module('functionsControllers',['dataTable','step'])

.controller('FunctionListCtrl', [ '$scope', '$compile', '$http', 'stateStorage', '$interval', '$modal',
    function($scope, $compile, $http, $stateStorage, $interval, $modal) {
      $stateStorage.push($scope, 'functions', {});	

      $scope.autorefresh = true;

      function openModal(function_) {
    	  var modalInstance = $modal.open({
            templateUrl: 'newFunctionModalContent.html',
            controller: 'newFunctionModalCtrl',
            resolve: {function_: function () {return function_;}}
          });
  
          modalInstance.result.then(function (functionParams) {
              $http.post("rest/functions",functionParams).success(function() {
              	if($scope.table) {
             		$scope.table.Datatable.ajax.reload(null, false);
          		}
              });
  
          }, function () {});
      }

      $scope.editFunction = function(id) {
    	$http.get("rest/functions/"+id).success(function(function_) {
    	  openModal(function_);
    	});
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

          modalInstance.result.then(function (argument) {
              $http.post("rest/functions/"+id+"/execute",argument).success(function() {
              		if($scope.table) {
              			$scope.table.Datatable.ajax.reload(null, false);
          			}
              });

          }, function () {});
      }
      
      $scope.deleteFunction = function(id) {
    	  $http.delete("rest/functions/"+id).success(function() {
      		if($scope.table) {
      			$scope.table.Datatable.ajax.reload(null, false);
  			}
      });
      }
      
      $scope.table = {};

      $scope.tabledef = {}
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
        	var function_ = JSON.parse(row[row.length-1]);
        	if(data.indexOf('class:step.core.tokenhandlers.ArtefactMessageHandler')!=-1) {
        	  if(function_.handlerProperties) {
        		return '<a href="#/root/artefacteditor/' + function_.handlerProperties['artefactid'] + '">Composite</a>'
        	  } else {
        		return 'Unknown';
        	  }
        	} else {
        	  return 'Handler'        	  
        	}
          };
        });
        _.each(_.where(columns,{'title':'Actions'}),function(col){
            col.title="Actions";
            col.searchmode="none";
            col.width="130px";
            col.render = function ( data, type, row ) {
            	var html = '<div class="input-group">' +
	            	'<div class="btn-group">' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().editFunction(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().executeFunction(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().deleteFunction(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-trash" aria-hidden="true"></span>' +
	            	'</button> ' +
	            	'</div>' +
	            	'</div>';
            	return html;
            }
           });
        return columns;
      };
    } ])
    
.controller('newFunctionModalCtrl', function ($scope, $modalInstance, $http, $location, function_) {

  var newFunction = function_==null;
  $scope.mode = newFunction?"add":"edit";
  
  $scope.getFunctionAttributes = function() {
	return _.keys($scope.function_.attributes); 
  }
  
  $scope.type = function(value) {
	if(value) {
	  $scope.function_.handlerChain = (value=="Composite")?"class:step.core.tokenhandlers.ArtefactMessageHandler":"";
	}
	return  ($scope.function_.handlerChain&&$scope.function_.handlerChain.indexOf("ArtefactMessageHandler")!=-1)?"Composite":"Handler";
  }
  
  if(newFunction) {
	$scope.function_= {"attributes":{}};
	$http.get("rest/screens/functionTable").success(function(data){
	  _.each(data,function(input) {
		eval('$scope.function_.'+input.id+"=''");
	  })
	});	
  } else {
	$scope.function_=function_;	
  } 
  
  $scope.save = function (editAfterSave) {	
	function close() {
	  $modalInstance.close($scope.function_);
	}
	
	if($scope.type()=='Composite') {
  	  function closeAndEdit() {
  		close();
  		if(editAfterSave) {
  		  $location.path('/root/artefacteditor/' + $scope.function_.handlerProperties['artefactid'])
  		}
  	  }
	
  	  if(newFunction || !($scope.function_.handlerProperties && $scope.function_.handlerProperties.artefactid)  ) {
  		var newArtefact = {"name":$scope.function_.attributes.name,"_class":"step.artefacts.Sequence"};
  		$http.post("rest/controller/artefact",newArtefact).success(function(artefact){
			$scope.function_.handlerProperties = {"artefactid":artefact.id}
			closeAndEdit();
		})		  
  	  } else {
  		closeAndEdit();
	  }
	} else {
		close();
	}
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
})

.controller('executeFunctionModalCtrl', function ($scope, $modalInstance, $http, functionId) {

  $scope.argument = '';
	
  $scope.ok = function () {
	$http.post("rest/functions/"+functionId+"/execute",$scope.argument).success(function(data) {
		$scope.output = data;
		if(data) {
		  $scope.result = JSON.stringify(data.result)
		  $scope.error = data.error
		}
	});
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
});