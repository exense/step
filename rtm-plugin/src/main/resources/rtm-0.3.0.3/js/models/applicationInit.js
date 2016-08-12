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