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
angular.module('repositoryControllers', [ 'step','dataTable' ])

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
      
      $scope.repoRef = {'repositoryID':$location.search().repositoryId,'repositoryParameters':$location.search()};
      $scope.loading = true;
      $http.post("rest/controller/repository/artefact/info",$scope.repoRef).then(
          function(data) {
            $scope.loading = false
            $scope.artefactInfo = data.data;
          }, 
          function errorCallback(data) { 
            $scope.loading = false
            $scope.error = data.data;
          });
      
      $scope.functions = {};
  	 }
  	])
  	  
.controller('TestSetOverviewCtrl', [
    '$scope',
    '$http',
    '$location',
    'stateStorage',
    function($scope, $http, $location, $stateStorage) {
      $scope.testCaseTable = {};
      $scope.testCaseTable.columns = [ { "title" : "ID", "visible" : false },
                                   {"title" : "Name"},
                                   { "title" : "Status", "width":"80px", "searchmode":"select","render": function ( data, type, row ) {
                                     return '<div class="text-center reportNodeStatus status-' + data +'">'  +data+ '</div>';
                                   }} ];
      $scope.testCaseTable.defaultSelection = "all";
      
      $scope.repoRef = {'repositoryID':$location.search().repositoryId,'repositoryParameters':$location.search()};
      $http.post("rest/controller/repository/report",$scope.repoRef).success(
          function(data) {
            var dataSet = [];
            var runs = data.runs;
            for (i = 0; i < runs.length; i++) {
              dataSet[i] = [ runs[i].testplanName, runs[i].testplanName, runs[i].status];
            }
            $scope.testCaseTable.data = dataSet;
          });
      $scope.functions.getIncludedTestcases = function() {
        var result = [];
        if($scope.testCaseTable.getRows!=null) {
          _.each($scope.testCaseTable.getRows(true),function(value){result.push(value[0])});
        }
        return result;
      }
    }
])