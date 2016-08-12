var Aggregate = Backbone.Model.extend({});

var Aggregates = Backbone.Collection.extend({
      //url: '/rtm/rest/service/allmeasurements',
      url: '/rtm/rest/service/aggregate',
      model: Aggregate,

      refreshData: function(rawInput){

        var that = this;

        input = JSON.stringify(rawInput);
        //console.log('Fetching Aggregates with following service input : ');
        //console.log(input);
        this.fetch({
          type : 'POST',
          dataType:'json',
          contentType:'application/json; charset=utf-8',
          data: input,
          success: function (response) {
           // console.log('loaded ' + response.length + ' objects.');
            //console.log(response.models[0].get('warning'));
        	//console.log(response.models[0]);
           if(response.models[0].get('warning'))
              displayWarning(' [SERVER] ' + response.models[0].get('warning'));
           that.trigger('AggregatesRefreshed');
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
