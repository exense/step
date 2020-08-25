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
var tecAdminApp = angular.module('tecAdminApp', ['step','entities','tecAdminControllers','plans','planEditor','planTree','artefacts','schedulerControllers','gridControllers','repositoryControllers','functionsControllers','executionsControllers','parametersControllers','resourcesControllers','reportBrowserControllers','adminControllers','screenConfigurationControllers', 'dashboardsControllers','operationsControllers'])

.config(['$locationProvider', function($locationProvider) {
	$locationProvider.hashPrefix('');
}])

.config(['$compileProvider', function ($compileProvider) {
	// Unfortunately required to retrieve scope from elements: angular.element(\'#MyCtrl\').scope() 
	$compileProvider.debugInfoEnabled(true);
	//$compileProvider.commentDirectivesEnabled(false);
	//$compileProvider.cssClassDirectivesEnabled(false);
}])

.config(['$httpProvider', function($httpProvider) {
	//initialize get if not there
	if (!$httpProvider.defaults.headers.get) {
		$httpProvider.defaults.headers.get = {};    
	}    

	// Answer edited to include suggestions from comments
	// because previous version of code introduced browser-related errors

	//disable IE ajax request caching
	$httpProvider.defaults.headers.get['If-Modified-Since'] = 'Mon, 26 Jul 1997 05:00:00 GMT';
	// extra
	$httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
	$httpProvider.defaults.headers.get['Pragma'] = 'no-cache';
	$httpProvider.defaults.withCredentials = true;
	$httpProvider.interceptors.push('authInterceptor');
	$httpProvider.interceptors.push('genericErrorInterceptor');
}])

.factory('ViewRegistry', function() {

	var api = {};

	var customViews = {};

	var customMenuEntries = [];

	function getCustomView(view) {
		var customView = customViews[view];
		if(customView) {
			return customView;
		} else {
			throw "Undefined view: "+view
		}
	}

	api.getViewTemplate = function (view) {
		return getCustomView(view).template;
	}

	api.isPublicView = function (view) {
		return getCustomView(view).isPublicView;
	}  

	api.registerView = function(viewId,template,isPublicView) {
		if(!isPublicView) {
			isPublicView = false;
		}
		customViews[viewId] = {template:template, isPublicView:isPublicView}
	}

	api.registerCustomMenuEntry = function(label, viewId, mainMenu, menuIconClass, right) {
		customMenuEntries.push({label: label, viewId: viewId, mainMenu: mainMenu, menuIconClass: menuIconClass, right: right})
	}

	api.getCustomMenuEntries = function() {
		return _.filter(customMenuEntries,function(e){return !e.mainMenu});
	}

	api.getCustomMainMenuEntries = function() {
		return _.filter(customMenuEntries,function(e){return e.mainMenu == true});
	}

	var customDashlets = {};

	api.getDashlets = function (path) {
		var dashlets = customDashlets[path];
		if(!dashlets) {
			dashlets = []
			customDashlets[path] = dashlets
		}
		return dashlets;
	}

	api.registerDashlet = function(path,label,template, id) {
		api.getDashlets(path).push({label:label, template:template, id: id});
	}  

	return api;
})

.run(function(ViewRegistry, EntityRegistry) {
	ViewRegistry.registerView('myaccount','partials/myaccount.html');
	ViewRegistry.registerView('login','partials/loginForm.html',true);
})

.controller('AppController', function($rootScope, $scope, $location, $http, stateStorage, AuthService, MaintenanceService, ViewRegistry) {
	stateStorage.push($scope, 'root',{});

	$scope.isInitialized = false;
	function finishInitialization() {
		$scope.isInitialized = true;
		$scope.logo = "images/logotopleft.png";
		if(!$location.path()) {
			AuthService.gotoDefaultPage();
		}
	}
	AuthService.init().then(function() {
		AuthService.getSession().then(finishInitialization,finishInitialization)
	})

	$scope.setView = function (view) {
		$scope.$state = view;
		stateStorage.store($scope, {lastview:view});
	};

	$scope.isViewActive = function (view) {
		return ($scope.$state === view);
	};

	$scope.getViewTemplate = function () {
		return ViewRegistry.getViewTemplate($scope.$state);
	};

	$scope.isPublicView = function () {
		return ViewRegistry.isPublicView($scope.$state);
	};

	$scope.authService = AuthService;
	$scope.maintenanceService = MaintenanceService;
	$scope.viewRegistry = ViewRegistry;

   $scope.handleKeys = function(e) {
    if (e.key === 'z' && (e.ctrlKey || e.metaKey)) {
      $rootScope.$broadcast('undo-requested');
    } else if (e.key === 'y' && (e.ctrlKey || e.metaKey)) {
      $rootScope.$broadcast('redo-requested');
    }
  }
})

