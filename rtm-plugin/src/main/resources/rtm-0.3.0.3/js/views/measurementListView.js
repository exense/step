/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
var MeasurementListView = Backbone.View.extend({
	el: '.TableView',
	events : {
		"click .mlmCheckbox": "updateTableMetricChoice",
		"click #previous" : "triggerMeasurementPrevious",
		"click #next" : "triggerMeasurementNext"
	},
	checkedTableMetrics: [],
	dateMetrics : [],
	excludeList : [],
	pagingValue : '',
	
	nextFactor : 0,

	initialize : function(){
		
		var splitter = Config.getProperty('client.splitChar');

		this.checkedTableMetrics =  Config.getProperty('client.MeasurementListView.checkedTableMetrics').split(splitter);
		this.dateMetrics =  Config.getProperty('client.MeasurementListView.dateMetrics').split(splitter);
		this.excludeList =  Config.getProperty('client.MeasurementListView.excludeList').split(splitter);
		this.pagingValue =  Config.getProperty('client.MeasurementListView.pagingValue');
		
	},
	getServiceFragment :function(){
		return {
				'nextFactor' : this.getNextFactor()
			};
	},

	getGuiFragment :function(){
		return {
				'nextFactor' : this.getNextFactor(),
				'tableMetricChoice' : this.getCurrentMetricChoices(),
			};
	},
	
	getGuiDomain: function(){
		return 'measurementListView';
	},

	getServiceDomain: function(){
		return 'measurementService';
	},
	
	triggerMeasurementPrevious: function(){
		this.nextFactor = this.nextFactor - 1;
		this.trigger('MeasurementPrevious');
		event.preventDefault();
	},
	triggerMeasurementNext: function(){
		this.nextFactor = this.nextFactor + 1;
		this.trigger('MeasurementNext');
		event.preventDefault();
	},
	getCurrentMetricChoices: function () {
		return this.checkedTableMetrics;
	},
	getExcludeList: function(){
		return this.excludeList;
	},
	toSkipValue: function(){
		return this.nextFactor * this.pagingValue;
	},
	getNextFactor : function(){
		return this.nextFactor;
	},
	resetPager: function(){
		this.nextFactor = 0;
	},
	render: function () {
		var that = this;
		
		$.get(resolveTemplate('measurement-list-template'), function (data) {
			template = _.template(data, 
					{
				measurements: that.collection.models,
				metricsList : that.getMetricsList(), 
				checkedTableMetrics : that.checkedTableMetrics,
				dateMetric : that.dateMetrics,
				pagingValue : that.pagingValue,
				nextFactor : that.nextFactor,
					});

			that.$el.html(template);
		}, 'html')
		.fail(function(model, response, options ) {
			displayError('response=' + JSON.stringify(response));
		});
	},
	getMetricsList: function(){
		var metricsList = [];
		var firstModel = this.collection.models[0];
		var excludes = this.getExcludeList();
		if(firstModel){
			for ( var key in firstModel.attributes.t){
				metricsList.push(key);
			}
			for ( var key in firstModel.attributes.n){
				metricsList.push(key);
			}
		}
		return metricsList;
	},
	clearAll: function () {
		this.collection.reset();
	},
	loadGuiState: function (guiParams) {
		this.checkedTableMetrics = guiParams.tableMetricChoice;
		this.nextFactor = guiParams.nextFactor;
	},
	updateTableMetricChoice: function (e) {
		var choice = e.currentTarget.id;
		var index = $.inArray(choice, this.checkedTableMetrics);
		if(index < 0){
			this.checkedTableMetrics.push(choice);
		}else{
			this.checkedTableMetrics.splice(index,1);
		}

		this.render();
		e.preventDefault();
	}
});