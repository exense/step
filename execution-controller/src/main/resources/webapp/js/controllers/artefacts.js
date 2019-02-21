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
angular.module('artefacts',['step'])

.factory('artefactTypes', function() {
  
  var registry = {};
  
  var api = {};
  
  function getType(typeName) {
    if(registry[typeName]) {
      return registry[typeName];
    } else {
      throw "Unknown artefact type "+typeName;
    }
  }
  
  api.register = function(typeName,typeInfo) {
    if(!typeInfo.label) {
      typeInfo.label = typeName;
    }
    registry[typeName] = typeInfo;
  }
  
  api.getEditor = function(typeName) {
    return getType(typeName).form;
  }

  api.getDefaultIcon = function(typeName) {
    return 'glyphicon-unchecked';
  }
  
  api.getIcon = function(typeName) {
    return getType(typeName).icon;
  }
  
  api.getLabel = function(typeName) {
    return getType(typeName).label;
  }
  
  api.getTypes = function() {
    return _.keys(registry);
  }
  
  return api;
})

.run(function(artefactTypes) {
  artefactTypes.register('TestSet',{icon:'glyphicon-folder-close', form:'partials/artefacts/testSet.html'});
  artefactTypes.register('TestCase',{icon:'glyphicon-list-alt', form:'partials/artefacts/testCase.html'});
  artefactTypes.register('TestScenario',{icon:'glyphicon-equalizer', form:'partials/artefacts/testScenario.html'});
  artefactTypes.register('CallPlan',{icon:'glyphicon-new-window', form:'partials/artefacts/callPlan.html'});
  artefactTypes.register('CallKeyword',{icon:'glyphicon-record', form:'partials/artefacts/callFunction.html'});
  artefactTypes.register('For',{icon:'glyphicon-th', form:'partials/artefacts/for.html'});
  artefactTypes.register('ForEach',{icon:'glyphicon-th', form:'partials/artefacts/forEach.html'});
  artefactTypes.register('While',{icon:'glyphicon-repeat', form:'partials/artefacts/while.html'});
  artefactTypes.register('DataSet',{icon:'glyphicon-th-large', form:'partials/artefacts/dataSet.html'});
  artefactTypes.register('Synchronized',{icon:'glyphicon-align-justify', form:'partials/artefacts/synchronized.html'});
  artefactTypes.register('Sequence',{icon:'glyphicon-align-justify', form:'partials/artefacts/sequence.html'});
  artefactTypes.register('Return',{icon:'glyphicon-share-alt', form:'partials/artefacts/return.html'});
  artefactTypes.register('Echo',{icon:'glyphicon-zoom-in', form:'partials/artefacts/echo.html'});
  artefactTypes.register('If',{icon:'glyphicon-unchecked', form:'partials/artefacts/if.html'});
  artefactTypes.register('Session',{icon:'glyphicon-magnet', form:'partials/artefacts/functionGroup.html'});
  artefactTypes.register('Set',{icon:'glyphicon-save', form:'partials/artefacts/set.html'});
  artefactTypes.register('Sleep',{icon:'glyphicon-hourglass', form:'partials/artefacts/sleep.html'});
  artefactTypes.register('Script',{icon:'glyphicon-align-left', form:'partials/artefacts/script.html'});
  artefactTypes.register('ThreadGroup',{icon:'glyphicon-resize-horizontal', form:'partials/artefacts/threadGroup.html'});
  artefactTypes.register('Switch',{icon:'glyphicon-option-vertical', form:'partials/artefacts/switch.html'});
  artefactTypes.register('Case',{icon:'glyphicon-minus', form:'partials/artefacts/case.html'});
  artefactTypes.register('RetryIfFails',{icon:'glyphicon-retweet', form:'partials/artefacts/retryIfFails.html'});
  artefactTypes.register('Check',{icon:'glyphicon-ok', form:'partials/artefacts/check.html'});
  artefactTypes.register('Assert',{icon:'glyphicon-ok', form:'partials/artefacts/assert.html'});
  artefactTypes.register('Placeholder',{icon:'glyphicon-unchecked', form:'partials/artefacts/placeholder.html'});
  artefactTypes.register('Export',{icon:'glyphicon-export', form:'partials/artefacts/export.html'});
});