.directive('ngCompiledInclude', function($compile, $templateCache, $http) {
		return {
			restrict: 'A',
			priority: 400,
			compile: function(element, attrs){
				var templatePath = attrs.ngCompiledInclude;
				return function(scope, element){
					$http.get(templatePath, { cache: $templateCache }).then(function(response) {
						var contents = element.html(response).contents();
						$compile(contents)(scope);
					});
				}
			}
		};
	}
);


angular.module('step',['ngStorage','ngCookies','angularResizable'])

.service('helpers', function() {
	this.formatAsKeyValueList = function(obj) {
		var result='';
		_.each(_.keys(obj),function(key){
			result+=key+'='+JSON.stringify(obj[key])+', ';
		})
		return result;
	}
})

.service('stateStorage', function($localStorage,$rootScope,$location,$timeout,$cookies,AuthService){
	$rootScope.$$statepath=[];

	this.localStore = {};

	this.persistState = false;

	function debug() {
	  return AuthService.debug();
	}
	
	var lockLocationChangesUntilPathIsReached = function(targetPath) {
		if(!$rootScope.locationChangeBlocked) {
			$rootScope.locationChangeBlocked = true;
			if(debug()) {
			  console.log('Locking location changes until '+targetPath.join('/')+' is reached');
			}
			var unbind = $rootScope.$watch(function() {
				if(_.isEqual(targetPath,$rootScope.currentPath)) {
				  if(debug()) {
				    console.log('Unlocking location changes');
				  }
					$rootScope.locationChangeBlocked = false;
					unbind();
				}
			});
		}
	}

	if($location.path()) {
		lockLocationChangesUntilPathIsReached($location.path().substr(1).split("/"));
	}

	$rootScope.$on('$locationChangeStart',function(event) {
		if($location.path()) {
			lockLocationChangesUntilPathIsReached($location.path().substr(1).split("/"));
		}
	});

	this.push = function($scope, ctrlID, defaults) {
		if($scope.hasOwnProperty('$$statepath')) {
			$scope.$$statepath.pop();
			$scope.$$statepath.push(ctrlID);

			var path = $location.path().substr(1).split("/");
			if(_.isEqual(path.slice(0,$scope.$$statepath.length),$scope.$$statepath)) {
			  if(debug()) {
			    console.log('existing scope pushed. id:'+$scope.$id+'. Path matched. Setting $state. path '+  path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0]);
			  }
				$scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
			}

		} else {
		  if(debug()) {
		    console.log('new scope pushed. id:'+$scope.$id+' ctrlID: ' + ctrlID);
		  }
			$scope.$state = null;

			var currentScope = $scope;
			while(currentScope.$$statepath==null) {
				currentScope = currentScope.$parent;
			}
			var parentStatepath = currentScope.$$statepath;
			$scope.$$statepath=parentStatepath.slice();
			$scope.$$statepath.push(ctrlID);

			var path = $location.path().substr(1).split("/");
			if(_.isEqual(path.slice(0,$scope.$$statepath.length),$scope.$$statepath)) {
			  if(debug()) {
			    console.log('new scope pushed. id:'+$scope.$id+'. Path matched. Setting $state. path '+  path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0]);
			  }
				$scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
			}

			$scope.$on('$locationChangeStart',function() {
				var path = $location.path().substr(1).split("/");
				if(_.isEqual(path.slice(0,$scope.$$statepath.length),$scope.$$statepath)) {
				  if(debug()) {
				    console.log('scope '+$scope.$id+' remains selected after path change. new path '+path.slice(0,$scope.$$statepath.length).toString()+ ' scope path:'+$scope.$$statepath.toString());
				  }
					$scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
				} else {
				  if(debug()) {
				    console.log('scope '+$scope.$id+' unselected but not destroyed. setting state to null');
				  }
					$scope.$state = null;
				}
			});

			$scope.$watch('$state',function(newStatus, oldStatus) {
				if(newStatus!=null) {
					var newPath = $scope.$$statepath.slice();
					newPath.push(newStatus);
					if(debug()) {
					  console.log('changing current path  to '+ newPath);
					}
					$rootScope.currentPath = newPath; 
					if(!$rootScope.locationChangeBlocked) {
						$location.path(newPath.join('/'));
					}
				}
			})

			if(this.get($scope)==null) {
				this.store($scope,defaults)
			}
		}
	}
	this.get = function($scope, key) {
		var k = key?key:$scope.$$statepath.join('.');
		if(this.persistState) {
			return $cookies[k]
		} else {
			return this.localStore[k]      
		}
	};

	this.store = function ($scope, model, key) {
		var k = key?key:$scope.$$statepath.join('.');
		if(this.persistState) {
			$cookies[k]=model;
		} else {
			this.localStore[k]=model;
		}
	};
})

