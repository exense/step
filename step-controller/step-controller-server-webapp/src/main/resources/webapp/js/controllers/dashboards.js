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

	$scope.importSession = function() {
	  ImportDialogs.displayImportDialog('Dashboard sessions import','sessions', true).then(function () {
      reload();
    });
	}

	$scope.exportSession = function() {
	  ExportDialogs.displayExportDialog('Dashboard sessions export','sessions', 'allSession.zip', true).then(function () {})
  }
	
	$scope.initFromLocation = function(){
		if($scope.$state.startsWith('__pp__')){
			var dashboardClass = $scope.$state.split('__pp__')[1];
			var dashboardInst = window[dashboardClass]();
			// apply inputs
			dashboardInst.dstate.globalsettings.placeholders = $scope.getDynInputs();
			$scope.dashboardsendpoint.push(dashboardInst);
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

	$scope.initFromSession = function(){
		var session = $scope.authService.getContext().session;
		if(session.dashboards){
			$scope.dashboardsendpoint = session.dashboards;
			$scope.sessionName = session.sessionName;
		}
	}

	$scope.$on('$destroy', function () {
		var session = $scope.authService.getContext().session;
		session.dashboards = $scope.dashboardsendpoint;
		session.sessionName = $scope.sessionName;
	});


	if($scope.$state && $scope.$state){
		$scope.initFromLocation();
	}else{
		$scope.initFromSession();
	}

	var init = false;

	$scope.applyEntities = function(selected){
		$scope.$broadcast('apply-global-setting', { key: '__businessobjectid__', value : selected.array[0], isDynamic : false});
	};

	$scope.popApplyEntity = function(){
		Dialogs.selectEntityTypeForEntities(['repository'], $scope.applyEntities);
		//.then(function(result){ $scope.applyEntities(result.array[0]); });
	};

	$scope.pickPreset = function(){
		var $ctrl = this;
		$ctrl.animationsEnabled = true;
		$ctrl.tableElementParent = angular.element($element).find('presetparent');

		var modalInstance = $uibModal.open({
			backdrop: 'static',
			animation: $ctrl.animationsEnabled,
			ariaLabelledBy: 'modal-title',
			ariaDescribedBy: 'modal-body',
			templateUrl: 'partials/dashboardPresetsTable.html',
			controller: 'DashboardPresetsTableCtrl',
			controllerAs: '$ctrl',
			size: 'lg',
			appendTo: $ctrl.tableElementParent,
			resolve: {
				data: function () {
					return getVizDashboardList();
				},

				tableElementParent: function () {
					return $ctrl.tableElementParent;
				}
			}
		});

		return modalInstance.result;
	};

	$scope.loadFromPresets = function(){
		$scope.pickPreset().then(function(result){
			var newDashboard = (window[result[0]]());
			// prevent id collision
			newDashboard.oid += getUniqueId();
			$scope.dashboardsendpoint.push(newDashboard);
		});
	};
})

.controller('DashboardPresetsTableCtrl', function ($scope, $uibModalInstance, data, tableElementParent) {
	var $ctrl = this;
	$ctrl.selected = "";

	$(document).ready(function(){

		$ctrl.table = $('#presetsTable').DataTable({
			select: true,
			data: data,
			columns: [
				{ title: "Name" },
				]
		});

		tableElementParent.on('click', 'tr', function () {
			$ctrl.selected = $ctrl.table.row( this ).data();
		} );
	});

	$ctrl.ok = function () {
		$uibModalInstance.close($ctrl.selected);
	};

	$ctrl.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};
})


