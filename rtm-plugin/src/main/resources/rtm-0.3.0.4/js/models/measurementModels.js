var Measurement = Backbone.Model.extend({});

var Measurements = Backbone.Collection.extend({
      //url: '/rtm/rest/service/allmeasurements',
      url: '/rtm/rest/service/measurement',
      model: Measurement,

      refreshData: function(rawInput){

        var that = this;

        input = JSON.stringify(rawInput);
        //console.log('Fetching Measurements with following input : ' + input);
        this.fetch({
          type : 'POST',
          dataType:'json',
          contentType:'application/json; charset=utf-8',
          data: input,
          success: function (response) {
            //console.log('loaded ' + response.length + ' objects.');
            //console.log(response);
            that.trigger('MeasurementsRefreshed');
          },
          error: function( model, response, options ){
            //console.log('model=' + JSON.stringify(model) + ', response=' + JSON.stringify(response) + ', options=' + JSON.stringify(options));
          	 displayError('[SERVER_CALL] Technical Error=' + JSON.stringify(response)+ ';       input=' + input);
            that.reset();
          }
        });
        return status;
      }
    });