.factory('MaintenanceService', function ($http, $rootScope, Preferences, $sce, $interval) {
	var service = {};

	var maintenanceMessage;
	var toggleMaintenanceMessage;

	function loadMaintenanceMessage() {
		$http.get('rest/admin/maintenance/message').then(function(res) {
			maintenanceMessage = res.data;
		})
		$http.get('rest/admin/maintenance/message/toggle').then(function(res) {
			toggleMaintenanceMessage = res.data;
		})
	}

	loadMaintenanceMessage();

	$interval(loadMaintenanceMessage, 10000);

	service.getMaintenanceMessage = function() {
		return maintenanceMessage;
	}

	service.displayMaintenanceMessage = function() {
		return (maintenanceMessage && toggleMaintenanceMessage);
	}

	service.reloadMaintenanceMessage = function() {
		loadMaintenanceMessage();
	}

	service.trustAsHtml = function(html) {
		return $sce.trustAsHtml(html);
	}

	return service;
})

.factory('AuthService', function ($http, $rootScope, $location, $window, Preferences) {
	var authService = {};
	var serviceContext = {};

	function setContext(session) {
		$rootScope.context = {'userID':session.username, 'rights':session.role.rights, 'role':session.role.attributes.name, 'session': {}};
		Preferences.load();
	}

	authService.getContext = function() {
		return $rootScope.context;
	}

	authService.init = function() {
		return $http.get('rest/access/conf')
		.then(function(res) {
			serviceContext.conf = res.data;
			$window.document.title = serviceContext.conf.title;
		})
	}

	authService.getSession = function() {
		return $http.get('rest/access/session')
		.then(function(res) {
			setContext(res.data)
		})
	}

	authService.login = function (credentials) {
		return $http
		.post('rest/access/login', credentials)
		.then(function (res) {
			var session = res.data;
			setContext(session);
			$rootScope.$broadcast('step.login.succeeded');
			if($location.path().indexOf('login') !== -1) {
				authService.gotoDefaultPage();
			}
		});
	};

	authService.logout = function () {
		return $http
		.post('rest/access/logout')
		.then(function (res) {
			$rootScope.context = {'userID':'anonymous'};
			authService.gotoDefaultPage();
		});
	};

	authService.goToLoginPage = function () {
		return $location.path('/root/login')
	};  

	authService.gotoDefaultPage = function() {
		if(serviceContext.conf && serviceContext.conf.defaultUrl) {
			$location.path(serviceContext.conf.defaultUrl)
		} else {
			$location.path('/root/plans/list')
		}
	}

	authService.isAuthenticated = function () {
		return $rootScope.context.userID && $rootScope.context.userID!='anonymous';
	};

	authService.hasRight = function (right) {
		return $rootScope.context&&$rootScope.context.rights?$rootScope.context.rights.indexOf(right) !== -1:false;
	}; 

	authService.getConf = function() {
		return serviceContext.conf;
	}

	authService.debug =  function() {
	  var conf = serviceContext.conf;
    return conf?conf.debug:false;
  }
	
	return authService;
})

