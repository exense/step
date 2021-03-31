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
          display : (title),
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
      },
      fill: false
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
      dataFactory.get(viewId, eId).then(
        function(response) {
          var data = response.data;
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

    dataFactory.getTimeBasedGaugeChart = function (viewId, eId, startTime, endTime, title) {
      return $q(function(resolve, reject) {
        dataFactory.get(viewId, eId).then(
          function(response) {
            var data = response.data;
            var timeChart = defaultChart(title);
            timeChart.options.legend.display = true
            timeChart.options.scales.yAxes[0].stacked = true;
            timeChart.fill='origin';
            var i = 0;
            var size = Object.keys(data.intervals).length;
            var previousValue = 0;
            var previousValues = {};
            //init all series (requires if anything starts with an offset)
            _.mapObject(data.intervals,function(entry,date){
              _.mapObject(entry.byThreadGroupName, function(statistics, name) {
                previousValues[name]=0;
              });
            });
            var prevTimestamp = parseInt(startTime);
            var minInterval=Math.round((parseInt(endTime)-parseInt(startTime))/20);
            if (minInterval <= 0) {
              minInterval=1;
            }
            _.mapObject(data.intervals,function(entry,date){
              var dateInt = parseInt(date);
              //Gauge only have actual points when value changes, create intermediate points with current value
              while (dateInt >= (prevTimestamp+minInterval)) {
                prevTimestamp+=minInterval;
                timeChart.labels.push($filter('date')(prevTimestamp, 'HH:mm:ss'));
                _.mapObject(previousValues, function(count, name) {
                  var id = timeChart.series.indexOf(name);
                  if(id==-1) {
                    timeChart.series.push(name);
                    timeChart.data.push([]);
                    id = timeChart.series.length-1;
                  }
                  timeChart.data[id].push(count);
                });

              }
              //process the intervals with metrics
              timeChart.labels.push($filter('date')(date, 'HH:mm:ss'));
              _.mapObject(entry.byThreadGroupName, function(statistics, name) {
                var id = timeChart.series.indexOf(name);
                if(id==-1) {
                  timeChart.series.push(name);
                  timeChart.data.push([]);
                  id = timeChart.series.length-1;
                }
                timeChart.data[id].push(statistics.count);
                previousValues[name] = statistics.count;
              });
              previousValue = entry.count;
              prevTimestamp = parseInt(date);
            });
            //Finally insert points till end of test execution
            while ((prevTimestamp+minInterval) < endTime) {
              prevTimestamp+=minInterval;
              timeChart.labels.push($filter('date')(prevTimestamp, 'HH:mm:ss'));
              //timeChart.data[0].push(previousValue);
              _.mapObject(previousValues, function(count, name) {
                var id = timeChart.series.indexOf(name);
                if(id > -1) { //must already exist
                  timeChart.data[id].push(count);
                }
              });
            }
            resolve(timeChart);
        });
      });
    };
  
  dataFactory.getReportNodeStatisticCharts = function (eId) {
    return $q(function(resolve, reject) {
      dataFactory.get('ReportNodeStatistics', eId).then(function(response) {
        var data = response.data;
        var charts = {};
        charts.throughputchart = defaultChart('Throughput (Keywords/s)');
        charts.throughputchart.data.push([]);
        charts.throughputchart.series.push('Keywords/s');
        
        charts.responseTimeByFunctionChart = defaultChart('Reponse times (ms)');
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
