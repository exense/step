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
angular.module('adminControllers', ['step' ])

.run(function(FunctionTypeRegistry, EntityRegistry) {
  EntityRegistry.registerEntity('User', 'user', 'users', 'rest/admin/user/', 'rest/admin/user', 'st-table', '/partials/users/userSelectionTable.html');
})

.controller('AdminCtrl', ['$scope', 'stateStorage',
    function($scope, stateStorage) {
      // push this scope to the state stack
      stateStorage.push($scope, 'admin', {});
      
      $scope.tabs = [
          { id: 'users'},
          { id: 'controller'}
      ]
      
      // Select the "Users" tab per default
      if($scope.$state == null) { $scope.$state = 'users' };
      
      // Returns the item number of the active tab
      $scope.activeTab = function() {
        return _.findIndex($scope.tabs,function(tab){return tab.id==$scope.$state});
      }
      
      // Update the current $state and thus the browser location
      $scope.onSelection = function(tabid) {
        return $scope.$state=tabid;
      }
    
      $scope.autorefresh = true;
   }])   

.controller('UserListCtrl', function($scope, $interval, $http, helpers, $uibModal, Dialogs) {
  
  $scope.loadTable = function loadTable() {
    $http.get("rest/admin/users").then(function(response) {
      var data = response.data;
      $scope.users = data;
    });
  };

  $scope.loadTable();
  
  $scope.resetPwd = function(id) {
    $http.post("rest/admin/user/"+id+"/resetpwd").then(function() {
      $scope.loadTable();
    });
  }
  
  $scope.askAndRemoveUser = function(username) {
    Dialogs.showDeleteWarning().then(function() {
      $scope.removeUser(username)          
    })
  }
  
  $scope.removeUser = function(username) {
    $http.delete("rest/admin/user/"+username).then(function() {
      $scope.loadTable();
    });
  }
  
  $scope.addUser = function() {
    $scope.showEditUserPopup({});
  }
  
  $scope.editUser = function(username) {
    $http.get("rest/admin/user/"+username).then(function(response) {
      var user = response.data;
      $scope.showEditUserPopup(user);
    });
  }
  
  $scope.showEditUserPopup = function(user) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      animation: $scope.animationsEnabled,
      templateUrl: 'editUserModalContent.html',
      controller: 'editUserModalCtrl',
      resolve: {
        user: function () {
          return user;
        }
      }
    });
      
    modalInstance.result.then(function() {$scope.loadTable()}, function () {}); 
  }
})
      
.run(function(ViewRegistry) {
  ViewRegistry.registerDashlet('admin/controller','Maintenance','partials/maintenanceConfiguration.html','maintenance');
})

.controller('ControllerSettingsCtrl', function($scope, $http, stateStorage, ViewRegistry) {
  $scope.configurationItems = ViewRegistry.getDashlets("admin/controller");

  stateStorage.push($scope, 'controller', {});
  
  $scope.$watch('$state',function() {
    if($scope.$state!=null) {
      $scope.currentConfigurationItem = _.find($scope.configurationItems,function(item) {
        return item.id==$scope.$state})
    }
  });

  $scope.currentConfigurationItem = $scope.configurationItems[0];
  
  $scope.setCurrentConfigurationItem = function(item) {
    $scope.$state = item.id;
    $scope.currentConfigurationItem = item;
  }
})

.controller('MaintenanceSettingsCtrl', function($scope, $http, ViewRegistry, MaintenanceService) {
  $scope.maintenanceMessage;
  
  $scope.toggle = {};
  $scope.toggle.switch = false;
  
  $http.get("rest/admin/maintenance/message").then(function(res) {
    $scope.maintenanceMessage = res.data;
  });
  
  $http.get("rest/admin/maintenance/message/toggle").then(function(res) {
    $scope.toggle.switch = (res.data === 'true');
  });
  
  $scope.saveMaintenanceMessage = function() {
    $http.post("rest/admin/maintenance/message", $scope.maintenanceMessage).then(function() {
      MaintenanceService.reloadMaintenanceMessage();
    });
  }
  
  $scope.upateToggle = function() {
    $http.post("rest/admin/maintenance/message/toggle", $scope.toggle.switch).then(function() {
      MaintenanceService.reloadMaintenanceMessage();
    });
  }
  
  $scope.switchToggle = function() {
    $scope.toggle.switch = !$scope.toggle.switch;
    $scope.upateToggle();
  }

})

.controller('editUserModalCtrl', function ($scope, $uibModalInstance, $http, $location, AuthService, user) {
   $scope.roles = AuthService.getConf().roles;
   $scope.user = user;
   
   if(!user.role) {
     user.role = $scope.roles[0];     
   }
   
   $scope.save = function() {
     $http.post("rest/admin/user", user).then(function() {
       $uibModalInstance.close();  
     });
   }
   
   $scope.cancel = function() {
     $uibModalInstance.close();     
   }
})

.controller('MyAccountCtrl',
    function($scope, $rootScope, $interval, $http, helpers, $uibModal) {
      $scope.$state = 'myaccount';
      
      $scope.changePwd=function() {
        $uibModal.open({backdrop: 'static',animation: false,templateUrl: 'partials/changePasswordForm.html',
          controller: 'ChangePasswordModalCtrl', resolve: {}});  
      }
      
      $scope.user = {};
      $http.get("rest/admin/myaccount").then(function(response) {
        var user = response.data;
        $scope.user=user;
        
        $scope.preferences = [];
        if($scope.user.preferences) {
          _.mapObject($scope.user.preferences.preferences,function(val,key) {
            $scope.preferences.push({key:key,value:val});
          });        
        }
      });
      
      $scope.addPreference = function() {
        $scope.preferences.push({key:"",value:""});
      }
            
      $scope.savePreferences = function() {
        var preferences = {preferences:{}};
        _.each($scope.preferences, function(entry) {
          preferences.preferences[entry.key]=entry.value;
        });
        $http.post("rest/admin/myaccount/preferences",preferences).then(function() {
          
        },function() {
          $scope.error = "Unable to save preferences. Please contact your administrator.";
        });  
      }
    })

.controller('ChangePasswordModalCtrl', function ($scope, $rootScope, $uibModalInstance, $http, $location) {
  
  $scope.model = {newPwd:""};
  $scope.repeatPwd = ""
  
  $scope.save = function () {  
    if($scope.repeatPwd!=$scope.model.newPwd) {
      $scope.error = "New password doesn't match"
    } else {
      $http.post("rest/admin/myaccount/changepwd",$scope.model).then(function() {
        $uibModalInstance.close();
      },function() {
        $scope.error = "Unable to change password. Please contact your administrator.";
      });      
    }
  };

  $scope.cancel = function () {
    $uibModalInstance.close();
  };
});
