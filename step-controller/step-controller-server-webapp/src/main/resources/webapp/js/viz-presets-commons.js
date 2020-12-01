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
function getVizDashboardCommonsList(){
	return [["WikimediaDemo"], ["PerformanceDashboard"], ["RealtimePerformanceDashboard"], ["RTMDashboard"]];
}


var overtimeFillBlanksTransformFn = function(response, args) {
	var metric = args.metric;
	var retData = [], series = [];

	var payload = response.data.payload.stream.streamData;
	var payloadKeys = Object.keys(payload);

	for (i = 0; i < payloadKeys.length; i++) {
		var series_ = payload[payloadKeys[i]];
		var serieskeys = Object.keys(series_ )
		for (j = 0; j < serieskeys.length; j++) {
			if(!series.includes(serieskeys[j])){
				series.push(serieskeys[j]);
			}
		}
	}

	for (i = 0; i < payloadKeys.length; i++) {
		var series_ = payload[payloadKeys[i]];
		var serieskeys = Object.keys(series)
		for (j = 0; j < serieskeys.length; j++) {
			var key = series[serieskeys[j]];
			var yval;
			if(series_[key] && series_[key][metric]){
				yval = series_[key][metric];
			}else{
				yval = 0;
			}
			retData.push({
				x: payloadKeys[i],
				y: yval,
				z: key
			});
		}
	}
	return retData;
};

