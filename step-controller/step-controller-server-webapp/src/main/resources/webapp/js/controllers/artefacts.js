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
angular.module('artefacts',['step'])

.run(function(ViewRegistry, EntityRegistry) {
  EntityRegistry.registerEntity('Control', 'artefact', 'artefacts', null, null, 'st-table', '/partials/artefacts/artefactSelectionTable.html');
})

.factory('artefactTypes', function() {
  
  var registry = {};
  
  var api = {};
  
  function getType(typeName) {
    if(registry[typeName]) {
      return registry[typeName];
    } else {
      throw "Unknown artefact type "+typeName;
    }
  }
  
  api.register = function(typeName,typeInfo) {
    if(!typeInfo.label) {
      typeInfo.label = typeName;
    }
    if(!('isSelectable' in typeInfo)) {
      typeInfo.isSelectable = true;
    }
    registry[typeName] = typeInfo;
  }
  
  api.getEditor = function(typeName) {
    return getType(typeName).form;
  }

  api.getDefaultIcon = function(typeName) {
    return 'glyphicon-unchecked';
  }
  
  api.getIcon = function(typeName) {
    return getType(typeName).icon;
  }
  
  api.getLabel = function(typeName) {
    return getType(typeName).label;
  }
  
  api.getTypes = function() {
    return _.keys(registry);
  }
  
  api.isSelectable = function(typeName) {
    return getType(typeName).isSelectable;
  }
  
  return api;
})

.run(function(artefactTypes) {
  artefactTypes.register('TestSet',{icon:'glyphicon-folder-close', form:'partials/artefacts/testSet.html'});
  artefactTypes.register('TestCase',{icon:'glyphicon-list-alt', form:'partials/artefacts/testCase.html'});
  artefactTypes.register('TestScenario',{icon:'glyphicon-equalizer', form:'partials/artefacts/testScenario.html'});
  artefactTypes.register('CallPlan',{icon:'glyphicon-new-window', form:'partials/artefacts/callPlan.html'});
  artefactTypes.register('CallKeyword',{icon:'glyphicon-record', form:'partials/artefacts/callFunction.html'});
  artefactTypes.register('For',{icon:'glyphicon-th', form:'partials/artefacts/for.html'});
  artefactTypes.register('ForEach',{icon:'glyphicon-th', form:'partials/artefacts/forEach.html'});
  artefactTypes.register('While',{icon:'glyphicon-repeat', form:'partials/artefacts/while.html'});
  artefactTypes.register('DataSet',{icon:'glyphicon-th-large', form:'partials/artefacts/dataSet.html'});
  artefactTypes.register('Synchronized',{icon:'glyphicon-align-justify', form:'partials/artefacts/synchronized.html'});
  artefactTypes.register('Sequence',{icon:'glyphicon-align-justify', form:'partials/artefacts/sequence.html'});
  artefactTypes.register('Return',{icon:'glyphicon-share-alt', form:'partials/artefacts/return.html'});
  artefactTypes.register('Echo',{icon:'glyphicon-zoom-in', form:'partials/artefacts/echo.html'});
  artefactTypes.register('If',{icon:'glyphicon-unchecked', form:'partials/artefacts/if.html'});
  artefactTypes.register('Session',{icon:'glyphicon-magnet', form:'partials/artefacts/functionGroup.html'});
  artefactTypes.register('Set',{icon:'glyphicon-save', form:'partials/artefacts/set.html'});
  artefactTypes.register('Sleep',{icon:'glyphicon-hourglass', form:'partials/artefacts/sleep.html'});
  artefactTypes.register('Script',{icon:'glyphicon-align-left', form:'partials/artefacts/script.html'});
  artefactTypes.register('ThreadGroup',{icon:'glyphicon-resize-horizontal', form:'partials/artefacts/threadGroup.html'});
  artefactTypes.register('Thread',{icon:'glyphicon-resize-horizontal', form:'partials/artefacts/threadGroup.html', isSelectable:false});
  artefactTypes.register('Switch',{icon:'glyphicon-option-vertical', form:'partials/artefacts/switch.html'});
  artefactTypes.register('Case',{icon:'glyphicon-minus', form:'partials/artefacts/case.html'});
  artefactTypes.register('RetryIfFails',{icon:'glyphicon-retweet', form:'partials/artefacts/retryIfFails.html'});
  artefactTypes.register('Check',{icon:'glyphicon-ok', form:'partials/artefacts/check.html'});
  artefactTypes.register('Assert',{icon:'glyphicon-ok', form:'partials/artefacts/assert.html'});
  artefactTypes.register('Placeholder',{icon:'glyphicon-unchecked', form:'partials/artefacts/placeholder.html'});
  artefactTypes.register('Export',{icon:'glyphicon-export', form:'partials/artefacts/export.html'});
})

