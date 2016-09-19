
var TopLevelManager = function () {};

$.extend(TopLevelManager.prototype, Backbone.Events, {

	setRouterReference: function(obj){
		this.router = obj;
	},
	
	start: function () {

		this.activeContext = '';
		this.dynamicViewsManager = [];

		// collections
		this.measurements = new Measurements();
		this.aggregates = new Aggregates();

		// These views are static to the context :
		this.navbarView = new NavBarView();
		this.mainHeaderView = new MainHeaderView();

		// These views are dynamic to the context :
		this.measurementListView = new MeasurementListView({collection : this.measurements});
		//this.dynamicViewsManager.push({view : this.measurementListView, contextRelevancy : ['Measurement']});
		this.aggregateChartView = new AggregateChartView({collection : this.aggregates});
		this.aggregateTableView = new AggregateTableView({collection : this.aggregates});
		//this.dynamicViewsManager.push({view : this.aggregateListView, contextRelevancy : ['Measurement']});

		this.aggSPControllerView = new AggSPControllerView();
		this.postControllerView = new PostControllerView();

		this.listenTo( this.postControllerView, 'globalSearchEvent', this.dispatchTopLevelSearch );
		this.listenTo( this.aggSPControllerView, 'globalSearchEvent', this.dispatchTopLevelSearch );
		this.listenTo( this.measurements, 'MeasurementsRefreshed', this.dispatchMeasurementsRefreshed );
		this.listenTo( this.aggregates, 'AggregatesRefreshed', this.dispatchAggregatesRefreshed );
		this.listenTo( this.measurementListView, 'MeasurementPrevious', this.sendSearch );
		this.listenTo( this.measurementListView, 'MeasurementNext', this.sendSearch );
	},

//	can just reuse sendSearch?
//	dispatchMeasurementPrevious
//	dispatchMeasurementNext

	dispatchMeasurementsRefreshed: function(){
		this.measurementListView.cleanup();
		this.measurementListView.render();
	},

	dispatchAggregatesRefreshed: function(){
		// if Chart turned on
		this.aggregateChartView.cleanup();
		this.aggregateChartView.render();
		// if Table turned on
		this.aggregateTableView.cleanup();
		this.aggregateTableView.render();
	},
	
	serializeInput: function(){
		
		var selPayload = this.postControllerView.getServiceFragment();

		var serviceParams = new ServiceParams();
		
		serviceParams.setFragment(this.measurementListView.getServiceDomain(), this.measurementListView.getServiceFragment()); // toSkipValue()
		serviceParams.setFragment(this.aggSPControllerView.getServiceDomain(), this.aggSPControllerView.getServiceFragment());

		var serviceInput = new ServiceInput();
		serviceInput.setSelectors(selPayload);
		serviceInput.setServiceParams(serviceParams);
		
		return serviceInput;
	},
	
	serializeGui: function(){

		var guiState = new GuiState();
		
		guiState.setGuiParam(this.postControllerView.getGuiDomain(), this.postControllerView.getGuiFragment());
		guiState.setGuiParam(this.measurementListView.getGuiDomain(), this.measurementListView.getGuiFragment());
		guiState.setGuiParam(this.aggSPControllerView.getGuiDomain(), this.aggSPControllerView.getGuiFragment());
		guiState.setGuiParam(this.aggregateChartView.getGuiDomain(), this.aggregateChartView.getGuiFragment());
		guiState.setGuiParam(this.aggregateTableView.getGuiDomain(), this.aggregateTableView.getGuiFragment());
		
		return guiState;
	},

	setActiveContext: function(context){
		this.activeContext = context;
		this.navbarView.setActiveContext(context);
		this.mainHeaderView.setTitle(context);
	},

	getActiveContext: function(){
		return this.activeContext;
	},

	dispatchTopLevelSearch: function(){
		if(!this.postControllerView.hasValidFilters())
			displayError('filters are empty');
		else{
			this.measurementListView.resetPager();
			this.sendSearch();
		}
	},

	sendSearch: function(){

		var guiState = JSON.stringify(this.serializeGui());
		// Compression
		//var compressed = LZString.compressToBase64(string);

		var route = this.activeContext + "/select/"+ encodeURIComponent(guiState) + "/" + Date.now();

		this.router.navigate(route, true);
	},
	renderDefaultViews: function(){
		this.mainHeaderView.render();
		this.navbarView.render();
	},

	renderControllerForMeasurement: function(){
		this.aggSPControllerView.cleanup();
		this.postControllerView.render();
	},

	renderControllerForAggregate: function(){
		this.postControllerView.render();
		this.aggSPControllerView.render();
	},

	cleanupViews: function(){
		$("#errorZone").html('');
		this.measurementListView.cleanup();
		this.postControllerView.cleanup();
		this.aggSPControllerView.cleanup();
		this.aggregateChartView.cleanup();
		this.aggregateTableView.cleanup();
	},

	clearCollections: function(){
		this.measurementListView.clearAll();
		this.aggregateChartView.clearAll();
		this.aggregateTableView.clearAll();
	},

	renderMeasurementViews: function(){
		this.postControllerView.render();
		this.measurementListView.render();
	},

	renderAggregateViews: function(){
		this.postControllerView.render();
		this.aggSPControllerView.render();
		this.aggregateChartView.render();
		this.aggregateTableView.render();
		
	},

	loadGuiState: function(guiState){

//		console.log('-                              TopLevelManager : ---------[guiState]--------');
//		console.log(guiState);
		$.extend(guiState, guiStateFunctions);
		
//		console.log('-                              TopLevelManager : ---------[ guiState.getGuiParam]------------');
//		console.log(guiState.getGuiParam(this.postControllerView.getGuiDomain()));
	
		this.postControllerView.loadGuiState(guiState.getGuiParam(this.postControllerView.getGuiDomain()));
		this.aggSPControllerView.loadGuiState(guiState.getGuiParam(this.aggSPControllerView.getGuiDomain()));
		this.measurementListView.loadGuiState(guiState.getGuiParam(this.measurementListView.getGuiDomain()));
		this.aggregateChartView.loadGuiState(guiState.getGuiParam(this.aggregateChartView.getGuiDomain()));
		this.aggregateTableView.loadGuiState(guiState.getGuiParam(this.aggregateTableView.getGuiDomain()));
	},

	refreshMeasurementModel: function(){
		var serviceInput = this.serializeInput();
		serviceInput.setSelectors(guiToBackendInput(serviceInput.getSelectors()));
		this.measurements.refreshData(serviceInput);
	},

	refreshAggregateModel: function(){
		var serviceInput = this.serializeInput();
		
//		console.log('-                              TopLevelManager : ---------[ serviceInput]------------');
//		console.log(serviceInput);
//		console.log('-                              TopLevelManager : ---------[ guiToBackendInput]------------');
//		console.log(guiToBackendInput(serviceInput));
		serviceInput.setSelectors(guiToBackendInput(serviceInput.getSelectors()));
		
		this.aggregates.refreshData(serviceInput);
	},

	hasControllerData: function(input){
		return this.postControllerView.hasData();
	}

});