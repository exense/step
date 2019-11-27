function RealtimePerformanceDashboard(executionId, measurementType, entity) {

	var widgetsArray = [];
	var entityName = '';
	if(measurementType && measurementType === 'keyword'){
		entityName = 'Keyword';
	}else{
		entityName = 'Custom Transaction';
	}
	
	var timeFrame = 10000;
	
	var timeField = "begin";
	var timeFormat = "long";
	var valueField = "value";
	var groupby = "name";
	
	var textFilters = "[{ \"key\": \"eId\", \"value\": \"__eId__\", \"regex\": \"false\" }]";
	var numericalFilters = "[{ \"key\": \"begin\", \"minValue\": \"__from__\", \"maxValue\": \"__to__\" }]";

	addAggregatesOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addErrorsOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addLastMeasurementsTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	//addErrorsSummary(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addAggregatesSummaryTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);


	var dashboardObject = new Dashboard(
			entityName + ' Performance',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__eId__", executionId, false), new Placeholder("__measurementType__", measurementType, false),
							new Placeholder("__from__", "new Date(new Date().getTime() - "+timeFrame+").getTime()", true),
							new Placeholder("__to__", "new Date().getTime()", true)
							],
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

function PerformanceDashboard(executionId, measurementType, entity) {

	var widgetsArray = [];
	var entityName = '';
	if(measurementType && measurementType === 'keyword'){
		entityName = 'Keyword';
	}else{
		entityName = 'Custom Transaction';
	}
	
	var timeField = "begin";
	var timeFormat = "long";
	var valueField = "value";
	var groupby = "name";
	
	var textFilters = "[{ \"key\": \"eId\", \"value\": \"__eId__\", \"regex\": \"false\" }]";
	var numericalFilters = "[]";

	addAggregatesOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addErrorsOverTimeTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addLastMeasurementsTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	//addErrorsSummary(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);
	addAggregatesSummaryTpl(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters);


	var dashboardObject = new Dashboard(
			entityName + ' Performance',
			new DashboardState(
					new GlobalSettings(
							[new Placeholder("__eId__", executionId, false), new Placeholder("__measurementType__", measurementType, false)],
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

function EffectiveChartOptions(charType, xAxisOverride){
	var opts = new ChartOptions(charType, false, false,
			xAxisOverride?xAxisOverride:'function (d) {\r\n    var value;\r\n    if ((typeof d) === \"string\") {\r\n        value = parseInt(d);\r\n    } else {\r\n        value = d;\r\n    }\r\n\r\n    return d3.time.format(\"%H:%M:%S\")(new Date(value));\r\n}', 
			'function (d) { return d.toFixed(0); }'
			//,'[new Date(new Date().getTime() - 300000).getTime(), new Date().getTime()]'
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

function RTMAggBaseTemplatedQueryTmpl(metric, pGranularity, transform, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
	return new TemplatedQuery(
			"Template",
			new RTMAggBaseQueryTmpl(metric, transform),
			new DefaultPaging(),
			new Controls(
					new Template(
							"{ \"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }], \"serviceParams\": { \"measurementService.nextFactor\": \"0\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"__granularity__\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" } }",
							"",
							[new Placeholder("__granularity__", pGranularity, false)]
					)
			)
	);
};

var addAggregatesSummaryTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
	var summaryTransform = "function (response) {\r\n    //var metrics = response.data.payload.metricList;\r\n    var metrics = [\"cnt\",\"avg\", \"min\", \"max\", \"tpm\", \"tps\", \"90th pcl\"];\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    if (payload && payloadKeys.length > 0) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[0]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            for (i = 0; i < metrics.length; i++) {\r\n                var metric = metrics[i];\r\n                if (payload[payloadKeys[0]][serieskeys[j]][metric]) {\r\n                    retData.push({\r\n                        x: metric,\r\n                        y: Math.round(payload[payloadKeys[0]][serieskeys[j]][metric]),\r\n                        z: serieskeys[j]\r\n                    });\r\n                }else{\r\n                    retData.push({ x: metric, y: 0, z: serieskeys[j]});\r\n               }\r\n            }\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new WidgetState('col-md-12', false, true), new DashletState(entityName + " stats summary", false, 0, {}, new EffectiveChartOptions('seriesTable', 'function (d) { return d;}'), new Config('Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("sum", "max", summaryTransform, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};

var addAggregatesOverTimeTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
	var overtimeTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: payload[payloadKeys[i]][serieskeys[j]][metric],\r\n                z: serieskeys[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var overtimeFillBlanksTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = [];\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            if(!series.includes(serieskeys[j])){\r\n                series.push(serieskeys[j]);\r\n            }\r\n        }\r\n    }\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < series.length; j++) {\r\n            var yval;\r\n            if(payload[payloadKeys[i]][serieskeys[j]] && payload[payloadKeys[i]][serieskeys[j]][metric]){\r\n              yval = payload[payloadKeys[i]][serieskeys[j]][metric];\r\n            }else{\r\n              //console.log('missing dot: x=' + payloadKeys[i] + '; series=' + series[j]);\r\n              yval = 0;\r\n            }\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: yval,\r\n                z: series[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var config = getMasterSlaveConfig("raw", "Average "+entityName+" Duration (ms)", "Nb " + entityName + "s per second");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('lineChart'), config.masterconfig, new RTMAggBaseTemplatedQueryTmpl("avg", "auto", overtimeTransform,  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));
	//var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('lineChart'), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", overtimeTransform,  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('stackedAreaChart', false, true), config.slaveconfig, new RTMAggBaseTemplatedQueryTmpl("tps", "auto", overtimeFillBlanksTransform,  entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

//No paging: FACTOR 100 via template
var addLastMeasurementsTpl = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
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

	function RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
		return new TemplatedQuery(
				"Template",
				new RTMLatestMeasurementBaseQueryTmpl(),
				new DefaultPaging(),
				//new Paging("On", new Offset("__FACTOR__", "return 0;", "return value + 1;", "if(value > 0){return value - 1;} else{return 0;}"), null),
				new Controls(
						new Template(
								"{ \"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }], \"serviceParams\": { \"measurementService.nextFactor\": \"__FACTOR__\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" } }",
								"",
								[new Placeholder("__FACTOR__", "100", false)]
						)
				)
		);
	};

	var config = getMasterSlaveConfig("transformed", "Last 100 " + entityName +"s - Individual duration (ms)", "Last 100 "+entityName+"s - Value table (ms)");

	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('scatterChart'), config.masterconfig, new RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()) );
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('seriesTable'), config.slaveconfig, new RTMLatestMeasurementTemplatedQuery(entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()) );

	widgetsArray.push(master);
	//widgetsArray.push(slave);
};

