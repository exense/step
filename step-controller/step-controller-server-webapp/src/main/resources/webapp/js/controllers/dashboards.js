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
angular.module('dashboardsControllers',['tables','step', 'viz-session-manager'])

.run(function(ViewRegistry) {
	ViewRegistry.registerView('dashboards','partials/dashboards/dashboardsController.html');
	ViewRegistry.registerCustomMenuEntry('Dashboards','dashboards');
})

.controller('DashboardsController', function($rootScope, $scope, $http, stateStorage, Dialogs, ResourceDialogs, AuthService, $location) {
	stateStorage.push($scope, 'dashboards', {});	
	$scope.authService = AuthService;
	$scope.staticPresets = new StaticPresets();
	$scope.dashboardsendpoint = [];

	$scope.initFromLocation = function(){

		if($scope.$state && $scope.$state){
			if($scope.$state.startsWith('__pp__')){
				var dashboardClass = $scope.$state.split('__pp__')[1];
				$scope.dashboardsendpoint.push(window[dashboardClass]());
			}else{// custom (load from db)
				$scope.sessionName = $scope.$state;
				$http.get('/rest/viz/crud/sessions?name=' + $scope.$state)
				.then(function (response) {
					$scope.dashboardsendpoint.length = 0;
					if (response && response.data && response.data.object.state && response.data.object.state.length > 0) {
						//$scope.dashboardsendpoint = $scope.dashboardsendpoint.concat(response.data.object.state);
						_.each(response.data.object.state, function(item, index){
							$scope.dashboardsendpoint.push(item);
						});
					}

				}, function (response) {
					console.log('error response')
					console.log(response)
				});
			}
		}
	}

	//$scope.$watch('$location.$$search', function(){
	$scope.initFromLocation();
	//});

	var init = false;
	$scope.$on('dashboard-ready', function(event, arg){
		if(!init){
			init = true;
			if($location.$$search){
				var keys = Object.keys($location.$$search);
				_.each(keys, function(item, index){
					var dyn = false;
					if(item.startsWith('__dyn__')){
						dyn = true;
					}
					$scope.$broadcast('apply-global-setting', { key: item, value : $location.$$search[item], isDynamic : dyn});
				});
			}
		}
	});

//	console.log('--debug--');
//	console.log(stateStorage);
//	console.log($scope.$state);
//	console.log($scope.$$statepath);
//	console.log($location.$$search);
})
