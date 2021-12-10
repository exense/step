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
var tecAdminApp = angular.module('tecAdminApp', ['step','entities','tecAdminControllers','plans','planEditor','planTree','artefacts','schedulerControllers','gridControllers','repositoryControllers','functionsControllers','executionsControllers','parametersControllers','resourcesControllers','adminControllers','screenConfigurationControllers', 'dashboardsControllers', 'operationsControllers', 'asyncTask'])

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

	api.getCustomView = (view) => {
		var customView = customViews[view];
		if(customView) {
			return customView;
		} else {
			throw "Undefined view: "+view
		}
	}

	api.getViewTemplate = function (view) {
		return api.getCustomView(view).template;
	}

	api.isPublicView = function (view) {
		return api.getCustomView(view).isPublicView;
	}  

	api.registerView = function(viewId,template,isPublicView) {
		if(!isPublicView) {
			isPublicView = false;
		}
		customViews[viewId] = {template:template, isPublicView:isPublicView}
	}

	api.registerCustomMenuEntry = function(label, viewId, mainMenu, menuIconClass, right) {
		customMenuEntries.push({label: label, viewId: viewId, mainMenu: mainMenu, menuIconClass: menuIconClass, right: right, 
			isEnabledFct: function(){return true}})
	}

	api.registerCustomMenuEntryOptional = function(label, viewId, mainMenu, menuIconClass, right, isEnabledFct) {
		customMenuEntries.push({label: label, viewId: viewId, mainMenu: mainMenu, menuIconClass: menuIconClass, right: right, isEnabledFct: isEnabledFct})
	}

	api.getCustomMenuEntries = function() {
		return _.filter(customMenuEntries,function(e){return !e.mainMenu && (e && e.isEnabledFct())});
	}

	api.getCustomMainMenuEntries = function() {
		return _.filter(customMenuEntries,function(e){return e.mainMenu == true && (e && e.isEnabledFct())});
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

	api.registerDashlet = function(path,label,template, id, before) {
		if (before) {
			api.getDashlets(path).unshift({label: label, template: template, id: id});
		} else {
			api.getDashlets(path).push({label: label, template: template, id: id});
		}
	}

	api.registerDashletAdvanced = function(path,label,template, id, position, isEnabledFct) {
		api.getDashlets(path).splice(position,0,{label: label, template: template, id: id,
		isEnabledFct: isEnabledFct});
	}

	return api;
})

.run(function(ViewRegistry, EntityRegistry) {
	ViewRegistry.registerView('myaccount','partials/myaccount.html');
	ViewRegistry.registerView('login','partials/loginForm.html',true);
})

