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
var dynamicForms = angular.module('dynamicForms',['step','ngFileUpload'])

function initDynamicFormsCtrl($scope) {
  $scope.isDynamic = function() {
    if($scope.dynamicValue) {
      return $scope.dynamicValue.dynamic;
    } else {
      return false;
    }
  }
  $scope.useConstantValue = function() {
    $scope.dynamicValue.dynamic = false;
    $scope.dynamicValue.value = $scope.dynamicValue.expression;
    delete $scope.dynamicValue.expression;
    $scope.onSave();
  }
  
  $scope.useDynamicExpression = function() {
    $scope.dynamicValue.dynamic = true;
    $scope.dynamicValue.expression = $scope.dynamicValue.value;
    delete $scope.dynamicValue.value;
    $scope.onSave();
  }
} 

dynamicForms.directive('dynamicCheckbox', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      onSave: '&'
    },
    controller: function($scope) {
      initDynamicFormsCtrl($scope);
    },
    templateUrl: 'partials/dynamicforms/checkbox.html'}
})
.directive('dynamicTextfield', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      tooltip: '=',
      onSave: '&'
    },
    controller: function($scope) {
      initDynamicFormsCtrl($scope);
    },
    templateUrl: 'partials/dynamicforms/textfield.html'}
})
.directive('dynamicJsonEditor', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      onSave: '&'
    },
    controller: function($scope) {
      initDynamicFormsCtrl($scope);
      $scope.save = function(json) {
        $scope.dynamicValue.value = json;
        $scope.onSave();
      }
    },
    templateUrl: 'partials/dynamicforms/jsonEditor.html'}
})
.directive('expressionInput', function() {
  return {
    controller: function() {
    },
    templateUrl: 'partials/dynamicforms/expressionInput.html'}
})
.controller('dynamicValueCtrl',function($scope) {
  initDynamicFormsCtrl($scope);
})
.directive('fileInput', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      tooltip: '=',
      onSave: '&'
    },
    controller: function($scope,$http,Upload) {
      initDynamicFormsCtrl($scope);
      $scope.upload = function (file) {
        if(file) {
          Upload.upload({
            url: 'rest/files',
            data: {file: file}
          }).then(function (resp) {
            attachmentId = resp.data.attachmentId; 
            $scope.dynamicValue.value = "attachment:"+attachmentId;
            $scope.onSave();
          }, function (resp) {
            console.log('Error status: ' + resp.status);
          }, function (evt) {
            $scope.progress = parseInt(100.0 * evt.loaded / evt.total);
          });          
        }
      };
      $scope.isAttachment = function() {
        return $scope.dynamicValue && (typeof $scope.dynamicValue.value) == 'string' && $scope.dynamicValue.value.indexOf('attachment:')==0;
      }
      $scope.getAttachmentId = function() {
        return $scope.dynamicValue.value.replace("attachment:","");
      }
      
      $scope.attachmentFilename = "";
      $scope.$watch('dynamicValue.value',function(newValue) {
        if(newValue && $scope.isAttachment()) {
          $http.get("rest/files/"+$scope.getAttachmentId()+"/name").then(
            function(response) {
              $scope.attachmentFilename = response.data;
            });
        }
      })
      
      $scope.clear = function() {
        $http.delete("rest/files/"+$scope.getAttachmentId());
        $scope.dynamicValue.value = '';
      }
    },
    templateUrl: 'partials/dynamicforms/fileInput.html'}
})
.directive('resourceInput', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      type: '=',
      tooltip: '=',
      onSave: '&'
    },
    controller: function($scope,$http,Upload,Dialogs,ResourceDialogs) {
      initDynamicFormsCtrl($scope);
      
      $scope.uploading = false;
      
      function setResourceIdToFieldValue(resourceId) {
        $scope.dynamicValue.value = "resource:"+resourceId;
      }
      
      function upload(file, url) {
        $scope.uploading = true;
        if(file) {
          Upload.upload({
            url: url+"?type="+$scope.type,
            data: {file: file}
          }).then(function (resp) {
            $scope.uploading = false;

            var response = resp.data;
            var resourceId = response.resource.id; 
            if(!response.similarResources) {
              // No similar resource found
              setResourceIdToFieldValue(resourceId)
              $scope.onSave();
            } else {
              if(response.similarResources.length >= 1) {
                ResourceDialogs.showFileAlreadyExistsWarning(response.similarResources).then(function(existingResourceId){
                  if(existingResourceId) {
                    // Linking to an existing resource
                    setResourceIdToFieldValue(existingResourceId);
                    // Delete the previously uploaded resource a
                    $http.delete("rest/resources/"+resourceId);
                  } else {
                    // Creating a new resource
                    setResourceIdToFieldValue(resourceId);
                  }
                  $scope.onSave();
                })
              }
            }
          }, function (resp) {
            console.log('Error status: ' + resp.status);
            $scope.uploading = false;
          }, function (evt) {
            $scope.progress = parseInt(100.0 * evt.loaded / evt.total);
          });
        }
      }
      
      $scope.upload = function (file) {
        if($scope.isResource()) {
          ResourceDialogs.showUpdateResourceWarning().then(function(updateResource){
            if(updateResource) {
              // Updating resource
              upload(file,'rest/resources/'+$scope.getResourceId()+'/content');
            } else {
              // Creating a new resource
              upload(file,'rest/resources/content');              
            }
          })
        } else {
          // Creating a new resource
          upload(file,'rest/resources/content');
        }
      };
      
      $scope.selectResource = function() {
        ResourceDialogs.searchResource().then(function(resourceId) {
          setResourceIdToFieldValue(resourceId);
          $scope.onSave();
        })
      }
      
      $scope.isResource = function() {
        return $scope.dynamicValue && (typeof $scope.dynamicValue.value) == 'string' && $scope.dynamicValue.value.indexOf('resource:')==0;
      }
      $scope.getResourceId = function() {
        return $scope.dynamicValue.value.replace("resource:","");
      }
      
      $scope.resourceFilename = "";
      $scope.$watch('dynamicValue.value',function(newValue) {
        if(newValue && $scope.isResource()) {
          $http.get("rest/resources/"+$scope.getResourceId()).then(
            function(response) {
              var resource = response.data;
              if(resource) {
                $scope.resourceNotExisting = false;
                $scope.resourceFilename = resource.attributes.name;
                if(resource.attributes.name != resource.resourceName) {
                  $scope.resourceFilename += " ("+resource.resourceName+")"
                }
              } else {
                $scope.resourceNotExisting = true;
              }
            });
        }
      })
      
      $scope.clear = function() {
        $scope.dynamicValue.value = '';
      }
    },
    templateUrl: 'partials/dynamicforms/resourceInput.html'}
})