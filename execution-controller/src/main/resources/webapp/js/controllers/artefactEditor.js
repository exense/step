angular.module('artefactEditor',['dataTable','step'])

.controller('ArtefactEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $modal, $location) {
      stateStorage.push($scope, 'artefacteditor', {});

      $scope.artefactId = $scope.$state;
            
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
      
      $http.get("rest/controller/artefact/types").success(function(data){ 
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
    controller: function($scope,$location) {
      
      var artefactid = $scope.artefactid;
      
      var tree;
      $('#jstree_demo_div').jstree(
				  {
					'core' : {
					  'data' : []
					}
				  });
      tree = $('#jstree_demo_div').jstree(true);
      
      $('#jstree_demo_div').on('changed.jstree', function (e, data) {
    	var selectedArtefact = tree.get_selected(true);
    	$scope.selectedArtefactId = selectedArtefact?(selectedArtefact.length>0?selectedArtefact[0].id:null):null;
    	$scope.$apply();
      })
      
      function load() {
    	
    	$http({url:"rest/controller/artefact/"+artefactid+"/descendants", method:"GET"}).success(function(data){ 
    	  	treeData = [];
        	function asJSTreeNode(currentNode) {
        	  var children = [];
        	  _.each(currentNode.children, function(child) {
        		children.push(asJSTreeNode(child))
        	  }) 	  
        	  var artefact = currentNode.artefact;
        	  return { "id" : artefact.id, "children" : children, "text" : artefact.name }
        	}
          
        	treeData.push(asJSTreeNode(data))
        	tree.settings.core.data = treeData;
        	tree.refresh();
        });
      }
      
      load();
      
      $scope.handle.addFunction = function(id) {
    	var selectedArtefact = tree.get_selected(true);
    	
    	$http.get("rest/functions/"+id).success(function(function_) {

    	  var newArtefact = {"name":function_.attributes.name,"functionName":function_.attributes.name,"_class":"step.artefacts.CallFunction"};
    	  $http.post("rest/controller/artefact/"+selectedArtefact[0].id+"/children",newArtefact).success(function(){
    		load();
    	  })
    		
    	});
      }
      
      $scope.handle.addControl = function(id) {
    	var selectedArtefact = tree.get_selected(true);
    	
    	$http.get("rest/controller/artefact/types/"+id).success(function(artefact) {
    	  $http.post("rest/controller/artefact/"+selectedArtefact[0].id+"/children",artefact).success(function(){
    		load();
    	  })
    	});
      }
      
      $scope.remove = function() {
    	var selectedArtefact = tree.get_selected(true)[0];
    	var parentid = tree.get_parent(selectedArtefact);
    	$http.delete("rest/controller/artefact/"+parentid+"/children/"+selectedArtefact.id).success(function() {
    	  load();
    	});
      }
      
      $scope.move = function(offset) {
    	var selectedArtefact = tree.get_selected(true)[0];
    	var parentid = tree.get_parent(selectedArtefact);
    	$http.post("rest/controller/artefact/"+parentid+"/children/"+selectedArtefact.id+"/move",offset).success(function() {
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
      handle: '='
    },
    controller: function($scope,$location) {
      
      $scope.$watch('artefactid', function() {
    	var artefactid = $scope.artefactid;
    	
    	$http({url:"rest/controller/artefact/"+artefactid,method:"GET"}).success(function(artefact) {
    	  $scope.artefact = artefact;
    	})
    	
    	$scope.save = function() {
    	  $http.post("rest/controller/artefact/"+artefactid, $scope.artefact).success(function() {
    		
    	  });
    	}
    	
      })
      
      $scope.getEditableArtefactProperties = function() {
    	return _.without(_.keys($scope.artefact),'id','_id','childrenIDs','createSkeleton','_class','attachments')
      }
    	  
      
    },
    templateUrl: 'partials/artefactDetails.html'}
})