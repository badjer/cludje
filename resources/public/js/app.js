angular.module('mainapp', [], function($routeProvider, $locationProvider){
	$routeProvider.when('/hello', {
		templateUrl: 'hello.tpl.html'
	});
});
