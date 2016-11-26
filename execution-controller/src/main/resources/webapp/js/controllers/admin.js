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
angular.module('adminControllers', [ 'dataTable', 'step' ])

.controller('AdminCtrl', ['$scope', 'stateStorage',
    function($scope, $stateStorage) {
      $stateStorage.push($scope, 'admin', {lasttab:'users'});
      
      $scope.autorefresh = true;
      
      $scope.tabState = {adapters:false, tokens:false, quotamanager:false};
      if($scope.$state == null) { $scope.$state = 'adapters' };
      
      $scope.$watch('$state',function() {
        if($scope.$state!=null&&_.findWhere($scope.tabs, {id:$scope.$state})==null) {
          _.each(_.keys($scope.tabState),function(key) {
            $scope.tabState[key] = false;
          })
          $scope.tabState[$scope.$state] = true;
        }
      });
      
      $scope.$watchCollection('tabState',function() {
        _.each(_.keys($scope.tabState),function(key) {
          if($scope.tabState[key]) {
            $scope.$state =  key;
          }
        })
      })
  
}])   

.controller('UserListCtrl',
    function($scope, $interval, $http, helpers, $modal) {
      $scope.$state = 'users';
      
      $scope.datatable = {}
      
      $scope.loadTable = function loadTable() {
        $http.get("rest/admin/users").success(
          function(data) {
            var dataSet = [];
            for (i = 0; i < data.length; i++) {
              dataSet[i] = [ data[i].username, data[i].role];
            }
            $scope.tabledef.data = dataSet;
          });
        };

        $scope.tabledef = {};
        $scope.tabledef.columns = [ { "title" : "Username"}, { "title" : "Role" }, 
                                    {"title":"Actions", "width":"120px", "render":function ( data, type, row ) {
              var html = '<div class="input-group">' +
                '<div class="btn-group">' +
                '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#UserListCtrl\').scope().editUser(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon glyphicon-pencil" aria-hidden="true"></span>' +
                '<button type="button" class="btn btn-default" aria-label="Left Align" onclick="angular.element(\'#UserListCtrl\').scope().removeUser(\''+row[0]+'\')">' +
                '<span class="glyphicon glyphicon glyphicon glyphicon-trash" aria-hidden="true"></span>' +
                '</button> '+
                '</div></div>';
              return html;
            }}];

        $scope.tabledef.actions = [{"label":"Reset passwords","action":function() {$scope.forAllSelected('resetPwd')}},
                                   {"label":"Remove","action":function() {$scope.forAllSelected('removeUser')}}];
        
        $scope.loadTable();
        
        $scope.forAllSelected = function(fctName) {
          var rows = $scope.datatable.getSelection().selectedItems;
          
          for(i=0;i<rows.length;i++) {
            $scope[fctName](rows[i][0]);       
          }
        };
          
        $scope.resetPwd = function(id) {
          $http.post("rest/admin/user/"+id+"/resetpwd").success(function(data) {
            $scope.loadTable();
          });
        }
        
        $scope.removeUser = function(username) {
          $http.delete("rest/admin/user/"+username).success(function() {
            $scope.loadTable();
          });
        }
        
        $scope.addUser = function() {
          $scope.showEditUserPopup({});
        }
        
        $scope.editUser = function(username) {
          $http.get("rest/admin/user/"+username).success(function(user) {
            $scope.showEditUserPopup(user);
          });
        }
        
        $scope.showEditUserPopup = function(user) {
          var modalInstance = $modal.open({
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
      
.controller('editUserModalCtrl', function ($scope, $modalInstance, $http, $location, AuthService, user) {
   $scope.roles = _.keys(AuthService.getRoles());
   $scope.user = user;
   
   if(!user.role) {
     user.role = $scope.roles[0];     
   }
   
   $scope.save = function() {
     $http.post("rest/admin/user", user).success(function() {
       $modalInstance.close();  
     });
   }
   
   $scope.cancel = function() {
     $modalInstance.close();     
   }
})

.controller('MyAccountCtrl',
    function($scope, $rootScope, $interval, $http, helpers, $modal) {
      $scope.$state = 'myaccount';
      
      $scope.changePwd=function() {
        $modal.open({animation: false,templateUrl: 'partials/changePasswordForm.html',
          controller: 'ChangePasswordModalCtrl', resolve: {}});  
      }
      
      $scope.user = {};
      $http.get("rest/admin/user/"+$rootScope.context.userID).success(function(user) {
        $scope.user=user;
      });
    })

.controller('ChangePasswordModalCtrl', function ($scope, $rootScope, $modalInstance, $http, $location) {
  
  $scope.model = {newPwd:""};
  $scope.repeatPwd = ""
  
  $scope.save = function () {  
    if($scope.repeatPwd!=$scope.model.newPwd) {
      $scope.error = "New password doesn't match"
    } else {
      $http.post("rest/admin/myaccount/changepwd",$scope.model).success(function(user) {
        $modalInstance.close();
      }).error(function() {
        $scope.error = "Unable to change password. Please contact your administrator.";
      });      
    }
  };

  $scope.cancel = function () {
    $modalInstance.close();
  };
});
