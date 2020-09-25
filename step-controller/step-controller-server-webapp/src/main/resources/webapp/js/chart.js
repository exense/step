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
(function () {
  'use strict';

  Chart.defaults.global.responsive = true;
  Chart.defaults.global.legend.position = 'bottom';
  
  Chart.defaults.global.colours = [
    '#97BBCD', // blue
    '#DCDCDC', // light grey
    '#F7464A', // red
    '#46BFBD', // green
    '#FDB45C', // yellow
    '#949FB1', // grey
    '#4D5360'  // dark grey
  ];

  angular.module('chart.js', [])
    .directive('chartBase', function () { return chart(); })
    .directive('chartLine', function () { return chart('line'); })
    .directive('chartBar', function () { return chart('bar'); })

  function chart (type) {
    return {
      restrict: 'CA',
      scope: {
        handle: '=',
        colours: '=?',
        getColour: '=?',
        click: '='
      },
      link: function (scope, elem) {
        var chart, container = document.createElement('div');
        container.className = 'chart-container';
        elem.replaceWith(container);
        container.appendChild(elem[0]);

        if (typeof window.G_vmlCanvasManager === 'object' && window.G_vmlCanvasManager !== null) {
          if (typeof window.G_vmlCanvasManager.initElement === 'function') {
            window.G_vmlCanvasManager.initElement(elem[0]);
          }
        }
        
        scope.$watch('handle.data', function (newVal, oldVal) {
          if (! newVal || ! newVal.length || (Array.isArray(newVal[0]) && ! newVal[0].length)) return;

          if (chart) {
            //if (canUpdateChart(newVal, oldVal)) return updateChart(chart, newVal, scope);
            chart.destroy();
          }

          chart = createChart(type, scope, elem);
        }, true);

        scope.$watch('series', resetChart, true);
        scope.$watch('labels', resetChart, true);
        scope.$watch('options', resetChart, true);
        scope.$watch('colours', resetChart, true);

        scope.$watch('chartType', function (newVal, oldVal) {
          if (isEmpty(newVal)) return;
          if (angular.equals(newVal, oldVal)) return;
          if (chart) chart.destroy();
          chart = createChart(newVal, scope, elem);
        });

        scope.$on('$destroy', function () {
          if (chart) chart.destroy();
        });

        function resetChart (newVal, oldVal) {
          if (isEmpty(newVal)) return;
          if (angular.equals(newVal, oldVal)) return;

          // chart.update() doesn't work for series and labels
          // so we have to re-create the chart entirely
          if (chart) chart.destroy();

          chart = createChart(type, scope, elem);
        }
      }
    };
  }

  function canUpdateChart(newVal, oldVal) {
    if (newVal && oldVal && newVal.length && oldVal.length) {
      return Array.isArray(newVal[0]) ?
      newVal.length === oldVal.length && newVal[0].length === oldVal[0].length :
        oldVal.reduce(sum, 0) > 0 ? newVal.length === oldVal.length : false;
    }
    return false;
  }

  function sum (carry, val) {
    return carry + val;
  }

  function createChart (type, scope, elem) {
    if (! scope.handle.data || ! scope.handle.data.length) return;
    scope.getColour = typeof scope.getColour === 'function' ? scope.getColour : getRandomColour;
    scope.colours = getColours(scope);
    var cvs = elem[0], ctx = cvs.getContext('2d');
    var data = Array.isArray(scope.handle.data[0]) ?
      getDataSets(scope.handle.labels, scope.handle.data, scope.handle.series || [], scope.colours) :
      getData(scope.handle.labels, scope.handle.data, scope.colours);
    var chart = new Chart(ctx,{type: type,
                                data: data,
                                options: scope.handle.options || {}});
    scope.$emit('create', chart);

    if (scope.click) {
      cvs.onclick = function (evt) {
        var click = chart.getPointsAtEvent || chart.getBarsAtEvent || chart.getSegmentsAtEvent;

        if (click) {
          var activePoints = click.call(chart, evt);
          scope.click(activePoints, evt);
          scope.$apply();
        }
      };
    }
    return chart;
  }

  function getColours (scope) {
    var colours = angular.copy(scope.colours) || angular.copy(Chart.defaults.global.colours);
    while (colours.length < scope.handle.data.length) {
      colours.push(scope.getColour());
    }
    return colours.map(convertColour);
  }

  function convertColour (colour) {
    if (typeof colour === 'object' && colour !== null) return colour;
    if (typeof colour === 'string' && colour[0] === '#') return getColour(hexToRgb(colour.substr(1)));
    return getRandomColour();
  }

  function getRandomColour () {
    var colour = [getRandomInt(0, 255), getRandomInt(0, 255), getRandomInt(0, 255)];
    return getColour(colour);
  }

  function getColour (colour) {
    return {
      backgroundColor: rgba(colour, 0.2),
      borderColor: rgba(colour, 1),
      pointBorderColor: rgba(colour, 1),
      pointStrokeColor: '#fff',
      pointHighlightFill: '#fff',
      pointHighlightStroke: rgba(colour, 0.8)
    };
  }

  function getRandomInt (min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  function rgba(colour, alpha) {
    return 'rgba(' + colour.concat(alpha).join(',') + ')';
  }

  // Credit: http://stackoverflow.com/a/11508164/1190235
  function hexToRgb (hex) {
    var bigint = parseInt(hex, 16),
      r = (bigint >> 16) & 255,
      g = (bigint >> 8) & 255,
      b = bigint & 255;

    return [r, g, b];
  }

  function getDataSets (labels, data, series, colours) {
    return {
      labels: labels,
      datasets: data.map(function (item, i) {
        var dataSet = angular.copy(colours[i]);
        dataSet.label = series[i];
        dataSet.data = item;
        dataSet.fill = false;
        return dataSet;
      })
    };
  }

  function getData (labels, data, colours) {
    return labels.map(function (label, i) {
      return {
        label: label,
        value: data[i],
        color: colours[i].strokeColor,
        highlight: colours[i].pointHighlightStroke
      };
    });
  }

  function updateChart (chart, values, scope) {
    if (Array.isArray(scope.handle.data[0])) {
      chart.datasets.forEach(function (dataset, i) {
        (dataset.points || dataset.bars).forEach(function (dataItem, j) {
          dataItem.value = values[i][j];
        });
      });
    } else {
      chart.segments.forEach(function (segment, i) {
        segment.value = values[i];
      });
    }
    chart.update();
    scope.$emit('update', chart);
  }

  function isEmpty (value) {
    return ! value ||
      (Array.isArray(value) && ! value.length) ||
      (typeof value === 'object' && ! Object.keys(value).length);
  }

})();
