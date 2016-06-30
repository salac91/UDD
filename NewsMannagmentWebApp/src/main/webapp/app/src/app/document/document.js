angular.module('ngBoilerplate.document',['ui.router', 'ngResource', 'base64'])
.config(function($stateProvider) {
    $stateProvider.state('mainPage', {
        url:'/mainPage',
        views: {
            'main': {
                templateUrl:'document/mainPage.tpl.html',
                controller: 'MainPageCtrl'
            }
        },
        data : { pageTitle : "Main Page" },
        resolve: {
             documents: function(documentService) {
                  return documentService.getAllDocs();
             },
             role: function(accountService) {
                    return accountService.getRole();
             }
        }
    })
    .state('search', {
            url:'/search',
            views: {
                'main': {
                    templateUrl:'document/search.tpl.html',
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
                    templateUrl:'document/textPage.tpl.html',
                    controller: 'TextPageCtrl'
                }
            },
            data : { pageTitle : "TextPage" }                 
        }
    )
    .state('documentManagement', {
            url:'/documentManagement',
            views: {
                'main': {
                    templateUrl:'document/documentManagement.tpl.html',
                    controller: 'DocumentManagementCtrl'
                }
            },
            data : { pageTitle : "Document Management" },
            resolve: {
            documents: function(documentService) {
                 return documentService.getUnpublishedByAuthorDocs();
             }
        }
    })
    .state('publishPage', {
            url:'/publishPage',
            views: {
                'main': {
                    templateUrl:'document/publishPage.tpl.html',
                    controller: 'PublishPageCtrl'
                }
            },
            data : { pageTitle : "Publishing Page" },       
            resolve: {
            documents: function(documentService) {
                 return documentService.getAllUnpublishedDocs();
             }
     }
    })
     .state('textEdit', {
            url:'/textEdit',
            views: {
                'main': {
                    templateUrl:'document/textEdit.tpl.html',
                    controller: 'TextEditCtrl'
                }
            },
            data : { pageTitle : "Edit page" }
            }
    );
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
      
      service.getUnpublishedByAuthorDocs = function() {
          var Document = $resource("/NewsMannagment/rest/documents/unpublishedByAuthor");
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
.controller("MainPageCtrl", function($scope, $http, sharedProperties, documents, role) {
	$scope.documents = documents;
	$scope.role = role;
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
.controller("SearchCtrl", function($scope, $http, sharedProperties) {
    $scope.resultDocs = {};
    $scope.highlights = {};
    $scope.showSuggestions = false; 
    $scope.ready = false;
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
    
	$scope.searchAdvanced = function() {
          
            $http.post('/NewsMannagment/rest/search/searchAdvanced', $scope.model).success(function(data, status) {                     
                $scope.resultDocs = data.documents;
                $scope.showSuggestions = false;
                if(data.documents.length === 0) {              
                      $scope.highlights = data.suggestions;
                      $scope.ready = false;
                      $scope.showSuggestions = true;
                }
                else { 
                   $scope.ready = true;
                }
                alert("Success!");
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
	
	$scope.searchSimple = function() {
            $http.get('/NewsMannagment/rest/search/searchSimple/' + $scope.simpleSearchQuery).success(function(data, status) {                                  
                $scope.resultDocs = data.documents;
                $scope.showSuggestions = false;
                if(data.documents.length === 0) {              
                      $scope.highlights = data.suggestions;
                      $scope.ready = false;
                      $scope.showSuggestions = true;
                }
                else { 
                   $scope.ready = true;
                }
                alert("Success!");
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
	
	$scope.moreSearch = function(value) {
      $http.get('/NewsMannagment/rest/search/searchSimple/' + value).success(function(data, status) {                                  
                $scope.resultDocs = data.documents;
                $scope.showSuggestions = false;
                if(data.documents.length === 0) {              
                      $scope.highlights = data.suggestions;
                      $scope.ready = false;
                      $scope.showSuggestions = true;
                }
                else { 
                   $scope.ready = true;
                }
                alert("Success!");
			}).error(function(data, status) {
				alert("Error ... " + status);
            });
	};
	
	$scope.readText = function(document) {
		sharedProperties.setProperty(document);
   };
})
.controller("TextPageCtrl", function($scope, $http, $state, sharedProperties) {

        var document = sharedProperties.getProperty();
        $scope.doc = document;
        $scope.showMoreLikeThis = false;
        $scope.message = "Please wait...";      
        
         $http.get('/NewsMannagment/rest/documents/getImages/' + $scope.doc.uid).success(function(data, status) { 
            $scope.imagesPath = data;
			}).error(function(data, status) {
				alert("Error while loading images!");
            });
            
        $http.get('/NewsMannagment/rest/search/getMoreLikeThis/' + document.uid).success(function(data, status) {
                        $scope.moreLikeThisDocs = data.documents;
                        $scope.showMoreLikeThis = true;
                        if($scope.moreLikeThisDocs.length === 0) {  
                           $scope.showMoreLikeThis = false;  
                           $scope.message = "There are no similiar texts in our libary!"; 
                        }                                         
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });
      $scope.readText = function(document) {
          sharedProperties.setProperty(document);
          $state.reload();
      }; 
})
.controller("DocumentManagementCtrl", function($scope, $http, documentService, documents, sharedProperties) {
        $scope.unpublishedDocs = documents;
        $scope.showDocuments = true;
        $scope.updatedDoc = {};
        if($scope.unpublishedDocs.length === 0) { $scope.showDocuments = false; }
        $scope.selectedRow = null;
        $scope.setClickedRow = function (index, document) {  //function that sets the value of selectedRow to current index
           if (index == $scope.selectedRow) {             
               $scope.selectedRow = null;           
           }
           else {
			$scope.updatedDoc.uid = document.uid;
			$scope.updatedDoc.title = document.title;
			$scope.updatedDoc.apstract = document.apstract;
			$scope.updatedDoc.category = document.category;
			$scope.updatedDoc.keyWords = document.keyWords;
			$scope.updatedDoc.tags = document.tags;
			$scope.selectedRow = index;
           }
       };
       
       $scope.editText = function(document) {
          sharedProperties.setProperty(document);
       };
       
       $scope.setFile=function(fileInput){
          var file=fileInput.value;
       };
        
        $scope.updateDocument = function() {
          if($scope.selectedRow != null) {
            $http.post('/NewsMannagment/rest/documents/updateDocument', $scope.updatedDoc).success(function(data, status) {                       
				alert("Document is successfully updated!");
				$http.get('/NewsMannagment/rest/documents/unpublishedByAuthor').success(function(data, status) {
                       $scope.unpublishedDocs = data.documents;                                         
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });	
			}).error(function(data, status) {
				alert("Error while updating document!");
            });
          }
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
            alert("Text is successfully uploaded!");
            $http.get('/NewsMannagment/rest/documents/unpublishedByAuthor').success(function(data, status) {
                       $scope.unpublishedDocs = data.documents;         
                       $scope.showDocuments = true;                                
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });	
            if($scope.unpublishedDocs.length === 0) { $scope.showDocuments = false; } 
        }).error(function(data, status) {
            alert("Error uploading text!");
        });
  };
        
})
.controller("PublishPageCtrl", function($scope, $http, documents) {
	$scope.unpublishedDocs = documents;
	$scope.missingFields = {};
    $scope.updatedDoc = {};	
	$scope.publishDocument=function(uid){
            $scope.uid = uid;
			$http.post('/NewsMannagment/rest/documents/publishDocument/' + uid).success(function(data, status) {
                if(data == "ok") {                      
                   alert("Text is successfully published!");
				}
				else {
                   alert("Text is not published, missing fields!");
				}
				$http.get('/NewsMannagment/rest/documents/unpublished').success(function(data, status) {
                       $scope.unpublishedDocs = data.documents;                                         
                   }).error(function(data, status) {
                      alert("Error ... " + status);
                   });
                $scope.missingFields = data;
			}).error(function(data, status) {
				alert("Error while publishing text!");
            });
		
	};
	
})
.controller("TextEditCtrl", function($scope, $http, sharedProperties) {
        var document = sharedProperties.getProperty();
        $scope.doc = document;
        $scope.updatedText = $scope.doc.text;
        $scope.updateText=function(){
         $scope.doc.text = $scope.updatedText;
         $http.post('/NewsMannagment/rest/documents/updateText', $scope.doc).success(function(data, status) {                   
                   alert("Text is successfully updated!");				
			}).error(function(data, status) {
				alert("Error while updating text!");
            });
        };
        
        $http.get('/NewsMannagment/rest/documents/getImages/' + $scope.doc.uid).success(function(data, status) { 
            $scope.imagesPath = data;
			}).error(function(data, status) {
				alert("Error while loading images!");
            });
            
});