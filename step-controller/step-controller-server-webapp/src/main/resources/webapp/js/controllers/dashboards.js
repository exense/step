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
angular.module('dashboardsControllers',['tables','step', 'viz-session-manager'])

.run(function(ViewRegistry) {
	ViewRegistry.registerView('dashboards','partials/dashboards/dashboardsController.html');
})

.controller('DashboardsController', function($rootScope, $scope, $http, stateStorage, Dialogs, ResourceDialogs, ImportDialogs, ExportDialogs, AuthService, $location, ViewRegistry, EntityRegistry, $element, $uibModal) {
	stateStorage.push($scope, 'dashboards', {});	
	$scope.authService = AuthService;
	$scope.staticPresets = new StaticPresets();
	$scope.dashboardsendpoint = [];

	$scope.getDynInputs = function(){
		var inputs = [];
		if($location.$$search){
			var keys = Object.keys($location.$$search);
			_.each(keys, function(item, index){
				var dyn = false;
				if(item.startsWith('__dyn__')){
					dyn = true;
				}
				inputs.push({ key: item, value : $location.$$search[item], isDynamic : dyn});
			});
		}
		
		return inputs;
	};
	
	$scope.initFromLocation = function(){
		if($scope.$state.startsWith('__pp__')){
			var dashboardClass = $scope.$state.split('__pp__')[1];
			var dashboardInst = window[dashboardClass]();
			// apply inputs
			dashboardInst.dstate.globalsettings.placeholders = $scope.getDynInputs();
			$scope.dashboardsendpoint.push(dashboardInst);
		}
	}
		
	if($scope.$state && $scope.$state){
		$scope.initFromLocation();
	}
})