function TimelineWidget(outerScope) {

	var entityName = 'Measurement';
	var measurementType = 'keyword';

	var timeFrame = null;

	var timeField = "begin";
	var timeFormat = "long";
	var valueField = "value";
	var groupby = "rnStatus";

	var textFilters = "[{ \"key\": \"eId\", \"value\": \"__businessobjectid__\", \"regex\": \"false\" }, { \"key\": \"type\", \"value\": \"__measurementType__\", \"regex\": \"false\" }]";
	var numericalFilters = "[]";

	var config = new Config('Fire','Off', true, false, 'unnecessaryAsMaster');
	var wId = 'timelineWidget-' + getUniqueId();

	var wOptions = new EffectiveChartOptions('doesntMatter-overriden', null, timeFrame);

	var timelineWidget = new Widget(wId, new DefaultWidgetState(), new DashletState(wId, false, 0, {}, wOptions, config, new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", overtimeFillBlanksTransformFn.toString(),  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame), new DefaultGuiClosed(), new DefaultInfo()));

	var rtime;
	var timeout = false;
	var delta = 200;

	outerScope.resizeTimeline = function(){
		$(document).ready(function(){
			
			if(timelineWidget && timelineWidget.state && timelineWidget.state.api){
				console.log('resizing timeline...')
				/*
				var chartsvgScope = timelineWidget.state.api.getScope();
				$(chartsvgScope.svg[0]).find('.nv-focus').first().remove()
				$(chartsvgScope.svg[0]).find('.nv-y.nv-axis').first().remove()
				 */
				//timelineWidget.state.api.updateWithOptions();
				
			}else{
				console.log('Warning: timeline could not be resized')
			}
		});
	};

	outerScope.resizeend = function() {
		if (new Date() - rtime < delta && typeof resizeend !== 'undefined') {
			setTimeout(resizeend, delta);
		} else {
			timeout = false;
			outerScope.resizeTimeline();
		}               
	}
	
	outerScope.$on('resize-timeline', function(){
		console.log('resize-timeline event received');
		outerScope.resizeTimeline();
	});

	outerScope.$on('async-query-cycle-complete', function(event, arg){
		if(arg.startsWith('timeline')){
			$(document).ready(function(){
				if(!outerScope.timelineReady){
					outerScope.timelineReady = true;
					
					if(!outerScope.listenerSetup){
						console.log('timeline data was loaded after chart was configured: listener setup by timeline data complete.');
						outerScope.setupListener();
					}
				}
			})
		}
	});

	outerScope.extent = {};

	outerScope.timelineReady = false;
	outerScope.listenerSetup = false;

	outerScope.sendExtent = function (from, to){
		console.log('sendExtent: ['+from+', '+to+']');
		outerScope.$broadcast('apply-global-setting', new Placeholder('__from__', from, 'Off'));
		outerScope.$broadcast('apply-global-setting', new Placeholder('__to__', to, 'Off'));
	};

	outerScope.setupListener = function(){
		console.log('setting up timeline listener');
		if(outerScope.chartScope){
		var existing = outerScope.chartScope.focus.dispatch.onBrush.on;
		var newBrush = function(e){
			outerScope.extent.from =  Math.round(e[0]);
			outerScope.extent.to =  Math.round(e[1]);

			if(!timelineWidget.state.maxExtentInit || timelineWidget.state.maxExtentInit === false){
				timelineWidget.state.maxExtentInit = true;
				timelineWidget.state.minExtent = outerScope.extent.from;
				timelineWidget.state.maxExtent = outerScope.extent.to;
				console.log('timelineWidget.state.maxExtent')
				console.log(timelineWidget.state.maxExtent)
				timelineWidget.state.granularity = Math.round(
						(Math.round(outerScope.extent.to) - Math.round(outerScope.extent.from)) / 29
				);
				//console.log(timelineWidget.state.granularity)
			}else{
				// offset always present
				//outerScope.extent.to += timelineWidget.state.granularity + 1;

				// adding offset only if the ruler "hits the border"
				if(outerScope.extent.to == timelineWidget.state.maxExtent){
					outerScope.extent.to += timelineWidget.state.granularity + 1;
				}

			}

		}
		newBrush.on = existing;
		outerScope.chartScope.focus.dispatch.onBrush = newBrush;

		var brushC = d3.select("viz-dashlet.timelinewidget svg g");
		var existingBrushOn = outerScope.chartScope.focus.brush.on;

		//hijacked
		outerScope.chartScope.focus.brush.on('brushend', function(type, listener){
			outerScope.sendExtent(outerScope.extent.from, outerScope.extent.to)
			if(type && typeof(type) === 'string'){
				existingBrushOn(type, listener);
			}
		});

		outerScope.listenerSetup = true;
		}else{
			console.log('Warning: setupListener() was invoked before chartScope was set.');
		}
	}

	timelineWidget.state.options.innercontainer.height = 100;
	timelineWidget.state.options.chart = {
			type: 'stackedAreaWithFocusChart',
			colorFunction : function(str) {
				if (str === 'PASSED') {
					return "rgb(23,216,33)";
				}
				if (str === 'FAILED') {
					return "red";
				}
				if (str === 'TECHNICAL_ERROR') {
					return "black";
				}
				return "blue";
			},
			height: 75,
			margin: {
				top: 0, right: 30, bottom: 0, left: 30
			},
			tooltip: {
				enabled: true
			},
			showLegend: false, forceY: 0, showControls: false,
			xAxis: {
				strTickFormat: function (d) {
					var value;
					if ((typeof d) === "string") {
						value = parseInt(d);
					} else {
						value = d;
					}

					return $scope.toDynamicDateFormat(value);
				}.toString()
			},
			callback: function(scope, element){
				//console.log(scope)
				outerScope.chartScope = scope;

				if(outerScope.chartScope){
					//console.log(outerScope.chartScope);
					outerScope.extent = {};
					outerScope.chartScope.focus.xAxis.tickFormat(function (d) {
						var value;
						if ((typeof d) === "string") {
							value = parseInt(d);
						} else {
							value = d;
						}
						return d3.time.format("%H:%M:%S")(new Date(value));
					});

					//$(document).ready(function(){
						var chartsvgScope = timelineWidget.state.api.getScope()
						//console.log(chartsvgScope);
						$(chartsvgScope.svg[0]).find('.nv-focus').first().remove()
						$(chartsvgScope.svg[0]).find('.nv-y.nv-axis').first().remove()
						//});

					timelineWidget.state.api.update();
					
					window.addEventListener("resize", function(){
						rtime = new Date();
						if (timeout === false && typeof resizeend !== 'undefined') {
							timeout = true;
							setTimeout(resizeend, delta);
						}
					});
					
					//if(!outerScope.listenerSetup){
						if(outerScope.timelineReady){
							console.log('timeline data was loaded before chart was configured: listener setup by callback');
							outerScope.setupListener();
						}
					//}
					
					//console.log('callback complete')
				}
			}
	};
	

	return timelineWidget;
}

function WikimediaDemo() {

	var widgetsArray = [];
	var wikimediaTransformFunction = function (response, args) {
		var ret = [];
		var items = response.data.items;
		for(var i=0; i < items.length; i++){
			ret.push({x: items[i].timestamp, y: items[i].views, z: 'views'});
		}
		return ret;
	};

	var wikimediaBaseQuery = new SimpleQuery(
			"Raw", new Service(
					"", "Get","",
					new DefaultPreproc(),
					new Postproc("", wikimediaTransformFunction.toString(), [], {}, "")
			)
	);
	var wikimediaQueryTemplate = new TemplatedQuery(
			"Plain",
			wikimediaBaseQuery,
			new DefaultPaging(),
			new Controls(new Template("","https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/all-agents/Foo/daily/__dayFrom__/__dayTo__",
					[new Placeholder("__dayFrom__", "20151010", false), new Placeholder("__dayTo__", "20151030", false)]))
	);

	var xAxisFn = function(d) {
		var str = d.toString();
		var year = str.substring(0,4);
		var month = str.substring(4, 6);
		var day = str.substring(6, 8);
		return year + '-' + month + '-' + day;
	};

	var options = new EffectiveChartOptions('lineChart', xAxisFn.toString());
	options.showLegend = true;

	var widget = new Widget(getUniqueId(), new WidgetState('col-md-12', false, true), new DashletState(" Daily wikimedia stats", false, 0, {}, options, new Config('Fire','Off', false, false, ''), wikimediaQueryTemplate, new DefaultGuiClosed(), new DefaultInfo()));

	widgetsArray.push(widget);

	var dashboardObject = new Dashboard(
			'Daily Stats',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__businessobjectid__", "", false)],
							false,
							false,
							'Global Settings',
							3000
					),
					widgetsArray,
					'Wikimedia Dashboard',
					'aggregated',
					new DefaultDashboardGui()
			)
	);

	return dashboardObject;
}


