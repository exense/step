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
angular.module('tables', ['export'])
.directive('stTable', function($compile, $http, Preferences) {
  return {
    restrict : 'E',
    scope : {
      handle: '='
    },
    transclude : {
      'actions' : '?actions',
      'columns' : '?columns'
    },
    link : function(scope, element, attr, tabsCtrl, linker) {
      var tableElement = angular.element(element).find('table');

      var tableOptions = {}
      tableOptions.pageLength = Preferences.get("tables_itemsperpage", 10);
      tableOptions.processing = false;
      tableOptions.serverSide = true;
      tableOptions.dom = 'lrtip';
      tableOptions.sProcessing = '';


      // Table columns
      var columns = linker(null, null, 'columns');
      var dtColumns = [];
      columns.find("column").each(function() {
        var header = angular.element(this).find("header").first().text();
        var cell = angular.element(this).find("cell").first().html();
        var name = angular.element(this).attr("name");
        
        var colDef = {
          "title" : header,
          "name" : name
        };

        colDef.createdCell = function(td, cellData, rowData, row, col) {
          var rowScope = scope.$new(false, scope.$parent);
          rowScope.row = JSON.parse(rowData[0]);
          var content = $compile(cell)(rowScope);
          angular.element(td).empty();
          angular.element(td).append(content);
          rowScope.$digest();
        };
        dtColumns.push(colDef);
      })
      tableOptions.columns = dtColumns;

      
      var query = 'rest/table/' + attr.collection + '/data';
      tableOptions.ajax = {
        'url' : query,
        'type' : 'POST'
      }
      
      var table = tableElement.dataTable(tableOptions);
      
      // Table actions
      var tableActions = linker(null, null, 'actions');
      var cmdDiv;
      if (element.find('div.dataTables_filter').length > 0) {
        cmdDiv = element.find('div.dataTables_filter');
        cmdDiv.parent().removeClass('col-sm-6').addClass('col-sm-9');
        element.find('div.dataTables_length').parent().removeClass('col-sm-6').addClass('col-sm-3');
      } else {
        cmdDiv = element.find('div.dataTables_length');
      }
      angular.element('<div class="pull-right"></div>').append(tableActions).appendTo(cmdDiv);

      if(scope.handle) {
        scope.handle.reload = function() {
          table.api().ajax.reload(null, false);
        }
      }
    },
    templateUrl : 'partials/datatable.html'
  };
})

// hack to suppress DataTable warning
// see http://stackoverflow.com/questions/11941876/correctly-suppressing-warnings-in-datatables
window.alert = (function() {
    var nativeAlert = window.alert;
    return function(message) {
        message.indexOf("DataTables warning") === 0 ?
            console.warn(message) :
            nativeAlert(message);
    }
})();