var tecAdminApp = angular.module('tecAdminApp', ['step','tecAdminControllers','schedulerControllers','gridControllers','repositoryControllers','functionsControllers','artefactsControllers','artefactEditor','ngCookies'])

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
}])

.controller('AppController', ['$rootScope','$scope', '$location', 'stateStorage', function($rootScope, $scope, $location, $stateStorage) {
  $stateStorage.push($scope, 'root',{});
  
  $rootScope.context = {'userID':'anonymous'};
  
  $scope.setView = function (view) {
    $scope.$state = view;
    $stateStorage.store($scope, {lastview:view});
  };
  
  $scope.isViewActive = function (view) {
    return ($scope.$state === view);
  };
  
}])

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
  
  lockLocationChangesUntilPathIsReached($location.path().substr(1).split("/"));
  
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
}]);

