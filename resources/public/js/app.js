angular.module('mainapp', [], function($routeProvider, $locationProvider){
	$routeProvider.when('/hello', {
		templateUrl: 'hello.tpl.html'
	});
	$routeProvider.when('/goodbye', {
		templateUrl: 'goodbye.tpl.html'
	});
});

function MainCntl($scope, $http){
	$scope.data = {};
	$scope.action = function(actname){
		// Do some ajax here
		$http.post('/', $scope.data)
			.success(function(data){
				$scope.data = data;
				console.log(data);
			});
	};
};
