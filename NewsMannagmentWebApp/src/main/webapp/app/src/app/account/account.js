angular.module('ngBoilerplate.account', ['ui.router', 'ngResource', 'base64'])
.config(function($stateProvider) {
    $stateProvider.state('login', {
        url:'/login',
        views: {
            'main': {
                templateUrl:'account/login.tpl.html',
                controller: 'LoginCtrl'
            }
        },
        data : { pageTitle : "Login" }
    })
    .state('register', {
            url:'/register',
            views: {
                'main': {
                    templateUrl:'account/register.tpl.html',
                    controller: 'RegisterCtrl'
                }
            },
            data : { pageTitle : "Registration" }
            }
    )
    .state('passwordChange', {
            url:'/passwordChange',
            views: {
                'main': {
                    templateUrl:'account/passwordChange.tpl.html',
                    controller: 'PasswordChangeCtrl'
                }
            },
            data : { pageTitle : "Password Change" }
            }
    )
    .state('management', {
            url:'/management',
            views: {
                'main': {
                    templateUrl:'account/management.tpl.html',
                    controller: 'AccountManagementCtrl'
                }
            },
            data : { pageTitle : "Account Management" },
            resolve: {
            accounts: function(accountService) {
                 return accountService.getAllAccounts();
             }
         }
    });
})
.factory('sessionService', function($http, $base64, $resource) {
    var session = {};
    session.login = function(data) {
        return $http.post("/NewsMannagment/login", "username=" + data.name +
        "&password=" + data.password, {
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        } ).then(function(data) {
            //localStorage.setItem("session", {});
        }, function(data) {
            alert("Wrong email or password!");
        });
    };
    session.logout = function() {
        //localStorage.removeItem("session");
    };
    session.isLoggedIn = function() {
        //var Account = $resource("/NewsMannagment/rest/accounts/logedIn");
        // return Account.get().$promise.then(function(data) {
        //      return data.IsLoged;
        //  });
        return localStorage.getItem("session") != null;
    };
    return session;
})
.factory('accountService', function($resource) {
    var service = {};
    service.register = function(account, success, failure) {
        var Account = $resource("/NewsMannagment/rest/accounts");
        Account.save({}, account, success, failure);
    };
    service.getAccountById = function(accountId) {
        var Account = $resource("/NewsMannagment/rest/accounts/:paramAccountId");
        return Account.get({paramAccountId:accountId}).$promise;
    };
    service.getRole = function() {
     var Role = $resource("/NewsMannagment/rest/accounts/role");
        return Role.get().$promise.then(function(data) {
            return data.role;
          });
    };
    service.isUserLoggedIn = function() {
        var Account = $resource("/NewsMannagment/rest/accounts/logedIn");
         return Account.get().$promise.then(function(data) {
             return data.isLoged;
          });
    };
    service.userExists = function(account, success, failure) {
        var Account = $resource("/NewsMannagment/rest/accounts");
        var data = Account.get({name:account.name, password:account.password}, function() {
            var accounts = data.accounts;
            if(accounts.length !== 0) {
                success(account);
            } else {
                failure();
            }
        },
        failure);
    };
    service.userWithThisEmailExists = function(email) {
        var Account = $resource("/NewsMannagment/rest/accounts/userExists");
         return Account.get({email:email}).$promise.then(function(data) {
            return data.exists;
         });
    };
    service.getAllAccounts = function() {
          var Account = $resource("/NewsMannagment/rest/accounts");
          return Account.get().$promise.then(function(data) {
            return data.accounts;
          });
      };
    return service;
})
.controller("LoginCtrl", function($scope, sessionService, accountService, $state, $timeout) {
    $scope.login = function() {
        accountService.userExists($scope.account, function(account) {
            sessionService.login($scope.account);
            $timeout(function(){ $state.go("home"); }, 1000);             
        },
        function() {
            alert("Error logging in user");
        });
    };
})
.controller("RegisterCtrl", function($scope, sessionService, $http,  $state, accountService) {
	$scope.errorMessage = "";
    $scope.register = function() {
        $scope.account.status = "NON_ACTIVE";
        $scope.account.role = "Journalist";
        $http.get('/NewsMannagment/rest/accounts/userExists/' + $scope.account.name).success(function(data, status) {
                   var exists = data.exists;
                     if(exists === false) { 
                         $scope.errorMessage = "";
                         if($scope.account.password == $scope.passwordRepeat) {
                             accountService.register($scope.account,
                             function(returnedData) {               
                                 $state.go("home");          
                             },
                             function() {
                                 alert("Error registering user");           
                             });
                         }
                         else {
                             $scope.errorMessage = "Passwords must match!";
                         }
                      }
         else {
            $scope.errorMessage = "User with this email already exists!";
         }                               
				}).error(function(data, status) {
					alert("Error ... " + status);
				});
    };
})
.controller("PasswordChangeCtrl", function($scope, $http, sessionService, accountService) {
    $scope.errorMessage = "";
    $scope.changePassword = function() {
        if($scope.newPassword == $scope.newPasswordRepeat) {
             $http.get('/NewsMannagment/rest/accounts/changePassword/' + $scope.oldPassword + '/' + $scope.newPassword).success(function(data, status) {
                   if(data == "ok") {
                      alert("Password is successfully updated!");
                      $scope.errorMessage = "";   
                   }
                   else {
                      alert("Sorry but password is not updated!");
                   }                                          
				}).error(function(data, status) {
					alert("Error ... " + status);
				});
          }
        else {
          $scope.errorMessage = "Your new passwords doesn't match!";
        }
   };
})
.controller("AccountManagementCtrl", function($scope, accounts, accountService, $http) {
    $scope.accounts = accounts;
    $scope.Account = {};
    $scope.mode = "Add";
    
    $scope.selectedRow = null;
    $scope.setClickedRow = function (index, account) {  //function that sets the value of selectedRow to current index
         if (index == $scope.selectedRow) {
             $scope.mode = "Add";
             $scope.selectedRow = null;

             clearInputFields();
         }
         else {
            $scope.Account.id = account.id;
			$scope.Account.name = account.name;
			$scope.Account.password = account.password;
			$scope.Account.firstName = account.firstName;
			$scope.Account.lastName = account.lastName;
			$scope.Account.status = account.status;
			$scope.Account.role = account.role;
			
			$scope.selectedRow = index;             
			$scope.mode = "Update";
         }
     };
      
    $scope.createNewAccount = function() {
           if ($scope.mode == "Add") {              
                $http.post('/NewsMannagment/rest/accounts/createByAdmin', $scope.Account).success(function(data, status) {                                        
                   $http.get('/NewsMannagment/rest/accounts').success(function(data, status) {
                       $scope.accounts = data.accounts;                                         
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });
					
				}).error(function(data, status) {
					alert("Error ... " + status);
                });
           }
           else {
                $http.post('/NewsMannagment/rest/accounts/update', $scope.Account).success(function(data, status) {					
					$http.get('/NewsMannagment/rest/accounts').success(function(data, status) {
                       $scope.accounts = data.accounts;                                                              
					}).error(function(data, status) {
						alert("Error ... " + status);
					});
				}).error(function(data, status) {
					alert("Error ... " + status);
                });
           }
    };
    
    $scope.removeAccount = function(id) {
        $http.post('/NewsMannagment/rest/accounts/remove/' + id ).success(function(data, status) {                       
                   $http.get('/NewsMannagment/rest/accounts').success(function(data, status) {
                       $scope.accounts = data.accounts;
                       $scope.mode = "Add";                                         
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });
					
				}).error(function(data, status) {
					alert("Error ... " + status);
                });
    };
    
    function clearInputFields() {
         $scope.Account.name = null;
         $scope.Account.firstName = null;
         $scope.Account.lastName = null;
         $scope.Account.password = null;
         $scope.Account.status = null;
    }
});