.directive('artefactDetails', function($http,$timeout,$interval,stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      artefact: '=',
      onSave: '&',
      readonly: '=',
      handle: '='
    },
    controller: function($scope,$location,artefactTypes,AuthService) {
      
      $scope.authService = AuthService;
      
      $scope.$watch('artefact', function() {
        if($scope.artefact) {
          var classname = $scope.artefact._class;
          $scope.icon = artefactTypes.getIcon(classname);
          $scope.label = artefactTypes.getLabel(classname);
          $scope.editor = artefactTypes.getEditor(classname);
        }
      })

      $scope.save = function() {
        if(!$scope.readonly) {
          if($scope.onSave) {
            $scope.onSave({artefact:$scope.artefact});
          }
        }
      }
    },
    templateUrl: 'partials/artefacts/abstractArtefact.html'}
})
.controller('CallPlanCtrl' , function($scope,$location,$http, PlanDialogs) {  
  $scope.gotoPlan = function() {
    $location.path('/root/plans/editor/' + $scope.artefact.planId);
  }
  
  $scope.$watch('artefact.planId', function(planId) {
    if(planId) {
      $http.get('rest/plans/'+planId).then(function(response) {
    	if (response.data) {
            $scope.planName = response.data.attributes.name;
    	} else {
    		$scope.planName = "";
    	}
      })
    }
  })
  
  $scope.selectPlan = function() {
    PlanDialogs.selectPlan(function(plan) {
      $scope.artefact.planId = plan.id;
      $scope.artefact.attributes.name = plan.attributes.name;
      $scope.save();
    })
  }
})
.controller('CallFunctionCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs, Dialogs) {
  
  showTokenSelectionParameters = false;
  
  $scope.ready=false;
  
  function loadFunction(id, callback) {
    $http({url:"rest/functions/"+id,method:"GET"}).then(function(response) {
      $scope.targetFunction = response.data;
      $scope.ready = true;
      if(callback) {
        callback();
      }
    })
  }
  
  $scope.$watch('artefact.functionId', function(id) {
    $scope.ready = false;
    if(id!=null) {
      loadFunction(id);  
    } else {
      $scope.targetFunction = {};
      $scope.ready = true;
    }
  })
  
  $scope.gotoFunction = function() {
    FunctionDialogs.editFunction($scope.targetFunction.id);
  }
  
  $scope.openFunctionEditor = function(functionid) {
    FunctionDialogs.openFunctionEditor($scope.targetFunction.id);
  }
  
  $scope.setArgument = function(json) {
    $scope.artefact.argument = json;
    $scope.save();
  }
  
  $scope.selectFunction = function() {
    Dialogs.selectEntityOfType('function', true).then(function(result) {
      var id = result.item;
      $scope.artefact.functionId = id;
      loadFunction(id, function() {$scope.save()});
    });
  }
  
})
.controller('DataSourceCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {  
  $scope.dataSourceTypes = [{name:"excel",label:"Excel"},{name:"csv",label:"CSV"},{name:"sql",label:"SQL"},
                            {name:"file",label:"Flat file"},{name:"folder",label:"Directory"},{name:"sequence",label:"Integer sequence"},
                            {name:"json",label:"Json String"}, {name:"gsheet",label:"Google Sheet v4"}]
  
  $scope.loadInitialDataSourceConfiguration = function() {
    $http.get("rest/datapool/types/"+$scope.artefact.dataSourceType).then(function(response){
      $scope.artefact.dataSource = response.data;
      $scope.save();
    })
  }
})
.controller('DefaultArtefactFormCtrl' , function($scope) {
  $scope.getEditableArtefactProperties = function() {
    return _.without(_.keys($scope.artefact),'id','_id','root','attributes','childrenIDs','createSkeleton','_class','attachments')
  }
})
.controller('AssertCtrl' , function($scope,$uibModal,$location,$http,FunctionDialogs) {  
  $scope.operatorTypes = [{name:"EQUALS",label:"equals"},{name:"BEGINS_WITH",label:"begins with"},{name:"CONTAINS",label:"contains"},
                            {name:"ENDS_WITH",label:"ends with"},{name:"MATCHES",label:"matches"}]
  

})
.directive('customJsonEditor', function($http,$timeout,$interval,stateStorage,$filter,$location,$compile,Dialogs) {
  return {
    restrict: 'E',
    scope: {
      argsValue: '=',
      compileHtml: "=",
      onSave: '&'
    },
    controller: function($scope,$location,$rootScope, AuthService) {
      $scope.inputs = {};
      
      $scope.getFunctionInputs = function(validateJson) {
        try {
          $scope.inputs = JSON.parse($scope.argsValue);
          return true;
        }
        catch(err) {
          if(validateJson) {
            Dialogs.showErrorMsg("Invalid JSON: " + err)            
          }
          return false;
        }
      }
      
      $scope.save = function() {
        $scope.argsValue = JSON.stringify($scope.inputs);
        $timeout(function() {
          $scope.onSave({argsValue:$scope.argsValue});
        });
        
      }
      
    },
    link: function ($scope, element, attrs) {
      $scope.$watch('compileHtml', function(newCompileHtml) {
        if (newCompileHtml) {
          var template = $compile('<div id="customTemplate">' + $scope.compileHtml + '</div>')($scope);
          var el = $('#customTemplate');
          if (el.length) {
            el.replaceWith(template);
          } else {
            element.append(template);
          }
        }
      });
      
      $scope.$watch('argsValue', function(newArgs,oldArgs) {
        if (newArgs) {
          $scope.getFunctionInputs(false);
        }
      });
    }
  }
})

