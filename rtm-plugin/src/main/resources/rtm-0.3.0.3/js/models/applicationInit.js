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
function initApplication() {
	var mediator = new TopLevelManager();
	mediator.start();

	var router = new Router();
	
	mediator.setRouterReference(router);
	
	
	router.on('route:Measurement', function() {
		mediator.setActiveContext('Measurement');
		mediator.cleanupViews();
		mediator.renderDefaultViews();
		mediator.renderMeasurementViews();
	});

	router.on('route:Aggregate', function() {
		mediator.setActiveContext('Aggregate');
		mediator.cleanupViews();
		mediator.renderDefaultViews();
		mediator.renderAggregateViews();
	});

	router.on('route:Measurement/selected', function(guiState,
			id) {
		mediator.setActiveContext('Measurement');

		var guiStateText = decodeURIComponent(guiState);
		var guiStateObj = JSON.parse(guiStateText);

		mediator.loadGuiState(guiStateObj);

		mediator.cleanupViews();
		mediator.renderDefaultViews();
		mediator.renderControllerForMeasurement();
		mediator.refreshMeasurementModel();
		//mediator.renderMeasurementViews(); // <-- refreshes Automatically due to binding ---- not anymore -- yes now due to explicit event in fetch success()
	});

	router
	.on(
			'route:Aggregate/selected',
			function(guiState, id) {
				mediator.setActiveContext('Aggregate');

				var guiStateText = decodeURIComponent(guiState);
				var guiStateObj = JSON
				.parse(guiStateText);

				mediator.loadGuiState(guiStateObj);
				mediator.cleanupViews();
				mediator.renderDefaultViews();
				mediator.renderControllerForAggregate();
				mediator.refreshAggregateModel();

				//mediator.renderAggregateViews(); // <-- refreshes Automatically due to binding ---- not anymore -- yes now due to explicit event in fetch success()

				// the model refresh operation runs asynchronously
				// 1-fire event when model is refreshed, have mediator listen to that event <-- is currently implemented
				// or 2-go back to listening to the collection change event and find out why 'add' was needed, when change is what's really supposed to work (1 call to render instead of n)
			});

	Backbone.history.start();
}