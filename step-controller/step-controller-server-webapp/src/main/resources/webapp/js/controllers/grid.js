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
angular.module('gridControllers', ['step' ])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('grid','partials/grid/grid.html');
  ViewRegistry.registerDashlet('grid', 'Agents', 'partials/grid/gridAgents.html', 'agents');
  ViewRegistry.registerDashlet('grid', 'Tokens', 'partials/grid/gridTokens.html', 'tokens');
  ViewRegistry.registerDashlet('grid', 'Token Groups', 'partials/grid/gridTokenGroups.html', 'tokengroups');
  ViewRegistry.registerDashlet('grid', 'Quota Manager', 'partials/grid/gridQuotaManager.html', 'quotamanager');
})

.controller('GridCtrl', ['$scope', 'stateStorage', 'AuthService','ViewRegistry',
    function($scope, $stateStorage, AuthService,ViewRegistry) {
      $stateStorage.push($scope, 'grid');

      $scope.tabs = ViewRegistry.getDashlets("grid");

      $scope.authService = AuthService;
      
      if($scope.$state == null) { $scope.$state = 'agents' };

      // Returns the item number of the active tab
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
            $scope.agents = []
            _.each(response.data, function(e) {
              var type = e.agentRef.agentType;
              $scope.agents.push({
                id : e.agentRef.agentId,
                url: e.agentRef.agentUrl,
                typeLabel : type == 'default' ? 'Java' : (type == 'node' ? 'Node.js' : (type == 'dotnet' ? '.NET' : 'Unknown')),
                tokensCapacity : e.tokensCapacity,
                type : type,
              });
            })
          });
        };

        $scope.loadTable();
        
        $scope.interrupt = function(id) {
          $http.put("rest/grid/agent/"+id+"/interrupt").then(function() {
                $scope.loadTable();
            });
        }
          
        $scope.resume = function(id) {
          $http.put("rest/grid/agent/"+id+"/resume").then(function() {
                $scope.loadTable();
            });
        }
        
        $scope.removeTokenErrors = function(id) {
          $http.delete("/rest/grid/agent/"+id+"/tokens/errors").then(function() {
                $scope.loadTable();
            });
        }
        
        var refresh = function() {
            $scope.loadTable()
        }
        $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
}])

.controller('AdapterListCtrl', [
  	'$scope',
  	'$compile',
  	'$interval',
  	'$http',
  	'helpers',
  	function($scope, $compile, $interval, $http, helpers) {
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
    			  $scope.tokenGroups = []
    			  _.each(response.data, function(e) {
              $scope.tokenGroups.push({
                key : e.key,
                keyAsString : JSON.stringify(e.key),
                tokensCapacity : e
              });
            })
    		});
    	};
  	  
  	  $scope.$watchCollection('keySelectioModel',function() {$scope.loadTable()});
      
  	  var refresh = function() {
        $scope.loadTable()
  	  }
  	  $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
}])

.controller('TokenListCtrl', [
	'$scope',
	'$interval',
	'$http',
	function($scope, $interval, $http) {
	  $scope.$state = 'tokens'
	  
	  $scope.tokens;  
	    
	  $scope.loadTable = function loadTable() {
      $http.get("rest/grid/token").then(function(response) {
        $scope.tokens = []
        _.each(response.data, function(e) {
          var type = e.token.attributes.$agenttype;
          $scope.tokens.push({
            id : e.token.id,
            typeLabel : type == 'default' ? 'Java' : (type == 'node' ? 'Node.js' : (type == 'dotnet' ? '.NET' : 'Unknown')),
            type : type,
            attributes : e.token.attributes,
            attributesAsString : JSON.stringify(e.token.attributes),
            agentUrl : e.agent.agentUrl,
            executionId : e.currentOwner ? e.currentOwner.executionId : null,
            executionDescription : e.currentOwner ? e.currentOwner.executionDescription : null,
            currentOwner : e.currentOwner,
            state : e.state,
            errorMessage : e.tokenHealth.errorMessage,
            tokenHealth : e.tokenHealth
          });
        })
      });
	  };
	  
	  $scope.removeTokenError = function(tokenId) {
	    $http.delete("rest/grid/token/"+tokenId+"/error").then(function(){$scope.loadTable()});
	  }
	  
	  $scope.startTokenMaintenance = function(tokenId) {
      $http.post("rest/grid/token/"+tokenId+"/maintenance").then(function(){$scope.loadTable()});
    }

	  $scope.stopTokenMaintenance = function(tokenId) {
      $http.delete("rest/grid/token/"+tokenId+"/maintenance").then(function(){$scope.loadTable()});
    }
	  
	  $scope.loadTable();
	  var refresh = function() {
      $scope.loadTable()
	  }
	  $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
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
          
      var refresh = function() {
        $scope.load()
      }
      $scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
      $scope.load();

    } ]);
