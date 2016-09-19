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