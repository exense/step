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
var MainHeaderView = Backbone.View.extend({
  el: $('.mainheader'),
  events : {
  },
  initialize:function(){
    this.title = '';
  },
  setTitle:function(title){
    this.title = title;
  },
  render: function () {

    var that = this;

    $.get(resolveTemplate('mainHeader-template'), function (data) {
      template = _.template(data, {title : that.title});
      that.$el.html(template);
    }, 'html')
    .fail(function(model, response, options ) {
      displayError('response=' + JSON.stringify(response));
    });
  }
});
