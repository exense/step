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
angular.module('planEditor',['dataTable','step','artefacts','reportTable','dynamicForms','export'])

.run(function(ViewRegistry, EntityRegistry) {  
  ViewRegistry.registerView('planeditor','partials/plans/planEditor.html');
})

.controller('PlanEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $uibModal, $location,Dialogs,  AuthService, reportTableFactory, executionServices, ExportService) {
  $scope.authService = AuthService;
  stateStorage.push($scope, 'editor', {});
      
  $scope.$watch('$state',function() {
    if($scope.$state!=null) {
      loadPlan($scope.$state);
    }
  });
      
  function loadPlan(id) {
    $scope.planId = id;
    $http.get('rest/plans/'+id).then(function(response){
      $scope.plan = response.data;
    })
  }

  function savePlan(plan) {
    return $http.post("rest/plans", plan);
  }
  
  $scope.save = function() {
    savePlan($scope.plan);
  }

  $scope.exportPlan = function() {
    ExportService.get("rest/export/plan/"+$scope.planId);
  }
  
  $scope.clonePlan = function() {
    modalResult = Dialogs.enterValue('Clone plan as ',$scope.plan.attributes.name+'_Copy', 'md', 'enterValueDialog', function(value) {
      $http.get("rest/plans/"+$scope.planId+"/clone").then(function(response){
        var clonePlan = response.data;
        clonePlan.attributes.name = value;
        savePlan(clonePlan).then(function() {
          $location.path('/root/plans/editor/' + clonePlan.id);
        });
      })
    });
  }

  // ------------------------------------
  // Component table
  //--------------------------------------
  $scope.componentTabs = {selectedTab:0};
  $scope.handle = {};

  // Controls
  $scope.controlsTable = {uid:'planEditorControls'};
  $scope.controlsTable.columns = [ { "title" : "ID", "visible" : false },
                               {"title" : "Name"},
                               { "title" : "Actions", "width":"80px", "render": function ( data, type, row ) {
                            	 return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
                                     	'onclick="angular.element(\'#PlanEditorCtrl\').scope().addControl(\''+row[0]+'\')">' +
                         				'<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
                         				'</button> '
                               }} ];
  
  
  $http.get("rest/controller/artefact/types").then(function(response){ 
    var data = response.data;
    var dataSet = [];
    for (i = 0; i < data.length; i++) {
      dataSet[i] = [ data[i], data[i], ''];
    }
    $scope.controlsTable.data = dataSet;
  })
  
  $scope.addControl = function(id) {
    $scope.handle.addControl(id);
  }
      
  // Keywords
  $scope.table = {};

  $scope.tabledef = {uid:'planEditorFunctions'}
  $scope.tabledef.columns = function(columns) {
    _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns, { 'title' : 'Name' }), function(col) {

    });
    _.each(_.where(columns, { 'title' : 'Type' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns, { 'title' : 'Package' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns,{'title':'Actions'}),function(col){
      col.render = function(data, type, row) {
        return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
        	'onclick="angular.element(\'#PlanEditorCtrl\').scope().addFunction(\''+row[0]+'\')">' +
		'<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
		'</button> '
      };  
    });
    return columns;
  };
  
  $scope.addFunction = function(id) {
    $scope.handle.addFunction(id);
  }
  
  // Other plans
  $scope.otherPlansTable;
  
  $scope.addPlan = function(id) {
    $scope.handle.addPlan(id);
  }
                  
  // Interactive functions
  $scope.artefactRef = function() {return {repositoryID:'local',repositoryParameters:{planid:$scope.planId}}};

  $scope.executionParameters = {};
  executionServices.getDefaultExecutionParameters().then(function(data){
    $scope.executionParameters = data;
  })
  
  $scope.interactiveSession = {
      execute: function(artefact) {
        var parameters = {executionParameters:$scope.executionParameters}
        var sessionId = $scope.interactiveSession.id;
        $scope.componentTabs.selectedTab = 3;
        $http.post("rest/interactive/"+sessionId+"/execute/"+$scope.plan.id+"/"+artefact.id, parameters).then(function() {
          $scope.stepsTable.Datatable.ajax.reload(null, false);
        });
      },
      start: function() {
        $scope.startInteractive();
      }
  };
     
  $scope.isInteractiveSessionActive = function() {
    return $scope.interactiveSession.id != null;
  }
  
  $scope.startInteractive = function() {
    $http.post("rest/interactive/start").then(function(response){
      var interactiveSessionId = response.data;
      $scope.interactiveSession.id = interactiveSessionId;
    })
  }
  
  $scope.resetInteractive = function() {
    $scope.stopInteractive();
    $scope.startInteractive();
        
  }
  
  $scope.stopInteractive = function() {
    $http.post("rest/interactive/"+$scope.interactiveSession.id+"/stop").then()
    $scope.interactiveSession.id = null;
    //need to reset the context of the console table
    $scope.stepsTable = reportTableFactory.get(function() {
      return {'eid':$scope.interactiveSession.id};     
    }, $scope);
  }
  
  $scope.$on('$destroy', function() {
    if($scope.interactiveSession.id) {
      $scope.stopInteractive();
    }
  });

  $scope.stepsTable = reportTableFactory.get(function() {
    return {'eid':$scope.interactiveSession.id};     
  }, $scope);
})

