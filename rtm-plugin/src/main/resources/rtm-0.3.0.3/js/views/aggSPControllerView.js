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

var AggSPControllerView = Backbone.View.extend(
		{
			el: '.ServiceParamListView',
			events: {
				"change .aggserviceparams" : "update",
				"keypress" : "hijackEnter"
			},

			aggserviceparams : {},
			
			defaultSessionId : '',
			defaultGranularity : '',
			defaultGroupby : '',
			
			initialize : function(){
				  this.defaultSessionId = Config.getProperty('client.AggSPControllerView.defaultSid');
				  this.defaultGranularity = Config.getProperty('client.AggSPControllerView.defaultGranularity');
				  this.defaultGroupby = Config.getProperty('client.AggSPControllerView.defaultGroupby');
				  this.aggserviceparams = new AggregateServiceParams({defaultSessionId : this.defaultSessionId, defaultGranularity : this.defaultGranularity, defaultGroupby : this.defaultGroupby});
			},
			
			getGuiDomain: function(){
				return 'aggregateSPView';
			},

			getServiceDomain: function(){
				return 'aggregateService';
			},

			hijackEnter : function(e){
				if(e.keyCode === 13){
					this.trigger('globalSearchEvent');
					e.preventDefault();
				}
			},
			getGuiFragment: function(){
				return JSON.parse(JSON.stringify(this.aggserviceparams));
			},

			getServiceFragment: function(){
				return JSON.parse(JSON.stringify(this.aggserviceparams));
			},

			update: function(e){
				var paramName = e.currentTarget.id;
				this.aggserviceparams[paramName] = e.currentTarget.value;
			},

			setParam: function(key, value){
				this.aggserviceparams[key] = value;
			},

			loadGuiState: function(input){
				this.aggserviceparams = input;
			},

			render: function () {
				var that = this;
				$.get(resolveTemplate('aggserviceparams-template'), function (data) {
					template = _.template(data, {content : that.aggserviceparams});
					that.$el.html(template);  
				}, 'html')
				.fail(function(model, response, options ) {
					displayError('response=' + JSON.stringify(response));
				});
			},

			cleanup: function(){
				this.$el.html(''); 
			}

		});