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
angular.module('scriptEditor',['step'])

.run(function(FunctionTypeRegistry, ViewRegistry) {
  ViewRegistry.registerView('scripteditor','scripteditor/partials/scriptEditor.html');
})

.controller('ScriptEditorCtrl', function($scope, $http, stateStorage, AuthService) {
      stateStorage.push($scope, 'scripteditor', {});

      $scope.authService = AuthService;
      $scope.functionid = $scope.$state;
 
      $scope.functionExecutionPanelHandle = {};
      
      var editor = ace.edit("editor");
      editor.setTheme("ace/theme/chrome");
      editor.getSession().setMode("ace/mode/javascript");
      
      $http.get("rest/functions/"+$scope.functionid).then(function(response) {
        $scope.function_=response.data;
      })
      
      $http.get("rest/scripteditor/function/"+$scope.functionid+"/file").then(function(response) {
        editor.setValue(response.data);
      })

      $scope.save = function() {
        return $http.post("rest/scripteditor/function/" + $scope.functionid + "/file",editor.getValue())
      }
      
      $scope.execute = function() {
        $scope.functionExecutionPanelHandle.execute();
        $scope.save().then(function() {
          $scope.functionExecutionPanelHandle.execute();
        })
      }
})
//# sourceURL=scriptEditor.js