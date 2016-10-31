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
angular.module('artefactsControllers',['dataTable','step'])

.controller('ArtefactListCtrl', [ '$scope', '$compile', '$http', 'stateStorage', '$interval', '$modal','$location', 'AuthService',
    function($scope, $compile, $http, $stateStorage, $interval, $modal, $location, AuthService) {
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
    	$http.get("rest/controller/artefact/types").success(function(data){ 
          $scope.artefactTypes = data;
          var modalInstance = $modal.open({
        	animation: $scope.animationsEnabled,
        	templateUrl: 'newArtefactModalContent.html',
        	controller: 'newArtefactModalCtrl',
        	resolve: {
        	  artefactTypes: function () {
        		return $scope.artefactTypes;
        	  }
        	}
          });
          
          modalInstance.result.then(function (functionParams) {}, function () {}); 
        });
      }
      
      $scope.removeArtefact = function(id) {
    	$http.delete("rest/controller/artefact/"+id).success(function() {
    	  $scope.table.Datatable.ajax.reload(null, false);
    	});
      }
      
      $scope.table = {};

      $scope.tabledef = {}
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
            col.width="120px";
            col.render = function ( data, type, row ) {
            	var html = '<div class="input-group">' +
	            	'<div class="btn-group">' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#ArtefactListCtrl\').scope().editArtefact(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>' +
	            	'<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#ArtefactListCtrl\').scope().executeArtefact(\''+row[0]+'\')">' +
	            	'<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>';
            	
            	if(AuthService.hasRight('plan-delete')) {
            	  html+='<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#ArtefactListCtrl\').scope().removeArtefact(\''+row[0]+'\')">' +
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
    
.controller('newArtefactModalCtrl', function ($scope, $modalInstance, $http, $location, artefactTypes) {
  
  $scope.artefactTypes = artefactTypes;
  $scope.artefacttype = 'Sequence';
	
  $scope.attributes= {};
  
  $http.get("rest/screens/artefactTable").success(function(data){
    $scope.inputs=data;
  });	
  
  $scope.save = function (editAfterSave) {  
	$http.get("rest/controller/artefact/types/"+$scope.artefacttype).success(function(artefact) {
		artefact.root = true;
		artefact.attributes = {};
		_.mapObject($scope.attributes,function(value,key) {
			  eval('artefact.'+key+"='"+value+"'");
		})
		$http.post("rest/controller/artefact", artefact).success(function(artefact) {
			$modalInstance.close(artefact);
			
			if(editAfterSave) {
				$location.path('/root/artefacteditor/' + artefact.id)
			}			
		});
	});
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
});