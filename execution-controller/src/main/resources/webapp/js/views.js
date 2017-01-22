angular.module('views',[]).factory('viewFactory', ['$http','$q','$filter', function($http,$q,$filter) {
  var urlBase = 'rest/views';
  var dataFactory = {};

  dataFactory.get = function (viewId, eId) {
      return $http.get(urlBase + '/' + viewId + '/' + eId);
  };

  function defaultChart(title) {
    return {
      labels:[], 
      series:[], 
      data:[],
      options:{
        animation : false,
        title : {
          display : true,
          text : title
        },
        scales: {
          yAxes: [{
              ticks: {
                  suggestedMin: 0
              }
          }]
        },
        legend: {
          display : false
        }
      }
    }
  }
  
  function calculateThroughput(model, intervalId, intervalStartTime, intervalCount, count) {
    var intervalSize;
    if(intervalId==1) {
      intervalSize = intervalStartTime+model.resolution-model.minTime;
    } else if (intervalId==intervalCount) {
      intervalSize = model.maxTime-intervalStartTime
    } else {
      intervalSize = model.resolution;
    }
    return count/intervalSize*1000;
  }
  
  dataFactory.getTimeBasedChart = function (viewId, eId, title) {
    return $q(function(resolve, reject) {
      dataFactory.get(viewId, eId).success(
        function(data) {
          var timeChart = defaultChart(title);
          timeChart.data.push([]);
          timeChart.series.push(title);
          
          var i = 0;
          var size = Object.keys(data.intervals).length;
          _.mapObject(data.intervals,function(entry,date){
            timeChart.labels.push($filter('date')(date, 'HH:mm:ss'));
            timeChart.data[0].push(calculateThroughput(data, ++i, parseInt(date), size, entry.count))});
          resolve(timeChart);
        });
    });
  };
  
  dataFactory.getReportNodeStatisticCharts = function (eId) {
    return $q(function(resolve, reject) {
      dataFactory.get('ReportNodeStatistics', eId).success(function(data) {
        var charts = {};
        charts.throughputchart = defaultChart('Throughput (Keywords/s)');
        charts.throughputchart.data.push([]);
        charts.throughputchart.series.push('Keywords/s');
        
        charts.responseTimeByFunctionChart = defaultChart('Reponse times (s)');
        charts.responseTimeByFunctionChart.options.legend.display = true;
        
        charts.performancechart = defaultChart('Throughput (Keywords/s)');
        charts.performancechart.options.legend.display = true;

        var size = Object.keys(data.intervals).length
        var i = 0;
        _.mapObject(data.intervals,function(entry,date){
          charts.throughputchart.labels.push($filter('date')(date, 'HH:mm:ss'));
          charts.throughputchart.data[0].push(calculateThroughput(data, ++i, parseInt(date), size, entry.count))});

        var i = 0;
        _.mapObject(data.intervals,function(entry,date){
          i++;
          charts.performancechart.labels.push($filter('date')(date, 'HH:mm:ss'));
          charts.responseTimeByFunctionChart.labels.push($filter('date')(date, 'HH:mm:ss'));
          _.mapObject(entry.byFunctionName, function(statistics, functionName) {
            var id = charts.performancechart.series.indexOf(functionName);
            if(id==-1) {
              charts.performancechart.series.push(functionName);
              charts.performancechart.data.push([]);
              
              charts.responseTimeByFunctionChart.series.push(functionName);
              charts.responseTimeByFunctionChart.data.push([]);
              
              id = charts.performancechart.series.length-1;
            }
            charts.performancechart.data[id].push(calculateThroughput(data, i, parseInt(date), size, statistics.count));
            if(statistics.count>0) {
              charts.responseTimeByFunctionChart.data[id].push(statistics.sum/statistics.count)
            } else {
              charts.responseTimeByFunctionChart.data[id].push(0)
            }
            
          });
        })
        
        resolve(charts);
        
      });
    });
  };
  
  return dataFactory;
}])

.directive('throughputView', function($http,viewFactory) {
  return {
    restrict: 'E',
    scope: {
      eid: '=',
      handle: '='
    },
    controller: function($scope) {
      $scope.throughputchart = {};
    },
    link: function($scope, $element) { 
      $scope.handle.refresh = function() {
        viewFactory.getReportNodeStatisticCharts(eId).then(function(charts){
          $scope.throughputchart = charts.throughputchart;
        })
      }
    },
    templateUrl: 'partials/execution/views/throughput.html'}
})