function AggregateServiceParams(defaults){

  this.sessionId =  defaults.defaultSessionId;
  this.granularity = defaults.defaultGranularity;
  this.groupby = defaults.defaultGroupby;

  this.getSessionId = function(){ return this.sessionId;};
  this.getGranularity = function(){ return this.granularity;};
  this.getGroupby = function(){ return this.groupby;};
}
