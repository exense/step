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
angular.module('gridControllers', [ 'dataTable', 'step' ])

.controller('GridCtrl', ['$scope', 'stateStorage',
    function($scope, $stateStorage) {
      $stateStorage.push($scope, 'grid');
      
      $scope.autorefresh = true;

      if($scope.$state == null) { $scope.$state = 'adapters' };
      
      $scope.tabs = [
          { id: 'agents'},
          { id: 'adapters'},
          { id: 'tokens'},
          { id: 'quotamanager'}
      ]
      
      $scope.activeTab = function() {
        return _.findIndex($scope.tabs,function(tab){return tab.id==$scope.$state});
      }
      
      $scope.onSelection = function(tabid) {
        return $scope.$state=tabid;
      }
}])   

.controller('AgentListCtrl', [
    '$scope',
    '$interval',
    '$http',
    'helpers',
    function($scope, $interval, $http, helpers) {
      $scope.$state = 'agents';
      
      $scope.datatable = {}
      
      $scope.loadTable = function loadTable() {
        $http.get("rest/grid/agent").then(
          function(response) {
            var data = response.data;
            var dataSet = [];
            for (i = 0; i < data.length; i++) {
              dataSet[i] = [ data[i].agentId, data[i].agentUrl];
            }
            $scope.tabledef.data = dataSet;
          });
        };

        $scope.tabledef = {};
        $scope.tabledef.columns = [ { "title" : "ID", "visible" : false}, { "title" : "Url" } ];

        $scope.tabledef.actions = [{"label":"Interrupt","action":function() {$scope.interruptSelected()}},
                                   {"label":"Resume","action":function() {$scope.resumeSelected()}}];
        
        $scope.loadTable();
        
        $scope.interruptSelected = function() {
          var rows = $scope.datatable.getSelection().selectedItems;
          
          for(i=0;i<rows.length;i++) {
            $scope.interrupt(rows[i][0]);       
          }
        };
          
        $scope.interrupt = function(id) {
          $http.put("rest/grid/agent/"+id+"/interrupt").then(function() {
                $scope.loadTable();
            });
        }
        
        $scope.resumeSelected = function() {
          var rows = $scope.datatable.getSelection().selectedItems;
          
          for(i=0;i<rows.length;i++) {
            $scope.resume(rows[i][0]);       
          }
        };
          
        $scope.resume = function(id) {
          $http.put("rest/grid/agent/"+id+"/resume").then(function() {
                $scope.loadTable();
            });
        }
          
        var refreshTimer = $interval(function(){
            if($scope.autorefresh){$scope.loadTable()}}, 2000);
          
          $scope.$on('$destroy', function() {
            $interval.cancel(refreshTimer);
          });

      } ])

.controller('AdapterListCtrl', [
  	'$scope',
  	'$interval',
  	'$http',
  	'helpers',
  	function($scope, $interval, $http, helpers) {
  	  $scope.$state = 'adapters';
  	  
  	  $scope.keySelectioModel = {};
  	  
  	  $http.get("rest/grid/keys").then(
          function(response) { 
            $scope.keys = ['url']; $scope.keySelectioModel['url']=true;
            _.each(response.data,function(key){$scope.keys.push(key); $scope.keySelectioModel[key]=false});
          })
      
  	  $scope.loadTable = function loadTable() {
  	    var queryParam='';
  	    _.each(_.keys($scope.keySelectioModel),function(key){
  	      if($scope.keySelectioModel[key]) {
  	      queryParam+='groupby='+key+'&'
  	      }
  	    })
  		$http.get("rest/grid/token/usage?"+queryParam).then(
  			function(response) {
  			  var data = response.data;
  			  var dataSet = [];
  			  for (i = 0; i < data.length; i++) {
  				dataSet[i] = [ helpers.formatAsKeyValueList(data[i].key), {usage:data[i].usage, capacity:data[i].capacity} ];
  			  }
  			  $scope.tabledef.data = dataSet;
  			});
  	  };
  	  
  	  $scope.$watchCollection('keySelectioModel',function() {$scope.loadTable()});
  	  
  	  var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.loadTable();}}, 2000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
  	  
  	  $scope.tabledef = {};
  	  $scope.tabledef.columns = [ { "title" : "URL" }, 
  	                     { "title" : "Usage", "render" : function(data, type, row) {
                            	var usagePerc = data.usage*100/data.capacity;
                            	var style;
                            	if (usagePerc>90) {
                            	  style = 'progress-bar-danger'
                            	} else if (usagePerc>70) {
                            	  style = 'progress-bar-warning'
                                } else {
                                  style = 'progress-bar-success'
                            	}
                            	
                            	return '<div class="progress">'
                            	  		+'<div class="progress-bar ' + style + '" role="progressbar" aria-valuenow="'+usagePerc+'" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;width: '+usagePerc+'%;">'
                            	  		+data.usage+'/'+data.capacity
                            			+'</div></div>';
                              } }];

  	} ])

.controller('TokenListCtrl', [
	'$scope',
	'$interval',
	'$http',
	function($scope, $interval, $http) {
	  $scope.$state = 'tokens'
	  
	  $scope.loadTable = function loadTable() {
		$http.get("rest/grid/token").then(
			function(response) {
			  var data = response.data;
			  var dataSet = [];
			  for (i = 0; i < data.length; i++) {
				dataSet[i] = [ data[i].token.id, data[i].token.agentid,
					data[i].token.attributes != null ? JSON.stringify(data[i].token.attributes) : '',
					data[i].token.selectionPatterns != null ? JSON.stringify(data[i].token.selectionPatterns) : '',
					data[i].owner != null ? data[i].owner.executionID : '-'   ];
			  }
			  $scope.tabledef.data = dataSet;
			});
	  };

	  $scope.tabledef = {};
	  $scope.tabledef.columns = [ { "title" : "ID" }, { "title" : "AgentID" }, { "title" : "Attributes" },
		  { "title" : "Selection Pattern" }, { "title" : "Execution ID", "render": function (value) {return '<a href="#/root/executions/'+value+'">'+value+'</a>'}} ];

	  $scope.loadTable();
      
      var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.loadTable()}}, 2000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });

	} ])
	
.controller('QuotaManagerCtrl', [
    '$scope',
    '$http',
    '$interval',
    function($scope, $http, $interval) {
      $scope.$state = 'quotamanager'
      
      $scope.load = function loadTable() {
        $http.get("rest/quotamanager/status").then(
            function(response) {
              $scope.statusText = response.data;
            },function(error){
              $scope.statusText = "";
            });
      };
      
      var refreshTimer = $interval(function(){
        if($scope.autorefresh){$scope.load();}}, 2000);
      
      $scope.$on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
      
      $scope.load();

    } ]);