angular.module('components',['step'])

.directive('statusDistribution', function() {
  return {
    restrict: 'E',
    scope: {
      progress: '='
    },
    templateUrl: 'partials/components/statusSummary.html',
    controller: function($scope, $http) {
      
    }
  };
})

.directive('gridStatusDistribution', function() {
  return {
    restrict: 'E',
    scope: {
      tokenGroup: '='
    },
    templateUrl: 'partials/components/gridStatusSummary.html',
    controller: function($scope, $http) {
      
    }
  };
})

.directive('executionLink', function() {
  return {
    restrict: 'E',
    scope: {
      executionId: '=',
      executionDescription: '='
    },
    templateUrl: 'partials/components/executionLink.html',
    controller: function($scope, $http) {
      $scope.$watch('executionId', function() {
        if($scope.executionId && !$scope.executionDescription) {
          $http.get('rest/controller/execution/' + $scope.executionId).then(function(response) {
            var data = response.data;
            $scope.executionDescription = data.description;
          })
        }
      })
    }
  };
})


.directive('planLink', function() {
  return {
    restrict: 'E',
    scope: {
      planRef: '=',
      description: '=?'
    },
    templateUrl: 'partials/components/planLink.html',
    controller: function($scope, $http) {
      
    }
  };
})

.directive('date', function() {
  return {
    restrict: 'E',
    scope: {
      time: '='
    },
    template: "<span>{{ time | date:'dd.MM.yyyy HH:mm:ss'}}</span>",
    controller: function() {  
    }
  };
})

.directive('jsonViewer', function() {
  return {
    restrict: 'E',
    scope: {
      json: '='
    },
    templateUrl: 'partials/components/jsonViewer.html',
    controller: function($scope, $http) {
      $scope.$watch('json', function() {
        $scope.jsonObject = (typeof $scope.json == 'string')?JSON.parse($scope.json):$scope.json
        $scope.keys = Object.keys($scope.jsonObject);
      })
    }
  };
})

.directive('stTextPopup', function(Dialogs) {
  return {
    restrict: 'A',
    scope: {
      ngModel: '='
    },
    link: function(scope, element, attr) {
      element.on('dblclick', function(event) {
        // Prevent default dragging of selected content
        event.preventDefault();
        modalResult = Dialogs.editTextField(scope);
      });  
    },
    controller: function($scope) {
    }
  };
})

