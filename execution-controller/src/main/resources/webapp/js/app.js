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
var tecAdminApp = angular.module('tecAdminApp', ['step','tecAdminControllers','schedulerControllers','gridControllers','repositoryControllers','functionsControllers','artefactsControllers','artefactEditor','reportBrowserControllers','ngCookies'])

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
}])

.controller('AppController', function($rootScope, $scope, $location, $http, stateStorage, AuthService) {
  stateStorage.push($scope, 'root',{});
  
  $scope.isInitialized = false;
  
  $http.get('rest/access/profile')
  .success(function(user) {
    $rootScope.context = {'userID':user.username};
    $scope.isInitialized = true;
  })
  .error(function() {
    $rootScope.context = {'userID':'anonymous'};
    $scope.isInitialized = true;
  })
  
  $scope.setView = function (view) {
    $scope.$state = view;
    $stateStorage.store($scope, {lastview:view});
  };
  
  $scope.isViewActive = function (view) {
    return ($scope.$state === view);
  };
  
  $scope.isLoggedIn = function () {
    return ($rootScope.context.userID != 'anonymous');
  };
  
  if(!$location.path()) {
    $location.path('/root/functions')    
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
          $http.get(templatePath, { cache: $templateCache }).success(function(response) {
            var contents = element.html(response).contents();
            $compile(contents)(scope);
          });
        }
      }
    };
  }
]);


angular.module('step',['ngStorage'])

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

.factory('AuthService', function ($http) {
  var authService = {};
 
  authService.login = function (credentials) {
    return $http
      .post('rest/access/login', credentials)
      .then(function (res) {
        
        return credentials.username;
      });
  };
 
  authService.isAuthenticated = function () {
    return !!Session.userId;
  };
 
  authService.isAuthorized = function (authorizedRoles) {
    if (!angular.isArray(authorizedRoles)) {
      authorizedRoles = [authorizedRoles];
    }
    return (authService.isAuthenticated() &&
      authorizedRoles.indexOf(Session.userRole) !== -1);
  };
 
  return authService;
})

.controller('LoginController', function ($scope, $rootScope, AuthService) {
  $scope.credentials = {
    username: '',
    password: ''
  };
  $scope.login = function (credentials) {
    AuthService.login(credentials).then(function (user) {
      $rootScope.context = {'userID':credentials.username};
    }, function () {
    });
  };
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
;

