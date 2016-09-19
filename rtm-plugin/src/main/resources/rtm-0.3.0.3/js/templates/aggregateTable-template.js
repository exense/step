<div class="panel panel-primary" id="tablePanel">

<div class="panel-heading">
<h4 class="panel-title">
<a data-toggle="collapse" data-parent="#accordion" href="#collapseATable">
Table
</a>
&nbsp
<% if(displayTable === 'true'){ %>
	<input type="checkbox" class="displayTable" id="DisplayTable" value="displayed" checked="checked">
<% } else {%>
	<input type="checkbox" class="displayTable" id="DisplayTable" value="displayed">
<% }%>
</h4>
</div><!-- /panel-heading -->

<% if(displayTable === 'true'){ %>
<div id="collapseATable" class="panel-collapse collapse in">
<div class="panel-body">
<label> Metrics : </label>
<% var length = metricsList.length %>
<form class="form-inline">
<div class="container">
<div class="row">
<% for (i = 0; i < length; i++) { %>
  <div class="col-xs-1">
  <label role="presentation" class="<%=metricsList[i]%> control-label" id="<%=metricsList[i]%>"><%=metricsList[i]%></label>

  <% if(!($.inArray(metricsList[i], checkedAggTableMetrics) <0)){ %>
    <input type="checkbox" class="<%=metricsList[i]%> mlCheckbox" id="<%=metricsList[i]%>" value="<%=metricsList[i]%>" checked="checked">
    <% } else {%>
      <input type="checkbox" class="<%=metricsList[i]%> mlCheckbox" id="<%=metricsList[i]%>" value="<%=metricsList[i]%>">
      <% } %>

      </div>
      <% } %>
      </div>
      </div>
      </form>
      <% var tlength = checkedAggTableMetrics.length %>
      <div>&nbsp</div>
      <table class="table striped">
      <thead>
      <tr>
      <th>GroupBy</th>
      <% for (i = 0; i < tlength; i++) { %>
        <% var curMetric = checkedAggTableMetrics[i];%>
        <th><%= curMetric %></th>
        <% } %>
        </tr>
        </thead>
        <tbody>
        <% if (aggregates.length > 0) {%>
        <% _.each(aggregates[0].get('payload'), function(aggregate) { %>
          <% _.each(aggregate.data, function(itemZ) { %>
            <tr>
            <td><%= aggregate.groupby %></td>

            <% for (i = 0; i < tlength; i++) { %>
              <% curMetric = checkedAggTableMetrics[i]; %>
                <%if($.inArray(curMetric, dateMetric) >= 0){%>
                  <%var curDate = new Date(itemZ.n[curMetric]);%>
                  <%var printableDate = getPrintableDate(curDate)%>
                    <td><%=printableDate%>(<%=itemZ.n[curMetric]%>)</td>
                <%}else{%>
                    <td><%= itemZ.n[curMetric] %></td>
                <%}%>
              <% } %>
              </tr>
              <% }); %>
          <% }); %>
          <% }%>
        </tbody>
        </table>
        </div> <!-- /panel-body -->
        </div> <!-- /panel-collapse -->
        </div> <!-- /panel -->
        <% }%>
        <div>&nbsp</div>