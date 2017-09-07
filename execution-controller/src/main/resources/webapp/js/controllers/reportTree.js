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
angular.module('reportTree',['step'])

.directive('reportTree', function($http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      nodeid: '=',
      handle: '='
    },
    controller: function($scope) {
      
    },
    link: function($scope, $element) {
      var nodeid = $scope.nodeid;
      
      var treeDiv = angular.element($element[0]).find('#jstree_div');
      
      var tree;
      treeDiv.jstree(
          {
          'core' : {
            'check_callback' : function (operation, node, node_parent, node_position, more) {
                return false;
            },
            'data' : function (obj, cb) {
              var id = obj.id==='#'?nodeid:obj.id;
              $http.get("rest/controller/reportnode/"+id+"/children").then(function(response) {
                var nodes = response.data;
               var children=_.map(nodes,function(node){
                 var cssClass = 'glyphicon-unchecked'
                 if(node._class=='step.artefacts.reports.CallFunctionReportNode') {
                   cssClass = "glyphicon glyphicon-record";
                 }
                  return {id:node.id, text:node.name, children:true, icon:"glyphicon "+cssClass+" status-"+node.status};
                })
                cb.call(this,children);               
              })
            }
          }, 
          "plugins" : []
          });

      tree = treeDiv.jstree(true);
      
      treeDiv.on('changed.jstree', function (e, data) {
        var selectedNodes = tree.get_selected(true);
        var selectedNodeId = selectedNodes?(selectedNodes.length>0?selectedNodes[0].id:null):null;
        if(selectedNodeId) {
          $http.get("rest/controller/reportnode/"+selectedNodeId).then(function(response){
            $scope.selectedNode = response.data;
            $scope.node = $scope.selectedNode;
          })
        }
        
        
        $scope.$apply();
      })
      
      $scope.getDisplaiableProperties = function(node) {
        return _.without(_.keys(node),'id','_id','parentID','executionTime','duration','error','functionId','executionID','artefactID','customAttributes','_class','status','name','measures','attachments')
      }
      
      $scope.handle.refresh = function() {
        tree.refresh();
      }
    },
    templateUrl: 'partials/reportTree.html'}
})