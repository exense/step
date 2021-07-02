/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 * 
 * This file is part of STEP Enterprise
 * 
 * STEP Enterprise can not be copied and/or distributed without the express permission of exense GmbH
 *******************************************************************************/
angular.module('functionPackages',['step'])

.controller('selectFunctionPackageModalCtrl', function ($scope, $uibModalInstance, $http, AuthService) {

	$scope.result = {};
	$scope.authService = AuthService;

	$scope.tableHandle = {};

	function reload() {
		$scope.tableHandle.reload();
	}

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};

	$scope.selectPackage = function(id) {
		$uibModalInstance.close(id);
	};

})

.factory('FunctionPackageTypeRegistry', function() {

	var registry = {};

	var api = {};

	api.register = function(typeName,label) {
		registry[typeName] = {"label":label};
	}

	api.getLabel = function(typeName) {
		return registry[typeName]?registry[typeName].label:"Unknown";
	}

	api.getTypes = function() {
		return _.keys(registry);
	}

	return api;
})

.run(function(FunctionPackageTypeRegistry) {
	FunctionPackageTypeRegistry.register('java','Java Jar');
	FunctionPackageTypeRegistry.register('dotnet','.NET DLL');
})


.run(function(FunctionTypeRegistry, ViewRegistry, EntityRegistry) {
	ViewRegistry.registerView('functionPackages','functionpackages/partials/functionPackageList.html');
	ViewRegistry.registerCustomMenuEntry('Keyword packages','functionPackages');
	ViewRegistry.registerDashlet('/functions/actions', '', 'functionpackages/partials/functionPackageActions.html', '');
	EntityRegistry.registerEntity('KeywordPackage', 'functionPackage', 'functionPackage', '/rest/functionpackages/', '/rest/functionpackages/', 'st-table', 'functionpackages/partials/functionPackageSelectionTable.html');
})

.factory('FunctionPackagesDialogs', function ($rootScope, $uibModal, $http, Dialogs, $location) {

	function openModal(function_, packageId) {
		var modalInstance = $uibModal.open({
		  backdrop: 'static',
			templateUrl: 'functionpackages/partials/functionPackageConfigurationDialog.html',
			controller: 'newFunctionPackageModalCtrl',
			resolve: {
				packageId: function(){
					return packageId;
				},
				function_: function () {
					return function_;
					}
		}
		});

		return modalInstance.result;
	}

	var dialogs = {};

	dialogs.editFunctionPackage = function(id, callback, finally_callback) {
		finally_callback = finally_callback || null;
		$http.get("rest/functionpackages/"+id).then(function(response) {
			openModal(response.data, id).then(function() {
				if(callback){callback()};
			}).finally(function() {
				if(finally_callback){finally_callback()};
			});
		});
	}

	dialogs.addFunctionPackage = function(callback) {
		openModal().then(function() {
			if(callback){callback()};
		})
	}

	return dialogs;
})

.directive('functionPackageLink', function() {
	return {
		restrict: 'E',
		scope: {
			id: '='
		},
		templateUrl: 'functionpackages/partials/functionPackageLink.html',
		controller: function($scope, $rootScope, $http, FunctionPackagesDialogs, Dialogs) {
			
			$scope.isRefreshing = false
			
			$scope.$watch('id', function() {
				if($scope.id) {
					loadFunctionPackage()
				}
			})

			function loadFunctionPackage() {
				$http.get("rest/functionpackages/"+$scope.id).then(function(response) {
					var data = response.data;
					$scope.functionPackage = data;
				})
			}

			function reload() {
			  $rootScope.$broadcast('functions.collection.change', {});
				loadFunctionPackage();
			}

			$scope.edit = function() {
				$scope.isRefreshing = true
				FunctionPackagesDialogs.editFunctionPackage($scope.id, function() {
					reload();
				}, function() {
					$scope.isRefreshing = false;
				});
			}

			$scope.refresh = function() {
				$scope.isRefreshing = true
				$http.post("rest/functionpackages/"+$scope.id+"/reload").then(function() {
					reload();
				}).finally(function() {
					$scope.isRefreshing = false
				});
			}

			$scope.delete = function() {
				$scope.isRefreshing = true
				Dialogs.showDeleteWarning().then(function() {
					$http.delete("rest/functionpackages/"+$scope.id).then(function() {
						reload()
					}).finally(function() {
						$scope.isRefreshing = false
					});
				})
			}
		}
	};
})