.directive('jsonEditor', function($http,$timeout,$interval,stateStorage,$filter,$location,Dialogs) {
  return {
    restrict: 'E',
    scope: {
      model: '=',
      onChange: '&'
    },
    templateUrl: 'partials/jsonEditor.html',
    controller: function($scope,$location,$rootScope, AuthService) {
      $scope.localModel = {json:""}
      $scope.argumentAsTable = [];
      $scope.isFocusOnLastRow=false;
      $scope.doCommitLastRow=false;
      $scope.stillEditing=false;
      
      $scope.$watch('model', function(json) {
        if(json!=$scope.localModel.json) {
          $scope.localModel.json = json;
          $scope.updateEditors(false);
          initLastRow();
        }
      })
      
      $scope.$watch('isFocusOnLastRow', function(value) {
        if(value === true) { 
          $timeout(function() {
            $("#lastRowKey").focus();
            $scope.isFocusOnLastRow=false;
          });
        }
      })
      
      $scope.$watch('doCommitLastRow', function(value) {
        if(value === true) { 
          $timeout(function() {
          $scope.commitLastRow();
            $scope.doCommitLastRow=false;
          });
        }
      })
      
      $scope.$watch('stillEditing', function(value) {
        if(value === true) { 
          $timeout(function() {
            $scope.stillEditing=false;
          });
        }
      })
      
      $scope.save = function() {
        $scope.onChange({json:$scope.localModel.json});
      }
      
      $scope.updateJsonFromTable = function() {
        var json = {};
        _.each($scope.argumentAsTable, function(entry) {
          json[entry.key]=entry.value;
        })
        $scope.localModel.json = JSON.stringify(json);
      }
      
      $scope.containsKeyInTable = function(newKey) {
        var result=false;
        _.each($scope.argumentAsTable, function(entry) {
          if (newKey === entry.key) {
            result = true;;
          }
        })
        return result;
      }
      
      $scope.addRowToTable = function(row) {
        $scope.argumentAsTable.push(row)
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.removeRowFromTable = function(key) {
        $scope.argumentAsTable = _.reject($scope.argumentAsTable, function(entry){ return entry.key==key});
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.onRowEdit = function() {
        $scope.updateJsonFromTable();
        $scope.save();
      }
      
      $scope.onJsonFieldBlur = function() {
        if($scope.updateEditors(true)) {
          $scope.save();
        }
      }
      
      $scope.updateEditors = function(validateJson) {
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
      
      function initLastRow() {
        // init last row as a static value
        $scope.stillEditing=true;
        $scope.lastRow = {key:"", value:{value:"",dynamic:false}}  
        var inputElt = document.getElementById("lastRowKey");
        if (inputElt !== null) {
          inputElt.style.backgroundColor = "white";
        }
      }

      $scope.commitLastRow = function() {
        if ( $scope.lastRow !==  undefined && $scope.lastRow.key !== undefined) {
          //avoid duplicates
          if (!$scope.containsKeyInTable($scope.lastRow.key)) {
            var row = $scope.lastRow;
            $scope.addRowToTable({"key":row.key, "value":row.value});
            initLastRow();
            $scope.isFocusOnLastRow=true;
          } else  {
            if ($scope.lastRow.key !== "") {
               Dialogs.showErrorMsg("The key must be unique!");
            }
            document.getElementById("lastRowKey").style.backgroundColor = "#faebd7";
          }          
        }
      }
      
      $scope.onBlurFromLastRowKey = function() {
        //only save on the last key blur events if key is set 
        if ($scope.lastRow.key !== "") {
          $scope.saveLastRow();
        }
      }
      
      $scope.saveLastRow = function() {
        if ($scope.stillEditing) {
        } else {
             $scope.doCommitLastRow=true;
        }
      }
      
      $scope.lastRowTabKeyToValue = function(event) {
        var x = event.which || event.keyCode;
        if (x === 9 && !event.shiftKey) {
          $scope.stillEditing=true;
        }
      }
      
      $scope.lastRowTabValueToKey = function(event) {
        var x = event.which || event.keyCode;
        if (x === 9 && (event.shiftKey || 
                event.target.attributes['title'] === undefined || 
                event.target.attributes['title'].nodeValue!=='Use a dynamic expression to set this attribute')) {
          $scope.stillEditing=true;
        }
      }
      
      $scope.onClickOnLastRow = function () {
        $scope.stillEditing=true;
      }
      
      
    }
  }
})
.controller('ArtefactSelectionCtrl' , function($scope, $http, artefactTypes) {
  $http.get("rest/controller/artefact/types").then(function(response){ 
    $scope.artefacts = _.filter(_.map(response.data, function(e) {return {name:e}}), function(artefact) {
      return artefactTypes.isSelectable(artefact.name)
    });
  })
  $scope.artefactIcon = function(class_) { 
    return 'glyphicon '+artefactTypes.getIcon(class_)
  };
})