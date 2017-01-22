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
angular.module('artefactEditor',['dataTable','step'])

.controller('ArtefactEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $uibModal, $location, AuthService) {
      stateStorage.push($scope, 'artefacteditor', {});

      $scope.artefactId = $scope.$state;
      
      $scope.authService = AuthService;
            
      $scope.tabState = {'controls':true,'functions':false};
      
      $scope.controlsTable = {};
      $scope.controlsTable.columns = [ { "title" : "ID", "visible" : false },
                                   {"title" : "Name"},
                                   { "title" : "Actions", "width":"80px", "render": function ( data, type, row ) {
                                	 return '<button type="button" class="btn btn-default  btn-xs" aria-label="Left Align"' + 
                                         	'onclick="angular.element(\'#ArtefactEditorCtrl\').scope().addControl(\''+row[0]+'\')">' +
                             				'<span class="glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>' +
                             				'</button> '
                                   }} ];
      
      $http({url:"rest/controller/artefact/"+$scope.artefactId, method:"GET"}).then(function(response){
    	  $scope.artefact = response.data;
      })
      
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
      
      $scope.addFunction = function(id) {
    	$scope.handle.addFunction(id);
      }
      
      $scope.addControl = function(id) {
    	$scope.handle.addControl(id);
      }
      
      $scope.execute = function() {
    	$location.path('/root/repository').search({repositoryId:'local',artefactid:$scope.artefactId});
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
      
      var artefactid = $scope.artefactid;
      
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

      function load(callback) {
    	
    	$http({url:"rest/controller/artefact/"+artefactid+"/descendants", method:"GET"}).then(function(response){ 
    	    var data = response.data;
    	  	treeData = [];
        	function asJSTreeNode(currentNode) {
        	  var children = [];
        	  _.each(currentNode.children, function(child) {
        		children.push(asJSTreeNode(child))
        	  }) 	  
        	  var artefact = currentNode.artefact;
        	  var label = artefact._class
        	  if(artefact._class=='CallFunction'&&artefact['function']) {
        	    try {
        	      label = JSON.parse(artefact['function']).name;
        	    } catch(e) {}
        	  }        	  
        	  return { "id" : artefact.id, "children" : children, "text" : label }
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
      
      load(function(root) {
    	  
    	  tree.open_all();
    	  tree.select_node(root.id);
      });
      
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
    	  var tokenDefault = function_.type=="composite"?"{\"route\":\"local\"}":"{\"route\":\"remote\"}";
    	  
    	  var newArtefact = {"function":JSON.stringify(function_.attributes),"token":tokenDefault,"_class":"CallFunction"};
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
      
    },
    templateUrl: 'partials/artefact.html'}
})

.directive('artefactDetails', function($http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      artefactid: '=',
      readonly: '=',
      handle: '='
    },
    controller: function($scope,$location, AuthService) {
      
      $scope.authService = AuthService;
      
      $scope.$watch('artefactid', function() {
        if($scope.artefactid) {
        	var artefactid = $scope.artefactid;
        	
        	$http({url:"rest/controller/artefact/"+artefactid,method:"GET"}).then(function(response) {
        	  $scope.artefact = response.data;
        	})
        	
        	$scope.save = function() {
        	  $http.post("rest/controller/artefact/"+artefactid, $scope.artefact).then(function() {
        		
        	  });
        	}
        }
      })
      
      $scope.getEditableArtefactProperties = function() {
    	return _.without(_.keys($scope.artefact),'id','_id','root','attributes','childrenIDs','createSkeleton','_class','attachments')
      }
    	  
      
    },
    templateUrl: 'partials/artefactDetails.html'}
})