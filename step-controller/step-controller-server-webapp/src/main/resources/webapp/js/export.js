/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
angular.module('export',[])

.factory('ExportService', function($http,$timeout, Dialogs) {  
  var factory = {};

  factory.pollUrl = function(exportUrl, callback) {
    var pollCount = 0;
    (function poll() {
      $http.get(exportUrl).then(function (response) {
        pollCount++;
        var status = response.data;
        if(status.ready) {
          if(callback) {
            callback();
          }
          var attachmentID = status.attachmentID;
          if (status.warnings !== undefined && status.warnings !== null &&
              status.warnings.length > 0) {
            Dialogs.showListOfMsgs(status.warnings).then(function() {
              downloadExport(attachmentID);
            })
          } else {
            downloadExport(attachmentID);
          }
          
          
        } else {
          if(pollCount==4) {
          }
          $timeout(poll, 500);              
        }
      });
    })();
  }

  var downloadExport = function(attachmentID) {
    if (attachmentID !== undefined && attachmentID !== null) {
      download(attachmentID);
    } else {
      Dialogs.showErrorMsg("The export file could not be created, check the controller logs for more details");
    }
  }
  
  var download = function(attachmentID){
    $.fileDownload('rest/resources/'+attachmentID+'/content')
    .done(function () { 
    })
    .fail(function () { alert('File download failed!'); });
  }
  
  factory.poll = function(exportId, callback) {
    factory.pollUrl('rest/export/'+exportId+'/status', callback);
  }
  
  factory.get = function (exportUrl, callback) {
    $http.get(exportUrl).then(function(response){
      var exportStatus = response.data;
      factory.poll(exportStatus.id, callback);
    })
  };

  return factory;
})
