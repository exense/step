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
angular.module('dataTable', [])

.directive('dateinput', ['$http',function($http) {
  return {
    restrict: 'E',
    scope: {action:'='},
    link: function(scope, element, attr, tabsCtrl) { 
      scope.onEnter = function(event) {
        var inputValue = element.find('input').val();
        scope.action(inputValue);
      }
    },
    controller: function($scope){
      $scope.maxDate = new Date();
      $scope.open = false;
      
      
      
    },
    templateUrl: 'partials/datepicker.html'
  };
}])

.directive('inputdropdown', ['$http',function($http) {
  return {
    restrict: 'E',
    scope: {options:'=',action:'='},
    controller: function($scope){
      $scope.inputtext = '';
      
      $scope.createRegexpForSelection = function(selection) {
        regexp = '';
        if(selection.length>1) {
          regexp = '(';
          _.each(selection,function(value){regexp+=value.text+'|'});
          regexp=regexp.slice(0, -1)+')';
        } else if (selection.length==1){
          regexp=selection[0].text;
        }
        return regexp;
      }
      
      $scope.$watchCollection('options', function(newOptions, oldOptions) {
        _.each(newOptions,function(option) {
          var oldOption = _.findWhere($scope.options, {text: option.text});
          if(oldOption) {
            option.selected = oldOption.selected;
          }
        })
        $scope.options = newOptions;
      })
//      $scope.handle.updateOptions = function(options) {
//        if($scope.options) {
//          _.each(options,function(option) {
//            var currentOption = _.findWhere($scope.options, {text: option.text});
//            if(currentOption) {
//              option.selected = currentOption.selected;
//            }
//          })
//        }
//        $scope.options = options;
//      }

      $scope.selectionChanged = function() {
        var selection = _.where($scope.options, { selected : true });
        $scope.inputtext = $scope.createRegexpForSelection(selection);
        $scope.action($scope.inputtext);
      };
    },
    templateUrl: 'partials/inputdropdown.html'
  };
}])

