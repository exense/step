/*
 * A complex input contains both the attributes necessary to restore a GUI context (or state) and the service input
 * I considered it is okay to couple these elements seeing as the app strategy is to call a service upon navigation to a new route and hence upon generation of a new Gui state.
 * The service input will hence always be a subset of the controller's input, and decoupling them would mean doing redundant work.
 * Therefore, it will remain like that until something challenges that concept.
 * 
 */
function ServiceInput(){

	this.selectors = [];
	this.serviceParams = {};

	this.getSelectors = function(){ return this.selectors;};
	this.getServiceParams = function(){ return this.serviceParams;};

	this.setSelectors = function(selectors){ this.selectors = selectors;};
	this.setServiceParams = function(serviceParams){
		this.serviceParams = serviceParams;
	};
}