angular.module('artefactsControllers',['dataTable','step','ngFileUpload','export'])
.controller('ArtefactListCtrl', [ '$scope', '$rootScope', '$compile', '$http', 'stateStorage', '$interval', '$uibModal', 'Dialogs', '$location', 'AuthService', 'ExportService',
    function($scope, $rootScope, $compile, $http, $stateStorage, $interval, $uibModal, Dialogs, $location, AuthService, ExportService) {
      $stateStorage.push($scope, 'artefacts', {});	

      $scope.autorefresh = true;
      
      $scope.authService = AuthService;
      
      $scope.editArtefact = function(id) {
    	$scope.$apply(function() {
    	  $location.path('/root/artefacteditor/' + id);
    	});
      }
      
      $scope.executeArtefact = function(id) {
    	$scope.$apply(function() {
    	  $location.path('/root/repository').search({repositoryId:'local',artefactid:id});
    	});
      }
      
      $scope.addArtefact = function() {
    	$http.get("rest/controller/artefact/types").then(function(response){ 
          $scope.artefactTypes = response.data;
          var modalInstance = $uibModal.open({
        	animation: $scope.animationsEnabled,
        	templateUrl: 'newArtefactModalContent.html',
        	controller: 'newArtefactModalCtrl',
        	resolve: {
        	  artefactTypes: function () {
        		return $scope.artefactTypes;
        	  }
        	}
          });
          
          modalInstance.result.then(function (functionParams) {
            reload();
          }, function () {}); 
        });
      }
      
      function reload() {
        $scope.table.Datatable.ajax.reload(null, false);
      }
      
      $scope.removeArtefact = function(id) {
        Dialogs.showDeleteWarning().then(function() {
          $http.delete("rest/controller/artefact/"+id).then(function() {
            reload();
          });
        })
      }
      
      $scope.copyArtefact = function(id) {
        $rootScope.clipboard = {object:"artefact",id:id};
      }
      
      $scope.pasteArtefact = function() {
        if($rootScope.clipboard && $rootScope.clipboard.object=="artefact") {
          $http.post("rest/controller/artefact/"+$rootScope.clipboard.id+"/copy")
          .then(function() {
            reload();
          });
        }
      }
      
      $scope.importArtefact = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'partials/artefactImportDialog.html',
          controller: 'importArtefactModalCtrl',
          resolve: {}
        });

        modalInstance.result.then(function () {
          reload();
        });
      }
      
      $scope.exportArtefacts = function() {
        ExportService.get("rest/export/artefacts")
      }
      
      $scope.table = {};

      $scope.tabledef = {uid:'artefacts'};
      
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {
          col.render = function(data, type, row) {
            return data
          };
        });
        _.each(_.where(columns,{'title':'Actions'}),function(col){
            col.title="Actions";
            col.searchmode="none";
            col.width="160px";
            col.render = function ( data, type, row ) {
            	var html = '<div class="input-group">' +
	            	'<div class="btn-group">' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" title="Edit plan" onclick="angular.element(\'#ArtefactListCtrl\').scope().editArtefact(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" title="Execute plan" onclick="angular.element(\'#ArtefactListCtrl\').scope().executeArtefact(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>';
            	
            	if(AuthService.hasRight('plan-write')) {
                html+='<button type="button" class="btn btn-default" aria-label="Left Align" title="Copy plan"  onclick="angular.element(\'#ArtefactListCtrl\').scope().copyArtefact(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon-copy" aria-hidden="true"></span>' +
                '</button> ';
              }
            	
            	if(AuthService.hasRight('plan-delete')) {
            	  html+='<button type="button" class="btn btn-default" aria-label="Left Align" title="Delete plan" onclick="angular.element(\'#ArtefactListCtrl\').scope().removeArtefact(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon glyphicon-trash" aria-hidden="true"></span>' +
                '</button> ';
            	}
            	html+='</div></div>';
            	return html;
            }
           });
        return columns;
      };
    } ])
    
.controller('newArtefactModalCtrl', function ($scope, $uibModalInstance, $http, $location, artefactTypes) {
  
  $scope.artefactTypes = artefactTypes;
  $scope.artefacttype = 'Sequence';
	
  $scope.attributes= {};
  
  $http.get("rest/screens/artefactTable").then(function(response){
    $scope.inputs=response.data;
  });	
  
  $scope.save = function (editAfterSave) {  
	$http.get("rest/controller/artefact/types/"+$scope.artefacttype).then(function(response) {
	  var artefact = response.data
		artefact.root = true;
		artefact.attributes = {};
		_.mapObject($scope.attributes,function(value,key) {
			  eval('artefact.'+key+"='"+value+"'");
		})
		$http.post("rest/controller/artefact", artefact).then(function(response) {
		  var artefact = response.data;
			$uibModalInstance.close(artefact);
			
			if(editAfterSave) {
				$location.path('/root/artefacteditor/' + artefact.id)
			}			
		});
	});
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.controller('selectArtefactModalCtrl', function ($scope, $uibModalInstance, $http) {
  
  $scope.selectArtefact = function(id) {
    $http({url:"rest/controller/artefact/"+id,method:"GET"}).then(function(response) {
      $uibModalInstance.close(response.data);
    }) 
  }
  
  $scope.table = {};

  $scope.tabledef = {}      
  
  $scope.tabledef.columns = function(columns) {
    _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns,{'title':'Actions'}),function(col){
        col.title="Actions";
        col.searchmode="none";
        col.width="160px";
        col.render = function ( data, type, row ) {
          var html = '<div class="input-group">' +
            '<div class="btn-group">' +
            '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#ArtefactListCtrl\').scope().selectArtefact(\''+row[0]+'\')">' +
            '<span class="glyphicon glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>';
          html+='</div></div>';
          return html;
        }
       });
    return columns;
  };


  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.controller('importArtefactModalCtrl', function ($scope, $http, $uibModalInstance, Upload, Dialogs) {
  
  $scope.resourcePath; 
  
  $scope.save = function() {
    if($scope.resourcePath) {
      $http({url:"rest/import/artefact",method:"POST",params:{path:$scope.resourcePath}}).then(function(response) {
        $uibModalInstance.close(response.data);
      })      
    } else {
      Dialogs.showErrorMsg("Upload not completed.");
    }
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})