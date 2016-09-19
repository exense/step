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
