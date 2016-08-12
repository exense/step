var backendSelectorFunctions = {

  getTextFilters : function(){ return this.textFilters},
  getTextFilter : function(i){ return this.textFilters[i]},

  setTextFilters : function(tf){ this.textFilters=tf;},

  getNumFilters : function(){ return this.numericalFilters},
  getNumFilter : function(i){ return this.numericalFilters[i]},

  setNumFilters : function(nf){ this.numericalFilters=nf;},

  addKVR : function(key, value, regex){
    this.textFilters.push(
    {
      'key' : key,
      'value' : value,
      'regex' : regex,
    }
    );
  },

  getTKey : function(i){ return this.textFilters[i]['key']},
  getTVal : function(i){ return this.textFilters[i]['value']},
  setTKey : function(index, key){this.textFilters[index]['key'] = key;},
  setTVal : function(index, value){this.textFilters[index]['value'] = value;},

  addKMinMax: function(key, min, max){
    this.numericalFilters.push(
    {
      'key' : key,
      'minValue' : min,
      'maxValue' : max
    }
    );
  },

  getNKey: function(i){ return this.numericalFilters[i]['key']},
  getNMin: function(i){ return this.numericalFilters[i]['minValue']},
  getNMax: function(i){ return this.numericalFilters[i]['maxValue']},

  setNKey: function(index, key){this.numericalFilters[index]['key'] = key;},
  setNMin: function(index, minValue){this.numericalFilters[index]['minValue'] = minValue;},
  setNMax: function(index, maxValue){this.numericalFilters[index]['maxValue'] = maxValue;},
};

var TBackendSelector = function () {

  this.textFilters = [];
  this.numericalFilters = [];
};

$.extend(TBackendSelector.prototype, backendSelectorFunctions);

var guiSelectorFunctions = {

  getFilters : function(){ return this.filters;},
  setFilters : function(payload){ this.filters = payload;},

  getFilter : function(idx){ return this.filters[idx];},

  pushFilter: function(filter){
    this.filters.push(filter);  
  },

  popFilter: function(idx){
    this.filters.splice(idx,1);
  }
}

var GuiSelector  = function () {
  this.filters = [];
};
jQuery.extend(GuiSelector.prototype, guiSelectorFunctions);


var TextFilter = function () {
  this.type = 'text';
  this.key = null;
  this.value = null;
  this.regex = false;
};

var NumericalFilter = function () {

  this.type = 'numerical';
  this.key = null;
  this.minValue = null;
  this.maxValue = null;


}; 

var DateFilter = function () {
  this.type = 'date';
  this.key = null;
  this.minDate = null;
  this.maxDate = null;

};

var filterFunctions = {
  setValueGeneric : function (key, value){
    this[key] = value;
  },
  getValueGeneric : function (key){
    return this[key];
  },

  isEmpty : function(){
    if(this.type === 'text') 
      return this.isEmptyText();
    if(this.type === 'date') 
      return this.isEmptyDate();
    if(this.type === 'numerical') 
      return this.isEmptyNumerical();
  },
  
  isEmptyText : function(){
    if((this.key === null) || (this.key === "")){
      return true;
    }
    if((this.value === null) || (this.value === "")){
      return true;
    }
    return false;
  },

    isEmptyDate: function(){
    if((this.key === null) || (this.key === "")){
      return true;
    }
    if((this.minDate === null) || (this.minDate === "")){
      return true;
    }
    if((this.maxDate === null) || (this.maxDate === "")){
      return true;
    }
    return false;
  },
    isEmptyNumerical : function(){
    if((this.key === null) || (this.key === "")){
      return true;
    }
    if((this.minValue === null) || (this.minValue === "")){
      return true;
    }
    if((this.maxValue === null) || (this.maxValue === "")){
      return true;
    }
    return false;
  }

}

jQuery.extend(TextFilter.prototype, filterFunctions);
jQuery.extend(NumericalFilter.prototype, filterFunctions);
jQuery.extend(DateFilter.prototype, filterFunctions);