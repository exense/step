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

$.extend(Backbone.View.prototype, {

	cleanup: function(){ 
		this.$el.html('');
	}

});


/* Utility functions and extends*/
function displayError(msg){
	if($("#errorZone").html().indexOf(msg) < 0){
		$("#errorZone").append('<div class="alert alert-danger" role="alert"><span><strong>Error : </strong></span><span>&nbsp</span><span>'+ msg +'</span></div>');
	}
}
function displayWarning(msg){
	if($("#errorZone").html().indexOf(msg) < 0){
		$("#errorZone").append('<div class="alert alert-warning" role="alert"><span><strong>Warning : </strong></span><span>&nbsp</span><span>'+ msg +'</span></div>');
	}
}

function setMenuActive(choice){

	$(".menu-choice").each(function(){
		$(this).removeClass('active');
		if($(this).text() === choice){
			$(this).addClass('active');
		}
	});
}

function setMenuActive(choice){

	$(".menu-choice").each(function(){
		$(this).removeClass('active');
		if($(this).text() === choice){
			$(this).addClass('active');
		}
	});
}

function setTitleActive(choice){
	$(".manager-title").text(choice);
}

function getActiveMenu(choice){
	return $(".menu-choice.active").text();
}

function getNoCacheValue(){
	if(Config.getProperty("client.debug") === "true"){
		return (new Date()).getTime();
	}
	else{
		return Config.getProperty("rtmVersion");
	}
}
function resolveTemplate(templateName){
	
	return 'js/templates/'+ templateName + '.js'
	+ '?'
	+ 'nocache='+ getNoCacheValue();
}
function pad(num, size) {
	var s = num+"";
	while (s.length < size) s = "0" + s;
	return s;
}
function getPrintableDate(d) {
	var year = d.getFullYear() ;
	var month = d.getMonth() + 1;
	var day = d.getDate();
	var hours = d.getHours();
	var minutes = d.getMinutes();
	var seconds = d.getSeconds();
	var millis = d.getMilliseconds();

	return year + '.' + pad(month, 2) + '.' + pad(day, 2) + ' ' + pad(hours, 2) + ':' + pad(minutes, 2) + ':' + pad(seconds, 2) + '.' + pad(millis, 3);
}
function guiSelectorParseDate(input) {

	var msg = 'Incorrect date : ' + input + ', format is : yyyy-MM-dd hh:mm:ss.SSS';

	if(Math.floor(input) == input && $.isNumeric(input)) 
		return input;

	var spaceSplit = input.split(' ');

	var date;
	var time;
	var hms;

	var dotSplit;
	var columnSplit;

	if(spaceSplit.length !== 2){
		displayError(msg + '(spaceSplit)');
		return;
	}else{
		date = spaceSplit[0];
		time = spaceSplit[1];
		dotSplit = date.split('.');


		if(dotSplit.length !== 3){
			displayError(msg  + '(dotSplit)');
			return;
		}else{
			/*
			dotSplit = time.split('.');

			if(dotSplit.length !== 2){
				displayError(msg  + '(dotSplit)');
				return;
			}else{
				hms = dotSplit[0];

				columnSplit = hms.split(':');
				if(columnSplit.length !== 3){
					displayError(msg + '(columnSplit)');
					return;
				}

			 */

			columnSplit = time.split(':');
			if(columnSplit.length !== 3){
				displayError(msg + '(columnSplit)');
				return;
			}
		}

	}
	//return new Date(dotSplit[0], dotSplit[1]-1, dotSplit[2], columnSplit[0], columnSplit[1], columnSplit[2], dotSplit[1]).getTime();
	return new Date(dotSplit[0], dotSplit[1]-1, dotSplit[2], columnSplit[0], columnSplit[1], columnSplit[2]).getTime();
}

function guiToBackendInput(guiSelectors){

	var selPayload = [];

	// iterate over selectors
	var arrayLength = guiSelectors.length;
	for (var i = 0; i < arrayLength; i++) {
		//console.log('i='+i);
		var curGuiSelector = guiSelectors[i];
		$.extend(curGuiSelector, guiSelectorFunctions);

		var curBSelector = new TBackendSelector();

		var subArrayLength = guiSelectors[i].filters.length;
		for (var j = 0; j < subArrayLength; j++) {
			//console.log('j='+j);
			var curFilter = curGuiSelector.getFilter(j);

			$.extend(curFilter, filterFunctions);

			if(curFilter.getValueGeneric('type') === 'text'){
				var regexVal = 'false';
				if(curFilter.getValueGeneric('regex') === 'regex')
					regexVal = 'true';
				curBSelector.addKVR(curFilter.getValueGeneric('key'), curFilter.getValueGeneric('value'), regexVal);
			}

			if(curFilter.getValueGeneric('type') === 'numerical'){
				curBSelector.addKMinMax(curFilter.getValueGeneric('key'), curFilter.getValueGeneric('minValue'), curFilter.getValueGeneric('maxValue'));
			}
			if(curFilter.getValueGeneric('type') === 'date'){
				curBSelector.addKMinMax(curFilter.getValueGeneric('key'), guiSelectorParseDate(curFilter.getValueGeneric('minDate')), guiSelectorParseDate(curFilter.getValueGeneric('maxDate')));
			}
			//console.log('toBackend :');
			//console.log(curBSelector);
		}

		selPayload.push(curBSelector);
	}

	return selPayload;
}