.controller('FunctionPackageActionsCtrl', function($scope, $rootScope, FunctionPackagesDialogs)  {
	$scope.addFunctionPackage = function() {
		FunctionPackagesDialogs.addFunctionPackage(function() {
		  $rootScope.$broadcast('functions.collection.change', {});
		});
	}
})

.controller('FunctionPackageListCtrl', [ '$scope', '$rootScope', '$compile', '$http', 'stateStorage', '$interval', '$uibModal', 'Dialogs', 'FunctionPackagesDialogs', '$location','AuthService','FunctionPackageTypeRegistry',
	function($scope, $rootScope, $compile, $http, $stateStorage, $interval, $uibModal, Dialogs, FunctionPackagesDialogs, $location, AuthService, FunctionPackageTypeRegistry) {
	$stateStorage.push($scope, 'functionPackages', {});  

	$scope.isRefreshing = false
	
	$scope.authService = AuthService;

	$scope.tableHandle = {};

	function reload() {
		$scope.tableHandle.reload();
	}

	$scope.editFunctionPackage = function(id) {
		FunctionPackagesDialogs.editFunctionPackage(id, function() {reload()});
	}

	$scope.addFunctionPackage = function() {
		FunctionPackagesDialogs.addFunctionPackage(function() {reload()});
	}

	$scope.refreshFunctionPackage = function(id) {
		$scope.isRefreshing = true
		$http.post("rest/functionpackages/"+id+"/reload").then(function() {
		}).finally(function() {
			$scope.isRefreshing = false
		});
	}

	$scope.deleteFunctionPackage = function(id) {
		Dialogs.showDeleteWarning().then(function() {
			$http.delete("rest/functionpackages/"+id).then(function() {
				reload();
			});
		})
	}
}])

