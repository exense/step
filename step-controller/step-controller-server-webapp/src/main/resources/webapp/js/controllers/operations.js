/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
angular.module('operationsControllers',['step'])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('operations','partials/operations/operationsList.html');
  ViewRegistry.registerCustomMenuEntry('Current Operations','operations', null, null, 'admin');
})

.controller('OperationListCtrl', function($rootScope, $scope, $http, stateStorage, Dialogs, ResourceDialogs, AuthService) {
    $scope.authService = AuthService;
    $scope.tableHandle = {};
    $scope.operationList = [];
    $scope.showExec = {}
    
    $scope.loadOperationsData = function () {
      $http.get("rest/threadmanager/operations/list")
      .then(function(response) {
        $scope.operationList = response.data;
      });
    }
    
    $scope.loadOperationsData();
    
    var refresh = function() {
      $scope.loadOperationsData()
    }
    $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
    
  })

.directive('currentOperations', function($http) {
  return {
    restrict: 'E',
    scope: {
      reportNodeId: '=',
      operationOptions: '=',
      executionViewServices: '='
    },
    controller: function($scope) {
      $http.get("rest/threadmanager/operations/"+$scope.reportNodeId).then(function(response) {
        $scope.currentOperation = response.data;
        
      });
    },
      
    templateUrl: 'partials/operations/currentOperations.html'}
})


.directive('operation', function() {
  return {
    restrict: 'E',
    scope: {
      operation: '=',
      executionViewServices: '=',
      showOnlyDetails: '=?'
    },
    controller: function($scope) {
    	
    	var templates = {
    			'Keyword Call':'keywordCallOperation.html',
    			'Quota acquisition':'quotaAcquisitionOperation.html',
    		//	'Sleep':'sleepOperation.html',
    			'Token selection':'tokenSelection.html',
    			'Waiting for lock':'waitingForLock.html',
    			'Waiting for global lock':'waitingForLock.html',
    			'default':'defaultOperation.html'
    	}
    	
    	var getTemplate = function(name) {
    		return (templates[name]) ? templates[name] : templates['default']; 
    	}
    	//required as key starting with $ are skipped in ng-repeat
    	if ($scope.operation.details["$agenttype"]) {
    	  var prop=$scope.operation.details["$agenttype"];
    	  delete $scope.operation.details["$agenttype"];
    	  $scope.operation.details[" $agenttype"]=prop;
    	}
    	/*var opDetails = {};
    	for (key in $scope.operation.details){
    	  var prop=$scope.operation.details[key];
    	  var newKey = key.replace("\$"," $");
    	  opDetails[newKey]=prop;
    	}
    	$scope.operation.details=opDetails;*/
    	$scope.isObject = function (value) {
    	  return (value && typeof value === 'object');
    	};
    	
      $scope.detailsTemplate = 'partials/operations/' + getTemplate($scope.operation.name); 
    },
      
    templateUrl: 'partials/operations/operation.html'}
})
