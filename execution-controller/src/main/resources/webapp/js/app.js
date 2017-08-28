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
var tecAdminApp = angular.module('tecAdminApp', ['step','tecAdminControllers','schedulerControllers','gridControllers','repositoryControllers','functionsControllers','artefactsControllers','artefactEditor','reportBrowserControllers','adminControllers'])

.config(['$locationProvider', function($locationProvider) {
  $locationProvider.hashPrefix('');
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
  
  api.getViewTemplate = function (view) {
    var customView = customViews[view];
    if(customView) {
      return customView;
    } else {
      throw "Undefined view: "+view
    }
  }
  
  api.registerView = function(viewId,template) {
    customViews[viewId] = template;
  }
  
  return api;
})

.run(function(ViewRegistry) {
  ViewRegistry.registerView('artefacts','partials/artefactList.html');
  ViewRegistry.registerView('grid','partials/grid.html');
  ViewRegistry.registerView('executions','partials/execution.html');
  ViewRegistry.registerView('scheduler','partials/scheduler.html');
  ViewRegistry.registerView('repository','partials/repository.html');
  ViewRegistry.registerView('functions','partials/functionList.html');
  ViewRegistry.registerView('artefacteditor','partials/artefactEditor.html');
  ViewRegistry.registerView('reportBrowser','partials/reportBrowser.html');
  ViewRegistry.registerView('admin','partials/admin.html');
  ViewRegistry.registerView('myaccount','partials/myaccount.html');
})

.controller('AppController', function($rootScope, $scope, $location, $http, stateStorage, AuthService, ViewRegistry) {
  stateStorage.push($scope, 'root',{});
  
  $scope.isInitialized = false;
  function finishInitialization() {
    $scope.isInitialized = true;
    $scope.logo = AuthService.getConf().miscParams.logomain?AuthService.getConf().miscParams.logomain:"images/logo_step_line_black.png";
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
  
  $scope.authService = AuthService;
  
  if(!$location.path()) {
    $location.path('/root/artefacts')    
  }
  
})

.directive('ngCompiledInclude', [
  '$compile',
  '$templateCache',
  '$http',
  function($compile, $templateCache, $http) {
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
]);


angular.module('step',['ngStorage','ngCookies'])

.service('helpers', function() {
  this.formatAsKeyValueList = function(obj) {
    var result='';
    _.each(_.keys(obj),function(key){
      result+=key+'='+JSON.stringify(obj[key])+', ';
    })
    return result;
  }
})

.service('stateStorage', ['$localStorage','$rootScope','$location','$timeout','$cookies',function($localStorage,$rootScope,$location,$timeout,$cookies){
  $rootScope.$$statepath=[];

  this.localStore = {};
  
  this.persistState = false;
  
  var lockLocationChangesUntilPathIsReached = function(targetPath) {
    if(!$rootScope.locationChangeBlocked) {
      $rootScope.locationChangeBlocked = true;
      console.log('Locking location changes until '+targetPath.join('/')+' is reached');
      var unbind = $rootScope.$watch(function() {
        if(_.isEqual(targetPath,$rootScope.currentPath)) {
          console.log('Unlocking location changes');
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
//    if($rootScope.locationChangeBlocked) {
//      console.log('Preventing location change to '+$location.path());
//      event.preventDefault();
//    }
    lockLocationChangesUntilPathIsReached($location.path().substr(1).split("/"));
  });
  
  this.push = function($scope, ctrlID, defaults) {
    if($scope.hasOwnProperty('$$statepath')) {
      $scope.$$statepath.pop();
      $scope.$$statepath.push(ctrlID);

      var path = $location.path().substr(1).split("/");
      if(_.isEqual(path.slice(0,$scope.$$statepath.length),$scope.$$statepath)) {
        console.log('existing scope pushed. id:'+$scope.$id+'. Path matched. Setting $state. path '+  path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0]);
        $scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
      }

    } else {
      console.log('new scope pushed. id:'+$scope.$id+' ctrlID: ' + ctrlID);
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
        console.log('new scope pushed. id:'+$scope.$id+'. Path matched. Setting $state. path '+  path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0]);
        $scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
      }
      
      $scope.$on('$locationChangeStart',function() {
        var path = $location.path().substr(1).split("/");
        if(_.isEqual(path.slice(0,$scope.$$statepath.length),$scope.$$statepath)) {
          console.log('scope '+$scope.$id+' remains selected after path change. new path '+path.slice(0,$scope.$$statepath.length).toString()+ ' scope path:'+$scope.$$statepath.toString());
          $scope.$state = path.slice($scope.$$statepath.length, $scope.$$statepath.length+1)[0];
        } else {
          console.log('scope '+$scope.$id+' unselected but not destroyed. setting state to null');
          $scope.$state = null;
        }
      });
      
      $scope.$watch('$state',function(newStatus, oldStatus) {
        if(newStatus!=null) {
          var newPath = $scope.$$statepath.slice();
          newPath.push(newStatus);
          console.log('changing current path  to '+ newPath);
          $rootScope.currentPath = newPath; 
          if(!$rootScope.locationChangeBlocked) {
            $location.path(newPath.join('/'));
          }
  //        $timeout(function() {
  //        }) 
        }
      })
      
      if(this.get($scope)==null) {
        this.store($scope,defaults)
      }
    }
  
    //$location.path($scope.$$statepath.join('/'));
  }
  this.get = function($scope) {
    if(this.persistState) {
      return $cookies[$scope.$$statepath.join('.')]
    } else {
      return this.localStore[$scope.$$statepath.join('.')]      
    }
  };
  
  this.store = function ($scope, model) {
    if(this.persistState) {
      $cookies[$scope.$$statepath.join('.')]=model;
    } else {
      this.localStore[$scope.$$statepath.join('.')]=model;
    }
  };
}])

.factory('AuthService', function ($http, $rootScope, Preferences) {
  var authService = {};
  var serviceContext = {};

  function setContext(session) {
    $rootScope.context = {'userID':session.username, 'rights':session.profile.rights, 'role':session.profile.role};
    Preferences.load();
  }
  
  authService.getContext = function() {
    return $rootScope.context;
  }
  
  authService.init = function() {
    return $http.get('rest/access/conf')
      .then(function(res) {
        serviceContext.conf = res.data;
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
      });
  };
 
  authService.isAuthenticated = function () {
    return $rootScope.context.userID && $rootScope.context.userID!='anonymous';
  };
 
  authService.hasRight = function (right) {
    if(serviceContext.conf.authentication) {
      return $rootScope.context&&$rootScope.context.rights?$rootScope.context.rights.indexOf(right) !== -1:false;      
    } else {
      return true;
    }
  }; 
  
  authService.getConf = function() {
    return serviceContext.conf;
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
    }, function () {
      $scope.error = "Invalid username/password";
    });
  };
  $scope.logo = AuthService.getConf().miscParams.logologinpage?AuthService.getConf().miscParams.logologinpage:"images/logo_step_line_black.png";
  
})

.factory('Dialogs', function ($rootScope, $uibModal) {
  var dialogs = {};
  
  dialogs.showDeleteWarning = function() {
    var modalInstance = $uibModal.open({animation: false, templateUrl: 'partials/confirmationDialog.html',
      controller: 'DialogCtrl', 
      resolve: {message:function(){return 'Are you sure you want to delete this item?'}}});
    return modalInstance.result;
  }
  
  dialogs.showErrorMsg = function(msg) {
    var modalInstance = $uibModal.open({animation: false, templateUrl: 'partials/messageDialog.html',
      controller: 'DialogCtrl', 
      resolve: {message:function(){return msg}}});
    return modalInstance.result;
  }
  
  return dialogs;
})

.controller('DialogCtrl', function ($scope, $uibModalInstance, message) {
  $scope.message = message;
  
  $scope.ok = function() {
    $uibModalInstance.close(); 
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };  
})

.service('genericErrorInterceptor', function($q, $injector) {
    var service = this;
    service.responseError = function(response) {
        if (response.status == 500) {
          Dialogs = $injector.get('Dialogs');
          Dialogs.showErrorMsg(response.data)
        }
        return $q.reject(response);
    };
})
