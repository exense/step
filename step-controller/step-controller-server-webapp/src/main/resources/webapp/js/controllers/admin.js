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
angular.module('adminControllers', ['step' ])

.run(function(ViewRegistry, EntityRegistry, $timeout) {
  ViewRegistry.registerView('admin','partials/admin.html');
  ViewRegistry.registerView('settings','partials/settings.html');
  EntityRegistry.registerEntity('User', 'users', 'users', 'rest/admin/user/', 'rest/admin/user', 'st-table', '/partials/users/userSelectionTable.html');
  ViewRegistry.registerDashlet('admin','Users','partials/users/users.html','users');
  $timeout(function() {ViewRegistry.registerDashlet('admin','Settings','partials/adminSettings.html','controller');})
})

.controller('AdminCtrl', ['$scope', 'stateStorage', 'ViewRegistry',
    function($scope, stateStorage, ViewRegistry) {
      // push this scope to the state stack
      stateStorage.push($scope, 'admin', {});

      $scope.tabs = ViewRegistry.getDashlets("admin");

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
  
  //does not seem to be used anywhere
  $scope.resetPwd = function(id) {
    $http.post("rest/admin/user/"+id+"/resetpwd").then(function() {
      $scope.loadTable();
    });
  }

  $scope.resetUserPassword = function(id) {
    Dialogs.showWarning('Are you sure you want to reset this users password?').then(() => {
        $scope.showResetPasswordPopup(id);
    });
  }
  
  $scope.askAndRemoveUser = function(username) {
    Dialogs.showDeleteWarning(1, 'User "' + username + '"').then(function() {
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
    isNew = (user.username) ? false : true;
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      animation: $scope.animationsEnabled,
      templateUrl: 'partials/users/editUserModalContent.html',
      controller: 'editUserModalCtrl',
      resolve: {
        user: function () {
          return user;
        }
      }
    });
      
    modalInstance.result.then(function(result) {
      console.log(result);
      if (isNew && result === 'save') {
        $scope.showResetPasswordPopup(user);
      } else {
        $scope.loadTable();
      }
    }, function () {}); 
  }

  $scope.showResetPasswordPopup = function(user) {
    var modalInstance = $uibModal.open({
      backdrop: 'static',
      animation: $scope.animationsEnabled,
      templateUrl: 'partials/users/resetPasswordContent.html',
      controller: 'resetPasswordModalCtrl',
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

.controller('LightSettingsCtrl', function($scope, $http, stateStorage, ViewRegistry) {
  $scope.configurationItems = ViewRegistry.getDashlets('settings');

  stateStorage.push($scope, 'settings', {});

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
   $scope.new = ($scope.user.username) ? false : true;
   
   if(!user.role) {
     user.role = $scope.roles[0];     
   }
   
   $scope.save = function() {
     $http.post("rest/admin/user", user).then(function() {
       $uibModalInstance.close('save');
     });
   }
   
   $scope.cancel = function() {
     $uibModalInstance.close('cancel');     
   }
})

.controller('resetPasswordModalCtrl', function ($scope, $uibModalInstance, $http, $location, AuthService, user) {
  $scope.user = user;
  $http.post("rest/admin/user/" + user.username + "/resetpwd").then(function(response) {$scope.password = response.data.password});
  
  $scope.close = function() {
      $uibModalInstance.close();
  }
})

.controller('MyAccountCtrl',
    function($scope, $rootScope, $interval, $http, helpers, $uibModal, AuthService) {
      $scope.$state = 'myaccount';
      
      $scope.token = "";
      
      $scope.changePwd=function() {
        AuthService.showPasswordChangeDialog(false);
      }

      $scope.showGenerateApiKeyDialog = function () {
        var modalInstance = $uibModal.open({backdrop: 'static',animation: false,templateUrl: 'partials/generateApiKey.html',
          controller: 'GenerateApiKeyModalCtrl'});
        return modalInstance.result;
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

  .controller('GenerateApiKeyModalCtrl', function ($scope, $rootScope, $uibModalInstance, $http) {

    $scope.token = '';

    $scope.generateToken=function() {
      const days = $scope.lifetime ? $scope.lifetime : 0;
      $http.get("rest/access/service-account/token?lifetime="+ days).then(function(response) {
        $scope.token = response.data;
      });
    }

    $scope.copyToken=function() {
      navigator.clipboard.writeText($scope.token).then(function() {
        $scope.copied = true;
        $scope.$apply();
      });
    }

    $scope.cancel = function () {
      $uibModalInstance.close();
    };
  })