.factory('Preferences', function ($http) {
	var service = {};

	var preferences;

	service.load = function() {
		$http.get('rest/admin/myaccount/preferences').then(function(res) {
			preferences = res.data?res.data.preferences:null;
		})
	}

	service.get = function(key, defaultValue) {
		var value = preferences?preferences[key]:null;
		return value?value:defaultValue;
	}

	service.put = function(key, value) {
		$http.post('rest/admin/myaccount/preferences/'+key, value)
		.then(function (res) {
			if(!preferences) {
				preferences = {};
			}
			preferences[key]=value;
		});
	}

	return service;
})

.service('authInterceptor', function($q, $rootScope) {
	var service = this;
	service.responseError = function(response) {
		if (response.status == 401){
			$rootScope.context = {'userID':'anonymous'};
		}
		if (response.status == 403){
			// Fail silently for security reasons
			// or implement something like:
			//TODO: Dialogs.showErrorMsg("You are not authorized to perform this action.");
		}
		return $q.reject(response);
	};
})

.controller('LoginController', function ($scope, $rootScope, AuthService) {
	$scope.credentials = {
			username: '',
			password: ''
	};
	if(AuthService.getConf().demo) {
		$scope.credentials.password = 'init';
		$scope.credentials.username = 'admin';
	}
	$scope.login = function (credentials) {
		AuthService.login(credentials).then(function (user) {
		}, function (e) {
			$scope.error = e.data;
		});
	};
	$scope.logo = "images/logologin.png";

})

.factory('ImportDialogs', function ($rootScope, $uibModal, EntityRegistry,$sce) {
  var dialogs = {};
  
  dialogs.displayImportDialog = function(title,path,importAll,overwrite) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      templateUrl: 'partials/importDialog.html',
      controller: 'importModalCtrl',
      resolve: {
        title: function() {return title;},
        path: function() {return path;},
        importAll: function() {return importAll;},
        overwrite: function() {return overwrite;}}
    });
    return modalInstance.result;
  }

  return dialogs;
})

.controller('importModalCtrl', function ($scope, $http, $uibModalInstance, Upload, Dialogs, title, path, importAll, overwrite) {
  $scope.title = title;
  $scope.path = path;
  $scope.importAll = importAll;
  $scope.overwrite = overwrite;
  $scope.resourcePath; 
  
  $scope.save = function() {
    if($scope.resourcePath) {
      $http({url:"rest/import/" + path,method:"POST",params:{path:$scope.resourcePath,importAll:$scope.importAll,overwrite:$scope.overwrite}}).then(function(response) {
        $uibModalInstance.close(response.data);
      })      
    } else {
      Dialogs.showErrorMsg("Upload not completed.");
    }
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.factory('ExportDialogs', function ($rootScope, $uibModal, EntityRegistry,$sce) {
  var dialogs = {};
  
  dialogs.displayExportDialog = function(title, path, filename, recursively, parameters) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      templateUrl: 'partials/exportDialog.html',
      controller: 'exportModalCtrl',
      resolve: {
        title: function() {return title;},
        path: function() {return path;},
        filename: function() {return filename;},
        recursively: function() {return recursively;},
        parameters: function() {return parameters;}}
    });
    return modalInstance.result;
  }

  return dialogs;
})

