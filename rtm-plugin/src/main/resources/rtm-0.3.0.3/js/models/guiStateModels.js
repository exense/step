/*
 * Contains the attributes necessary to represent a GUI state (other than those already stored in a service input)
 */
function GuiState(){

  this.guiParams = {};
  
}

var guiStateFunctions = {
		setGuiParam : function(key, value){this.guiParams[key] = value;},
		setGuiParams : function(params){this.guiParams = params;},
		getGuiParam : function(key){ return this.guiParams[key];}
};

jQuery.extend(GuiState.prototype, guiStateFunctions);