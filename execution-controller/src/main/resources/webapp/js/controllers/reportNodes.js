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
angular.module('reportNodes',['step','artefacts'])

.directive('reportnode', function() {
  return {
    restrict: 'E',
    scope: {
      node: '='
    },
    templateUrl: 'partials/reportnodes/reportNode.html',
    controller: function($scope, $http) {
      var reportNodeTypes = {
          "step.artefacts.reports.CallFunctionReportNode":{template:"partials/reportnodes/callFunctionReportNode.html"},
          "step.artefacts.reports.EchoReportNode":{template:"partials/reportnodes/echo.html"},
          "step.artefacts.reports.AssertReportNode":{template:"partials/reportnodes/assert.html"}
      }
      
      $scope.children = [];
      $scope.$watch('node',function(node, oldStatus) {
        if(node) {
          $scope.reportNodeType = (node._class in reportNodeTypes)?reportNodeTypes[node._class]:reportNodeTypes['Default'];
          $http.get('rest/controller/reportnode/'+node.id+"/children").then(function(response) {
            $scope.children = response.data;
          })
          $http.get('rest/controller/artefact/'+node.artefactID).then(function(response) {
            $scope.artefact = response.data;
          })
        }
      })
    }
  };
})

.directive('reportnodeShort', function() {
  return {
    restrict: 'E',
    scope: {
      node: '=',
      executionViewServices: '=',
      includeStatus: '='
    },
    templateUrl: 'partials/reportnodes/reportNodeShort.html',
    controller: function($scope,$http,artefactTypes) {
      $scope.isShowDetails = false;
      
      $scope.artefactTypes = artefactTypes;
      $scope.concatenate = function(map) {
        var result = "";
        _.each(_.keys(map).sort(),function(key){
          result+=map[key]+".";
        });
        return result.substring(0,result.length-1);
      };
      
      $scope.toggleDetails = function() {
        $scope.isShowDetails = !$scope.isShowDetails;
        if($scope.isShowDetails) {
          var id = $scope.node._id.$oid?$scope.node._id.$oid:$scope.node.id;
          $http.get('rest/controller/reportnode/'+id).then(function(response) {
            $scope.reportNode = response.data;
            $scope.isShowDetails = true;
          })
        }
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
        if(node.attachments) {
          $scope.attachments = [];
          _.each(node.attachments, function(attachment) {
            $scope.attachments.push({id:attachment._id?attachment._id.$oid:attachment.id,name:attachment.name});
          })
        }
      })
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