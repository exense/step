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
angular.module('tables', ['export','dataTable'])
.directive('stTable', function($compile, $http, Preferences, stateStorage) {
  return {
    restrict : 'E',
    scope : {
      uid: '=',
      handle: '=',
      data: '=',
      collection: '=',
      persistState: '='
    },
    transclude : {
      'actions' : '?actions',
      'columns' : '?columns'
    },
    link : function(scope, element, attr, tabsCtrl, linker) {
      var serverSide = scope.collection?true:false;
      
      scope.scopesTracker = new ScopeTracker();
      scope.headerScopesTracker = new ScopeTracker();
      scope.$on('$destroy', function() {
        scope.scopesTracker.destroy();
        scope.headerScopesTracker.destroy();
        if(scope.table) {
          scope.table.destroy();
        }
      });
      
      var tableElement = angular.element(element).find('table');

      var tableOptions = {}
      tableOptions.pageLength = parseInt(Preferences.get("tables_itemsperpage", 10));
      tableOptions.dom = 'lrtip';
      tableOptions.fnDrawCallback = function() {
        scope.scopesTracker.newCycle();
      };

      if (scope.persistState) {
        if (scope.uid) {
          var uid = scope.uid;
          tableOptions.stateSave = true;
          tableOptions.stateSaveCallback = function(settings, data) {
            var state = stateStorage.get(scope, uid);
            if (!state) {
              state = {};
            }
            state.tableState = data;
            stateStorage.store(scope, state, uid);
          };
          tableOptions.stateLoadCallback = function(settings) {
            var state = stateStorage.get(scope, uid);
            return (state && state.tableState) ? state.tableState : null;
          }
        } else {
          console.error("Unable to persist table state if the table uid isn't specified. Please set the attribute 'uid'")
        }
      }

      // Table columns
      var columns = linker(function() {}, null, 'columns');
      var dtColumns = [];
      columns.find("column").each(function() {
        var header = '<div>'+angular.element(this).find("header").first().html()+'</div>';
        var secondHeaderSearch = angular.element(this).find("second-header");
        var secondHeader = '<div>'+(secondHeaderSearch.length>0?secondHeaderSearch.first().html():"")+'</div>';
        var cell = angular.element(this).find("cell").first().html();
        var name = angular.element(this).attr("name");
        
        var colDef = {
          "title" : header,
          "name" : name,
          "data": name
        };

        colDef.createdCell = function(td, cellData, rowData, row, col) {
          var rowScope = scope.$new(false, scope.$parent);
          if(serverSide) {
            rowScope.row = JSON.parse(rowData[0]);
          } else {
            rowScope.row = rowData;
          }
          
          if(!colDef.compiledCell) {
            colDef.compiledCell = $compile(cell);
          }
          
          var content = colDef.compiledCell(rowScope, function(){});
          angular.element(td).empty();
          angular.element(td).append(content);
          if(serverSide) {
            rowScope.$digest();
          }
          scope.scopesTracker.track(rowScope);
        };
        
        function createHeaderRenderer(headerHtml) {
          return function(element, column, handle) {
            var headerScope = scope.$new(false, scope.$parent);
            headerScope.initialValue = column.search();
            headerScope.search = function(expression){handle.search(name,expression)};
            var content = $compile(headerHtml)(headerScope);
            element.empty();
            element.append(content);
            scope.headerScopesTracker.track(headerScope);
          }
        }
        colDef.headerRenderer = createHeaderRenderer(header);
        colDef.secondHeaderRenderer = createHeaderRenderer(secondHeader);
        
        dtColumns.push(colDef);
      })
      tableOptions.columns = dtColumns;

      if(serverSide) {
        var query = 'rest/table/' + scope.collection + '/data';
        tableOptions.ajax = {
            'url' : query,
            'type' : 'POST'
        }
        tableOptions.processing = false;
        tableOptions.serverSide = true;
        tableOptions.sProcessing = '';
      } else {
        scope.$watchCollection('data', function(value) {
          if(scope.table) {
            scope.table.clear();
            if (value && value.length > 0) {
              scope.table.rows.add(value).draw(false);
            }
          }
        }) 
      }
      
      var table = tableElement.DataTable(tableOptions);
      scope.table = table;
      
      // Table actions
      var tableActions = linker(function() {}, null, 'actions');
      var cmdDiv;
      if (element.find('div.dataTables_filter').length > 0) {
        cmdDiv = element.find('div.dataTables_filter');
        cmdDiv.parent().removeClass('col-sm-6').addClass('col-sm-9');
        element.find('div.dataTables_length').parent().removeClass('col-sm-6').addClass('col-sm-3');
      } else {
        cmdDiv = element.find('div.dataTables_length');
      }
      angular.element('<div class="pull-right"></div>').append(tableActions).appendTo(cmdDiv);

      if(!scope.handle) {
        scope.handle = {};
      }
      scope.handle.reload = function() {
        table.ajax.reload(null, false);
      }
      scope.handle.search = function(columnName, searchExpression) {
        var column = table.column(columnName+':name');
        column.search(searchExpression,true,false).draw();
      }
      
      // render first header
      table.columns().indexes().flatten().each(function(i) {
        table.settings()[0].aoColumns[i].headerRenderer(angular.element(table.column(i).header()),table.column(i),scope.handle);
      })
      
      // render second header
      tableElement.find('thead').append('<tr class="searchheader"/>');
      $('th',tableElement.find('thead tr[role="row"]').eq(0)).css({ 'border-bottom': '0' }).each( function (colIdx) {
          tableElement.find('thead tr.searchheader').append('<th style="border-top:0" />' );
      });
      table.columns().indexes().flatten().each(function(i) {
        var thIdx = $('th',tableElement.find('thead tr[role="row"]')).index(table.column(i).header());
        if(thIdx>=0) {
          var secondHeader = $('th',tableElement.find('thead tr.searchheader')).eq(thIdx);
          table.settings()[0].aoColumns[i].secondHeaderRenderer(secondHeader,table.column(i),scope.handle);
        }
      });
      

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