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
