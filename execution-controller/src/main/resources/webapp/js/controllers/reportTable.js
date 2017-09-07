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
            rowScope.template = 'partials/reportnodes/reportNodeShort.html';
            rowScope.node = JSON.parse(cellData);
            var content = $compile("<div ng-include='template'></div>")(rowScope);
            $(td).empty();
            $(td).append(content);
          }
      });
      _.each(_.where(columns,{'title':'Status'}),function(col){
       col.searchmode="select";
       col.width="80px";
       col.render = function ( data, type, row ) {return '<div class="text-center small reportNodeStatus status-' + data +'">'  +data+ '</div>'};
      });
      _.each(_.where(columns,{'title':'Attachments'}),function(col){
        col.title="";
        col.width="15px";
        col.searchmode="none";
        col.render = function ( data, type, row ) {
          var dropdownHtml;
          if(data!=null&&data.length>0) {
            var data = JSON.parse(data)
            if(data.length>1) {
              dropdownHtml = '<div class="dropdown">'+
              '<span class="glyphicon glyphicon-paperclip dropdown-toggle" aria-hidden="true" data-toggle="dropdown"></span>'+
              '<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">';
              for(i=0;i<data.length;i++) {
                var attachment = data[i];
                var description = attachment.name?attachment.name:attachment._id
                var id = attachment._id?attachment._id.$oid:attachment.$oid
                dropdownHtml = dropdownHtml + '<li role="presentation"><a role="menuitem" tabindex="-1" href="files?uuid='+id+'">'+description+'</a></li>';
              }
              dropdownHtml = dropdownHtml+ '</ul></div>';
            } else if(data!=null&&data.length==1) {
              var attachment = data[0];
              var id = attachment._id?attachment._id.$oid:attachment.$oid
              dropdownHtml = '<a href="files?uuid='+id+'"><span class="glyphicon glyphicon-paperclip dropdown-toggle" aria-hidden="true"></span></a>';
            }
          } else {
            dropdownHtml = '';
          }
          return dropdownHtml;
        }
       });
      return columns;
    };
    
    stepsTable.params = filterFactory;
    
    stepsTable.detailRowRenderer = function(rowData, callback) {
      $http.get('rest/controller/reportnode/'+rowData[0]).then(function(response) {
        var rowScope = $scope.$new(true, $scope);
        rowScope.template = 'partials/reportnodes/reportNode.html';
        rowScope.node = response.data;
        callback($compile("<div ng-include='template'></div>")(rowScope));
      })
    }
    
    return stepsTable;
    
  };
  
  return tableFactory;
}]);
