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
angular.module('reportTable',['step','reportNodes'])

.factory('reportTableFactory', ['$http', '$compile','$timeout', function($http, $compile, $timeout) {
  var tableFactory = {};

  tableFactory.get = function (filterFactory, $scope, executionViewServices) {
    var stepsTable = {};
    $scope.stepsTableServerSideParameters = filterFactory;
    $scope.executionViewServices = executionViewServices;
    
    $http.get('/rest/table/executions/column/result/distinct').then(function(response) {
      $scope.reportNodeStatusOptions = _.map(response.data, function(e) {
        return {text: e};
      });
    })
       
    stepsTable.beforeRequest = function () {
      var tableAPI = $scope.stepsTable.Datatable;
      if (tableAPI && tableAPI.hasOwnProperty('settings') && tableAPI.settings()[0].jqXHR) {
        tableAPI.settings()[0].jqXHR.abort();
      }
      
      $timeout(function(){ 
        $scope.reloadingTable=true;
      });
    }

    stepsTable.afterRequest = function () {
      $timeout(function(){ 
        $scope.reloadingTable=false; 
      });
    }
    
    return stepsTable;
  };
  
  return tableFactory;
}]);
