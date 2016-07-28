angular.module('artefactEditor',['dataTable','step'])

.controller('ArtefactEditorCtrl', function($scope, $compile, $http, stateStorage, $interval, $modal) {
      stateStorage.push($scope, 'artefacteditor', {});

      $scope.handle = {};
      
      
      $scope.table = {};

      $scope.tabledef = {}
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {

        });
        _.each(_.where(columns, { 'title' : 'Handler chain' }), function(col) {
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