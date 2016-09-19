function AggregateDashlet(){
	this.collection = new Aggregates();
	this.view = new AggregateChartView({collection : this.collection});
}

$.extend(AggregateDashlet.prototype, Backbone.Events, {

	refreshView : function(){this.view.render();},
	init : function(){this.listenTo( this.collection, 'AggregatesRefreshed', this.refreshView);}
});

function startAggregateDashlet(){

	var testInput = JSON.parse('{"selectors":[{"textFilters":[{"key":"eId","value":"54abf338afb260e75c0eea8b","regex":"false"}],"numericalFilters":[]}],"serviceParams":{"measurementService.nextFactor":0,"aggregateService.sessionId":"defaultSid","aggregateService.granularity":"900000","aggregateService.groupby":"name"}}');
	
	var aggDashlet = new AggregateDashlet();
	aggDashlet.init();

	aggDashlet.collection.refreshData(testInput);

}