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
var Config = function(){

	Config.config = {};
	Config.confUrl  = '/rtm/rest/configuration/getConfiguration?noCache=' + (new Date()).getTime();
	Config.initVar = false;
};

var ConfigFunctions = {
		loadConfig : function(callback){

			var that = this;

			$.ajax(
					{
						url: that.getConfUrl(),
						success:function(result){
							that.setConfigObject(result['config']);
							that.setInit(true);
							callback();
						}
					});

		},
		getProperties : function(){return Config.config;},
		getProperty : function(key){return Config.config[key];},
		getConfUrl : function(){return Config.confUrl;},
		setConfigObject : function(obj){Config.config=obj;},
		isInit : function(){return Config.initVar;},
		setInit : function(obj){Config.initVar=obj;}
};

jQuery.extend(Config, ConfigFunctions);