.controller('newFunctionPackageModalCtrl', ['$scope', '$uibModal', '$uibModalInstance', '$http', 'function_', 'Dialogs', 'AuthService','FunctionPackageTypeRegistry', 'packageId',
	function ($scope, $uibModal,  $uibModalInstance, $http, functionPackage, Dialogs, AuthService, FunctionPackageTypeRegistry, packageId) {
	$scope.functionPackageTypeRegistry = FunctionPackageTypeRegistry;

	var newFunctionPackage = functionPackage==null;
	if (!newFunctionPackage) {
		// So we can cancel
		$scope.initPackage = functionPackage.packageLocation;
		$scope.initLibraries = functionPackage.packageLibrariesLocation;
	} else {
		// So we can cancel
		$scope.initPackage = null;
		$scope.initLibraries = null;
	}
	//preset edited Id as reference package if edit mode
	if(packageId && functionPackage){
		functionPackage.referencePackageId = packageId;
	}
	// $scope.previousPackageLocation = $scope.initPackage;
	// $scope.previousPackageLibrariesLocation = $scope.initLibraries;
	
	$scope.mode = newFunctionPackage?"add":"edit";
	$scope.showPreview = true;

	$scope.saveCustomAttributes = function() {
		$scope.functionPackage.packageAttributes = $scope.customAttributes.attributes;
	}

	$scope.selectPackage = function(){
		var modalInstance = $uibModal.open(
				{
				  backdrop: 'static',
					templateUrl: 'functionpackages/partials/selectFunctionPackage.html',
					controller: 'selectFunctionPackageModalCtrl'
				});

		modalInstance.result.then(function (selectedId) {
			$scope.functionPackage.referencePackageId = selectedId;
		});
	};
	
	$scope.clearReferencePackage = function() {
		$scope.functionPackage.referencePackageId = null
	};
	
	  $scope.addRoutingCriteria = function() {
	    $scope.criteria.push({key:"",value:""});
	  }
	  
	  $scope.removeRoutingCriteria = function(key) {
	    delete $scope.functionPackage.tokenSelectionCriteria[key];
	    loadTokenSelectionCriteria($scope.functionPackage);
	  }
	  
	  $scope.saveRoutingCriteria = function() {
	    var tokenSelectionCriteria = {}
	    _.each($scope.criteria, function(entry) {
	      tokenSelectionCriteria[entry.key]=entry.value;
	    });
	    $scope.functionPackage.tokenSelectionCriteria = tokenSelectionCriteria;
	  }
	  
	  var loadTokenSelectionCriteria = function(function_) {
	    $scope.criteria = [];
	    if(functionPackage && functionPackage.tokenSelectionCriteria) {
	      _.mapObject(functionPackage.tokenSelectionCriteria,function(val,key) {
	        $scope.criteria.push({key:key,value:val});
	      });
	    }
	  }
	  
	  loadTokenSelectionCriteria(functionPackage);	
	
	$scope.save = function () {
		if($scope.functionPackage.packageLocation) {

			$scope.isFunctionPackageReady=false
			$scope.isLoading = true
			
			$http.post("rest/functionpackages",$scope.functionPackage).then(function(response) {
				var function_ = response.data;
				$uibModalInstance.close(response.data);
				$scope.isFunctionPackageReady=false
				$scope.isLoading = false
			})
		}

		// delete the initial resources:
		if($scope.initPackage != null &&
		   $scope.initPackage != $scope.functionPackage.packageLocation) {
			$scope.deleteResource($scope.initPackage);
		}
		if($scope.initLibraries != null &&
		   $scope.initLibraries != $scope.functionPackage.packageLibrariesLocation) {
			$scope.deleteResource($scope.initLibraries);
		}
	};

	$scope.loadPackagePreview = function () {

		$scope.isLoading = true;
		
		// delete previous resources:
		if($scope.previousPackageLocation != null &&
		   $scope.previousPackageLocation != $scope.initPackage &&
		   $scope.previousPackageLocation != $scope.functionPackage.packageLocation) {

			$scope.deleteResource($scope.previousPackageLocation);
		}
		if($scope.previousPackageLibrariesLocation &&
		   $scope.previousPackageLibrariesLocation != $scope.initLibraries &&
		   $scope.previousPackageLibrariesLocation != $scope.functionPackage.packageLibrariesLocation) {

			$scope.deleteResource($scope.previousPackageLibrariesLocation);
		}
		
		if($scope.functionPackage.packageLocation != null) {

			$scope.isFunctionPackageReady=false;
			$scope.isLoading = true;
			$scope.previewError = null;
			$scope.addedFunctions = null;

			$http.post("rest/functionpackages/preview",$scope.functionPackage).then(function(response) {
				$scope.addedFunctions = response.data.functions;
				$scope.previewError = response.data.loadingError;

				if (!$scope.previewError) {
					// The user can save only if we have functions to add
					if ($scope.addedFunctions.length > 0) {
						$scope.isFunctionPackageReady = true;
					} else {
						$scope.previewError = "No keywords were found!"
					}
				} else {
					$scope.isFunctionPackageReady = false;
				}

				$scope.isLoading = false;
				if ($scope.previousPackageLocation != $scope.functionPackage.packageLocation) {
					$scope.previousPackageLocation = $scope.functionPackage.packageLocation;
				}
			},        
			//on error:
			function(response){
				$scope.isLoading = false;
				$scope.isFunctionPackageReady = false;
				$scope.previewError = response.data;
			});
		} else {
			$scope.isLoading = false;
		}
		// Save the resource of the library for future deletion:
		$scope.previousPackageLibrariesLocation = $scope.functionPackage.packageLibrariesLocation;
	}

	$scope.deleteResource = function (id) {
		resourceId = id.replace("resource:", "");
		if (resourceId!=id) {
			$http.delete("rest/resources/"+resourceId);
		}
	}

	$scope.cancel = function () {
		if($scope.functionPackage.packageLocation &&
		   $scope.functionPackage.packageLocation != $scope.initPackage) {
			$scope.deleteResource($scope.functionPackage.packageLocation);
		}

		if($scope.functionPackage.packageLibrariesLocation &&
		   $scope.functionPackage.packageLibrariesLocation != $scope.initLibraries) {
			$scope.deleteResource($scope.functionPackage.packageLibrariesLocation);
		}

		$uibModalInstance.dismiss('cancel');
	};

	if(newFunctionPackage) {
		$scope.functionPackage = {packageAttributes:{}}
	} else {
		$scope.functionPackage = functionPackage;
	}
	$scope.customAttributes = {"attributes":$scope.functionPackage.packageAttributes};
}]);

//# sourceURL=functionPackages.js
