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
angular.module('functionsControllers',['step'])

.run(function(FunctionTypeRegistry, ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('functions','partials/functionList.html');  
  FunctionTypeRegistry.register('step.plugins.functions.types.CompositeFunction','Composite','partials/functions/forms/composite.html');
  EntityRegistry.registerEntity('Keyword', 'functions', 'functions', 'rest/functions/', 'rest/functions/', 'st-table', '/partials/functions/functionSelectionTable.html', null, 'glyphicon glyphicon-record');
})

.factory('FunctionTypeRegistry', function() {
  
  var registry = {};
  
  var api = {};
  
  api.register = function(typeName,label,form) {
    registry[typeName] = {"label":label,"form":form};
  }
  
  api.getForm = function(typeName) {
    return registry[typeName].form;
  }
  
  api.getLabel = function(typeName) {
    return registry[typeName]?registry[typeName].label:"Unknown";
  }
  
  api.getTypes = function() {
    return _.keys(registry);
  }

  api.getFilteredTypes = function(arrayFilters){
    var keys = _.keys(registry);
    var resultsArray=[];
    for (var i=0; i<keys.length;i++) {
      if (arrayFilters.indexOf(keys[i]) >= 0) {
        resultsArray.push(keys[i])
      }
    }
    return resultsArray;
  }
  
  return api;
})

.factory('FunctionDialogsConfig', function ($rootScope, $uibModal, $http, Dialogs, $location) {
    var functionDialogsConfig = {};

    functionDialogsConfig.getConfigObject = function(title,serviceRoot,functionTypeFilters,lightForm, customScreenTable) {
      var config = {};
      config.title = title;
      config.serviceRoot = serviceRoot;
      config.functionTypeFilters = functionTypeFilters;
      config.lightForm = lightForm;
      config.customScreenTable = customScreenTable;
      return config;
    }

    functionDialogsConfig.getDefaultConfig = function() {
      return functionDialogsConfig.getConfigObject('Keyword','functions',[],false,'functionTable');
    }

    return functionDialogsConfig;
})

.factory('FunctionDialogs', function ($rootScope, $uibModal, $http, Dialogs,FunctionDialogsConfig, $location) {
  
  function openModal(function_, dialogConfig) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
        templateUrl: 'partials/functions/functionConfigurationDialog.html',
        controller: 'newFunctionModalCtrl',
        resolve: {
          function_: function () {return function_;},
          dialogConfig: function () {return (dialogConfig) ? dialogConfig : FunctionDialogsConfig.getDefaultConfig();;}
          }
      });

      return modalInstance.result;
  }
  
  var dialogs = {};

  dialogs.editFunction = function(id, callback, dialogConfig) {
    dialogConfig = (dialogConfig) ? dialogConfig : FunctionDialogsConfig.getDefaultConfig();
    $http.get("rest/"+dialogConfig.serviceRoot+"/"+id).then(function(response) {
      openModal(response.data,dialogConfig).then(function() {
        if(callback){callback()};
      })
    });
  }
  
  dialogs.addFunction = function(callback, dialogConfig) {
    dialogConfig = (dialogConfig) ? dialogConfig : FunctionDialogsConfig.getDefaultConfig();
    openModal(null,dialogConfig).then(function() {
      if(callback){callback()};
    })
  }
  
  dialogs.openFunctionEditor = function(functionid, dialogConfig) {
    dialogConfig = (dialogConfig) ? dialogConfig : FunctionDialogsConfig.getDefaultConfig();
    $http.get("rest/"+dialogConfig.serviceRoot+"/"+functionid+"/editor").then(function(response){
      var path = response.data
      if(path) {
        $location.path(path);              
      } else {
        Dialogs.showErrorMsg("No editor configured for this function type");
      }
    })
  }
  
  return dialogs;
})

