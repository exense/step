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
