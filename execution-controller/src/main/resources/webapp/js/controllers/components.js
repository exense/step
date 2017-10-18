angular.module('components',['step'])

.directive('statusDistribution', function() {
  return {
    restrict: 'E',
    scope: {
      progress: '='
    },
    templateUrl: 'partials/components/statusSummary.html',
    controller: function($scope, $http) {
      
    }
  };
})