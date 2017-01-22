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
var schedulerController = angular.module('schedulerControllers',['dataTable']);

schedulerController.controller('SchedulerCtrl', ['$scope', '$http','stateStorage', '$uibModal', 
  function($scope, $http,$stateStorage, $uibModal) {
    $stateStorage.push($scope, 'scheduler', {});
    
	$scope.datatable = {}

	var actionColRender = function ( data, type, row ) {
    	var html = '<div class="input-group">' +
        	'<div class="btn-group">' +
        	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#SchedulerCtrl\').scope().editTask(\''+row[0]+'\')">' +
        	'<span class="glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>' +
        	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#SchedulerCtrl\').scope().deleteTask(\''+row[0]+'\',true)">' +
        	'<span class="glyphicon glyphicon glyphicon glyphicon-trash" aria-hidden="true"></span>' +
        	'</button> ' +
        	'</div>' +
        	'</div>';
    	return html;
    }
	
	var statusColRender = function ( data, type, row ) {
	  var html = '<button type="button" class="btn btn-primary" onclick="angular.element(\'#SchedulerCtrl\').scope().toggleTaskState(\''+row[0]+'\','+data+')" aria-pressed="false" autocomplete="off">'+(data?'On':'Off')+'</button>';
    return html;
	}
	
	$scope.tabledef = {}
	$scope.tabledef.columns = [ {"title" : "ID", "visible": false}, 
	                   {"title" : "cronExpression"}, 
	                   {"title" : "Description"},
	                   {"title" : "Actions", "render" : actionColRender},
	                   {"title" : "Status", "render" : statusColRender}];
	
	$scope.tabledef.actions = [{"label":"Enable","action":function() {$scope.enableSelected()}},
                             {"label":"Disable","action":function() {$scope.deleteSelected(false)}},
                             {"label":"Remove","action":function() {$scope.deleteSelected(true)}}];
	
	$scope.loadTable = function loadTable() {	
		$http.get("rest/controller/task").then(function(response) {
		  var data = response.data;
			var dataSet = [];
			for (i = 0; i < data.length; i++) {
				dataSet[i] = [data[i]._id,
				              data[i].cronExpression,
				              data[i].name,
				              data[i]._id,
				              data[i].active];
			}
			$scope.tabledef.data = dataSet;
			
		});
	};
	
	$scope.enableSelected = function(remove) {
      var rows = $scope.datatable.getSelection().selectedItems;
      
      for(i=0;i<rows.length;i++) {
    	  $scope.enableTask(rows[i][0]);       
      }
    };
  
  $scope.toggleTaskState = function(id, currentState) {
    if(currentState) {
      $scope.deleteTask(id, false);
    } else {
      $scope.enableTask(id);
    }
  }
    
  $scope.enableTask = function(id) {
  	$http.put("rest/controller/task/"+id).then(function() {
          $scope.loadTable();
      });
  }

	$scope.deleteSelected = function(remove) {
		var rows = $scope.datatable.getSelection().selectedItems;
		
		for(i=0;i<rows.length;i++) {
			$scope.deleteTask(rows[i][0], remove);		
		}
	};
	
	$scope.editTask = function(id) {
	  $http.get("rest/controller/task/"+id).then(function(task){
	    var task = response.task;
      var modalInstance = $uibModal.open({
        animation: $scope.animationsEnabled,
        templateUrl: 'editSchedulerTaskModalContent.html',
        controller: 'editSchedulerTaskModalCtrl',
        resolve: {
          task: function () {
          return task;
          }
        }
        });
        
      modalInstance.result.then(function (functionParams) {
        $scope.loadTable()}, 
      function (task) {});
    }); 
	}
	
	$scope.deleteTask = function(id, remove) {
		$http.delete("rest/controller/task/"+id+"?remove="+remove).then(function() {
			$scope.loadTable();
		});		
	}
	
	$scope.loadTable($scope,$http);
  }]);

schedulerController.controller('editSchedulerTaskModalCtrl', function ($scope, $uibModalInstance, $http, $location, task) {
	  
  $scope.task = task;

  $scope.executionsParameters = function(value) {
    if(arguments.length) {
      $scope.task.executionsParameters = JSON.parse(value);
      return value;
    } else {
      return JSON.stringify($scope.task.executionsParameters);
    }
  }
  
  $scope.save = function () {  
    $http.post("rest/controller/task",$scope.task).then(
      function(response) {
        $uibModalInstance.close(response.data);
      },function(error) {
        $scope.error = "Invalid CRON expression or server error.";
      });
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
});