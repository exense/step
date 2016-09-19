var AggregateChartView = Backbone.View.extend({
	el: '.ChartView',
	events : {
		"click .metricChoice": "updateChartMetricChoice",
	},

	currentChartMetricChoice: '',
	chartBeginKey : '',
	chartGroupbyKey : '',
	chartMaxSeries : 0,
	chartMaxDotsPerSeries : 0,

	initialize : function(){
		  this.currentChartMetricChoice = Config.getProperty('client.AggregateChartView.currentChartMetricChoice');
		  this.chartBeginKey = Config.getProperty('client.AggregateChartView.chartBeginKey');
		  this.chartGroupbyKey = Config.getProperty('client.AggregateChartView.chartGroupbyKey');
		  this.chartMaxSeries = Config.getProperty('client.AggregateChartView.chartMaxSeries');
		  this.chartMaxDotsPerSeries = Config.getProperty('client.AggregateChartView.chartMaxDotsPerSeries');
	},  
	getGuiFragment :function(){
		return {
			'chartMetricChoice' : this.getCurrentChartMetricChoice()
		};
	},

	getServiceFragment :function(){
		return {};
	},

	getGuiDomain: function(){
		return 'aggregateGraphView';
	},

	getServiceDomain: function(){
		return 'aggregateService';
	},

	getCurrentChartMetricChoice: function () {
		return this.currentChartMetricChoice;
	},
	loadGuiState: function (guiParams) {
		this.currentChartMetricChoice = guiParams.chartMetricChoice;
	},
	updateChartMetricChoice: function (e) {
		this.currentChartMetricChoice = e.currentTarget.id;
		this.render();
		e.preventDefault();
	},
	render: function () {

		var that = this;

		this.$el.html('');

		this.renderChart();
	},

	renderChart: function () {
		var that = this;
		$.get(resolveTemplate('aggregateChart-template'), function (data) {
			template = _.template(data, {metricsList : that.getChartableMetricsList(), currentChartMetricChoice : that.currentChartMetricChoice});
			that.$el.append(template);
		}, 'html')
		.fail(function(model, response, options ) {
			displayError('response=' + JSON.stringify(response));
		})
		.success(function(){
			if(that.collection.models.length > 0){
				google.load('visualization', '1.0',  {'callback': function(){that.drawChart(that.collection.models[0].get('payload'),
						{
					metric : that.currentChartMetricChoice,
					chartBeginKey : that.chartBeginKey,
					chartGroupbyKey : that.chartGroupbyKey,
					chartMaxSeries : that.chartMaxSeries,
					chartMaxDotsPerSeries : that.chartMaxDotsPerSeries
						}
				);}, 'packages':['corechart', 'table']});
			}
		});
	},

	getChartableMetricsList: function(){
		var metricsList = [];
		var firstModel = this.collection.models[0];
		var excludes = this.getExcludeList();
		if(firstModel){
			for ( var key in firstModel.attributes.payload[0].data[0].n){
				if($.inArray(key, excludes) < 0){
					metricsList.push(key);
				}
			}
		}
		return metricsList;
	},
	getMetricsList: function(){
		var metricsList = [];
		var firstModel = this.collection.models[0];
		var excludes = this.getExcludeList();
		if(firstModel){
			for ( var key in firstModel.attributes.payload[0].data[0].n){
				metricsList.push(key);
			}
		}
		return metricsList;
	},
	getExcludeList: function(){ // CONFIGURATIVE
		return this.excludeList;
	},
	drawChart: function(pAggregates, pChartParams){
		var payloadLength = pAggregates.length;
		var chartData;
		var isChartMaxSeriesReached = false;
		var isChartMaxDotsPerSeriesReached = false;
		if(payloadLength > 0) {  

			// calculate number of aggregates in the series (must match)
			var dataLength = pAggregates[0].data.length;
			var headers = [];
			var index = 0;
			_.each(pAggregates, function(aggregate) {
				// pick up groupby values as column headers
				if(headers.length < pChartParams.chartMaxSeries){
					headers.push(aggregate[pChartParams.chartGroupbyKey]);
				}else{
					isChartMaxSeriesReached = true;
				}
				var curDataLength = aggregate.data.length;
				if(curDataLength !== dataLength) {
					displayError("Inconsistent data, cannot be charted.");
				}
				index = index + 1;
			});

			chartData = new google.visualization.DataTable();
			chartData.addColumn('datetime', 'time');
			var hLength = headers.length;
			for (var i = 0; i < hLength; i++) {
				chartData.addColumn('number', headers[i]);
			}

			var dataArray = [];
			var rowArray = [];
			var metric = pChartParams.metric;
			for (var i = 0; i < dataLength; i++) {

				if(dataArray.length >= pChartParams.chartMaxDotsPerSeries){
					isChartMaxDotsPerSeriesReached = true;
					break;
				}
				rowArray.push(new Date(new Number(pAggregates[0].data[i].n[pChartParams.chartBeginKey])));
				var limit = 0;
				_.each(pAggregates, function(aggregate) {
					if(limit < pChartParams.chartMaxSeries){
						rowArray.push(aggregate.data[i].n[metric]);
						limit = limit + 1;
					}else{
						isChartMaxSeriesReached = true;
					}
				});
				dataArray.push(rowArray);
				rowArray = [];
			}
			chartData.addRows(dataArray);
		}
		if(isChartMaxSeriesReached)
			displayWarning('The maximum number of displayable chart series in the Chart view (' +this.chartMaxSeries+ ') has been reached, but the Table view is not affected.');
		if(  isChartMaxDotsPerSeriesReached)
			displayWarning('The maximum number of dots per series in the Chart view (' +this.chartMaxDotsPerSeries+ ') has been reached, but the Table view is not affected.');

		var myoptions = {
				'curveType': 'function', 'legend': { position: 'bottom' }, 'vAxis': { viewWindow: { min: 0 } },'pointSize': 5, 'height':500,
				chartArea:{left:60,top:30,width:"90%",height:"85%"}
		};
		//var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
		var chart = new google.visualization.LineChart(this.$('#gviz').get(0));
		chart.draw(chartData, myoptions);
	},
	clearAll: function () {
		this.collection.reset();
	}
});
