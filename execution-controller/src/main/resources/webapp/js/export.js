angular.module('export',[])

.factory('ExportService', function($http,$timeout) {  
  var factory = {};

  factory.get = function (exportUrl) {
    $http.get(exportUrl).then(function(response){
      var exportStatus = response.data;
      var pollCount = 0;
      (function poll() {
        $http.get('rest/export/'+exportStatus.id+'/status').then(function (response) {
          pollCount++;
          var status = response.data;
          if(status.ready) {
            var attachmentID = status.attachmentID;
            $.fileDownload('files?uuid='+attachmentID+'&deleteAfterDownload=true')
            .done(function () { 
              
              
            })
            .fail(function () { alert('File download failed!'); });
          } else {
            if(pollCount==4) {
              var modalInstance = $uibModal.open({
                templateUrl: 'partials/exportStatusDialog.html',
                controller: 'importArtefactModalCtrl',
                resolve: {}
              });

              modalInstance.result.then(function (artefact) {
                $scope.function_.artefactId = artefact.id;
              });
            }
            $timeout(poll, 500);              
          }
        });
      })();
    })
  };

  return factory;
})