.controller('exportModalCtrl', function ($scope, $http, $uibModalInstance, Upload, Dialogs, ExportService, title, path, filename, recursively, parameters) {
  $scope.title = title;
  $scope.path = path;
  $scope.filename = filename;
  $scope.recursively = recursively;
  $scope.parameters = parameters;
  
  $scope.save = function() {
    if($scope.filename) {
      urlParams = "?recursively=" + $scope.recursively + "&filename=" + $scope.filename;
      if ($scope.parameters) {
        urlParams += "&additionalEntities=parameters"
      }
      ExportService.get("rest/export/" + $scope.path + urlParams);
      $uibModalInstance.close();
      
      
  /*    $http({url:"rest/import/" + path,method:"POST",params:{path:$scope.resourcePath,importAll:$scope.importAll,overwrite:$scope.overwrite}}).then(function(response) {
        $uibModalInstance.close(response.data);
      })*/      
    } else {
      Dialogs.showErrorMsg("Upload not completed.");
    }
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.factory('Dialogs', function ($rootScope, $uibModal, EntityRegistry,$sce) {
	var dialogs = {};

	dialogs.showDeleteWarning = function(i) {
		var msg;
		if(i == undefined || i==1) {
			msg = 'Are you sure you want to delete this item?'
		} else {
			msg = 'Are you sure you want to delete these ' + i + ' items?'
		}
		return dialogs.showWarning(msg);
	}

	dialogs.showWarning = function(msg) {
		var modalInstance = $uibModal.open({backdrop: 'static', animation: false, templateUrl: 'partials/confirmationDialog.html',
			controller: 'DialogCtrl', 
			resolve: {message:function(){
				return msg
			}}});
		return modalInstance.result;
	}

	dialogs.showErrorMsg = function(msg) {
		var modalInstance = $uibModal.open({backdrop: 'static',animation: false, templateUrl: 'partials/messageDialog.html',
			controller: 'DialogCtrl', 
			resolve: {message:function(){return  $sce.trustAsHtml(msg)}}});
		return modalInstance.result;
	}
	
	 dialogs.showListOfMsgs = function(messages) {
	    var modalInstance = $uibModal.open({backdrop: 'static',animation: false, templateUrl: 'partials/messagesListDialog.html',
	      controller: 'DialogCtrl', 
	      resolve: {message:function(){return  messages}}});
	    return modalInstance.result;
	  }

	dialogs.editTextField = function(scope) {
		var modalInstance = $uibModal.open({
		  backdrop: 'static',
			animation: false,
			templateUrl: 'partials/textFieldDialog.html',
			size: 'lg',
			controller: 'DialogCtrl', 
			resolve: {message:function(){return scope.ngModel}}
		}).result.then(
				function (value) {
					// Use the value you passed from the $modalInstance.close() call
					scope.ngModel = value;
				},
				function (dismissed) {
					// Use the value you passed from the $modalInstance.dismiss() call
				}
		);
	}
	//template as param?
	//sizes: sm, md, lg
	//templates: enterValueDialog or enterTextValueDialog
	dialogs.enterValue = function(title,message,size,template,functionOnSuccess) {
		var modalInstance = $uibModal.open({
		  backdrop: 'static',
			animation: false,
			templateUrl: 'partials/'+template+'.html',
			controller: 'ExtentedDialogCtrl',
			size: size,
			resolve: {
				message:function(){return message},
				title:function(){return title}
			}
		}).result.then(
				function (value) {
					// Use the value you passed from the $modalInstance.close() call
					functionOnSuccess(value);
					//scope.ngModel = value;
				},
				function (dismissed) {
					// Use the value you passed from the $modalInstance.dismiss() call
				}
		);
	}

	//Select entities knowing type
	dialogs.selectEntityOfType = function(entityName, singleSelection){
	  var entityType = EntityRegistry.getEntityByName(entityName);  
	  
		var modalInstance = $uibModal.open(
				{
				  backdrop: 'static',
					templateUrl: 'partials/selection/selectEntityOfType.html',
					controller: 'SelectSttableEntityCtrl',
					resolve: {
					  entityType:function(){
							return entityType;
						}, 
						singleSelection:function() {
						  return singleSelection;
						}
					}
				});

		return modalInstance.result;
	};
	
	//Select entity type only
	dialogs.selectEntityType = function(excludeArray){
		var modalInstance = $uibModal.open(
				{
				  backdrop: 'static',
					templateUrl: 'partials/selection/selectEntityType.html',
					controller: 'SelectEntityTypeCtrl',
					resolve: {
						excludeArray:function(){
							return excludeArray;
						}
					}
				});


		return modalInstance.result;
	};
	
	//Select Type and then entities immedately after
	dialogs.selectEntityTypeForEntities = function(excludeArray, callback, arg){
		dialogs.selectEntityType(excludeArray).then(function(result1){
			dialogs.selectEntityOfType(result1.entity.entityName).then(function(result2){
				callback(result2, arg);
			});
		});
	};
	
	return dialogs;
})  

.controller('SelectEntityTypeCtrl', function ($scope, $uibModalInstance, EntityRegistry, excludeArray) {

	$scope.excludeEntities = function (excludeArray){
		var fullEntityList = EntityRegistry.getEntities();
		if(excludeArray && excludeArray.length > 0){
			var filtered = [];
			$.each(fullEntityList, function(index, item){
				if(!excludeArray.includes(item.entityName)){
					filtered.push(item);
				}
			});
			return filtered;
		}else{
			return fullEntityList;
		}
	};

	$scope.entities = $scope.excludeEntities(excludeArray);
	$scope.result = {};

	$scope.$watch('selectedEntity',function(newValue){
		$scope.currentEntityType = newValue;
	});

	$scope.proceed = function () {
		$uibModalInstance.close({ entity : $scope.currentEntityType});
	};

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};
})

.controller('SelectSttableEntityCtrl', function ($scope, $uibModalInstance, entityType, singleSelection) {
  $scope.type = entityType.entityName;
	$scope.multipleSelection = !singleSelection;
	$scope.selectEntityHandle = {};
	
	$scope.select = function(item) {
	  $uibModalInstance.close({entity: entityType, item: item});
	}

	$scope.proceed = function () {
	  var resultArray = [];
	  _.each($scope.selectEntityHandle.getSelection(), function(key) {
	    resultArray.push(key);
	  });
		$uibModalInstance.close({entity: entityType, array: resultArray});
	};

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};
})

.directive('selectEntity', function() {
  return {
    restrict: 'E',
    scope: {
      type: '=',
      multipleSelection: '=?',
      onSelection: '=?',
      handle: '=?'
    },
    template: '<ng-include src="templateUrl" />',
    controller: function($scope, EntityRegistry) {
      var entityType = EntityRegistry.getEntityByName($scope.type);  
      
      $scope.tableHandle = {}
      
      $scope.templateUrl = entityType.templateUrl;
      
      $scope.notifySelection = function(selection) {
        if($scope.onSelection) {
          $scope.onSelection(selection);
        }
      }
      
      if($scope.handle) {
        $scope.handle.getSelection = function() {
          return $scope.tableHandle.getSelectedIds();
        }
      }
    }
  };
})

.directive('autofocus', function($timeout) {
  return {
    restrict: 'A',
    link : function($scope, $element, attr) {
      if(attr.autofocus=="" || $scope.$eval(attr.autofocus)) {
        $timeout(function() {
          function focusChild(el) {
            if(el) {
              if(el.hasChildNodes()) {
                focusChild(el.children[0])
              } else {
                el.focus();
              }
            }
          }
          focusChild($element[0])
        });
      }
    }
  }
})

.controller('ExtentedDialogCtrl', function ($scope, $uibModalInstance, message, title) {
	$scope.message = message;
	$scope.title = title;

	$scope.ok = function() {
		$uibModalInstance.close(); 
	}

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};

	$scope.saveTextField = function (newValue) {
		$uibModalInstance.close($scope.message);
		//.replace(/\r?\n|\r/g,"")
	};
})

