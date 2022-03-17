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
    if ($scope.updateConstantValue) {
      $scope.updateConstantValue();
    }
    delete $scope.dynamicValue.expression;
    $scope.onSave();
  }

  $scope.useDynamicExpression = function() {
    if ($scope.dynamicValue) {
      $scope.dynamicValue.dynamic = true;
      $scope.dynamicValue.expression = $scope.dynamicValue.value;
      delete $scope.dynamicValue.value;
      $scope.onSave();
    }
  }

  $scope.keydownUseDynamicExpression = function(event) {
    var x = event.which || event.keyCode;
    if (x === 32 || x === 13 ){
      $scope.useDynamicExpression();
    }
  }

  $scope.keydownUseConstantValue = function(event) {
    var x = event.which || event.keyCode;
    if (x === 32 || x === 13){
      $scope.useConstantValue();
    }
  }
}

dynamicForms.directive('dynamicCheckbox', function() {
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
      $scope.updateConstantValue = function () {
        if ($scope.dynamicValue.value === "false") {
          $scope.dynamicValue.value = false;
        } else if ($scope.dynamicValue.value === "true") {
          $scope.dynamicValue.value = true;
        }
      }
    },
    templateUrl: 'partials/dynamicforms/checkbox.html'}
})
  .directive('dynamicTextfield', function() {
    return {
      restrict: 'E',
      scope: {
        dynamicValue: '=',
        defaultValue: '=?',
        label: '=',
        tooltip: '=',
        onSave: '&'
      },
      controller: function($scope,Dialogs) {
        if ($scope.defaultValue && angular.isUndefined($scope.dynamicValue)) {
          $scope.dynamicValue = $scope.defaultValue
        }
        initDynamicFormsCtrl($scope);
        $scope.editConstantValue = function() {
          Dialogs.enterValue('Free text editor', $scope.dynamicValue.value, 'lg','enterTextValueDialog',function(value) {
            $scope.dynamicValue.value = value;
            $scope.onSave();
          });
        }

        $scope.editDynamicExpression = function() {
          Dialogs.enterValue('Free text editor', $scope.dynamicValue.expression, 'lg','enterTextValueDialog',function(value) {
            $scope.dynamicValue.expression = value;
            $scope.onSave();
          });
        }

        $scope.keydownEditConstantValue= function(event) {
          var x = event.which || event.keyCode;
          if (x === 32 || x === 13 ){
            $scope.editConstantValue();
          }
        }

        $scope.keydownEditDynamicExpression= function(event) {
          var x = event.which || event.keyCode;
          if (x === 32 || x === 13 ){
            $scope.editDynamicExpression();
          }
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
        type: '@',
        onSave: '&'
      },
      controller: function($scope, $attrs, $http, ScreenTemplates, EntityScopeResolver) {

        $scope.argumentAsTable = [];
        $scope.localModel = {json: ''};
        $scope.sorting=[];

        $scope.type = $attrs.type;
        initDynamicFormsCtrl($scope);
        $scope.save = function(json) {
          $scope.dynamicValue.value = json;

          if ($scope.type === 'keyword') {
            refresh({value: json});
          }

          $scope.onSave();
        }

        function refresh(newVal) {
          if (newVal !== undefined && newVal.value !== undefined) {
            $scope.editKeyword.value = $scope.dynamicValue.value;
            $scope.localModel.json = newVal.value;
            updateEditors(false);
            $scope.sortArgumentAsTable();

            $scope.keyword = '';
            keywordLookup().then((keyword) => {
              $scope.keyword = keyword;
              $scope.$apply();
            });
          }
        }

        if ($scope.type === 'keyword') {
          $scope.editKeyword = {editing: false, dynamicValue: $scope.dynamicValue, save: $scope.save};
          $scope.$watch('dynamicValue', refresh);

          ScreenTemplates.getScreenInputsByScreenId('functionTable').then(inputs => {
            $scope.sorting = [];
            for (const key in inputs) {
              $scope.sorting.push(inputs[key].id.replace('attributes.', ''));
            }

            // sort desc
            $scope.sorting.reverse();

            $scope.sortArgumentAsTable();
          });
        }

        $scope.sortArgumentAsTable = function() {
          // go through sorting last -> first and put found element to the beginning so argumentAsTable is sorted asc
          for (const sorting of $scope.sorting) {
            if ($scope.containsKeyInTable(sorting)){
              const element = $scope.argumentAsTable.find(function( obj ) {
                return obj.key === sorting;
              });
              $scope.argumentAsTable = $scope.argumentAsTable.filter(function( obj ) {
                return obj.key !== sorting;
              });
              $scope.argumentAsTable.unshift(element);
            }
          }
        }

        $scope.containsKeyInTable = function(newKey) {
          var result=false;
          _.each($scope.argumentAsTable, function(entry) {
            if (newKey === entry.key) {
              result = true;
            }
          })
          return result;
        }

        function updateEditors(validateJson) {
          try {
            $scope.argumentAsTable = _.map(JSON.parse($scope.localModel.json), function(val, key) {
              if(_.isObject(val) && _.has(val,'dynamic')) {
                return {"key":key,"value":val};
              } else {
                // support the static json format without dynamic expressions
                return {"key":key,"value":{"value":val,dynamic:false}};
              }
            });
            return true;
          }
          catch(err) {
            if(validateJson) {
              Dialogs.showErrorMsg("Invalid JSON: " + err)
            }
            return false;
          }
        }

        function extractArtefact(scope, countOut) {
          if (countOut === 0) {
            return false;
          }

          if (scope.artefact) {
            return scope.artefact;
          }

          return extractArtefact(scope.$parent, countOut - 1);
        }

        function keywordLookup() {
          return new Promise((resolve, reject) => {

            if ($scope.argumentAsTable.length === 0) {
              resolve({iconCss: 'ng-scope glyphicon glyphicon-search', iconTooltip: 'Select a keyword', displayName: 'Select a keyword', description: 'No keyword'});
            }

            const artefact = extractArtefact($scope, 10);

            $http.post("rest/functions/lookup",artefact).then(function(response) {
              if (response.data && response.data.attributes) {
                const keyword = response.data.attributes;
                keyword.displayNames = getDisplayNames($scope.argumentAsTable);

                const entityScope = EntityScopeResolver.getScope(response.data)
                if (entityScope) {
                  // keyword belongs to scope
                  keyword.iconCss = entityScope.cssClass;
                  keyword.iconTooltip = entityScope.tooltip;
                  keyword.description = entityScope.tenantName;
                }

                resolve(keyword);
              } else {
                if (checkIfDynamicParameter($scope.argumentAsTable)) {
                  resolve({errorCss: 'ng-scope glyphicon glyphicon-flash', error: 'Dynamic keyword', displayNames: getDisplayNames($scope.argumentAsTable)});
                } else {
                  resolve({errorCss: 'ng-scope glyphicon glyphicon-exclamation-sign red', error: 'Keyword not found', displayNames: getDisplayNames($scope.argumentAsTable)});
                }
              }
            });

          });
        }

        function checkIfDynamicParameter(parameters) {
          return parameters.find(( param ) => param.value && param.value.dynamic) !== undefined;
        }

        function getDisplayNames(parameters) {
          return parameters
            .map((parameter) => ({
              value: ((parameter.value && parameter.value.dynamic) ? '[dynamic-parameter]' :
                (typeof parameter.value === 'object') ?  parameter.value.value :  parameter.value),
              key: ((parameter.value && parameter.value.dynamic) ? parameter.key + ' (expression: ' + parameter.value.expression + ')' : parameter.key)}));
        }
      },
      templateUrl: 'partials/dynamicforms/jsonEditor.html'}
  })
  .directive('expressionInput', function() {
    return {
      controller: function($scope, $attrs) {},
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
        directory: '=?',
        tooltip: '=',
        onSave: '&'
      },
      controller: function($scope,$http,Upload,Dialogs,ResourceDialogs) {
        initDynamicFormsCtrl($scope);
      },
      templateUrl: 'partials/dynamicforms/dynamicResourceInput.html'}
  })
