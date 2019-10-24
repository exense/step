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
angular.module('dashboardsControllers',['tables','step', 'viz-dashboard-manager'])

.run(function(ViewRegistry) {
  ViewRegistry.registerView('dashboards','partials/dashboards/sessionManager.html');
  ViewRegistry.registerCustomMenuEntry('Dashboards','dashboards');
})

.controller('SessionManagerCtrl', function($rootScope, $scope, $http, stateStorage, Dialogs, ResourceDialogs, AuthService) {
    stateStorage.push($scope, 'dashboards', {});	
    $scope.authService = AuthService;
    $scope.sessionName = "New Session";
	$scope.staticPresets = new StaticPresets();
	$scope.dashboardsendpoint = [];
	//$scope.dashboardsendpoint.push(new PerformanceDashboard());
	
	$scope.deriveEventName = function(sbName) {
		return sbName.split('.')[1];
	};

	$scope.$on('sb.dashboard-new', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('sb.dashboard-clear', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('sb.dashboard-current-addWidget', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('sb.dashboard-current-clearWidgets', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('dashboard-change', function(event) {
		$scope.$broadcast('dashboard-reload');
	});
	$scope.$on('sb.dashboard-load', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('sb.dashboard-configure', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	$scope.$on('sb.docs', function(event) {
		$scope.$broadcast($scope.deriveEventName(event.name))
	});
	
	$scope.$on('sb.saveDashboard', function(event) {
		$scope.saveDashboard($scope.sessionName);
	});
	$scope.$on('sb.loadDashboard', function(event) {
		$scope.loadDashboard($scope.sessionName);
	});

	$scope.saveDashboard = function(sessionName){
		console.log($scope.dashboards);
		var serialized = angular.toJson({ name : sessionName, state : $scope.dashboardsendpoint }); 
		$http.post('rest/crud/session', serialized)
		.then(function (response) {
			console.log('response')
			console.log(response)
		}, function (response) {
			console.log('error response')
			console.log(response)
		});
	};

	$scope.loadDashboard = function(sessionName){
		$http.get('rest/crud/session?name='+sessionName)
		.then(function (response) {
			if(response && response.data && response.data.state && response.data.state.length > 0){
				$scope.dashboardsendpoint = response.data.state;
			}else{
				$scope.dashboardsendpoint = [];	
			}
		}, function (response) {
			console.log('error response')
			console.log(response)
		});

	};

  })
.directive('toolbar', function () {
	return {
		restrict: 'E',
		scope:{
			dashboards: '='
		},
		templateUrl: 'partials/dashboards/toolbar.html',
		controller: function ($scope, $element, $http) {

		}
	};
})