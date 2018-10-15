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


.directive('fileUploadField', function() {
  return {
    restrict: 'E',
    scope: {
      file: '=',
      saveButton: '@',
      saveButtonLabel: '@',
      onFileSelected: '&'
    },
    controller: function($scope,$http,$timeout,Upload) {
      
      if(!$scope.saveButtonLabel) {
        $scope.saveButtonLabel="Save"
      }
      
      $scope.upload = function (file) {
        if(file) {
          Upload.upload({
            url: 'rest/files',
            data: {file: file}
          }).then(function (resp) {
            attachmentId = resp.data.attachmentId; 
            $scope.file = "attachment:"+attachmentId;
            notifyFileSelection();
          }, function (resp) {
            console.log('Error status: ' + resp.status);
          }, function (evt) {
            $scope.progress = parseInt(100.0 * evt.loaded / evt.total);
          });          
        }
      };
      $scope.isAttachment = function() {
        return typeof $scope.file == 'string' && $scope.file.indexOf('attachment:')==0;
      }
      $scope.getAttachmentId = function() {
        return $scope.file.replace("attachment:","");
      }
      
      $scope.attachmentFilename = "";
      $scope.$watch('file',function(newValue) {
        $scope.model.filePath = newValue;
        if(newValue && $scope.isAttachment()) {
          $http.get("rest/files/"+$scope.getAttachmentId()+"/name").then(
            function(response) {
              $scope.attachmentFilename = response.data;
            });
        }
      })
      
      $scope.clear = function() {
        $http.delete("rest/files/"+$scope.getAttachmentId());
        $scope.file = '';
      }
      
      // Using model.filePath and not filePath directly to avoid two-way binding issues caused by the ng-if in the template
      // See https://stackoverflow.com/questions/12618342/ng-model-does-not-update-controller-value
      $scope.model = {filePath: $scope.file};
      
      $scope.blur = function() {
        if(!$scope.showSaveButton()) {
          $scope.save();
        }
       }
      
      $scope.showSaveButton = function() {
        return $scope.saveButton && $scope.saveButton=='true';
      }
      
      $scope.save = function() {
       $scope.file = $scope.model.filePath;
       notifyFileSelection();
      }
      
      function notifyFileSelection() {
        $timeout(function() {
          $scope.onFileSelected(); 
        })
      }
    },
    templateUrl: 'partials/components/fileUploadField.html'}
})