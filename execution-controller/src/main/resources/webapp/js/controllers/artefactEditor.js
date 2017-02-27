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
angular.module('artefactEditor',['dataTable','step','dynamicForms'])

.controller('ArtefactEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $uibModal, $location, AuthService) {
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
            
      $scope.tabState = {'controls':true,'functions':false,'artefacts':false};
      
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
      
      $scope.execute = function() {
    	$location.path('/root/repository').search({repositoryId:'local',artefactid:$scope.artefactId});
      }
      
      $scope.executeWithDefaultParams = function() {
        var executionParams = {userID:AuthService.getContext().userID};
        executionParams.description = $scope.description;
        executionParams.mode = 'RUN';
        executionParams.artefact = {repositoryID:'local',repositoryParameters:{artefactid:$scope.artefact.id}};
        executionParams.exports = [];
        $http.post("rest/controller/execution",executionParams).then(
            function(response) {
              var eId = response.data;
              
              $location.$$search = {};
              $location.path('/root/executions/'+eId);

              $timeout(function() {
                $scope.onExecute();
              });
              
            });
      }
})

.directive('artefact', function($http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      artefactid: '=',
      handle: '='
    },
    controller: function($scope,$location,$rootScope, AuthService) {
      
      $scope.$watch('artefactid',function() {
        if($scope.artefactid!=null) {
          load(function(root) {
            tree.open_all();
            tree.select_node(root.id);
          });
        }
      });
      
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
        var label = artefact._class
        if(artefact._class=='CallFunction'&&artefact['function']) {
          try {
            label = JSON.parse(artefact['function']).name;
          } catch(e) {}
        } else if(artefact._class=='CallCompositeControl') {
          if(artefact.cachedArtefactName) {
            label =  artefact.cachedArtefactName;             
          }
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

        	  var artefactIcon = {'Default':'glyphicon-unchecked', 'CallCompositeControl':'glyphicon glyphicon-new-window', 'CallFunction':'glyphicon-record' ,'For':'glyphicon glyphicon-th','ForEach':'glyphicon glyphicon-th'}
        	  
        	  var icon = artefact._class in artefactIcon ? artefactIcon[artefact._class]:artefactIcon['Default'];
        	  
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
    	  var remote = !(function_.type=="composite")
    	  
    	  var newArtefact = {"function":JSON.stringify(function_.attributes),"functionId":function_.id,"remote":{"value":remote},"_class":"CallFunction"};
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
          var newArtefact = {"artefactId":id,"cachedArtefactName":artefact.attributes.name,"_class":"CallCompositeControl"};
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
    controller: function($scope,$location, AuthService) {
      
      var customEditors = {
          "Default":{template:"partials/artefacts/defaultArtefactForm.html"},
          "ForEach":{template:"partials/artefacts/forEach.html"},
          "For":{template:"partials/artefacts/for.html"},
          "Sequence":{template:"partials/artefacts/sequence.html"},
          "Return":{template:"partials/artefacts/return.html"},
          "Echo":{template:"partials/artefacts/echo.html"},
          "If":{template:"partials/artefacts/if.html"},
          "CallFunction":{template:"partials/artefacts/callFunction.html"},
          "Set":{template:"partials/artefacts/set.html"},
          "Sleep":{template:"partials/artefacts/sleep.html"},
          "Script":{template:"partials/artefacts/script.html"},
          "TestGroup":{template:"partials/artefacts/testGroup.html"},
          "Case":{template:"partials/artefacts/case.html"},
          "Switch":{template:"partials/artefacts/switch.html"},
          "RetryIfFails":{template:"partials/artefacts/retryIfFails.html"},
          "Check":{template:"partials/artefacts/check.html"},
          "CallCompositeControl":{template:"partials/artefacts/callCompositeControl.html"}
      }
      
      $scope.authService = AuthService;
      
      
      
      $scope.$watch('artefactid', function(artefactId) {
        if(artefactId) {
        	$http({url:"rest/controller/artefact/"+artefactId,method:"GET"}).then(function(response) {
        	  $scope.artefact = response.data;
        	  $scope.editor = ($scope.artefact._class in customEditors)?customEditors[$scope.artefact._class]:customEditors['Default'];
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
    template: '<div ng-include="editor.template"></div>'}
})
.controller('CallCompositeCtrl' , function($scope,$uibModal,$location,$http) {  
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
      $scope.artefact.cachedArtefactName = artefact.attributes.name;
      $scope.save();
    });
  }
})
.controller('CallFunctionCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {
  
  showTokenSelectionParameters = false;
  
  function loadFunction(id, callback) {
    $http({url:"rest/functions/"+id,method:"GET"}).then(function(response) {
      $scope.targetFunction = response.data;
      $scope.artefact['function'] = JSON.stringify($scope.targetFunction.attributes);
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
.controller('ForEachCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {  
  $scope.dataSourceTypes = [{name:"excel",label:"Excel"},{name:"csv",label:"CSV"},{name:"sql",label:"SQL"},
                            {name:"file",label:"Flat file"},{name:"folder",label:"Directory"}]
  
  $scope.$watch('artefact.dataSourceType',function(type,oldType){
    if(type&&(($scope.artefact&&_.isEmpty($scope.artefact.dataSource))||type!=oldType)) {
      //updateConfigurationForm(type);
      $http.get("rest/datapool/types/"+type).then(function(response){
        $scope.artefact.dataSource = response.data;
      })
    }
  });
})
.controller('DefaultArtefactFormCtrl' , function($scope) {
  $scope.getEditableArtefactProperties = function() {
    return _.without(_.keys($scope.artefact),'id','_id','root','attributes','childrenIDs','createSkeleton','_class','attachments')
  }
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
        $scope.argumentAsTable.push(row?row:{"key":"","value":""})
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
            return {"key":key,"value":val};
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
      
      $scope.lastRow = {key:"", value:""}

      $scope.commitLastRow = function() {
        var row = $scope.lastRow;
        $scope.addRowToTable({"key":row.key, "value":row.value});
        $scope.lastRow = {key:"", value:""}
      }
    }
  }
})
