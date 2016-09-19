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
 * A complex input contains both the attributes necessary to restore a GUI context (or state) and the service input
 * I considered it is okay to couple these elements seeing as the app strategy is to call a service upon navigation to a new route and hence upon generation of a new Gui state.
 * The service input will hence always be a subset of the controller's input, and decoupling them would mean doing redundant work.
 * Therefore, it will remain like that until something challenges that concept.
 * 
 */
function ServiceInput(){

	this.selectors = [];
	this.serviceParams = {};

	this.getSelectors = function(){ return this.selectors;};
	this.getServiceParams = function(){ return this.serviceParams;};

	this.setSelectors = function(selectors){ this.selectors = selectors;};
	this.setServiceParams = function(serviceParams){
		this.serviceParams = serviceParams;
	};
}