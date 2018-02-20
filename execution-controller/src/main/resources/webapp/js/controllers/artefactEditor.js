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
angular.module('artefactEditor',['dataTable','step','artefacts','reportTable','dynamicForms','export'])

.controller('ArtefactEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $uibModal, $location, AuthService, reportTableFactory, executionServices, ExportService) {
      stateStorage.push($scope, 'artefacteditor', {});
      
      $scope.$watch('$state',function() {
        if($scope.$state!=null) {
          loadArtefact($scope.$state);
        }
      });
      
      function loadArtefact(id) {
        $scope.artefactId = id;
        $http({url:"rest/controller/artefact/"+$scope.artefactId, method:"GET"}).then(function(response){
          $scope.artefact = response.data;
        })
      }
      
      $scope.authService = AuthService;
            
      $scope.componentTabs = {selectedTab:0};
      
      $scope.executionParameters = {};
      executionServices.getDefaultExecutionParameters().then(function(data){
        $scope.executionParameters = data;
      })
      
      $scope.controlsTable = {};
      $scope.controlsTable.columns = [ { "title" : "ID", "visible" : false },
                                   {"title" : "Name"},
                                   { "title" : "Actions", "width":"80px", "render": function ( data, type, row ) {
                                	 return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
                                         	'onclick="angular.element(\'#ArtefactEditorCtrl\').scope().addControl(\''+row[0]+'\')">' +
                             				'<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
                             				'</button> '
                                   }} ];
      
      $scope.saveAttributes = function() {
    	  $http.post("rest/controller/artefact/"+$scope.artefact.id+"/attributes", $scope.artefact.attributes);
      }
      
      $http.get("rest/controller/artefact/types").then(function(response){ 
        var data = response.data;
        var dataSet = [];
        for (i = 0; i < data.length; i++) {
          dataSet[i] = [ data[i], data[i], ''];
        }
        $scope.controlsTable.data = dataSet;
      })
      
      
      $scope.handle = {};
      $scope.table = {};

      $scope.tabledef = {}
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {

        });
        _.each(_.where(columns, { 'title' : 'Type' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns,{'title':'Actions'}),function(col){
          col.render = function(data, type, row) {
            return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
            	'onclick="angular.element(\'#ArtefactEditorCtrl\').scope().addFunction(\''+row[0]+'\')">' +
				'<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
				'</button> '
          };  
        });
        return columns;
      };
      
      $scope.artefactTable = {};
      $scope.artefactTable.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {

        });
        _.each(_.where(columns,{'title':'Actions'}),function(col){
          col.render = function(data, type, row) {
            return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
              'onclick="angular.element(\'#ArtefactEditorCtrl\').scope().addArtefact(\''+row[0]+'\')">' +
        '<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
        '</button> '
          };  
        });
        return columns;
      };
      
      
      $scope.addFunction = function(id) {
    	$scope.handle.addFunction(id);
      }
      
      $scope.addControl = function(id) {
    	$scope.handle.addControl(id);
      }
      
      $scope.addArtefact = function(id) {
        $scope.handle.addArtefact(id);
      }
      
      $scope.artefactRef = function() {return {repositoryID:'local',repositoryParameters:{artefactid:$scope.artefactId}}};
                  
      $scope.interactiveSession = {
          execute: function(artefact) {
            var parameters = {executionParameters:$scope.executionParameters}
            var sessionId = $scope.interactiveSession.id;
            $scope.componentTabs.selectedTab = 3;
            $http.post("rest/interactive/"+sessionId+"/execute/"+artefact.id, parameters).then(function() {
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
      }
      
      $scope.$on('$destroy', function() {
        if($scope.interactiveSession.id) {
          $scope.stopInteractive();
        }
      });

      $scope.stepsTable = reportTableFactory.get(function() {
        return {'eid':$scope.interactiveSession.id};     
      }, $scope);
      
      $scope.exportArtefact = function() {
        ExportService.get("rest/export/artefact/"+$scope.artefact.id);
      }
      
})

.directive('artefact', function(artefactTypes, $http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      artefactid: '=',
      handle: '=',
      interactiveSessionHandle: '='
    },
    controller: function($scope,$location,$rootScope, AuthService) {
      
      $scope.$watch('artefactid',function() {
        if($scope.artefactid!=null) {
          load(function(root) {
            tree.open_all();
            setupInitialState(root);
          });
        }
      });
      
      function setupInitialState(root) {
        var initialState = $rootScope.artefactEditorInitialState;
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
					"plugins" : ["dnd"]
				  });
      tree = $('#jstree_demo_div').jstree(true);
      
      $('#jstree_demo_div').on('changed.jstree', function (e, data) {
      	var selectedArtefact = tree.get_selected(true);
      	$scope.selectedArtefactId = selectedArtefact?(selectedArtefact.length>0?selectedArtefact[0].id:null):null;
      	$scope.$apply();
      })
      
      $('#jstree_demo_div').on("move_node.jstree", function (e, data) {
        $http.post("rest/controller/artefact/"+data.node.id+"/move?from="+data.old_parent+"&to="+data.parent+"&pos="+data.position)
        .then(function() {
          load();
        })
      })

      function getNodeLabel(artefact) {
        var label = "Unnamed";
        if(artefact.attributes && artefact.attributes.name) {
          label = artefact.attributes.name
        }
        return label;
      }
      
      function load(callback) {
    	
    	$http({url:"rest/controller/artefact/"+$scope.artefactid+"/descendants", method:"GET"}).then(function(response){ 
    	    var data = response.data;
    	  	treeData = [];
        	function asJSTreeNode(currentNode) {
        	  var children = [];
        	  _.each(currentNode.children, function(child) {
        		children.push(asJSTreeNode(child))
        	  }) 	  
        	  var artefact = currentNode.artefact;

        	  var icon = artefactTypes.getIcon(artefact._class);
        	  
        	  return { "id" : artefact.id, "children" : children, "text" : getNodeLabel(artefact), icon:"glyphicon "+icon }
        	}
        	
        	var root = asJSTreeNode(data);
          
        	treeData.push(root)
        	tree.settings.core.data = treeData;
        	
        	$('#jstree_demo_div').one("refresh.jstree", function() {
        		if(callback) {
        			callback(root); 		
        		}
        	})

        	tree.refresh();
        });
      }
      
      function reloadAfterArtefactInsertion(artefact) {
    	load(function() {
  			tree.deselect_all(true);
  			tree._open_to(artefact.id);
  			tree.select_node(artefact.id);    			
  		});  
      }
      
      $scope.handle.addFunction = function(id) {
    	var selectedArtefact = tree.get_selected(true);
    	
    	$http.get("rest/functions/"+id).then(function(response) {
    	  var function_ = response.data;
        var remote = !(function_.type=="step.plugins.functions.types.CompositeFunction");

        var newArtefact = {
          "attributes":{name:function_.attributes.name},
          "functionId":function_.id,"remote":{"value":remote},
          "_class":"CallKeyword"
         };

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

        $http.post("rest/controller/artefact/"+selectedArtefact[0].id+"/children",newArtefact).then(function(response){
    	    reloadAfterArtefactInsertion(response.data);
    	  })
    		
    	});
      }
      
      $scope.handle.addControl = function(id) {
    	var selectedArtefact = tree.get_selected(true);
    	
    	$http.get("rest/controller/artefact/types/"+id).then(function(response) {
    	  var artefact = response.data;
    	  $http.post("rest/controller/artefact/"+selectedArtefact[0].id+"/children",artefact).then(function(){
    		  reloadAfterArtefactInsertion(artefact);
    	  })
    	});
      }
      
      $scope.handle.addArtefact = function(id) {
        var selectedArtefact = tree.get_selected(true);
        $http.get("rest/controller/artefact/"+id).then(function(response) {
          var artefact = response.data;         
          var newArtefact = {"attributes":{"name":artefact.attributes.name},"artefactId":id,"_class":"CallPlan"};
          $http.post("rest/controller/artefact/"+selectedArtefact[0].id+"/children",newArtefact).then(function(response){
            reloadAfterArtefactInsertion(response.data);
          })
        });
      }
      
      $scope.copy = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        $rootScope.clipboard = {object:"artefact",id:selectedArtefact.id};
      }
      
      $scope.paste = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        if($rootScope.clipboard && $rootScope.clipboard.object=="artefact") {
          $http.post("rest/controller/artefact/"+$rootScope.clipboard.id+"/copy?to="+selectedArtefact.id)
          .then(function() {
            load();
          });
        }
      }
      
      $scope.remove = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        var parentid = tree.get_parent(selectedArtefact);
        $http.delete("rest/controller/artefact/"+parentid+"/children/"+selectedArtefact.id).then(function() {
          load();
        });
        }
      
      $scope.move = function(offset) {
    	var selectedArtefact = tree.get_selected(true)[0];
    	var parentid = tree.get_parent(selectedArtefact);
    	$http.post("rest/controller/artefact/"+parentid+"/children/"+selectedArtefact.id+"/move",offset).then(function() {
    	  load();
    	});
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
        } else {
          console.error("Unable to find not with id: "+artefact.id);
        }
      }
      
    },
    templateUrl: 'partials/artefact.html'}
})

