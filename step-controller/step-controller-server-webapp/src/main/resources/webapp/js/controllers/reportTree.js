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
angular.module('reportTree',['step','artefacts'])

.directive('reportTree', function($http,$timeout,$interval,stateStorage,$filter,$location,artefactTypes) {
  return {
    restrict: 'E',
    scope: {
      nodeid: '=',
      handle: '='
    
    },
    controller: function($scope) {
      $scope.reportTreeSettings = {
    	'skip' : '',
    	'limit' : ''
      };
    },
    link: function($scope, $element) {
      var nodeid = $scope.nodeid;
      
      var treeDiv = angular.element($element[0]).find('#jstree_div');
      //console.log($scope.reportTreeSettings);
      var treeScrollDiv = angular.element($element[0]).find('#jstree_scroll_div')[0];
      var scrollTopPos;
      var scrollLeftPos;
      var tree;
      treeDiv.jstree(
          {
          'core' : {
            'check_callback' : function (operation, node, node_parent, node_position, more) {
                return false;
            },
            'data' : function (obj, cb) {
              var id = obj.id==='#'?nodeid:obj.id;
              //console.log($scope.reportTreeSettings);
              $http.get("rest/controller/reportnode/"+id+"/children?skip="+$scope.reportTreeSettings.skip+"&limit="+$scope.reportTreeSettings.limit).then(function(response) {
              //$http.get("rest/controller/reportnode/"+id+"/children").then(function(response) {
                var nodes = response.data;
               var children=_.map(nodes,function(node){
                 // node.resolvedArtefact has been introduced with 3.6.0. We're checking it here for retrocompatibility. Remove this check has soon as possible
                 var cssClass = node.resolvedArtefact?artefactTypes.getIcon(node.resolvedArtefact._class):artefactTypes.getDefaultIcon();
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
          })
        }
        
        if(!$scope.$$phase) {
          $scope.$apply();
        }
      })
      
      treeDiv.on('refresh.jstree', function () { 
        if (scrollTopPos && scrollTopPos > 0) {
          treeScrollDiv.scrollTop = scrollTopPos;
        }
        if (scrollLeftPos && scrollLeftPos > 0) {
          treeScrollDiv.scrollLeft = scrollLeftPos;
        }
      });
      
      $scope.getDisplaiableProperties = function(node) {
        return _.without(_.keys(node),'id','_id','parentID','executionTime','duration','error','functionId','executionID','artefactID','customAttributes','_class','status','name','measures','attachments')
      }
      
      $scope.handle.refresh = function() {
        scrollTopPos = treeScrollDiv.scrollTop;
        scrollLeftPos = treeScrollDiv.scrollLeft;
        tree.refresh();
      }
      
      function expandPath(path, callback) {
        tree.open_node(path[0].id, function() {
          path.shift();
          if(path.length>0) { 
            $scope.handle.expandPath(path, callback);            
          } else {
            if(callback) {
              callback();
            }
          }
        });
      }
      
      function selectNode(id) {
        tree.deselect_all();
        tree.select_node(id);
      }
      
      $scope.handle.expandPath = function(path, reportTreeSettings) {
    	  expandPath(path.slice(0), function() {
          selectNode(path[path.length-1].id);
        })
      }
    },
    templateUrl: 'partials/reportTree.html'}
})