function RealtimePerformanceDashboard(executionId, measurementType, entity, autorefresh) {

	var widgetsArray = [];
	var entityName = 'Measurement';

	var timeFrame = 30000;

	var timeField = "begin";
	var timeFormat = "long";
	var valueField = "value";
	var groupby = "name";

	var textFilters = "[{ \"key\": \"eId\", \"value\": \"__businessobjectid__\", \"regex\": \"false\" }, { \"key\": \"type\", \"value\": \"__measurementType__\", \"regex\": \"false\" }]";
	var numericalFilters = "[{ \"key\": \"begin\", \"minValue\": \"__from__\", \"maxValue\": \"__to__\" }]";

	addAggregatesOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame);
	addErrorsOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame);

	//addErrorsSummary(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame);
	addAggregatesSummaryTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame);
	addLastMeasurementsTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame);

	// currently applying an offset of 10% to take in account the query execution time and make sure to visually "frame" the data
	// this a temporary workaround to the fact that new Date() is called at different times for the scale & query input
	var offset = Math.round(timeFrame * 0.08);
	var adjustedFrom = (timeFrame * 1) + offset;
	
	var effectiveFrom = 'new Date().getTime() - '+ timeFrame;
	var effectiveTo = 'new Date().getTime()';

	var dashboardObject = new Dashboard(
			entityName + ' Performance',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__businessobjectid__", executionId, false), new Placeholder("__measurementType__", measurementType?measurementType:'custom', false),
								new Placeholder("__from__", effectiveFrom, true),
								new Placeholder("__to__", effectiveTo, true)
							],
							autorefresh?autorefresh:true,
									false,
									'Global Settings',
									3000
					),
					widgetsArray,
					entityName + ' Dashboard',
					'aggregated',
					new DefaultDashboardGui()
			)
	);

	dashboardObject.oid = "realTimePerfDashboardId";
	return dashboardObject;
};

