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
var NavBarView = Backbone.View.extend({
  el: $('#navDiv'),
  events : {
  },
  initialize:function(){

    this.contexts = {};

    this.contexts.Measurement = {active : true};
    this.contexts.Aggregate = {active : false};
    this.contexts.Misc = {active : false};
  },
  getContextKeysArrays: function() {
    var keys = [];
    for(var k in this.contexts)
      keys.push(k);
    return keys;
  },
  setActiveContext:function(context){
    var keysArray = this.getContextKeysArrays();
    var that = this;
    _.each(keysArray, function(key){
      that.contexts[key].active = false;
    });

    this.contexts[context].active = true;
  },
  render: function () {

    var that = this;

    $.get(resolveTemplate('navbar-template'), function (data) {
      template = _.template(data, {contexts : that.contexts, contextKeys : that.getContextKeysArrays(), rtmVersion : Config.getProperty('rtmVersion')});
      that.$el.append(template);
    }, 'html')
    .fail(function(model, response, options ) {
      displayError('response=' + JSON.stringify(response));
    });
  }
});