.controller('AppController', function($rootScope, $scope, $location, $http, stateStorage, AuthService, MaintenanceService, ViewRegistry, DashboardService) {
	stateStorage.push($scope, 'root',{});

	$scope.isInitialized = false;
	AuthService.init().then(function() {
		AuthService.getSession().then(() => {
			$scope.isInitialized = true;

			$scope.logo = "images/logotopleft.png";
			if (!$location.path()) {
				AuthService.gotoDefaultPage();
			}
			$scope.$apply();
		})
	});

	$scope.$watch(function() {
		$scope.isAllTenant = $location.search().tenant === '[All]';
		$scope.isNoTenant = $location.search().tenant === '[None]';
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

.service('helpers', function($rootScope) {
	this.formatAsKeyValueList = function(obj) {
		var result='';
		_.each(_.keys(obj),function(key){
			result+=key+'='+JSON.stringify(obj[key])+', ';
		})
		return result;
	}

	this.getProjectById = (id) => {
		for (let tenant of $rootScope.tenants) {
			if (tenant.projectId === id) {
				return tenant;
			}
		}
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

.factory('AuthService', function ($http, $rootScope, $location, $window, Preferences, $uibModal) {
	var authService = {};
	var serviceContext = {};

	function setContext(session) {
		$rootScope.context = {'userID':session.username, 'rights':session.role.rights, 'role':session.role.attributes.name, 'otp':session.otp, 'session': {}};
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
		return new Promise((resolve, reject) => {
      return $http.get('rest/access/session').then((res) => {
        if (res.data.otp) {
            authService.showPasswordChangeDialog(true).then(() => {
              res.data.otp = false;
              setContext(res.data);
              resolve(res);
            });
        } else {
          setContext(res.data);
          resolve(res);
        }
      }, resolve)
		});
	}
	
	authService.login = function (credentials) {
		return $http
		.post('rest/access/login', credentials)
		.then(function (res) {
			authService.getSession().then(() => {
				if (authService.getContext() && !authService.getContext().otp) {
					$rootScope.$broadcast('step.login.succeeded');
					if ($location.path().indexOf('login') !== -1) {
						authService.gotoDefaultPage();
					}
				}
      });
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
		console.log('gotoDefaultPage', serviceContext.conf.defaultUrl);
		if(serviceContext.conf && serviceContext.conf.defaultUrl) {
			$location.path(serviceContext.conf.defaultUrl)
		} else {
			$location.path('/root/plans/list')
		}
	}

	authService.isAuthenticated = function () {
		return $rootScope.context.userID && $rootScope.context.userID!='anonymous';
	};

	authService.isExtLoginAuth = function () {
		return serviceContext.conf.noLoginMask;
	}

	authService.hasRight = function (right) {
	  // don't allow write- or delete- actions in the [All] tenant except for user and project
	  if ($rootScope.tenant && ($rootScope.tenant.name === '[All]' || $rootScope.tenant.name === '[None]') &&
		  !(right.startsWith('user') || right.startsWith('project')) &&
		  (right.endsWith('-write') || right.endsWith('-delete')  || right.endsWith('-execute'))) {
	    return false;
	  }
	  
		return $rootScope.context&&$rootScope.context.rights?$rootScope.context.rights.indexOf(right) !== -1:false;
	}; 

	authService.getConf = function() {
		return serviceContext.conf;
	}

	authService.debug =  function() {
	  var conf = serviceContext.conf;
    return conf?conf.debug:false;
  }
	
	authService.showPasswordChangeDialog = function (otp) {
		var modalInstance = $uibModal.open({backdrop: 'static',animation: false,templateUrl: 'partials/changePasswordForm.html',
			controller: 'ChangePasswordModalCtrl', resolve: {
				otp: function () {
					return otp;
				}
			}});
		return modalInstance.result;
	}

	return authService;
})

.controller('ChangePasswordModalCtrl', function ($scope, $rootScope, $uibModalInstance, $http, $location, otp) {
	$scope.otp = otp;
	$scope.model = {newPwd:""};
	$scope.repeatPwd = ""

	$http.get("rest/admin/security/passwordpolicies").then(function ({data}) {
		if (data.length > 0) {
			$scope.passwordScheme = data.map(element => {
				return {rule: new RegExp('^(?=' + element.rule + ')'), description: element.description}
			});
		}
	});

	$scope.save = function () {
		if($scope.repeatPwd!=$scope.model.newPwd) {
			$scope.error = "Passwords don't match, please try again."
		} else {
			$http.post("rest/admin/myaccount/changepwd",$scope.model).then(function(response) {
			    console.log("changepwd returned response: "+ JSON.stringify(response));

			    if (response.data.status == "KO") {
			        $scope.error = "Password could not be changed: " + response.data.message;
			    } else {
			        // all good. notify user that password was changed?
				    $uibModalInstance.close();
			    }
			},function() {
				$scope.error = "Unable to change password. Please contact your administrator.";
			});
		}
	};

	$scope.cancel = function () {
		$uibModalInstance.close();
	};
	
	$scope.showCancel = function() {
		return $scope.otp;
	}
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
	 
	document.querySelectorAll('input[type=password]')[0].addEventListener('keyup', function (e) {
     if (e.getModifierState && e.getModifierState('CapsLock')) {
        $scope.capsWarning = true;
     } else {
        $scope.capsWarning = false;
     }
    });

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
        if (response.data && response.data.length > 0) {
          Dialogs.showListOfMsgs(response.data);
        }
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

  /*
   * i = number of items
   * itemName (optional) the name of the item (often with the category in front)
   */
	dialogs.showDeleteWarning = function(i, itemName, secondaryText) {
		var msg;
		if (i == undefined || i==1) {
      if (itemName) {
        msg = 'Are you sure you want to delete the ' + itemName + '?';
      } else {
        msg = 'Are you sure you want to delete this item?';
      }
		} else {
			msg = 'Are you sure you want to delete these ' + i + ' items?';
		}

		if (secondaryText) {
			msg += '\n\n' + secondaryText;
		}

		return dialogs.showWarning(msg);
	}

	dialogs.showInfo = function(msg) {
		var modalInstance = $uibModal.open({backdrop: 'static', animation: false, templateUrl: 'partials/infoMessageDialog.html',
			controller: 'DialogCtrl', 
			resolve: {message:function(){
				return msg
			}}});
		return modalInstance.result;
	}

	dialogs.showWarning = function(msg) {
		var modalInstance = $uibModal.open({backdrop: 'static', animation: false, templateUrl: 'partials/confirmationDialog.html',
			controller: 'DialogCtrl',
			resolve: {message:function(){
					return msg
				}}});
		return modalInstance.result;
	}

	dialogs.showAssignmentWarning = function(msg) {
		var modalInstance = $uibModal.open({backdrop: 'static', animation: false, templateUrl: 'partials/confirmAssignmentDialog.html',
			controller: 'DialogCtrl',
			resolve: {message:function(){
					return msg
				}}});
		return modalInstance.result;
	}

	dialogs.showErrorMsg = function(msg, callback) {
		var modalInstance = $uibModal.open({backdrop: 'static',animation: false, templateUrl: 'partials/messageDialog.html',
			controller: 'DialogCtrl', 
			resolve: {message:function(){return  $sce.trustAsHtml(msg)}}}).result.then(
				function () {
					if(callback) {
						callback();
					}
				});
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
	dialogs.selectEntityOfType = function(entityName, singleSelection, id){
	  console.log(entityName, EntityRegistry);
	  console.log(id);
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
						},
						targetId: function() {return id;},
					}
				});

		return modalInstance.result;
	};
	
	//Select entity type only
	dialogs.selectEntityType = function(excludeArray, id){
		var modalInstance = $uibModal.open(
				{
				  backdrop: 'static',
					templateUrl: 'partials/selection/selectEntityType.html',
					controller: 'SelectEntityTypeCtrl',
					resolve: {
						excludeArray:function(){return excludeArray;},
						targetId: function() {return id;},
					}
				});


		return modalInstance.result;
	};
	
	//Select Type and then entities immedately after
	dialogs.selectEntityTypeForEntities = function(excludeArray, callback, arg){
		dialogs.selectEntityType(excludeArray, arg).then(function(result1){
			if (!result1.selectAll) {
				dialogs.selectEntityOfType(result1.entity.entityName, false, arg).then(function(result2){
					callback(result2, arg);
				});
			} else {
				callback({entity: result1.entity, assignAll: true}, arg);
			}

		});
	};
	
	return dialogs;
})  

.controller('SelectEntityTypeCtrl', function ($scope, $rootScope, $uibModalInstance, EntityRegistry, helpers, excludeArray, targetId) {

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

	if (targetId) {
		$scope.migrationTarget = helpers.getProjectById(targetId).name;
		$scope.currentProject = $rootScope.tenant.name;
	}
	$scope.entities = $scope.excludeEntities(excludeArray);
	$scope.result = {};

	$scope.$watch('selectedEntity',function(newValue){
		$scope.currentEntityType = newValue;
	});

	$scope.selectAll = 'false';
	$scope.$watch('selectAll',function(newValue){
		$scope.selectAll = newValue;
	});

	$scope.proceed = function () {
		$uibModalInstance.close({ entity: $scope.currentEntityType, selectAll: $scope.selectAll === 'true'});
	};

	$scope.cancel = function () {
		$uibModalInstance.dismiss('cancel');
	};
})

.controller('SelectSttableEntityCtrl', function ($scope, $rootScope, $uibModalInstance, helpers, entityType, singleSelection, targetId) {
  $scope.type = entityType.entityName;
	$scope.multipleSelection = !singleSelection;
	$scope.selectEntityHandle = {};

	if (targetId) {
		$scope.migrationTarget = helpers.getProjectById(targetId).name;
		$scope.currentProject = $rootScope.tenant.name;
	}

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

	$scope.ok = function(value) {
		$uibModalInstance.close(value);
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
		Dialogs = $injector.get('Dialogs');
		var responsePayload = response.data;
		if (response.status != 200 && responsePayload && responsePayload.errorMessage) {
			Dialogs.showErrorMsg(responsePayload.errorMessage);
		} else {
			// Legacy error handling
			if (response.status == 500) {
				if (responsePayload && responsePayload.metaMessage && responsePayload.metaMessage.indexOf("org.rtm.stream.UnknownStreamException")>=0) {
					console.log('genericErrorInterceptor for rtm: ' + responsePayload.metaMessage);
				} else {
					Dialogs.showErrorMsg(responsePayload);
				}
			}
		}
		return $q.reject(response);
	};
})


.factory('IsUsedByDialogs', function ($rootScope, $uibModal, IsUsedByService) {
	var dialogs = {};

	dialogs.displayDialog = function(title, type, id) {
		var modalInstance = $uibModal.open({
			backdrop: 'static',
			templateUrl: 'partials/isUsedByDialog.html',
			controller: 'isUsedByDialogCtrl',
			resolve: {
				title: function () {
					return title;
				},
				type: function () {
					return type;
				},
				id: function () {
					return id;
				}
			}
		});
		return modalInstance.result;
	}

	return dialogs;
})

.service('IsUsedByService', function($http,$rootScope) {

	this.type = {
		PLAN_ID: 'PLAN_ID',
		KEYWORD_ID: 'KEYWORD_ID'
	};

	this.lookUp = (value, searchType) => {
		return $http.post("rest/references/findReferences", {"searchType":searchType, "searchValue": value});
	}

})


.controller('isUsedByDialogCtrl', function ($scope, $http, $uibModalInstance, Upload, Dialogs, ExportService, title, type, id, IsUsedByService) {
	$scope.title = title;
	$scope.type = type;
	$scope.id = id;
	$scope.result = false;

	IsUsedByService.lookUp(id, type).then(
		(result) => {
			$scope.result = result.data;
		}
	);

	$scope.close = function () {
		$uibModalInstance.close();
	};
})

.service('DashboardService', function($http,$rootScope,AuthService,ViewRegistry) {

this.isGrafanaAvailable = false;

this.checkAvailability = (override = false) => {
	try {
		if (override || (AuthService.getConf().displayNewPerfDashboard && ViewRegistry.getCustomView('grafana'))) {
			$http.get("rest/g-dashboards/isGrafanaAvailable").then(response => {
				this.isGrafanaAvailable = !!response.data.available;
				if (this.isGrafanaAvailable) {
					$rootScope.$broadcast('step.grafana.available');
				}
			});
		}
	} catch (e) {}
}
this.checkAvailability();

$rootScope.$on('step.login.succeeded', () => {
	this.checkAvailability();
});

this.getDashboardLink = taskId => {
	if (this.isGrafanaAvailable) {
		return '/#/root/grafana?d=3JB9j357k&orgId=1&var-taskId_current=' + taskId;
	} else {
		return '/#/root/dashboards/__pp__RTMDashboard?__filter1__=text,taskId,' + taskId;
	}
}

this.whenGrafanaAvailable = (override = false) => {
	return new Promise((resolve, reject) => {
		this.checkAvailability(override);

		if (this.isGrafanaAvailable) {
			resolve();
		}

		$rootScope.$on('step.grafana.available', () => {
			resolve();
		});
	});
}
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