//No paging: hardcoded simple query
var addLastMeasurements = function(widgetsArray, entityName,timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){

	function RTMLatestMeasurementBaseQuery(){
		return new SimpleQuery(
				"Raw", new Service(
						"/rtm/rest/measurement/latest", "Post",
						"{\"selectors1\": [{ \"textFilters\": "+textFilters+", \"numericalFilters\": "+numericalFilters+" }],\"serviceParams\": { \"measurementService.nextFactor\": \"100\", \"aggregateService.timeField\" : \""+timeField+"\", \"aggregateService.timeFormat\" : \""+timeFormat+"\", \"aggregateService.valueField\" : \""+valueField+"\", \"aggregateService.sessionId\": \"defaultSid\", \"aggregateService.granularity\": \"auto\", \"aggregateService.groupby\": \""+groupby+"\", \"aggregateService.cpu\": \"1\", \"aggregateService.partition\": \"8\", \"aggregateService.timeout\": \"600\" }\}",
						new Preproc(""), new Postproc("", "function (response, args) {\r\n    var x = '"+timeField+"', y = '"+valueField+"', z = '"+groupby+"';\r\n    var retData = [], index = {};\r\n    var payload = response.data.payload;\r\n    for (var i = 0; i < payload.length; i++) {\r\n        retData.push({\r\n            x: payload[i][x],\r\n            y: payload[i][y],\r\n            z: payload[i][z]\r\n        });\r\n    }\r\n    return retData;\r\n}",
								[], {}, "")
				)
		);
	};

	var config = getMasterSlaveConfig("raw", "Last 100 Measurements - Scattered values (ms)", "Last 100 Measurements - Value table (ms)");
	var master = new Widget(config.masterid, new DefaultWidgetState(), new DashletState(config.mastertitle, false, 0, {}, new EffectiveChartOptions('scatterChart'), config.masterconfig, new RTMLatestMeasurementBaseQuery(), new DefaultGuiClosed(), new DefaultInfo()) );
	var slave = new Widget(config.slaveid, new DefaultWidgetState(), new DashletState(config.slavetitle, false, 0, {}, new EffectiveChartOptions('seriesTable'), config.slaveconfig, new RTMLatestMeasurementBaseQuery(), new DefaultGuiClosed(), new DefaultInfo()) );

	widgetsArray.push(master);
	widgetsArray.push(slave);
};

