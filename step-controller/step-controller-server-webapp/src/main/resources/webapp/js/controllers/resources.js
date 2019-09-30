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
angular.module('resourcesControllers',['tables','step'])

.run(function(ViewRegistry) {
  ViewRegistry.registerView('resources','partials/resources/resourceList.html');
  ViewRegistry.registerCustomMenuEntry('Resources','resources');
})

.controller('ResourceListCtrl', function($rootScope, $scope, $http, stateStorage, Dialogs, ResourceDialogs, AuthService) {
    stateStorage.push($scope, 'resources', {});	
    $scope.authService = AuthService;
    
    function reload() {
      $scope.tableHandle.reload();
    }
    
    $scope.addResource = function() {
      //ParameterDialogs.editParameter(null, function() {reload()});
    }

    $scope.editResource = function(id) {
      ResourceDialogs.editResource(id, function() {reload()});
    }
    
    $scope.deleteResource = function(id) {
      Dialogs.showDeleteWarning().then(function() {
        $http.delete("rest/resources/"+id).then(function() {
          reload();
        });
      })
    }
    
    $scope.tableHandle = {};
  })

.factory('ResourceDialogs', function ($uibModal, $http, Dialogs) {
  
  function openModal(id) {
    var modalInstance = $uibModal.open({
        templateUrl: 'partials/resources/editResourceDialog.html',
        controller: 'editResourceCtrl',
        resolve: {id: function () {return id;}}
      });

      return modalInstance.result;
  }
  
  var dialogs = {};
  
  dialogs.editResource = function(id, callback) {
    openModal(id).then(function() {
      if(callback){callback()};
    })
  }
  
  dialogs.searchResource = function(type) {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/resources/searchResourceDialog.html',
      controller: 'searchResourceCtrl',
      resolve: {type: function() {return type}}
    })

    return modalInstance.result;    
  }
  
  dialogs.showFileAlreadyExistsWarning = function(similarResources) {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/resources/fileAlreadyExistsWarning.html',
      controller: 'fileAlreadyExistsWarningCtrl',
      resolve: {similarResources: function() {return similarResources}}
    })

    return modalInstance.result;    
  }
  
  dialogs.showUpdateResourceWarning = function(resource) {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/resources/updateResourceWarning.html',
      controller: 'updateResourceWarningCtrl',
      resolve: {resource: function() {return resource}}
    })

    return modalInstance.result;    
  }
  
  return dialogs;
})

.controller('editResourceCtrl', function ($scope, $uibModalInstance, $uibModalStack, $http, AuthService, Upload, id) {
  
  function loadResource(id) {
    $http.get("rest/resources/"+id).then(function(response){
      $scope.resource = response.data;
    })    
  }
  
  if(id==null) {
    $http.get("rest/resources").then(function(response){
      $scope.resource = response.data;
    })  
  } else {
    $scope.mode = 'edit';
    loadResource(id);
  }
  
  $scope.uploading = false;

  $scope.upload = function (file) {
    if(file) {
      $scope.uploading = true;
      Upload.upload({
        url: 'rest/resources/'+$scope.resource.id+'/content',
        data: {file: file}
      }).then(function (resp) {
        // Reload resource to get the updated resourceName
        loadResource(id);
        $scope.uploading = false;
      }, function (resp) {
        console.log('Error status: ' + resp.status);
        $scope.uploading = false;
      }, function (evt) {
        $scope.progress = parseInt(100.0 * evt.loaded / evt.total);
      });
    }
  };

  $scope.save = function () {
    $http.post("rest/resources",$scope.resource).then(function(response) {
      $uibModalInstance.close();
    })
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
  
  $scope.$watch(function() {
    return $('.modal').length;
  }, function(val) { // every time the number of modals changes
    if (val > 0) {
      $uibModalStack.getTop().value.backdrop = 'static'; // disable default close behaviour
      $('.modal').on('mousedown', function(e) {
        if (e.which === 1) { // left click
          //close top modal when clicking anywhere, you can close all modals using $modalStack.dismissAll() instead
          $uibModalStack.getTop().key.dismiss();
        }
      });
      $('.modal-content').on('mousedown', function(e) {
        e.stopPropagation(); // avoid closing the modal when clicking its body
      });
    }
  });
})

.controller('searchResourceCtrl', function ($scope, $uibModalInstance, $http, AuthService, type) {

  $scope.type = type;
  
  $scope.selectResource = function (id) {
    $uibModalInstance.close(id);
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.controller('fileAlreadyExistsWarningCtrl', function ($scope, $uibModalInstance, $http, AuthService, similarResources) {

  $scope.similarResources = similarResources;
  
  $scope.selectResource = function (id) {
    $uibModalInstance.close(id);
  }
  
  $scope.createNewResource = function () {
    $uibModalInstance.close(null);
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})

.controller('updateResourceWarningCtrl', function ($scope, $uibModalInstance, $http, AuthService, resource) {

  $scope.resource = resource;
  
  $scope.updateResource = function () {
    $uibModalInstance.close(true);
  }
  
  $scope.createNewResource = function () {
    $uibModalInstance.close(false);
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})