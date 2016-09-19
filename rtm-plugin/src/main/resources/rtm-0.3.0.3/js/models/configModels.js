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