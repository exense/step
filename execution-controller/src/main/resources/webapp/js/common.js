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
  
  // 'custom','none', 'all'
  this.currentSelectionMode;
  
  this.selectionModel = {};
  
  this.defaultSelector = function() {return true};
  
  this.setDefaultSelection = function(selected) {
    me.setSelectionAll(selected);
    me.defaultSelector = function() {return selected};
  }
  
  this.setDefaultSelector = function(defaultSelector) {
    me.currentSelectionMode = 'custom';
    me.defaultSelector = defaultSelector;
    me.resetSelection(); 
  }
  
  this.resetSelection = function() {
    me.selectionModel = {}; 
  }
  
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
    me.currentSelectionMode = 'custom';
    me.getModel(id).selected = selected;
  }
  
  this.setSelectionAll = function(value) {
    _.each(dataFunction(),function(dataRow){
      me.setSelection(dataRow[0],value);
    })
    me.currentSelectionMode = value?'all':'none';    
  };
  
  this.isSelected = function(id) {
    return me.getModel(id).selected;
  }
  
  this.getSelectionMode = function() {
    return me.currentSelectionMode;
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

function ObjectTracker(destroyer) {
  var me = this;
  
  me.objectRegistry = {};
  me.currentCycleId;
  
  this.newCycle = function() {
    me.currentCycleId = new Date().getTime();
  };
  
  this.destroyObjectsFromPreviousCycle = function() {
    if(_.size(me.objectRegistry)>1) {
      var newObjectRegistry = {};
      _.mapObject(me.objectRegistry, function(value, key) {
        if(key!=me.currentCycleId) {
          _.each(value, function(o) {
            destroyer(o);
          })
        } else {
          newObjectRegistry[me.currentCycleId] = value;
        }
      })
      me.objectRegistry = newObjectRegistry;                
    }
  }
  
  this.track = function(o) {
    me.destroyObjectsFromPreviousCycle();
    var objectList = me.objectRegistry[me.currentCycleId];
    if(!objectList) {
      objectList = [];
      me.objectRegistry[me.currentCycleId] = objectList;
    }
    objectList.push(o);
  }
  
  this.destroy = function() {
    _.mapObject(me.objectRegistry, function(value, key) {
        _.each(value, function(o) {
          destroyer(o);
        })
    })
  }
  
  this.newCycle();
}

// When a scope is created manually using the scope.$new method
// it has to be destroyed manually using scope.$destroy
// In some cases it is impossible to find a good hook where to call the $destroy method.
// This is for instance the case in Datatables where we call scope.$new in the
// createdCell hook but where we have no hook for cell deletion
// 
// The ScopeTracker i.e ObjectTracker works like a simple garbage collector with cycles
// All the objects registered (tracked) during one cycle are deleted in the follwoing cycle
// It is the responsibility of the application to increment the cycles using the newCycle() method.
function ScopeTracker(destroyer) {
  return new ObjectTracker(function(scope) {
    scope.$destroy();
  });
}