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
angular.module('dynamicForms',['step'])

.directive('dynamicCheckbox', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      onSave: '&'
    },
    controller: function($scope) {
      $scope.isDynamic = function() {
        return $scope.dynamicValue&&$scope.dynamicValue.value==undefined;
      }
      $scope.useConstantValue = function() {
        $scope.dynamicValue.value = '';
      }
      $scope.useDynamicExpression = function() {
        delete $scope.dynamicValue.value;
      }
    },
    templateUrl: 'partials/dynamicforms/checkbox.html'}
})
.directive('dynamicTextfield', function() {
  return {
    restrict: 'E',
    scope: {
      dynamicValue: '=',
      label: '=',
      onSave: '&'
    },
    controller: function($scope) {
      $scope.isDynamic = function() {
        return $scope.dynamicValue&&$scope.dynamicValue.value==undefined;
      }
      $scope.useConstantValue = function() {
        $scope.dynamicValue.value = '';
      }
      $scope.useDynamicExpression = function() {
        delete $scope.dynamicValue.value;
      }
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
      $scope.isDynamic = function() {
        return $scope.dynamicValue&&$scope.dynamicValue.value==undefined;
      }
      $scope.useConstantValue = function() {
        $scope.dynamicValue.value = '';
        $scope.onSave();
      }
      $scope.useDynamicExpression = function() {
        delete $scope.dynamicValue.value;
        $scope.onSave();
      }
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
  $scope.isDynamic = function() {
    return $scope.dynamicValue&&$scope.dynamicValue.value==undefined;
  }
  $scope.useConstantValue = function() {
    $scope.dynamicValue.value = '';
  }
  $scope.useDynamicExpression = function() {
    delete $scope.dynamicValue.value;
  }
})