.directive('datatable', ['$compile','$http','$timeout','$q',function($compile,$http,$timeout,$q) {
  return {
    restrict:'E',
    scope: {
      tabledef: '=',
      idattr: '@',
      handle: '=',
      initialselection: '='
    },
    link: function(scope, element, attr, tabsCtrl) {       
      var tableElement = angular.element(element).find('table');
      
      var tableOptions = {"data" : []};
      
      var tableInitializationPromises = [];

      var init = function(tableOptions) {
        if(attr.selectionmode=='multiple') {
          tableOptions.columns.push({'title':'','data': null,'width':'15px','render': function(data, type, row) {return '<input id="selectionInput-'+row[0]+'" type="checkbox" />'}})
        }

        var table = tableElement.dataTable(tableOptions); 
        var tableAPI = table.DataTable();
        
        scope.$watchCollection('tabledef.data', function(value) {
          var val = value || null;
          if (val) {
            tableElement.show();
            table.fnClearTable();
            if(val.length>0) {
              table.fnAddData(val);

              if(!tableOptions.serverSide) {
                tableAPI.columns().indexes().flatten().each( function ( i ) {
                  if(tableOptions.columns[i].searchmode && tableOptions.columns[i].searchmode!='none') {
                    var column = tableAPI.column( i );
                    var distinct = column.data().unique().sort();
                    var options = tableOptions.columns[i].distinct;
                    options.length = 0;
                    _.each(distinct, function (d) {
                      options.push({text:d,selected:false});
                    } );
                  }
                });
              }
            }
          }
        });
        
        var nCol = tableElement.find('thead tr[role="row"] th').length;
        var cmdRow = tableElement.find('thead').prepend('<tr class="tableactions"><th colspan="'+nCol+'"><div id="commandButtons" class="btn-group btn-group-xs pull-right"></div><div id="selectionButtons" class="btn-group btn-group-xs pull-right"></div></th></tr>');
        var selectionButtons = cmdRow.find('#selectionButtons');
        var commandButtons = cmdRow.find('#commandButtons');

        if(scope.handle) {
          scope.handle.buttonRow = commandButtons;
        }
        
        if(scope.tabledef.actions) {
          _.each(scope.tabledef.actions, function(action){
            $('<button type="button" class="btn btn-default" aria-label="Left Align">'+action.label+'</button>').appendTo(commandButtons).click(function(){
              action.action();
            });
          })
        }
        
        
        if(tableOptions.serverSide) {
          $('<button type="button" class="btn btn-default" aria-label="Left Align">Export as CSV</button>').appendTo(selectionButtons).click(function(){
            scope.export()
          });
        }
        
        if(attr.selectionmode=='multiple') {
          
          angular.element(tableElement).find('tbody').on( 'click', 'input', function () {
            scope.selectionModel.toggleSelection(tableAPI.row($(this).closest('tr')).data()[0]);
            scope.sendSelectionChangeEvent();
            scope.$digest();
          });
          
          $('<button type="button" class="btn btn-default" aria-label="Left Align">Unselect all</button>').appendTo(selectionButtons).click(function(){
            scope.setSelectionOnFilteredRows(false);
            scope.sendSelectionChangeEvent();
            scope.refreshInputs();
          });
          
          $('<button type="button" class="btn btn-default" aria-label="Left Align">Select all</button>').appendTo(selectionButtons).click(function(){
            scope.setSelectionOnFilteredRows(true);
            scope.sendSelectionChangeEvent();
            scope.refreshInputs();
          });
        
        }
        
        if(attr.columnsearch=='true') {         
          tableElement.find('thead').append('<tr class="searchheader"/>');
          $('th',tableElement.find('thead tr[role="row"]').eq(0)).css({ 'border-bottom': '0' }).each( function (colIdx) {
              tableElement.find('thead tr.searchheader').append('<th style="border-top:0" />' );
          });
          
          tableAPI.columns().eq( 0 ).each( function ( colIdx ) {
            var thIdx = $('th',tableElement.find('thead tr[role="row"]')).index($(tableAPI.column( colIdx ).header()));
            if(thIdx>=0) {
              $('th',tableElement.find('thead tr.searchheader')).eq(thIdx).attr("id",'column-search-input-'+colIdx);
            }
          });
          
          tableAPI.columns().indexes().flatten().each( function ( i ) {
            var column = tableAPI.column( i );
            var inputContainer = tableElement.find('#column-search-input-'+i);
            
            if(tableOptions.columns[i].searchmode!='none' ) {
              var inputScope;
              if(inputContainer.find().length==0 ) {
                inputScope = scope.$new(true, scope);
                inputScope.action = function(value) {
                  column.search(value,true,false).draw();
                }
                
                if(!tableOptions.columns[i].distinct) {
                  tableOptions.columns[i].distinct = [];
                }
                inputScope.options = tableOptions.columns[i].distinct;
                
                var input;
                if(!tableOptions.columns[i].inputType || tableOptions.columns[i].inputType=='TEXT_DROPDOWN' || tableOptions.columns[i].inputType=='TEXT') {
                  input = '<inputdropdown options ="options" action="action"/>';
                } else if(tableOptions.columns[i].inputType=='DATE_RANGE') {
                  input = '<dateinput action="action"/>';
                } 
                inputContainer.empty().append($compile(input)(inputScope));       

              }
             
            }
        } );
        }
        
        angular.element(tableElement).find('tbody').on('click', 'td.rowDetailsToggle', function (e) {
          var onClick = scope.tabledef.onClick;
          var detailRowRenderer = scope.tabledef.detailRowRenderer;
          
          var tr = $(this).closest('tr');
          var row = tableAPI.row(tr);
          var data = tableAPI.row(tr).data();

          if(detailRowRenderer) {
            if (row.child.isShown()) {
                row.child.hide();
                tr.removeClass('shown');
            } else {
                detailRowRenderer(data, function(content) {
                  row.child(content).show();                    
                })
                tr.addClass('shown');
            }
          }
          if(onClick) {
            onClick(data);
          }
        });
  
        if(scope.handle) {
          scope.handle.datatable = table;
          scope.handle.Datatable = tableAPI;
        }
        
        scope.datatable = table;
        scope.Datatable = tableAPI;
      }
      
      tableOptions.fnDrawCallback = function () {
          scope.refreshInputs();
      };
      
      if(attr.order) {
        tableOptions.order = JSON.parse(attr.order);
      }
      
      if(attr.serverside) {
        tableOptions.processing = false;
        tableOptions.serverSide = true;
        tableOptions.dom = 'lrtip';
        tableOptions.sProcessing = '';
        
        tableInitializationPromises.push($http.get('rest/datatable/' + attr.serverside + '/columns').success(function(data) {
          var columns = [];
          _.each(data, function(col) {
            var colDef = { "title" : col.title, "inputType" : col.inputType};
            if(col.distinctValues) {
              var selectOptions = [];
              _.each(col.distinctValues,function(value){
                selectOptions.push({text:value ,selected:false});
              })
              colDef.distinct = selectOptions;
            }
            columns.push(colDef);
          });
          
          if(scope.tabledef.columns) {
            columns = scope.tabledef.columns(columns); 
          }
          
          tableOptions.columns = columns;
        }));
        
        var query = 'rest/datatable/' + attr.serverside + '/data';
        if(attr.params) {
          query = query + '?' + attr.params;
        }
        tableOptions.ajax = {'url':query,'type':'POST',beforeSend:function(a,b) {
          console.log(a);
          console.log(b);
          b.data = b.data + "&" + attr.params;
          
          if(scope.tabledef.params) {
            b.data = b.data + "&params=" + encodeURIComponent(JSON.stringify(scope.tabledef.params()));
          }
          
          if(scope.reportRequested) {
            b.data = b.data + "&export="+scope.reportID;
            scope.reportRequested = false;
          }
        }}
      } else {
        var columnOptions = scope.tabledef.columns;
        tableOptions.columns = columnOptions;
      }

      scope.sendSelectionChangeEvent = function() {
        if(scope.tabledef.onSelectionChange) {
          scope.tabledef.onSelectionChange();        
        }
      }
      
      scope.refreshInput = function(id, selected) {
        tableElement.find("[id='selectionInput-"+id+"']").prop( "checked",selected);
      }
      
      scope.refreshInputs = function() {
        var idPattern = /selectionInput-(.+?)$/;
        tableElement.find("input[id^=selectionInput-]").each(function() {
          var id = idPattern.exec($(this).attr('id'))[1];
          scope.refreshInput(id,scope.selectionModel.isSelected(id));
        });
      }
      
      scope.selectionModel = new SelectionModel(function(){return scope.tabledef.data});

      var allSelector = function() {return true;};
      var noneSelector = function() {return false;}
      
      if(scope.tabledef.defaultSelection) {
        if(_.isFunction(scope.tabledef.defaultSelection)) {
          scope.selectionModel.defaultSelector = scope.tabledef.defaultSelection;
        } else if (scope.tabledef.defaultSelection=='all') {
          scope.selectionModel.defaultSelector = allSelector;
        } else {
          scope.selectionModel.defaultSelector = noneSelector;
        }
      } else {
        scope.selectionModel.defaultSelector = noneSelector;
      }
      
      function getFilteredData() {
        return scope.datatable._('tr', {"filter":"applied"});
      }
      
      function isTableFiltered() {
        var hasFilter = false;
        scope.Datatable.columns().eq(0).each(function(index) {
          var col = scope.Datatable.column(index);
          if(!hasFilter && col.search().length>0) {
            hasFilter = true;
          }
        });
        if(!hasFilter && scope.datatable.fnSettings().oPreviousSearch.sSearch.length>0) {
          hasFilter = true;
        }
        return hasFilter;
      }

      scope.setSelectionOnFilteredRows = function(value) {
        if(!isTableFiltered()) {
          scope.selectionModel.defaultSelector = value?allSelector:noneSelector;
          scope.selectionModel.setSelectionAll(value);
        } else {
          scope.selectionModel.setSelectionAll(false);
          _.each(getFilteredData(),function(dataRow){
            scope.selectionModel.setSelection(dataRow[0],value);
          })
        }
      };
    
      scope.export = function() {
        scope.reportID = Math.random().toString(36).substr(2,9);
        scope.reportRequested = true;
        scope.Datatable.ajax.reload(null, false);
        
        (function poll() {
          $http.get('rest/datatable/exports/' + scope.reportID).success(function (data) {
            if(data.ready) {
              var attachmentID = data.attachmentID;
              $.fileDownload('files?uuid='+attachmentID)
              .done(function () { alert('File download a success!'); })
              .fail(function () { alert('File download failed!'); });
            } else {
              $timeout(poll, 1000);              
            }
          });
        })();
        
      }

      if(scope.handle) {
        scope.handle.getRows = scope.selectionModel.getDataRowsBySelection.bind(scope.selectionModel);
        scope.handle.getSelection = scope.selectionModel.getSelection.bind(scope.selectionModel);
        scope.handle.setSelection = scope.selectionModel.setSelection.bind(scope.selectionModel);
        scope.handle.export = scope.export;
      }
      
      $q.all(tableInitializationPromises).then(function(){
        init(tableOptions);
      })
      
    },
    templateUrl: 'partials/datatable.html'
  };
}])