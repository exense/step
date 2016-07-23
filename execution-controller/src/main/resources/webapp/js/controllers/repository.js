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