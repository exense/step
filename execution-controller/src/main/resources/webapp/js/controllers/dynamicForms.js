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
.directive('dynamicResourceInput', function() {
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
      
      $scope.$watch('dynamicValue.value',function(newValue) {
        if(newValue) {
          $scope.onSave();
        }
      })
    },
    templateUrl: 'partials/dynamicforms/dynamicResourceInput.html'}
})