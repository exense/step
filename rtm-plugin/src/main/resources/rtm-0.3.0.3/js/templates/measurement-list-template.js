<div class="panel panel-primary">
<div class="panel-heading">
<h4 class="panel-title">
<a data-toggle="collapse" data-parent="#accordion" href="#collapseMTable">
Table
</a>
</h4>
</div>

<div id="collapseMTable" class="panel-collapse collapse in">
<div class="panel-body">
<label> Metrics : </label>
<% var length = metricsList.length %>
<form class="form-inline">
<div class="container">
<div class="row">
<% for (i = 0; i < length; i++) { %>
  <div class="col-xs-2">
  <% if(!($.inArray(metricsList[i], checkedTableMetrics) <0)){ %>
    <input type="checkbox" class="<%=metricsList[i]%> mlmCheckbox" id="<%=metricsList[i]%>" value="<%=metricsList[i]%>" checked="checked">
    <% } else {%>
      <input type="checkbox" class="<%=metricsList[i]%> mlmCheckbox" id="<%=metricsList[i]%>" value="<%=metricsList[i]%>">
     <% } %>
<label role="presentation" class="<%=metricsList[i]%> control-label" id="<%=metricsList[i]%>"><%=metricsList[i]%></label>
      </div>
      <% } %>
      </div>
      </div>
      </form>

<div>&nbsp</div>

<% if(nextFactor > 0){ %>
  <div class="col-xs-1">
  <button type="submit" id="previous" class="btn btn-warning"><</button>
  </div>
<%}else{%>
<div class="col-xs-1">
  <button type="submit" id="previous" class="btn btn-default" disabled><</button>
  </div>
<%}%>
  <div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div>
  <div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div><div class="col-xs-1"></div>
<% if(measurements.length >= pagingValue){ %>
  <div class="col-xs-1">
  <button type="submit" id="next" class="btn btn-warning">></button>
  </div>
<%}else{%>
    <div class="col-xs-1">
  <button type="submit" id="next" class="btn btn-default" disabled>></button>
  </div>
<%}%>

<div>&nbsp</div>
      <% var tlength = checkedTableMetrics.length %>
      <div>&nbsp</div>
      <table class="table striped">
      <thead>
      <tr>
      <% for (i = 0; i < tlength; i++) { %>
        <% var curMetric = checkedTableMetrics[i];%>
        <th><%= curMetric %></th>
        <% } %>
        </tr>
        </thead>
        <tbody>
        <% _.each(measurements, function(measurement) { %>
          <% for (i = 0; i < tlength; i++) { %>
            <% curMetric = checkedTableMetrics[i]; %>
            <%if($.inArray(curMetric, dateMetric) >= 0){%>
              <%var curDate = new Date(measurement.attributes.n[curMetric]);%>
              <%var printableDate = getPrintableDate(curDate)%>
              <td><%=printableDate%>(<%=measurement.attributes.n[curMetric]%>)</td>
            <%}else{%>
                <%if(measurement.attributes.t[curMetric]){%>
                  <td><%= measurement.attributes.t[curMetric] %></td>
                <%}else{%>
                       <%if(measurement.attributes.n[curMetric]){%>
                         <td><%= measurement.attributes.n[curMetric] %></td>
                       <%}else{%>
                          <td>no data</td>
                       <%}%>
                <%}%>
            <%}%>
          <% } %>
                </tr>
        <% }); %>
        </tbody>
        </table>
        </div> <!-- /panel-body -->
        </div> <!-- /panel-collapse -->
        </div> <!-- /panel -->
        <div>&nbsp</div>