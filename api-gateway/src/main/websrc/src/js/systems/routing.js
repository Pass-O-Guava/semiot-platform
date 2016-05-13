"use strict";

export default function($routeProvider, $locationProvider) {
    $routeProvider
        .when('/systems', {
            templateUrl: '/partials/system-list.html',
            controller: 'SystemListCtrl'
        })
        .when('/systems/:system_id*', {
            templateUrl: '/partials/system-detail.html',
            controller: 'SystemDetailCtrl'
        });

    // use the HTML5 History API
    $locationProvider.html5Mode(true);
}
