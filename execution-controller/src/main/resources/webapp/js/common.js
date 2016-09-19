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