.controller('FunctionListCtrl', function($scope, $rootScope, $compile, $http, $interval, $uibModal, stateStorage, Dialogs, FunctionDialogs, FunctionDialogsConfig, ImportDialogs, ExportDialogs, $location, AuthService, FunctionTypeRegistry) {
  stateStorage.push($scope, 'functions', {});	
  
  $scope.authService = AuthService;
  $scope.tableHandle = {};

  $scope.config = FunctionDialogsConfig.getConfigObject('Keyword','functions',[],false,'functionTable')
  $scope.initCtrl = function(type){
    if (type == 'masks') {
      $scope.config = FunctionDialogsConfig.getConfigObject('Mask','masks',['step.plugins.pdftest.PdfTestFunction','step.plugins.compare.image.ImageCompareFunction'],true,'functionTable');
    }
  }
  
  function reload() {
    $scope.tableHandle.reload();
  }

  $scope.reload = reload;

  $scope.$on('functions.collection.change', function(evt, data){
    reload();
  })
  
  $scope.editFunction = function(id) {
    FunctionDialogs.editFunction(id, function() {reload()}, $scope.config);
  }
  
  $scope.copyFunction = function(id) {
    $rootScope.clipboard = {object:"function",id:id};
  }
  
  $scope.pasteFunction = function() {
    if($rootScope.clipboard && $rootScope.clipboard.object=="function") {
      $http.post("rest/"+$scope.config.serviceRoot+"/"+$rootScope.clipboard.id+"/copy")
      .then(function() {reload()});
    }
  }
  
  $scope.openFunctionEditor = function(functionid) {
    FunctionDialogs.openFunctionEditor(functionid, $scope.config);
  }
  
  $scope.addFunction = function() {
    FunctionDialogs.addFunction(function() {reload()}, $scope.config);
  }
  
  $scope.executeFunction = function(id) {
    $http.post("rest/interactive/functiontest/"+id+"/start").then(function(response) {
      var result = response.data;
      $rootScope.planEditorInitialState = {
          interactive : true,
          selectedNode : result.callFunctionId
      }
      $location.path('/root/plans/editor/' + result.planId);
    });
  }
  
  $scope.deleteFunction = function(id, name) {
    Dialogs.showDeleteWarning(1, 'element "' + name + '"').then(function() {
      $http.delete("rest/"+$scope.config.serviceRoot+"/"+id).then(function() {reload()});
    })
  }
  
  $scope.importFunctions = function() {
    ImportDialogs.displayImportDialog('Keyword import','functions', true).then(function () {
      reload();
    });
  }

  $scope.exportFunction = function(id, name) {
      ExportDialogs.displayExportDialog('Keywords export','functions/' + id, name + '.sta', true).then(function () {})
    }
  
  $scope.exportFunctions = function() {
    ExportDialogs.displayExportDialog('Keywords export','functions', 'allKeywords.sta', true).then(function () {})
  }
  
  $scope.functionTypeLabel = function(type) {
    return FunctionTypeRegistry.getLabel(type);
  }
})

