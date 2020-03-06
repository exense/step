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
      var paging = {};
      var limit=1000;
      getPreviousNodeLabel = function (node) {
        var cSkip = (paging[node.id]) ? paging[node.id].skip : 0;
        var newSkip = (cSkip>=limit) ? cSkip-limit:0;
        return "Previous Nodes " +  ((cSkip > 0) ? "("+ newSkip + ".." + cSkip +")" : ""); 
      }
      getNextNodeLabel = function (node) {
        var nbChidlren = node.children.length;
        var cSkip = (paging[node.id]) ? paging[node.id].skip : 0;
        return "Next Nodes " + ((nbChidlren >= limit) ? "("+ (cSkip+limit) + ".." + (cSkip+limit*2) +")" : "");
      }
      treeDiv.jstree(
          {
          'core' : {
            'check_callback' : function (operation, node, node_parent, node_position, more) {
                return false;
            },
            'data' : function (obj, cb) {
              var id = obj.id==='#'?nodeid:obj.id;
              var skip=0;
              if (paging[id]) {
                skip = paging[id].skip;
              }
              $http.get("rest/controller/reportnode/"+id+"/children?skip="+skip+"&limit="+limit).then(function(response) {
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
          "plugins" : ["contextmenu"],
          "contextmenu": {
            "items": function ($node) {
            //  var tree = $("#jstree_demo_div").jstree(true);
              return {
                "PagingBefore": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": getPreviousNodeLabel($node),
                  "icon"       : false,
                  "action": function (obj) {
                    $scope.pagingBefore(obj);
                  },
                  "_disabled" : !(paging[$node.id] && paging[$node.id].skip > 0) 
                },
                "PagingNext": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": getNextNodeLabel($node),
                  "icon"       : false,
                  "action": function (obj) {
                    $scope.pagingNext(obj);
                  },
                  "_disabled" : ($node.children.length<limit)
                }
              }
            }
          }
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
      
      $scope.pagingBefore = function() {
        var node = tree.get_selected(true)[0];
        if (paging[node.id]) {
          var newSkip = paging[node.id].skip - limit;
          paging[node.id] = {skip: (newSkip>0)?newSkip:0};
          $scope.handle.refresh();
        }
      }
      
      $scope.pagingNext = function(obj) {
        var node = tree.get_selected(true)[0];
        if (paging[node.id]) {
          paging[node.id] = {skip: paging[node.id].skip + limit};
        } else {
          paging[node.id] = {skip: limit};
        }
        $scope.handle.refresh();
      }
      
      $scope.getDisplaiableProperties = function(node) {
        return _.without(_.keys(node),'id','_id','parentID','executionTime','duration','error','functionId','executionID','artefactID','customAttributes','_class','status','name','measures','attachments')
      }
      
      $scope.skipRefesh=false;
      $scope.handle.refresh = function() {
        if (!$scope.skipRefesh) {
          scrollTopPos = treeScrollDiv.scrollTop;
          scrollLeftPos = treeScrollDiv.scrollLeft;
          tree.refresh();
        }
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
        var el = document.getElementById( id );
        if (el && el.offsetTop) {
          treeScrollDiv.scrollTop = el.offsetTop;
        }
        if (el && el.offsetLeft) {
          treeScrollDiv.scrollLeft = el.offsetLeft;
        }
      }
      
      $scope.handle.expandPath = function(path, reportTreeSettings) {
        $scope.skipRefesh=true;
    	  expandPath(path.slice(0), function() {
          selectNode(path[path.length-1].id);
          $scope.skipRefesh=false;
        })
      }
    },
    templateUrl: 'partials/reportTree.html'}
})