function PerformanceDashboard(executionId, measurementType, entity, from, to) {

	var widgetsArray = [];
	var entityName = 'Measurement';

	var __from__;
	var __to__;

	if(!from){
		__from__ = 0;
	}else{
		__from__ = from;
	}

	if(!to){
		__to__ = 4078218676000;
	}else{
		__to__ = to;
	}

	var timeFrame = null;

	var timeField = "begin";
	var timeFormat = "long";
	var valueField = "value";
	var groupby = "name";

	var textFilters = "[{ \"key\": \"eId\", \"value\": \"__businessobjectid__\", \"regex\": \"false\" }, { \"key\": \"type\", \"value\": \"__measurementType__\", \"regex\": \"false\" }]";
	var numericalFilters = "[{ \"key\": \"begin\", \"minValue\": \"__from__\", \"maxValue\": \"__to__\" }]";

	addAggregatesOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__);
	addErrorsOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__);


	//addErrorsSummary(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__);
	addAggregatesSummaryTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__);
	//addLastMeasurementsTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__);


	var dashboardObject = new Dashboard(
			entityName + ' Performance',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__businessobjectid__", executionId, false), new Placeholder("__measurementType__", measurementType?measurementType:'custom', false)],
							false,
							false,
							'Global Settings',
							3000
					),
					widgetsArray,
					entityName + ' Dashboard',
					'aggregated',
					new DefaultDashboardGui()
			)
	);

	dashboardObject.oid = "perfDashboardId";
	return dashboardObject;
};

function EffectiveChartOptions(charType, xAxisOverride, timeFrame, yAxisOverride){
	
	// currently applying an offset of 10% to take in account the query execution time and make sure to visually "frame" the data
	// this a temporary workaround to the fact that new Date() is called at different times for the scale & query input
	var offset = Math.round(timeFrame * 0.08);
	var adjustedFrom = (timeFrame * 1) + offset;
	
	var axisFrom = 'new Date().getTime() - '+ adjustedFrom;
	var axisTo = 'new Date().getTime() - '+ offset;
	
	var opts = new ChartOptions(charType, true, false,
			xAxisOverride?xAxisOverride:'function (d) {\r\n    var value;\r\n    if ((typeof d) === \"string\") {\r\n        value = parseInt(d);\r\n    } else {\r\n        value = d;\r\n    }\r\n\r\n    return $scope.toDynamicDateFormat(value);\r\n}',
					yAxisOverride?yAxisOverride:'function (d) { return d.toFixed(1); }',
							timeFrame?'['+axisFrom+','+axisTo+']':undefined
	);
	opts.margin.left = 75;
	return opts;
}

function RTMAggBaseQueryTmpl(metric, transform){
	return new AsyncQuery(
			null,
			new Service(//service
					"/rtm/rest/aggregate/get", "Post",
					"",//templated
					new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].key, workData[i].value);}return newRequestFragment;}"),
					new Postproc("", "",[], "function(response){if(!response.data.payload){console.log('No payload ->' + JSON.stringify(response)); return null;}return [{ placeholder : '__streamedSessionId__', value : response.data.payload.streamedSessionId, isDynamic : false }];}", "")
			),
			new Service(//callback
					"/rtm/rest/aggregate/refresh", "Post",
					"{\"streamedSessionId\": \"__streamedSessionId__\"}",
					new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].placeholder, workData[i].value);}return newRequestFragment;}"),
					new Postproc("function(response){return response.data.payload.stream.complete;}", transform ,[{"key" : "metric", "value" : metric, "isDynamic" : false}], {}, ""))
	);
};

function RTMAggBaseTemplatedQueryTmpl(metric, pGranularity, transform, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	return new TemplatedQuery(
			"Plain",
			new RTMAggBaseQueryTmpl(metric, transform),
			new DefaultPaging(),
			new Controls(
					new Template(
							"{ \"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }], \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"__granularity__\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"1\", \"aggregateService.timeout\": \"600\" } }",
							"",
							[new Placeholder("__granularity__", pGranularity, false),
								new Placeholder("__from__", __from__, true),
								new Placeholder("__to__", __to__, true)]
					)
			)
	);
};

function RTMAggTimeFrameTemplatedQueryTmpl(metric, pGranularity, transform, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	return new TemplatedQuery(
			"Plain",
			new RTMAggBaseQueryTmpl(metric, transform),
			new DefaultPaging(),
			new Controls(
					new Template(
							"{ \"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }], \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"__granularity__\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"1\", \"aggregateService.timeout\": \"600\" } }",
							"",
							[new Placeholder("__granularity__", pGranularity, false),
								new Placeholder("__from__", "new Date().getTime() - "+timeFrame, true),
								new Placeholder("__to__", "new Date().getTime()", true)]
					)
			)
	);
};

var addAggregatesSummaryTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	var summaryTransform = "function (response) {\r\n    //var metrics = response.data.payload.metricList;\r\n    var metrics = [\"cnt\",\"avg\", \"stddev\",\"min\", \"max\", \"tpm\", \"tps\", \"90th pcl\", \"pcl precision\"];\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    if (payload && payloadKeys.length > 0) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[0]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            for (i = 0; i < metrics.length; i++) {\r\n                var metric = metrics[i];\r\n                if (payload[payloadKeys[0]][serieskeys[j]][metric]) {\r\n                    retData.push({\r\n                        x: metric,\r\n                        y: Math.round(payload[payloadKeys[0]][serieskeys[j]][metric]),\r\n                        z: serieskeys[j]\r\n                    });\r\n                }else{\r\n                    retData.push({ x: metric, y: 0, z: serieskeys[j]});\r\n               }\r\n            }\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new WidgetState('col-md-6', false, true), new DashletState(entityName + " stats summary", false, 0, {}, new EffectiveChartOptions('seriesTable', 'function(d) { return d; }', timeFrame), new Config('Fire','Off', false, false, '', 3000, 1000, 'Off', 8, 'On'), new RTMAggBaseTemplatedQueryTmpl("sum", "max", summaryTransform, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};

var addAggregatesOverTimeTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	var overtimeTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: payload[payloadKeys[i]][serieskeys[j]][metric],\r\n                z: serieskeys[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var config = getMasterSlaveConfig("raw", "Average "+entityName+" Duration (ms)", "Nb " + entityName + "s per second");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('lineChart', null, timeFrame, __from__, __to__), config.masterconfig, new RTMAggBaseTemplatedQueryTmpl("avg", "auto", overtimeTransform,  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));
	//var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('lineChart'), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", overtimeTransform,  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('stackedAreaChart', null, timeFrame, __from__, __to__), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("tps", "auto", overtimeFillBlanksTransformFn.toString(),  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

//No paging: FACTOR 100 via template
var addLastMeasurementsTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	function RTMLatestMeasurementBaseQueryTmpl(){
		return new SimpleQuery(
				"Raw", new Service(
						"/rtm/rest/measurement/latest", "Post",
						"",
						new Preproc("function(requestFragment, workData){var newRequestFragment = requestFragment;for(i=0;i<workData.length;i++){newRequestFragment = newRequestFragment.replace(workData[i].key, workData[i].value);}return newRequestFragment;}"),
						new Postproc("", "function (response, args) {\r\n    var x = '"+timeField+"', y = '"+valueField+"', z = '"+groupby+"';\r\n    var retData = [], index = {};\r\n    var payload = response.data.payload;\r\n    for (var i = 0; i < payload.length; i++) {\r\n        retData.push({\r\n            x: payload[i][x],\r\n            y: payload[i][y],\r\n            z: payload[i][z]\r\n        });\r\n    }\r\n    return retData;\r\n}",
								[], {}, "")
				)
		);
	};

	function RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
		return new TemplatedQuery(
				"Plain",
				new RTMLatestMeasurementBaseQueryTmpl(),
				new DefaultPaging(),
				//new Paging("On", new Offset("__FACTOR__", "return 0;", "return value + 1;", "if(value > 0){return value - 1;} else{return 0;}"), null),
				new Controls(
						new Template(
								"{ \"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }], \"serviceParams\": { \"measurementService.nextFactor\": \"__FACTOR__\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"1\", \"aggregateService.timeout\": \"600\" } }",
								"",
								[new Placeholder("__FACTOR__", "100", false)]
						)
				)
		);
	};

	var config = getMasterSlaveConfig("transformed", "Last 100 " + entityName +"s - Individual duration (ms)", "Last 100 "+entityName+"s - Value table (ms)");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('scatterChart', null, null), config.masterconfig, new RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()) );
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('seriesTable', null, null), config.slaveconfig, new RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()) );

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