.directive('planTreeEditor', function(artefactTypes, $http,$timeout,$interval,stateStorage,$filter,$location, Dialogs) {
  return {
    restrict: 'E',
    scope: {
      plan: '=',
      stOnChange: '&',
      handle: '=',
      interactiveSessionHandle: '='
    },
    controller: function($scope,$location,$rootScope, AuthService) {
      
      $scope.$watch('plan',function() {
        if($scope.plan) {
          load(function(root) {
            tree.open_all();
            setupInitialState(root);
            overrideJSTreeKeyFunctions();
          });
        }
      });
      
      function setupInitialState(root) {
        var initialState = $rootScope.planEditorInitialState;
        if(initialState) {
          if(initialState.selectedNode) {
            tree.deselect_all(true);
            tree.select_node(initialState.selectedNode);            
          }
          if(initialState.interactive) {
            $scope.interactiveSessionHandle.start();
          }
          delete $rootScope.artefactEditorInitialState;
        } else {
          tree.select_node(root.id);
        }
      }
      
      function setSelectedNode(o) {
        if(o && o.length) { 
          var artefact = o[0];
          tree.deselect_all(true);
          tree._open_to(artefact.id);
          tree.select_node(artefact.id);          
          focusOnNode(artefact.id);
        }
      }
      
      function overrideJSTreeKeyFunctions() {
        kb = tree.settings.core.keyboard;
        if (kb.hasOwnProperty('up')) {
          orig = kb['up'];
          newfunction = function (e) {
            e.preventDefault();
            var o = tree.get_prev_dom(e.currentTarget);
            setSelectedNode(o);
          }
          kb['up']=newfunction;
        }
        if (kb.hasOwnProperty('down')) {
          orig = kb['up'];
          newfunction = function (e) {
            e.preventDefault();
            var o = this.get_next_dom(e.currentTarget);
            setSelectedNode(o);
          }
          kb['down']=newfunction;
        }
      }
      
      $scope.authService = AuthService;
      
      var tree;
      $('#jstree_demo_div').jstree(
				  {
					'core' : {
					  'data' : [],
					  'check_callback' : function (operation, node, node_parent, node_position, more) {
					    if(AuthService.hasRight('plan-write')) {
					      if(operation=='move_node') {
					        return node_parent.parent?true:false;
					      } else {
					        return true;	              
					      }					      
					    } else {
					      return false;
					    }
					  }
					}, 
					"plugins" : ["dnd","contextmenu"],
					"contextmenu": {
					  "items": function ($node) {
					  //  var tree = $("#jstree_demo_div").jstree(true);
					    return {
					      "Rename": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Rename \u00A0\u00A0(F2)",
					        "icon"       : false,
					        "action": function (obj) {
					          $scope.rename();
					        }
					      },
					      "Move": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Move",
					        "action": false,
					        "submenu": {
					          "Up": {
					            "seperator_before": false,
					            "seperator_after": false,
					            "label": "Up \u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0(Ctrl+Up)",
					            "icon"       : false,
					            action: function (obj) {
					              $scope.move(-1);
					            }
					          },
					          "Down": {
					            "seperator_before": false,
					            "seperator_after": false,
					            "label": "Down (Ctrl+Down)",
					            "icon"       : false,
					            action: function (obj) {
					              $scope.move(1);
					            }
					          }
					        }
					      },
					      "Copy": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": "Copy \u00A0\u00A0(Ctrl+c)",
                  "icon"        : false,
                  "action": function (obj) {
                    $scope.copy();
                  }
                },
                "Paste": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": "Paste \u00A0(Ctrl+v)",
                  "icon"        : false,
                  "action": function (obj) {
                    $scope.paste();
                  }
                },
					      "Delete": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Delete (del)",
					        "icon"       : false,
					        "action": function (obj) {
					          $scope.remove();
					        }
					      },
					      "Open": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": "Open \u00A0\u00A0(Ctrl+o)",
                  "icon"        : false,
                  "action": function (obj) {
                    if ($node.original.planId || $node.original.callFunctionId) {
                      $scope.openSelectedArtefact();
                    }
                  }
                  ,"_disabled" : !($node.original.planId || $node.original.callFunctionId)
                }
					    }
					  }
					}
				  });
      tree = $('#jstree_demo_div').jstree(true);
      
      function getArtefactById(artefact, id) {
        if(artefact.id == id) {
          return artefact;
        } else {
          var children = artefact.children;
          if(children) {
            for(var i=0; i<children.length; i++) {
              var child = children[i];
              var result = getArtefactById(child, id);
              if(result != null) {
                return result;
              }
            }
          }
          return null;
        }
      }
      
      $('#jstree_demo_div').on('changed.jstree', function (e, data) {
      	var selectedNodes = tree.get_selected(true);
      	if(selectedNodes.length > 0) {
      	  $scope.selectedArtefact = getArtefactById($scope.plan.root, selectedNodes[0].id);
      	}
      	$scope.$apply();
      })
      
      $(document).on("dnd_move.vakata", function (e, data) {
        //Triggered continuously during drag 
      }).bind("dnd_stop.vakata", function(e, data) { //Triggered on drag complete
          if ($scope.nodesToMove !== undefined && $scope.nodesToMove.length > 0) {
            _.each($scope.nodesToMove, function(node) {
              var id = node.id;
              var artefact = getArtefactById($scope.plan.root, node.id);
              var oldParent = getArtefactById($scope.plan.root, node.oldParent);
              var newParent = getArtefactById($scope.plan.root, node.parent);
              oldParent.children = _.reject(oldParent.children, function(child) {
                return child.id == id;
              });
              newParent.children.splice(node.position, 0, artefact);
              reloadAfterArtefactInsertion(artefact);
              $scope.fireChangeEvent();
              $scope.nodesToMove=[];
            });
          }
      });
      
      //Triggered for each node moved before the dnd_stop.vakata event
      $('#jstree_demo_div').on("move_node.jstree", function (e, data) {
        if ($scope.nodesToMove === undefined) {
          $scope.nodesToMove = [];
        }
        var node = {
            "id" : data.node.id,
            "text" : data.node.text,
            "oldParent" : data.old_parent,
            "parent" : data.parent,
            "position" : data.position
        }
        $scope.nodesToMove.push(node);
      })
      
      $("#jstree_demo_div").delegate("a","dblclick", function(e) {
        $scope.openSelectedArtefact();
      });

      $('#jstree_demo_div').on('keydown.jstree', '.jstree-anchor', function (e, data) {
        //Only react on keyboard while not renaming a node
        if (!$scope.renaming) {
          if(e.which === 46) {
            e.preventDefault(); 
            $scope.remove();
          }
          else if(e.which === 67 && (e.ctrlKey || e.metaKey)) {
            e.preventDefault(); 
            $scope.copy();
          }
          else if(e.which === 86 && (e.ctrlKey || e.metaKey)) {
            e.preventDefault(); 
            $scope.paste();
          }
          else if (e.which === 38 && (e.ctrlKey || e.metaKey)) {
            $scope.move(-1);
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 40 && (e.ctrlKey || e.metaKey)) {
            $scope.move(1);
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 79 && (e.ctrlKey || e.metaKey)) {
            $scope.openSelectedArtefact();
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 113) {
            $scope.rename();
          }
          else if (e.which === 13 && (e.ctrlKey || e.metaKey) && $scope.isInteractiveSessionActive()) {
            $scope.execute();
            e.stopImmediatePropagation();
            e.preventDefault();
          }
        }
      })

      function getNodeLabel(artefact) {
        var label = "Unnamed";
        if(artefact.attributes && artefact.attributes.name) {
          label = artefact.attributes.name
        }
        return label;
      }
      
      function load(callback) {
  	  	treeData = [];
      	function asJSTreeNode(currentNode) {
      	  var children = [];
      	  _.each(currentNode.children, function(child) {
      	    children.push(asJSTreeNode(child))
      	  }) 	  
      	  
      	  var artefact = currentNode;

      	  var icon = artefactTypes.getIcon(artefact._class);
      	  
      	  var node = { "id" : artefact.id, "children" : children, "text" : getNodeLabel(artefact), icon:"glyphicon "+icon, data: {"artefact": artefact} }
      	  
      	  if (artefact._class === 'CallPlan') {
      	    node.planId = artefact.id;
      	  } else if (artefact._class === 'CallKeyword') {
      	    node.callFunctionId = artefact.id; 
      	  }
      	  
      	  return node;
      	}
      	
      	var root = asJSTreeNode($scope.plan.root);
        
      	treeData.push(root)
      	tree.settings.core.data = treeData;
      	
      	$('#jstree_demo_div').one("refresh.jstree", function() {
      		if(callback) {
      			callback(root); 		
      		}
      	})

      	tree.refresh();
      }
      
      function focusOnNode(nodeId) {
        var node = tree.get_node(nodeId, true);
        if (typeof node.children === "function" && (child = node.children('.jstree-anchor')) !== "undefined") { 
          child.focus();
        }
      }
      
      function reloadAfterArtefactInsertion(artefact) {
      	load(function() {
    			tree.deselect_all(true);
    			tree._open_to(artefact.id);
    			tree.select_node(artefact.id);    			
    			focusOnNode(artefact.id);
    		});  
      }
      
      function addArtefactToCurrentNode(newArtefact) {
        var selectedArtefact = getSelectedArtefact();
        selectedArtefact.children.push(newArtefact);
        reloadAfterArtefactInsertion(newArtefact)
        $scope.fireChangeEvent();
      }
      
      $scope.handle.addFunction = function(id) {
      	var selectedArtefact = tree.get_selected(true);
      	
      	$http.get("rest/functions/"+id).then(function(response) {
      	  var function_ = response.data;
          var remote = !(function_.type=="step.plugins.functions.types.CompositeFunction");
  
          $http.get("rest/controller/artefact/types/CallKeyword").then(function(response) {
            var newArtefact = response.data;
            newArtefact.attributes.name=function_.attributes.name;
            newArtefact.functionId=function_.id;
            
            if(AuthService.getConf().miscParams.enforceschemas === 'true'){
              var targetObject = {};
              
              if(function_.schema && function_.schema.required){
                _.each(Object.keys(function_.schema.properties), function(prop) {
                  var value = "notype";
                  if(function_.schema.properties[prop].type){
                    var propValue = {};
                    value = function_.schema.properties[prop].type;
                    if(value === 'number' || value === 'integer')
                      propValue = {"expression" : "<" + value + ">", "dynamic" : true};
                    else
                      propValue = {"value" : "<" + value + ">", "dynamic" : false};
                    
                    targetObject[prop] = propValue;
                  }
                });
                
                _.each(function_.schema.required, function(prop) {
                  if(targetObject[prop] && targetObject[prop].value)
                    targetObject[prop].value += " (REQ)";
                  if(targetObject[prop] && targetObject[prop].expression)
                    targetObject[prop].expression += " (REQ)";
                });
                
                newArtefact.argument = {  
                    "dynamic":false,
                    "value": JSON.stringify(targetObject),
                    "expression":null,
                    "expressionType":null
                }
              }
            }
            
            addArtefactToCurrentNode(newArtefact)
          });
        });
      }
      
      $scope.handle.addControl = function(id) {
      	$http.get("rest/controller/artefact/types/"+id).then(function(response) {
      	  var artefact = response.data;
      	  addArtefactToCurrentNode(artefact);
      	});
      }
      
      $scope.handle.addPlan = function(id) {
        $http.get("rest/plans/"+id).then(function(response) {
          var plan = response.data;
          $http.get("rest/controller/artefact/types/CallPlan").then(function(response) {
            var newArtefact = response.data;
            newArtefact.attributes.name=plan.attributes.name;
            newArtefact.planId=id;
            addArtefactToCurrentNode(newArtefact);
          });
          
        });
      }
      
      $scope.openSelectedArtefact = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        if (selectedArtefact.original.planId) {
          $http.get('rest/plans/'+$scope.plan.id+'/artefacts/'+selectedArtefact.id+'/lookup/plan').then(function(response) {
            if (response.data) {
              var planId = response.data.id
              if (planId) {
                openPlan(planId);
              } else {
                Dialogs.showErrorMsg("No editor configured for this plan type");
              }
            } else {
              Dialogs.showErrorMsg("The related plan was not found");
            }
          });
        } else if (selectedArtefact.original.callFunctionId) {
          var artefact = getSelectedArtefact();
          $http.post("rest/functions/lookup",artefact).then(function(response) {
            if (response.data) {
              var planId = response.data.planId;
              if (planId) {
                openPlan(planId);
              } else {
                Dialogs.showErrorMsg("No editor configured for this function type");
              }
            } else {
              Dialogs.showErrorMsg("The related keyword was not found");
            }
          });
        }
      }
      
      function getSelectedArtefact() {
        var selectedNode = tree.get_selected(true)[0];
        var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
        return selectedArtefact;
      }
      
      openPlan = function(planId) {
        $timeout(function() {
          $location.path('/root/plans/editor/' + planId);
        });
      }
      
      $scope.rename = function() {
        $scope.renaming=true;
        var selectedNode = tree.get_selected(true)[0];
        tree.edit(selectedNode.id, selectedNode.text, function (selectedNode, status, cancelled) {
          if (!selectedNode.text || !status || cancelled) {
            //skip
          } else {
            var selectedArtefact = getSelectedArtefact();
            selectedArtefact.attributes.name = selectedNode.text;
            load(function () {
              focusOnNode(selectedArtefact.id);
            });
          }
          $scope.renaming=false;
          $scope.fireChangeEvent();
        });
      }
      
      $scope.copy = function() {
        var selectedArtefact = getSelectedArtefact();
        $rootScope.clipboard = {object:"artefact",artefact:selectedArtefact};
      }
      
      $scope.paste = function() {
        var selectedArtefact = getSelectedArtefact();
        if($rootScope.clipboard && $rootScope.clipboard.object=="artefact") {
          $http.post("rest/plans/artefacts/clone", $rootScope.clipboard.artefact).then(function(response) {
            var clone = response.data;
            selectedArtefact.children.push(clone)
            load(function () {
              focusOnNode(clone.id);
            });
          });
          $scope.fireChangeEvent();
        }
      }
      
      $scope.remove = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        var parentid = tree.get_parent(selectedArtefact);
        var previousNode = tree.get_prev_dom(selectedArtefact.id);
        
        var parentArtefact = getArtefactById($scope.plan.root, parentid);
        parentArtefact.children = _.reject(parentArtefact.children, byId(selectedArtefact.id))
        
        load(function () {
          setSelectedNode(previousNode);
        });
        $scope.fireChangeEvent();
      }
      
      function byId(id) {
        return function(artefact) {
          return artefact.id == id;
        }
      }
      
      $scope.move = function(offset) {
        var selectedNode = tree.get_selected(true)[0];
      	var parentid = tree.get_parent(selectedNode);

      	var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
      	var parentArtefact = getArtefactById($scope.plan.root, parentid);
      	var children = parentArtefact.children;
      	
      	var index = _.findIndex(children, byId(selectedArtefact.id));
      	var newIndex = index + offset;
      	
      	if(newIndex>=0 && newIndex<children.length) {
      	  var temp = children[newIndex] 
      	  children[newIndex] = selectedArtefact
      	  children[index] = temp
      	  
      	  load(function () {
      	    focusOnNode(selectedArtefact.id);
      	  })
      	  $scope.fireChangeEvent();
      	}
      }
      
      $scope.isInteractiveSessionActive = function() {
      	return $scope.interactiveSessionHandle.id!=null;
      }
      
      $scope.execute = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        $scope.interactiveSessionHandle.execute(selectedArtefact);
      }
      
      $scope.onSelectedArtefactSave = function(artefact) {
        var currentNode = tree.get_node(artefact.id);
        if(currentNode) {
          var currentLabel = tree.get_text(currentNode);
          var newLabel = getNodeLabel(artefact);
          if(newLabel!=currentLabel) {
            tree.rename_node(currentNode,newLabel);
          }
          $scope.fireChangeEvent();
        } else {
          console.error("Unable to find not with id: "+artefact.id);
        }
      }
      
      $scope.fireChangeEvent = function() {
        if($scope.stOnChange) {
          $scope.stOnChange();
        }
      }
      
    },
    templateUrl: 'partials/plans/planTree.html'}
})