angular.module('export',[])

.factory('ExportService', function($http,$timeout, Dialogs) {  
  var factory = {};

  factory.pollUrl = function(exportUrl) {
    var pollCount = 0;
    (function poll() {
      $http.get(exportUrl).then(function (response) {
        pollCount++;
        var status = response.data;
        if(status.ready) {
          var attachmentID = status.attachmentID;
          if (status.warnings !== undefined && status.warnings.length>0) {
            Dialogs.showListOfMsgs(status.warnings).then(function() {
              download(attachmentID);          
            })
          } else {
            download(attachmentID);
          }
          
          
        } else {
          if(pollCount==4) {
          }
          $timeout(poll, 500);              
        }
      });
    })();
  }
  
  var download = function(attachmentID){
    $.fileDownload('rest/resources/'+attachmentID+'/content')
    .done(function () { 
    })
    .fail(function () { alert('File download failed!'); });
  }
  
  factory.poll = function(exportId) {
    factory.pollUrl('rest/export/'+exportId+'/status');
  }
  
  factory.get = function (exportUrl) {
    $http.get(exportUrl).then(function(response){
      var exportStatus = response.data;
      factory.poll(exportStatus.id);
    })
  };

  return factory;
})