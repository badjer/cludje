angular.module('mainapp', [], function($routeProvider, $locationProvider){
	$routeProvider.when('/hello', {
		templateUrl: 'hello.tpl.html'
	});
	$routeProvider.when('/goodbye', {
		templateUrl: 'goodbye.tpl.html'
	});
});

function MainCntl($scope){
	$scope.data = {}
};

