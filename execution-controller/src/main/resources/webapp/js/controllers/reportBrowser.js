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
angular.module('reportBrowserControllers', [ 'dataTable', 'step' ])

.controller('ReportNodeBrowserCtrl', ['$scope', 'stateStorage','$http','$location',
    function($scope, $stateStorage, $http, $location) {
      $stateStorage.push($scope, 'reportBrowser', {});
      
      $scope.query = "";
      
      $scope.toExecution = function(eid) {
        $scope.$apply(function() {
          $location.path('/root/executions/'+eid);
        })
      }
      
      $scope.search = function() {
        $scope.stepsTable.Datatable.ajax.reload(null, false);
      }
      
      $scope.stepsTable = {};
      $scope.stepsTable.columns = function(columns) {
        _.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
        _.each(_.where(columns,{'title':'Execution'}),function(col){col.sClass = 'rowDetailsToggle';col.width="80px"});
        _.each(_.where(columns,{'title':'Begin'}),function(col){col.sClass = 'rowDetailsToggle';col.width="80px"});
        _.each(_.where(columns,{'title':'Step'}),function(col){
          //col.width="50%";
          col.sClass = 'rowDetailsToggle';
          col.render = function ( data, type, row ) {
            var reportNode = JSON.parse(data);
            var renderer = reportNodeRenderer[reportNode._class];
            if(!renderer) {
              renderer = reportNodeRenderer['default'];
            }
            //return JSON.stringify(data)
            return renderer.renderer(reportNode);
            };
        });
        _.each(_.where(columns,{'title':'Error'}),function(col){
          col.render = function ( data, type, row ) {return '<div><small>'  + escapeHtml(data).replace(/\./g, '.<wbr>') + '</small></div>'};
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
      
      
      $scope.stepsTable.params = function() {
        var filter = {'oql':$scope.query};
        return filter;
      };
      
      var reportNodeRenderer = {
          'step.artefacts.reports.CallFunctionReportNode' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.input)
                html += '<div>Input: <small><em>' + escapeHtml(reportNode.input) + '</em></small></div>';
              if(reportNode.output)
                html += '<div>Output: <small><em>' + escapeHtml(reportNode.output) + '</em></small></div>';
              if(reportNode.error)
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error.msg) + '</em></small></div>';
              return html},
            icon: '' },
          'default' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.error)
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error.msg) + '</em></small></div>';
              return html},
            icon: '' },
          };
      
      
      $scope.stepsTable.detailRowRenderer = function(rowData, callback) {
        $http.get('rest/controller/reportnode/'+rowData[0]+'/path').then(function(response) {
          var data = response.data;
          var currentNode = _.last(data);
          var html = '<ul class="list-unstyled node-details">';
          if(currentNode.reportNode && currentNode.reportNode.agentUrl) {html+='<li><strong>Agent</strong> <span>'+currentNode.reportNode.agentUrl+'</span></li>'}
          if(currentNode.reportNode && currentNode.reportNode.tokenId) {html+='<li><strong>Token ID</strong> <span>'+currentNode.reportNode.tokenId+'</span></li>'}
          if(currentNode.reportNode){html+='<li><strong>Duration (ms)</strong> <span>'+currentNode.reportNode.duration+'</span></li>'}
          html+='<li><strong>Stacktrace</strong><div><table class="stacktrace">';
          _.each(data.slice(2), function(entry){
            var node = entry.reportNode;
            var artefact = entry.artefact; 
            html+='<tr><td>'+(artefact?_.last(artefact._class.split('.')):'')+'</td><td>'+node.name+'</td><td>';
            var artefactInstance = node.artefactInstance?node.artefactInstance:artefact; 
            
            _.mapObject(artefactInstance, function(value,key){
              if(['_class','id','_id','name','childrenIDs','customAttributes','attachments','createSkeleton','input','output','expectedOutput'].indexOf(key)==-1) {
                if(value) {html+=key+'='+value+' '}
              }})
            html+='</td></tr>'
          })
          html+='</table></div></li></ul>'
          html+='<div class="btn-toolbar"><button type="button" class="btn btn-default" onclick="angular.element(\'#ReportNodeBrowserCtrl\').scope().toExecution(\''+currentNode.reportNode.executionID+'\')">To execution</button></div>'
          callback(html);
        })
      }
      
      
}])   