.directive('resourceInput', function() {
  return {
    restrict: 'E',
    scope: {
      stType: '=',
      stBounded: "=?",
      stModel: '=',
      stOnChange: '&?',
      saveButton: '@?',
      saveButtonLabel: '@?',
      uploadOnly: '=?'
    },
    controller: function($scope,$http,$timeout,Upload,ResourceDialogs) {
      // Defaults
      if(!$scope.saveButtonLabel) {
        $scope.saveButtonLabel="Save"
      }
      
      $scope.stBounded = angular.isDefined($scope.stBounded)?$scope.stBounded:false;
      
      function resetStaginModel() {
        // Initialize staging model
        // Using stagingModel.value and not value directly to avoid two-way binding issues caused by the ng-if in the template
        // See https://stackoverflow.com/questions/12618342/ng-model-does-not-update-controller-value
        $scope.stagingModel = {value: $scope.stModel};
      }
      
      resetStaginModel();
      
      $scope.stagingModel = {value: $scope.stModel};
      
      // Called when the staging field is left
      $scope.blurStagingField = function() {
        // Commit the changes only if the save button is disabled.
        if(!$scope.showSaveButton()) {
          $scope.commitChanges();
        }
      }
      
      // Called when the save button is clicked or the staging field is blurred (depending if the save button is enabled or not)
      $scope.commitChanges = function() {
        $scope.stModel = $scope.stagingModel.value;
        callOnChangeListener();
      }
      
      $scope.showSaveButton = function() {
        return $scope.saveButton && $scope.saveButton=='true';
      }
      
      function setResourceIdToFieldValue(resourceId) {
        $scope.stModel = "resource:"+resourceId;
        callOnChangeListener();
      }
      
      $scope.openFileChooser = function() {
        $("#fileInput").click();
      }
      
      function callOnChangeListener() {
        if($scope.stOnChange) {
          $timeout(function() {
            $scope.stOnChange();
          })
        }
      }
      
      $scope.uploading = false;
      function upload(file, url) {
        if(file) {
          $scope.uploading = true;
          // do not perform any duplicate check for bounded resources as we do not want to link bounded resources 
          // to any other resource
          Upload.upload({
            url: url+"?type="+$scope.stType+"&duplicateCheck="+!$scope.stBounded,
            data: {file: file}
          }).then(function (resp) {
            $scope.uploading = false;

            var response = resp.data;
            var resourceId = response.resource.id; 
            if(!response.similarResources) {
              // No similar resource found
              setResourceIdToFieldValue(resourceId)
              $scope.resourceFilename = response.resource.resourceName
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
        if($scope.isResource() && !$scope.resourceNotExisting) {
          if(!$scope.stBounded) {
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
            // Update the current resource
            upload(file,'rest/resources/'+$scope.getResourceId()+'/content');
          }
        } else {
          // Creating a new resource
          upload(file,'rest/resources/content');
        }
      };
      
      $scope.selectResource = function() {
        ResourceDialogs.searchResource($scope.stType).then(function(resourceId) {
          setResourceIdToFieldValue(resourceId);
        })
      }
      
      $scope.resourceFilename = "";
      $scope.$watch('stModel',function(newValue) {
        if(newValue) {
          if($scope.isResource()) {
            $http.get("rest/resources/"+$scope.getResourceId()).then(
                function(response) {
                  var resource = response.data;
                  if(resource) {
                    $scope.resourceNotExisting = false;
                    $scope.resourceFilename = resource.resourceName;
                  } else {
                    $scope.resourceNotExisting = true;
                  }
                });
          } else {
            $scope.absoluteFilepath = $scope.stModel;
          }
        }
      })
      
      $scope.clear = function() {
        $scope.stModel = "";
        $scope.absoluteFilepath = "";
        resetStaginModel();
        callOnChangeListener();
      }
      
      $scope.isResource = function() {
        return $scope.stModel && (typeof $scope.stModel) == 'string' && $scope.stModel.indexOf('resource:')==0;
      }
      $scope.getResourceId = function() {
        return $scope.stModel.replace("resource:","");
      }
    },
    templateUrl: 'partials/components/resourceInput.html'}
})

.directive('resourceLabel', function() {
  return {
    restrict: 'E',
    scope: {
      stModel: '=', 
      stFormat: '@?'
    },
    controller: function($scope,$http,$timeout,Upload,ResourceDialogs) {
      $scope.resourceFilename = "";
      $scope.$watch('stModel',function(newValue) {
        if(newValue) {
          if($scope.isResource()) {
            $http.get("rest/resources/"+$scope.getResourceId()).then(
                function(response) {
                  var resource = response.data;
                  if(resource) {
                    $scope.resourceNotExisting = false;
                    $scope.resourceFilename = resource.resourceName;
                  } else {
                    $scope.resourceNotExisting = true;
                  }
                });
          } else {
            $scope.absoluteFilepath = $scope.stModel;
            $scope.fileName = $scope.absoluteFilepath.replace(/^.*[\\\/]/, '');
          }
        }
      })
      
      $scope.isResource = function() {
        return $scope.stModel && (typeof $scope.stModel) == 'string' && $scope.stModel.indexOf('resource:')==0;
      }
      $scope.getResourceId = function() {
        return $scope.stModel.replace("resource:","");
      }
    },
    templateUrl: 'partials/components/resourceLabel.html'}
})

.directive('executionResult', function() {
  return {
    restrict: 'E',
    scope: {
      execution: '='    
    },
    templateUrl: 'partials/components/executionResult.html',
    controller: function($scope) {
      $scope.iconClass = function() {
        var css = ''
        if($scope.execution) {
          css += 'glyphicon';
          if($scope.execution.status!='ENDED') {
            css += ' glyphicon-refresh icon-refresh-animate smaller';
          } else {
            if($scope.execution.result=='PASSED') {
              css += ' glyphicon-ok-sign'
            } else {
              css += ' glyphicon-exclamation-sign'
            }
          }       
        }
        return css;
      }
      $scope.result = function() {
        if($scope.execution) {
          if($scope.execution.status!='ENDED') {
            return $scope.execution.status
          } else {
            if($scope.execution.result) {
              return $scope.execution.result
            } else {
              // backward compatibility with executions run before 3.9
              return "UNKNOW";
            }
          }          
        } else {
          return "";
        }
      }
    }
  };
})

.directive('multiLineInput', function() {
  return {
    restrict: 'A',
    scope: {},
    link: function(scope, element, attrs) {
      // workaround for IE as IE only pastes the first line 
      if (window.clipboardData) {
        element.bind('paste', function (e) {
            var clipped = window.clipboardData.getData('Text');
            clipped = clipped.replace(/(\r\n|\n|\r)/gm, " ");
            $(this).val(clipped);
            return false;
        });
      }
    }
  };
})