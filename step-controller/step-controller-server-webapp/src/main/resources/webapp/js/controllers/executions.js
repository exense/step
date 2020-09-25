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
angular.module('executionsControllers',['step'])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('executions','partials/execution.html');
  EntityRegistry.registerEntity('Execution', 'execution', 'executions', 'rest/executions/', 'rest/executions/', 'st-table', '/partials/executions/executionSelectionTable.html');
//  ViewRegistry.registerDashlet('execution','History','partials/executions/latestExecutions.html','latestExecutions');
})

.controller('ExecutionListCtrl', function($scope, $compile, $http, stateStorage, $interval) {
  stateStorage.push($scope, 'list',{});

  $scope.tableHandle = {};

  $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: function() {
    $scope.tableHandle.reload();
  }};
  
  $http.get('/rest/table/executions/column/result/distinct').then(function(response) {
    $scope.resultOptions = _.map(response.data, function(e) {
      return {text: e};
    });
  })
  
  $http.get('/rest/table/executions/column/status/distinct').then(function(response) {
    $scope.statusOptions = _.map(response.data, function(e) {
      return {text: e};
    });
  })
})

.directive('executionHistory', function($http) {
  return {
    restrict: 'E',
    scope: {
      artefactRef: '='
    },
    templateUrl: 'partials/executions/executionHistory.html',
    controller: function($scope) {
      function loadLatestExecutions() {
        $http.post('rest/executions/search/by/ref', $scope.artefactRef).then(function(response) {
          $scope.executions = response.data;
        });
      }
      loadLatestExecutions();
    }
  };
})

.controller('LatestExecutionsCtrl', function($scope, $http) {
  
})