.directive('artefactDetails', function($http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      artefactid: '=',
      onSave: '&',
      readonly: '=',
      handle: '='
    },
    controller: function($scope,$location,artefactTypes,AuthService) {
      
      $scope.authService = AuthService;
      
      $scope.$watch('artefactid', function(artefactId) {
        if(artefactId) {
        	$http({url:"rest/controller/artefact/"+artefactId,method:"GET"}).then(function(response) {
        	  $scope.artefact = response.data;
        	  var classname = $scope.artefact._class;
        	  $scope.icon = artefactTypes.getIcon(classname);
        	  $scope.label = artefactTypes.getLabel(classname);
        	  $scope.editor = artefactTypes.getEditor(classname);
        	})
        }
      })

      $scope.save = function() {
        if(!$scope.readonly) {
          $http.post("rest/controller/artefact/"+$scope.artefact.id, $scope.artefact).then(function() {
            if($scope.onSave) {
              $scope.onSave({artefact:$scope.artefact});
            }
          });
        }
      }      
    },
    templateUrl: 'partials/artefacts/abstractArtefact.html'}
})
.controller('CallPlanCtrl' , function($scope,$uibModal,$location,$http) {  
  $scope.gotoArtefact = function() {
    $location.path('/root/artefacteditor/' + $scope.artefact.artefactId);
  }
  
  $scope.selectArtefact = function() {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/selectArtefact.html',
      controller: 'selectArtefactModalCtrl',
      resolve: {}
    });

    modalInstance.result.then(function (artefact) {
      $scope.artefact.artefactId = artefact.id;
      $scope.artefact.attributes.name = artefact.attributes.name;
      $scope.save();
    });
  }
})
.controller('CallFunctionCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {
  
  showTokenSelectionParameters = false;
  
  function loadFunction(id, callback) {
    $http({url:"rest/functions/"+id,method:"GET"}).then(function(response) {
      $scope.targetFunction = response.data;
      if(callback) {
        callback();
      }
    })
  }
  
  $scope.$watch('artefact.functionId', function(id) {
    if(id!=null) {
      loadFunction(id);
    }
  })
  
  $scope.gotoFunction = function() {
    FunctionDialogs.editFunction($scope.targetFunction.id);
  }
  
  $scope.openFunctionEditor = function(functionid) {
    FunctionDialogs.openFunctionEditor($scope.targetFunction.id);
  }
  
  $scope.setArgument = function(json) {
    $scope.artefact.argument = json;
    $scope.save();
  }
  
  $scope.selectFunction = function() {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/selectFunction.html',
      controller: 'selectFunctionModalCtrl',
      resolve: {}
    });

    modalInstance.result.then(function (id) {
      $scope.artefact.functionId = id;
      loadFunction(id, function() {$scope.save()});
    });
  }
  
})
.controller('DataSourceCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {  
  $scope.dataSourceTypes = [{name:"excel",label:"Excel"},{name:"csv",label:"CSV"},{name:"sql",label:"SQL"},
                            {name:"file",label:"Flat file"},{name:"folder",label:"Directory"},{name:"sequence",label:"Integer sequence"},
                            {name:"json",label:"Json String"}]
  
  $scope.loadInitialDataSourceConfiguration = function() {
    $http.get("rest/datapool/types/"+$scope.artefact.dataSourceType).then(function(response){
      $scope.artefact.dataSource = response.data;
      $scope.save();
    })
  }
})
.controller('DefaultArtefactFormCtrl' , function($scope) {
  $scope.getEditableArtefactProperties = function() {
    return _.without(_.keys($scope.artefact),'id','_id','root','attributes','childrenIDs','createSkeleton','_class','attachments')
  }
})
.controller('AssertCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {  
  $scope.operatorTypes = [{name:"EQUALS",label:"equals"},{name:"BEGINS_WITH",label:"begins with"},{name:"CONTAINS",label:"contains"},
                            {name:"ENDS_WITH",label:"ends with"},{name:"MATCHES",label:"matches"}]
  

})
.directive('jsonEditor', function($http,$timeout,$interval,stateStorage,$filter,$location,Dialogs) {
  return {
    restrict: 'E',
    scope: {
      model: '=',
      onChange: '&'
    },
    templateUrl: 'partials/jsonEditor.html',
    controller: function($scope,$location,$rootScope, AuthService) {
      $scope.localModel = {json:""}
      $scope.argumentAsTable = [];
      
      $scope.$watch('model', function(json) {
        if(json!=$scope.localModel.json) {
          $scope.localModel.json = json;
          $scope.updateEditors(false);          
        }
      })
      
      $scope.save = function() {
        $scope.onChange({json:$scope.localModel.json});
      }
      
      $scope.updateJsonFromTable = function() {
        var json = {};
        _.each($scope.argumentAsTable, function(entry) {
          json[entry.key]=entry.value;
        })
        $scope.localModel.json = JSON.stringify(json);
      }
      
      $scope.addRowToTable = function(row) {
        $scope.argumentAsTable.push(row)
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.removeRowFromTable = function(key) {
        $scope.argumentAsTable = _.reject($scope.argumentAsTable, function(entry){ return entry.key==key});
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.onRowEdit = function() {
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.onJsonFieldBlur = function() {
        if($scope.updateEditors(true)) {
          $scope.save();
        }
      }
      
      $scope.updateEditors = function(validateJson) {
        try {
          $scope.argumentAsTable = _.map(JSON.parse($scope.localModel.json), function(val, key) {
            if(_.isObject(val) && _.has(val,'dynamic')) {
              return {"key":key,"value":val};              
            } else {
            	// support the static json format without dynamic expressions
              return {"key":key,"value":{"value":val,dynamic:false}};              
            }
          });
          return true;
        }
        catch(err) {
          if(validateJson) {
            Dialogs.showErrorMsg("Invalid JSON: " + err)            
          }
          return false;
        }
      }
      
      function initLastRow() {
      	// init last row as a static value
        $scope.lastRow = {key:"", value:{value:"",dynamic:false}}        
      }
      
      initLastRow();

      $scope.commitLastRow = function() {
        var row = $scope.lastRow;
        $scope.addRowToTable({"key":row.key, "value":row.value});
        initLastRow();
      }
    }
  }
})