//No paging: hardcoded simple query
var addLastMeasurements = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){

	function RTMLatestMeasurementBaseQuery(){
		return new SimpleQuery(
				"Raw", new Service(
						"/rtm/rest/measurement/latest", "Post",
						"{\"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }],\"serviceParams\": { \"measurementService.nextFactor\": \"100\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"1\", \"aggregateService.timeout\": \"600\" }\}",
						new Preproc(""), new Postproc("", "function (response, args) {\r\n    var x = '"+timeField+"', y = '"+valueField+"', z = '"+groupby+"';\r\n    var retData = [], index = {};\r\n    var payload = response.data.payload;\r\n    for (var i = 0; i < payload.length; i++) {\r\n        retData.push({\r\n            x: payload[i][x],\r\n            y: payload[i][y],\r\n            z: payload[i][z]\r\n        });\r\n    }\r\n    return retData;\r\n}",
								[], {}, "")
				)
		);
	};

	var config = getMasterSlaveConfig("raw", "Last 100 Measurements - Scattered values (ms)", "Last 100 Measurements - Value table (ms)");
	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('scatterChart', null, timeFrame, __from__, __to__), config.masterconfig, new RTMLatestMeasurementBaseQuery(), new DefaultGuiClosed(), new DefaultInfo()) );
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('seriesTable', null, timeFrame, __from__, __to__), config.slaveconfig, new RTMLatestMeasurementBaseQuery(), new DefaultGuiClosed(), new DefaultInfo()) );

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

var addErrorsSummary = function(widgetsArray, entityName, timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	var summaryTransform = "function (response) {\r\n    //var metrics = response.data.payload.metricList;\r\n    var metrics = [\"cnt\"];\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    if (payload && payloadKeys.length > 0) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[0]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            for (i = 0; i < metrics.length; i++) {\r\n                var metric = metrics[i];\r\n                if (payload[payloadKeys[0]][serieskeys[j]][metric]) {\r\n                    retData.push({\r\n                        x: metric,\r\n                        y: Math.round(payload[payloadKeys[0]][serieskeys[j]][metric]),\r\n                        z: '\u0020' + serieskeys[j].split('_')[0]\r\n                    });\r\n                }else{\r\n                    retData.push({ x: metric, y: 0, z: serieskeys[j]});\r\n               }\r\n                        }\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new DefaultWidgetState(), new DashletState(entityName + " status summary", false, 0, {}, new EffectiveChartOptions('singleValueTable', 'function(d) { return d; }', timeFrame, __from__, __to__), new Config('Fire','Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("cnt", "max", summaryTransform, entityName,timeField, timeFormat, valueField, "rnStatus", textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};

var addErrorsOverTimeTpl = function(widgetsArray, entityName, timeField, timeFormat, valueField, groupby, textFilters, numericalFilters, timeFrame, __from__, __to__){
	var summaryTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = [];\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            if(!serieskeys[j].includes(';PASSED') && !series.includes(serieskeys[j])){\r\n                series.push(serieskeys[j]);\r\n            }\r\n        }\r\n    }\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        for (j = 0; j < series.length; j++) {\r\n            var yval;\r\n            if(payload[payloadKeys[i]][series[j]] && payload[payloadKeys[i]][series[j]][metric]){\r\n              yval = payload[payloadKeys[i]][series[j]][metric];\r\n            }else{\r\n              //console.log('missing dot: x=' + payloadKeys[i] + '; series=' + series[j]);\r\n              yval = 0;\r\n            }\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: yval,\r\n                z: series[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new DefaultWidgetState(), new DashletState(entityName + " errors over time", false, 0, {}, new EffectiveChartOptions('stackedAreaChart', null, timeFrame, __from__, __to__), new Config('Fire','Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", summaryTransform, entityName,timeField, timeFormat, valueField, "name;rnStatus", textFilters, numericalFilters, timeFrame, __from__, __to__), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};


var getMasterSlaveConfig = function(rawOrTransformed, masterTitle, slaveTitle){
	var masterId, slaveId, masterTitle, slaveTitle, masterConfig, slaveConfig, datatype;

	if(rawOrTransformed === 'raw'){
		datatype = 'state.data.rawresponse';
	}else{
		datatype = 'state.data.transformed';
	}

	var random = getUniqueId();
	masterId = random + "-master";
	slaveId = random + "-slave";

	masterConfig = new Config('Fire','Off', true, false, 'unnecessaryAsMaster');
	slaveConfig = new Config('Fire','Off', false, true, datatype);
	slaveConfig.currentmaster = {
			oid: masterId,
			title: masterTitle
	};

	return {masterid: masterId, slaveid: slaveId, mastertitle: masterTitle, slavetitle: slaveTitle, masterconfig : masterConfig, slaveconfig: slaveConfig};
};

function StaticPresets() {
	return {
		queries: [],
		controls: {
			templates: []
		},
		configs: []
	};
}
