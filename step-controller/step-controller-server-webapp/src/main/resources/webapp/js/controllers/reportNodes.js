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
angular.module('reportNodes',['step','artefacts','screenConfigurationControllers'])

.factory('ReportNodeCommons', function(ScreenTemplates) {
  
  var api = {};
  
  var functionAttributes = [];
  
  ScreenTemplates.getScreenInputsByScreenId('functionTable').then(function(attributes) {
    functionAttributes = _.sortBy(attributes,function(value){return value.id});
  })
  
  api.getFunctionAttributes = function() {
    return functionAttributes;
  } 
  
  return api;
})

.directive('reportnode', function() {
  return {
    restrict: 'E',
    scope: {
      id: '=',
      showArtefact: '='
    },
    templateUrl: 'partials/reportnodes/reportNode.html',
    controller: function($scope, $http) {
      var reportNodeTypes = {
          "step.artefacts.reports.CallFunctionReportNode":{template:"partials/reportnodes/callFunctionReportNode.html"},
          "step.artefacts.reports.EchoReportNode":{template:"partials/reportnodes/echo.html"},
          "step.artefacts.reports.AssertReportNode":{template:"partials/reportnodes/assert.html"}
      }
      
      $scope.children = [];
      $scope.$watch('id',function(nodeId) {
        if(nodeId) {
          $http.get('rest/controller/reportnode/'+nodeId).then(function(response) {
            var node = response.data;
            $scope.node = node;
            $scope.reportNodeType = (node._class in reportNodeTypes)?reportNodeTypes[node._class]:reportNodeTypes['Default'];
            $http.get('rest/controller/reportnode/'+node.id+"/children").then(function(response) {
              $scope.children = response.data;
            })
          })
        }
      })
    }
  };
})

.directive('reportnodeShort', function(ReportNodeCommons) {
  return {
    restrict: 'E',
    scope: {
      node: '=',
      executionViewServices: '=',
      includeStatus: '=',
      showDetails: '='
    },
    templateUrl: 'partials/reportnodes/reportNodeShort.html',
    controller: function($scope,$http,artefactTypes) {
      $scope.isShowDetails = $scope.showDetails;
      
      $scope.artefactTypes = artefactTypes;
      $scope.concatenate = function(map) {
        var result = "";
        if(map) {
          _.each(ReportNodeCommons.getFunctionAttributes(),function(key){
            var attributeValue = map[key.id.replace('attributes.','')];
            if(attributeValue) {
              result+=attributeValue+".";
            }
          });
        }
        return result.substring(0,result.length-1);
      };
      $scope.reportNodeId = $scope.node.id;
      $scope.$watch('node',function(node, oldStatus) {
        if(node) {
          $scope.reportNodeId = $scope.node.id;
        }
      })
       
      $scope.toggleDetails = function() {
        $scope.isShowDetails = !$scope.isShowDetails;
      }
    }
  };
})

.directive('reportNodeIcon', function() {
  return {
    restrict: 'E',
    scope: {
      node: '='
    },
    templateUrl: 'partials/reportnodes/reportNodeIcon.html',
    controller: function($scope,artefactTypes) {
      $scope.artefactTypes = artefactTypes;
      
      $scope.cssClass = function() {
        var cssClass = '';
        var node = $scope.node;
        if(node) {
          var cssClass = 'reportNodeIcon status-'+node.status+' glyphicon ';
          if(node.resolvedArtefact) {
            cssClass += artefactTypes.getIcon(node.resolvedArtefact._class)
          } else {
            cssClass += artefactTypes.getDefaultIcon()
          }
        }
        return cssClass;
      }
    }
  };
})

.directive('attachments', function() {
  return {
    restrict: 'E',
    scope: {
      node: '='
    },
    templateUrl: 'partials/reportnodes/attachments.html',
    controller: function($scope) {
      $scope.$watch("node",function(node) {
        $scope.attachments = node.attachments;
      })
    }
  };
})

.directive('attachmentsPreview', function() {
  return {
    restrict: 'E',
    scope: {
      attachments: '='
    },
    templateUrl: 'partials/reportnodes/attachmentsPreview.html',
    controller: function($scope, $http) {

    }
  };
})

.directive('attachmentPreview', function() {
  return {
    restrict: 'E',
    scope: {
      attachment: '='
    },
    templateUrl: 'partials/reportnodes/attachmentPreview.html',
    controller: function($scope, $http) {
      $scope.$watch("attachment",function(attachment) {

      })
      
      $scope.isImage = function() {
        return $scope.attachment.name.endsWith(".jpg") || $scope.attachment.name.endsWith(".png")
      }
      
      $scope.isText = function() {
        return $scope.attachment.name.endsWith(".log") || $scope.attachment.name.endsWith(".txt")
      }
      
      $scope.showLabel = function() {
        return $scope.isText() || !$scope.attachment.name.startsWith("screenshot.");
      }
    }
  };
})

.directive('reportnodeStatus', function() {
  return {
    restrict: 'E',
    scope: {
      status: '='
    },
    templateUrl: 'partials/reportnodes/reportNodeStatus.html',
    controller: function($scope) {
    }
  };
})