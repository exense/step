var AggregateTableView = Backbone.View.extend({
	el: '.TableView',
	events : {
		"click .mlCheckbox": "updateTableMetricChoice",
		"click .displayTable": "switchTable"
	},

	checkedAggTableMetrics: [],
	dateMetrics : [],
	excludeList : [],
	switchedOn: '',

	initialize : function(){
		
		var splitter = Config.getProperty('client.splitChar');
		
		this.checkedAggTableMetrics =  Config.getProperty('client.AggregateTableView.checkedAggTableMetrics').split(splitter);
		this.dateMetrics =  Config.getProperty('client.AggregateTableView.dateMetrics').split(splitter);
		this.excludeList =  Config.getProperty('client.AggregateTableView.excludeList').split(splitter);
		this.switchedOn =  Config.getProperty('client.AggregateTableView.switchedOn');
		
	},
	
	switchTable:function(e){
		if(this.switchedOn === 'false'){
			this.switchedOn = 'true';
		}else{
			this.switchedOn = 'false';
		}
		this.render();
	},
	getGuiFragment :function(){
		return {
			'checkedAggTableMetrics' : this.getCurrentTableMetricChoices(),
			'isSwitchedOn' : this.switchedOn
		};
	},

	getServiceFragment :function(){
		return {};
	},

	getGuiDomain: function(){
		return 'aggregateTableView';
	},

	getServiceDomain: function(){
		return 'aggregateService';
	},

	getCurrentTableMetricChoices: function () {
		return this.checkedAggTableMetrics;
	},
	loadGuiState: function (guiParams) {
		this.checkedAggTableMetrics = guiParams.checkedAggTableMetrics;
		this.switchedOn = guiParams.isSwitchedOn;
	},
	updateTableMetricChoice: function (e) {
		var choice = e.currentTarget.id;
		var index = $.inArray(choice, this.checkedAggTableMetrics);
		if(index < 0){
			this.checkedAggTableMetrics.push(choice);
		}else{
			this.checkedAggTableMetrics.splice(index,1);
		}

		this.render();
		e.preventDefault();
	},
	render: function () {
		var that = this;

		this.$el.html('');

		this.renderTable();
	},

	renderTable: function () {
		var that = this;
		$.get(resolveTemplate('aggregateTable-template'), function (data) {

			template = _.template(data, 
					{
				aggregates: that.collection.models, 
				metricsList : that.getMetricsList(), 
				checkedAggTableMetrics : that.getCurrentTableMetricChoices(),
				dateMetric : that.dateMetrics,
				displayTable : that.switchedOn
					});
			that.$el.append(template);
		}, 'html')
		.fail(function(model, response, options ) {
			displayError('response=' + JSON.stringify(response));
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
	clearAll: function () {
		this.collection.reset();
	}
});
