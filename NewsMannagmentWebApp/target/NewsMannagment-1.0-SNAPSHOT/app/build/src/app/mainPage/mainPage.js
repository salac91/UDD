angular.module('ngBoilerplate.mainPage',['ui.router', 'ngResource', 'base64'])
.config(function($stateProvider) {
    $stateProvider.state('mainPage', {
        url:'/mainPage',
        views: {
            'main': {
                templateUrl:'mainPage/mainPage.tpl.html',
                controller: 'MainPageCtrl'
            }
        },
        data : { pageTitle : "Main Page" },
        resolve: {
             documents: function(documentService) {
                  return documentService.getAllDocs();
             }
        }
    })
    .state('search', {
            url:'/search',
            views: {
                'main': {
                    templateUrl:'mainPage/search.tpl.html',
                    controller: 'SearchCtrl'
                }
            },
            data : { pageTitle : "Search" }
            }
    )
    .state('textPage', {
            url:'/textPage',
            views: {
                'main': {
                    templateUrl:'mainPage/textPage.tpl.html',
                    controller: 'TextPageCtrl'
                }
            },
            data : { pageTitle : "TextPage" }
            }
    )
    .state('publishPage', {
            url:'/publishPage',
            views: {
                'main': {
                    templateUrl:'mainPage/publishPage.tpl.html',
                    controller: 'PublishPageCtrl'
                }
            },
            data : { pageTitle : "Publishing Page" },       
            resolve: {
            documents: function(documentService) {
                 return documentService.getAllUnpublishedDocs();
             }
     }
    });
})
.service('sharedProperties', function () {
    var property = "";

    return {
        getProperty: function () {
            return property;
        },
        setProperty: function (value) {
            property = value;
        }
    };
})
.factory('documentService', function($resource) {
    var service = {};

    service.getAllUnpublishedDocs = function() {
          var Document = $resource("/NewsMannagment/rest/documents/unpublished");
          return Document.get().$promise.then(function(data) {
            return data.documents;
          });
      };
      
      service.getAllDocs = function() {
          var Document = $resource("/NewsMannagment/rest/documents");
          return Document.get().$promise.then(function(data) {
            return data.documents;
          });
      };
      
    return service;
})
.controller("MainPageCtrl", function($scope, $http, sharedProperties, documents) {
	$scope.documents = documents;
	$scope.setFile=function(fileInput){
	var file=fileInput.value;
  };
	$scope.uploadFile=function(){
	var formData=new FormData();
	formData.append("file",file.files[0]);
	$http.post('/NewsMannagment/rest/documents/newDocument', formData, {
        transformRequest: function(data, headersGetterFunction) {
            return data;
        },
        headers: {'Content-Type': undefined }
        }).success(function(data, status) {                       
            alert("Success ... " + status);
        }).error(function(data, status) {
            alert("Error ... " + status);
        });
  };
  
   $scope.removeDocument = function(uid) {
            $http.post('/NewsMannagment/rest/documents/removeDocument/' + uid).success(function(data, status) {                       
				alert("Success ... " + status);
                $scope.documents = data.documents;
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
   };
  
   $scope.readText = function(document) {
		sharedProperties.setProperty(document);
   };
})
.controller("SearchCtrl", function($scope, $http) {
    $scope.resultDocs = {};
    $scope.model = {};
    $scope.model.titleSearchType = "regular";
    $scope.model.titleSearchCondition = "must";
    $scope.model.authorSearchType = "regular";
    $scope.model.authorSearchCondition = "must";
    $scope.model.apstractSearchType = "regular";
    $scope.model.apstractSearchCondition = "must";
    $scope.model.dateSearchType = "regular";
    $scope.model.dateSearchCondition = "must";
    $scope.model.categorySearchType = "regular";
    $scope.model.categorySearchCondition = "must";
    $scope.model.textSearchType = "regular";
    $scope.model.textSearchCondition = "must";
    
	$scope.search = function() {
            $http.get('/NewsMannagment/rest/documents/search/' + $scope.searchTerm + '/' + $scope.searchType).success(function(data, status) {                       
                $scope.resultDocs = data.documents;
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
	
	$scope.searchAdvanced = function() {
            $http.post('/NewsMannagment/rest/search/searchAdvanced', $scope.model).success(function(data, status) {                       
                $scope.resultDocs = data.documents;
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
})
.controller("TextPageCtrl", function($scope, $http, sharedProperties, documentService) {
        var document = sharedProperties.getProperty();
        $scope.doc = document;
})
.controller("PublishPageCtrl", function($scope, $http, documents) {
	$scope.unpublishedDocs = documents;
	$scope.missingFields = {};
    $scope.updatedDoc = {};	
	$scope.publishDocument=function(uid){
            $scope.uid = uid;
			$http.post('/NewsMannagment/rest/documents/publishDocument/' + uid).success(function(data, status) {                       
				alert("Success ... " + status);
                $scope.missingFields = data;
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
		
	};
	
	$scope.updateDocument = function() {
            $scope.updatedDoc.uid = $scope.uid; 
            $http.post('/NewsMannagment/rest/documents/updateDocument', $scope.updatedDoc).success(function(data, status) {                       
				alert("Success ... " + status);
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
});