.controller('newFunctionModalCtrl', [ '$rootScope', '$scope', '$uibModalInstance', '$http', '$location', 'function_', 'dialogConfig', 'Dialogs', 'AuthService','FunctionTypeRegistry',
function ($rootScope, $scope, $uibModalInstance, $http, $location, function_, dialogConfig, Dialogs, AuthService, FunctionTypeRegistry) {
  $scope.functionTypeRegistry = FunctionTypeRegistry;
  
  var newFunction = function_==null;
  $scope.mode = newFunction?"add":"edit";
  $scope.dialogConfig = dialogConfig;
  $scope.isSchemaEnforced = AuthService.getConf().miscParams.enforceschemas;

  $scope.getTypes = function() {
    if ($scope.dialogConfig.functionTypeFilters != null && $scope.dialogConfig.functionTypeFilters.length > 0) {
      return $scope.functionTypeRegistry.getFilteredTypes($scope.dialogConfig.functionTypeFilters);
    } else {
      return $scope.functionTypeRegistry.getTypes();
    }
  }
  
  $scope.addRoutingCriteria = function() {
    $scope.criteria.push({key:"",value:""});
  }
  
  $scope.removeRoutingCriteria = function(key) {
    delete $scope.function_.tokenSelectionCriteria[key];
    loadTokenSelectionCriteria($scope.function_);
  }
  
  $scope.saveRoutingCriteria = function() {
    var tokenSelectionCriteria = {}
    _.each($scope.criteria, function(entry) {
      tokenSelectionCriteria[entry.key]=entry.value;
    });
    $scope.function_.tokenSelectionCriteria = tokenSelectionCriteria;
  }
  
  var loadTokenSelectionCriteria = function(function_) {
    $scope.criteria = [];
    if(function_ && function_.tokenSelectionCriteria) {
      _.mapObject(function_.tokenSelectionCriteria,function(val,key) {
        $scope.criteria.push({key:key,value:val});
      });
    }
  }
  
  loadTokenSelectionCriteria(function_);
  
  $scope.loadInitialFunction = function() {
    $http.get("rest/"+$scope.dialogConfig.serviceRoot+"/types/"+$scope.function_.type).then(function(response){
      var initialFunction = response.data;
      if($scope.function_) {
        initialFunction.id = $scope.function_.id;
        if($scope.function_.attributes) {
          initialFunction.attributes = $scope.function_.attributes;
        }
        if($scope.function_.schema) {
          initialFunction.schema = $scope.function_.schema;
        }
      }
      
      loadTokenSelectionCriteria(initialFunction);
      
      $scope.function_ = initialFunction;
      $scope.schemaStr = JSON.stringify($scope.function_.schema);
    })  
  }
  
  if(newFunction) {
    if ($scope.dialogConfig.functionTypeFilters != null && $scope.dialogConfig.functionTypeFilters.length > 0) {
      $scope.function_ = {type: $scope.dialogConfig.functionTypeFilters[0]}
    } else {
      $scope.function_ = {type:'step.plugins.java.GeneralScriptFunction'}
    }
    $scope.loadInitialFunction();
  } else {
    $scope.function_ = function_;
    $scope.schemaStr = JSON.stringify($scope.function_.schema);
  }

  $scope.save = function (editAfterSave) {

	var schemaJson;
	try {
  	schemaJson = JSON.parse($scope.schemaStr);
  	$scope.function_.schema = schemaJson;
  	
  	$http.post("rest/"+$scope.dialogConfig.serviceRoot+"",$scope.function_).then(function(response) {
  	  var function_ = response.data;
  	  $uibModalInstance.close(response.data);

  	  if(editAfterSave) {
  	    $http.get("rest/"+$scope.dialogConfig.serviceRoot+"/"+function_.id+"/editor").then(function(response){
  	      var path = response.data;
  	      if(path) {
  	        $location.path(path);
  	      } else {
  	        Dialogs.showErrorMsg("No editor configured for this function type");
  	      }
  	    })     
  	  }
  	})

  	}catch(e){
  		Dialogs.showErrorMsg("incorrect schema format (must be Json) : " + e);
  	} 		
   };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
}])
   
.controller('selectFunctionModalCtrl', function ($scope, $uibModalInstance) {
  
  $scope.selectFunction = function(id) {
    $uibModalInstance.close(id);
  }
  
  $scope.table = {};

  $scope.tabledef = {}      
  
  $scope.tabledef.columns = function(columns) {
    _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
      col.visible = false
    });
    _.each(_.where(columns,{'title':'Actions'}),function(col){
        col.title="Actions";
        col.searchmode="none";
        col.width="160px";
        col.render = function ( data, type, row ) {
          var html = '<div class="input-group">' +
            '<div class="btn-group">' +
            '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#FunctionListCtrl\').scope().selectFunction(\''+row[0]+'\')">' +
            '<span class="glyphicon glyphicon glyphicon glyphicon-plus" aria-hidden="true"></span>';
          html+='</div></div>';
          return html;
        }
       });
    return columns;
  };


  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
})
.controller('CompositeFunctionFormCtrl' , function($scope,PlanDialogs,$location,$http) {  
  $scope.gotoArtefact = function() {
    $scope.save(false);
    $location.path('/root/artefacteditor/' + $scope.function_.artefactId);
  }
  
  $scope.selectPlan = function() {
    PlanDialogs.selectPlan(function(plan) {
      $scope.function_.planId = plan.id;
    })
  }
})

.directive('functionLink', function() {
  return {
    restrict: 'E',
    scope: {
      function_: '=',
      stOptions: '=?'
    },
    templateUrl: 'partials/functions/functionLink.html',
    controller: function($scope, FunctionDialogs, FunctionDialogsConfig) {
      $scope.noLink = $scope.stOptions && $scope.stOptions.includes("noEditorLink")
      $scope.openFunctionEditor = function() {
        FunctionDialogs.openFunctionEditor($scope.function_.id, FunctionDialogsConfig.getConfigObject('Keyword','functions',[],false,'functionTable'))
      }
    }
  };
})
