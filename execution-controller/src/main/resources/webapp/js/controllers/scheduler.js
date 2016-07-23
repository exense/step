var schedulerController = angular.module('schedulerControllers',['dataTable']);

schedulerController.controller('SchedulerCtrl', ['$scope', '$http','stateStorage',
  function($scope, $http,$stateStorage) {
    $stateStorage.push($scope, 'scheduler', {});
    
	console.log('Entering SchedulerCtrl');
	
	$scope.datatable = {}
	
	$scope.tabledef = {}
	$scope.tabledef.columns = [ {"title" : "ID", "visible": false}, 
	                   {"title" : "cronExpression"}, 
	                   {"title" : "executionsParameters"},
	                   {"title" : "Status"}];
	
	$scope.loadTable = function loadTable() {	
		$http.get("rest/controller/task").success(function(data) {
			var dataSet = [];
			for (i = 0; i < data.length; i++) {
				dataSet[i] = [data[i]._id,
				              data[i].cronExpression,
				              JSON.stringify(data[i].executionsParameters),
				              data[i].active];
			}
			$scope.tabledef.data = dataSet;
			
		});
	};
	
	$scope.enableSelected = function(remove) {
      var rows = $scope.datatable.getSelection().selectedItems;
      
      for(i=0;i<rows.length;i++) {
          $http.put("rest/controller/task/"+rows[i][0]).success(function(data) {
              $scope.loadTable();
          });         
      }
  };

	$scope.deleteSelected = function(remove) {
		var rows = $scope.datatable.getSelection().selectedItems;
		
		for(i=0;i<rows.length;i++) {
			$http.delete("rest/controller/task/"+rows[i][0]+"?remove="+remove).success(function(data) {
				$scope.loadTable();
			});			
		}
	};
	
	$scope.loadTable($scope,$http);
  }]);