var addErrorsSummary = function(widgetsArray, entityName, timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
	var summaryTransform = "function (response) {\r\n    //var metrics = response.data.payload.metricList;\r\n    var metrics = [\"cnt\"];\r\n    var retData = [], series = {};\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    if (payload && payloadKeys.length > 0) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[0]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            for (i = 0; i < metrics.length; i++) {\r\n                var metric = metrics[i];\r\n                if (payload[payloadKeys[0]][serieskeys[j]][metric]) {\r\n                    retData.push({\r\n                        x: metric,\r\n                        y: Math.round(payload[payloadKeys[0]][serieskeys[j]][metric]),\r\n                        z: '\u0020' + serieskeys[j].split('_')[0]\r\n                    });\r\n                }else{\r\n                    retData.push({ x: metric, y: 0, z: serieskeys[j]});\r\n               }\r\n                        }\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new DefaultWidgetState(), new DashletState(entityName + " status summary", false, 0, {}, new EffectiveChartOptions('singleValueTable'), new Config('Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("cnt", "max", summaryTransform, entityName,timeField, timeFormat, valueField, "rnStatus", textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));
	widgetsArray.push(standalone);
};

var addErrorsOverTimeTpl = function(widgetsArray, entityName, timeField, timeFormat, valueField, groupby, textFilters, numericalFilters){
	var summaryTransform = "function (response, args) {\r\n    var metric = args.metric;\r\n    var retData = [], series = [];\r\n\r\n    var payload = response.data.payload.stream.streamData;\r\n    var payloadKeys = Object.keys(payload);\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        var serieskeys = Object.keys(payload[payloadKeys[i]])\r\n        for (j = 0; j < serieskeys.length; j++) {\r\n            if(!serieskeys[j].includes(';PASSED') && !series.includes(serieskeys[j])){\r\n                series.push(serieskeys[j]);\r\n            }\r\n        }\r\n    }\r\n\r\n    for (i = 0; i < payloadKeys.length; i++) {\r\n        for (j = 0; j < series.length; j++) {\r\n            var yval;\r\n            if(payload[payloadKeys[i]][series[j]] && payload[payloadKeys[i]][series[j]][metric]){\r\n              yval = payload[payloadKeys[i]][series[j]][metric];\r\n            }else{\r\n              //console.log('missing dot: x=' + payloadKeys[i] + '; series=' + series[j]);\r\n              yval = 0;\r\n            }\r\n            retData.push({\r\n                x: payloadKeys[i],\r\n                y: yval,\r\n                z: series[j]\r\n            });\r\n        }\r\n    }\r\n    return retData;\r\n}";
	var standalone = new Widget(getUniqueId(), new DefaultWidgetState(), new DashletState(entityName + " errors over time", false, 0, {}, new EffectiveChartOptions('stackedAreaChart'), new Config('Off', false, false, ''), new RTMAggBaseTemplatedQueryTmpl("cnt", "auto", summaryTransform, entityName,timeField, timeFormat, valueField, "name;rnStatus", textFilters, numericalFilters), new DefaultGuiClosed(), new DefaultInfo()));
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

	masterConfig = new Config('Off', true, false, 'unnecessaryAsMaster');
	slaveConfig = new Config('Off', false, true, datatype);
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