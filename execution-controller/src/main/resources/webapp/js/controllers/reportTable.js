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
angular.module('reportTable',['step','reportNodes'])

.factory('reportTableFactory', ['$http', '$compile', function($http, $compile) {
  var tableFactory = {};

  tableFactory.get = function (filterFactory, $scope) {
    var stepsTable = {};
    stepsTable.columns = function(columns) {
      _.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
      _.each(_.where(columns,{'title':'Begin'}),function(col){col.sClass = 'rowDetailsToggle';col.width="80px"});
      _.each(_.where(columns,{'title':'Step'}),function(col){
        col.sClass = 'rowDetailsToggle';
        col.createdCell =  function (td, cellData, rowData, row, col) {
          var rowScope = $scope.$new(true, $scope);
          stepsTable.trackScope(rowScope);
          rowScope.node = JSON.parse(cellData);
          var content = $compile("<reportnode-short node='node' />")(rowScope);
          $(td).empty();
          $(td).append(content);
          rowScope.$apply();
        };
      });
      _.each(_.where(columns,{'title':'Status'}),function(col){
        col.searchmode="select";
        col.width="80px";
        col.createdCell =  function (td, cellData, rowData, row, col) {
          var rowScope = $scope.$new(true, $scope);
          stepsTable.trackScope(rowScope);
          rowScope.status = cellData;
          var content = $compile("<reportnode-status status='status' />")(rowScope);
          $(td).empty();
          $(td).append(content);
          rowScope.$apply();
        };
      });
      return columns;
    };
    
    stepsTable.params = filterFactory;
    
    stepsTable.detailRowRenderer = function(rowData, callback) {
      $http.get('rest/controller/reportnode/'+rowData[0]).then(function(response) {
        var rowScope = $scope.$new(true, $scope);
        rowScope.node = response.data;
        callback($compile("<reportnode node='node' />")(rowScope));
      })
    }
    
    return stepsTable;
  };
  
  return tableFactory;
}]);
