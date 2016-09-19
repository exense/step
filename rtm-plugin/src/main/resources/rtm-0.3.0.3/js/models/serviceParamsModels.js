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
/*
 * A top level object for harvesting all necessary service parameters (independently from a specific service) 
 */
function ServiceParams(){

	this.setParam = function(key, val){this.key = val};
	this.getParam = function(key){return this.key};
	this.setFragment = function(domain, obj){

		for (var key in obj) {
			  if (obj.hasOwnProperty(key)) {
				  //console.log(domain + "." + key +    "     --->      " + obj[key]);
			    this[domain + "." + key] = obj[key];
			  }
			}
		
		};
}
