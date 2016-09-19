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