.controller('DialogCtrl', function ($scope, $uibModalInstance, message) {
	$scope.message = message;

	$scope.ok = function() {
		$uibModalInstance.close(); 
	}

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};

	$scope.saveTextField = function (newValue) {
		$uibModalInstance.close($scope.message);
		//.replace(/\r?\n|\r/g,"")
	};
})

.service('genericErrorInterceptor', function($q, $injector) {
	var service = this;
	service.responseError = function(response) {
		if (response.status == 500) {
			Dialogs = $injector.get('Dialogs');
			if (response.data && response.data.metaMessage && response.data.metaMessage.indexOf("org.rtm.stream.UnknownStreamException")>=0) {
				console.log('genericErrorInterceptor for rtm: ' + response.data.metaMessage);
			} else {
				Dialogs.showErrorMsg(response.data);
			}
		}
		return $q.reject(response);
	};
})

//The following functions are missing in IE11

if (!String.prototype.endsWith) {
	String.prototype.endsWith = function(searchString, position) {
		var subjectString = this.toString();
		if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
			position = subjectString.length;
		}
		position -= searchString.length;
		var lastIndex = subjectString.indexOf(searchString, position);
		return lastIndex !== -1 && lastIndex === position;
	};
}

if (!String.prototype.startsWith) {
	String.prototype.startsWith = function(searchString, position) {
		return this.substr(position || 0, searchString.length) === searchString;
	};
}
