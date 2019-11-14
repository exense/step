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
    $scope.sessionName = "New Session";
	$scope.staticPresets = new StaticPresets();
	$scope.dashboardsendpoint = [];
	
	//$(document).ready(function(){
		if($scope.$state && $scope.$state){
	        $http.get('/rest/viz/crud/sessions?name=' + $scope.$state)
	        .then(function (response) {
	            if (response && response.data && response.data.object.state && response.data.object.state.length > 0) {
	                $scope.dashboardsendpoint.push(response.data.object.state[0]);

	            }
	        }, function (response) {
	            console.log('error response')
	            console.log(response)
	        });
		}
	//});
	
	
	console.log('--debug--');
	//console.log(stateStorage);
	//console.log($scope.$state);
	//console.log($scope.$$statepath);
	//console.log($location.$$search);
  })
