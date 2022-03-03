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
angular.module('repositoryControllers', ['step'])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerViewWithConfig('repository','partials/repository.html',{isStaticView:true});
  EntityRegistry.registerEntity('Repository', 'repository', null, null, null, null, null, null);
})

// This controller is used to force reload of the following Controllers after location change. This is a trick but it works
.controller('RepositoryLoadCtrl', function($scope, $location ,$timeout) {
  $scope.reload = true;
  $scope.$on('$locationChangeSuccess',function(event) {
    $scope.reload = false; 
    $timeout(function(){$scope.reload=true});
  });
})

.controller('RepositoryCtrl', [
    '$rootScope',                           
  	'$scope',
  	'$http',
  	'$location',
  	'stateStorage',
  	function($rootScope, $scope, $http, $location, $stateStorage) {
  	  $stateStorage.push($scope, 'repository', {});
  	    	  
  	  if($location.search().user) {
  	    $rootScope.context.userID = $location.search().user;  	      
  	  }
      
  	  $scope.isolateExecution = $location.search().isolate?$location.search().isolate:false;
  	  
  	  if($location.search().repositoryId) {
  	    $scope.repoRef = {'repositoryID':$location.search().repositoryId,'repositoryParameters':
  	      _.omit($location.search(), 'repositoryId')};
  	    $scope.loading = true;
  	    $http.post("rest/controller/repository/artefact/info",$scope.repoRef).then(
  	        function(response) {
  	          $scope.loading = false
  	          $scope.artefactInfo = response.data;
  	        }, 
  	        function errorCallback(response) { 
  	          $scope.loading = false
  	          $scope.error = response.data;
  	        });
  	    
  	    $scope.functions = {};  	    
  	  }
  	 }
  	])
  	  
.controller('TestSetOverviewCtrl', [
    '$scope',
    '$http',
    '$location',
    'stateStorage',
    function($scope, $http, $location, $stateStorage) {
      $scope.tableHandle = {};
      
      $scope.repoRef = {'repositoryID':$location.search().repositoryId,'repositoryParameters':
        _.omit($location.search(), 'repositoryId')};
      $scope.trackTestcasesBy = $scope.repoRef.repositoryID=="local"?"id":"testplanName";
      $http.post("rest/controller/repository/report",$scope.repoRef).then(function(response) {
        var data = response.data;
        $scope.testCases = data.runs;
        $scope.statusOptions = _.map(_.uniq(_.map(data.runs, function(e){return e.status})), function(e){return {text:e}});
      });
      $scope.functions.getIncludedTestcases = function() {
        var table = $scope.tableHandle;
        var selectionMode = table.getSelectionMode();
        if(selectionMode=='all' || table.areAllSelected()) {
          return null;
        } else if (selectionMode=='custom' || selectionMode=='none') {
          var trackBy = $scope.trackTestcasesBy=='id'?'id':'name'
          var includedTestCases = {"by":trackBy};
          var result = [];
          if(table.getRows!=null) {
            _.each(table.getRows(true),function(value){result.push(trackBy=='id'?value.id:value.testplanName)});
          }
          includedTestCases.list = result;
          return includedTestCases;          
        } else {
          throw "Unsupported selection mode: "+selectionMode;
        }
      }
    }
])
