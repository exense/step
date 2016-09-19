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