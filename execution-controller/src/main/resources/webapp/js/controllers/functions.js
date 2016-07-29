angular.module('functionsControllers',['dataTable','step'])

.controller('FunctionListCtrl', [ '$scope', '$compile', '$http', 'stateStorage', '$interval', '$modal',
    function($scope, $compile, $http, $stateStorage, $interval, $modal) {
      $stateStorage.push($scope, 'functions', {});	

      $scope.autorefresh = true;


      
      $scope.addFunction = function() {
    	  var modalInstance = $modal.open({
              animation: $scope.animationsEnabled,
              templateUrl: 'newFunctionModalContent.html',
              controller: 'newFunctionModalCtrl',
              resolve: {
                items: function () {
                  return $scope.items;
                }
              }
            });

            modalInstance.result.then(function (functionParams) {
                $http.post("rest/functions",functionParams).success(function() {
                  
                	if($scope.table) {
               			$scope.table.Datatable.ajax.reload(null, false);
            		}
                });

            }, function () {});
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
            return '<a href="#/root/executions/' + row[0] + '">' + data + '</a>'
          };
        });
        _.each(_.where(columns, { 'title' : 'Type' }), function(col) {
          col.render = function(data, type, row) {
        	var function_ = JSON.parse(row[row.length-1]);
        	if(data.indexOf('class:step.core.tokenhandlers.ArtefactMessageHandler')!=-1) {
        	  if(function_.handlerProperties) {
        		return '<a href="#/root/artefacteditor/' + function_.handlerProperties['artefactid'] + '">STEP-Flow</a>'
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
            col.width="100px";
            col.render = function ( data, type, row ) {
            	var html = '<div class="input-group">' +
	            	'<div class="btn-group">' +
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
    
.controller('newFunctionModalCtrl', function ($scope, $modalInstance, $http) {

  $scope.model = {};
  $scope.type = "STEP-Flow";
  $scope.handler = '';
  
  
  $http.get("rest/screens/functionTable").success(function(data){
	$scope.inputs=data;
  });	
	
  $scope.ok = function () {
	var function_ = {"attributes":{}};
	
	function close() {
	  $modalInstance.close(function_);
	}
	
	_.mapObject($scope.model,function(value,key) {
	  eval('function_.'+key+"='"+value+"'");
	  if($scope.type=='STEP-Flow') {
		var newArtefact = {"name":function_.attributes.name,"_class":"step.artefacts.Sequence"};
  	  	$http.post("rest/controller/artefact",newArtefact).success(function(artefact){
  	  	  function_.handlerChain = "class:step.core.tokenhandlers.ArtefactMessageHandler";
  	  	  function_.handlerProperties = {"artefactid":artefact.id}
  	  	  close();
  	  	})
	  } else {
		function_.handlerChain = $scope.handler;
		close();
	  }
	});
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