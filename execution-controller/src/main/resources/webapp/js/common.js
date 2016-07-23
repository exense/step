SelectionModel = function(dataFunction) {

  var me = this;
  
  this.selectionModel = {};
  
  this.defaultSelector = function() {return true};
  
  this.getModel = function(id) {
    var entry =  this.selectionModel[id];
    if(entry==null) {
      var row = _.find(dataFunction(),function(val){return val[0]==id})
      entry = {selected:me.defaultSelector(row)};
      this.selectionModel[id]=entry;
    }
    return entry;
  }
  
  this.setSelection = function(id, selected) {
    me.getModel(id).selected = selected;
  }
  
  this.setSelectionAll = function(value) {
    _.each(dataFunction(),function(dataRow){
      me.setSelection(dataRow[0],value);
    })};
  
  this.isSelected = function(id) {
    return me.getModel(id).selected;
  }
  
  this.toggleSelection = function(id) {
    me.setSelection(id, !me.isSelected(id));
  }
  
  this.getSelection = function() {
    var result = {};
    result['selectedItems'] = me.getDataRowsBySelection(true);
    result['notSelectedItems'] = me.getDataRowsBySelection(false);
    return result;
  };
  
  this.getDataRowsBySelection = function(selected) {
    return _.filter(dataFunction(), function(val) {
      return selected?me.isSelected(val[0]):!me.isSelected(val[0])})
  };
}

