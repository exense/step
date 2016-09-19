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
<div class="panel panel-primary" id="chartPanel">
<div class="panel-heading">
	      <h4 class="panel-title">
        <a data-toggle="collapse" data-parent="#accordion" href="#collapseAChart">
          Chart
        </a>
      </h4>
</div>	
<div id="collapseAChart" class="panel-collapse collapse in">
<div class="panel-body">
<div class="dropdown">
<label> Metric : </label>
  <a id="dLabel" class="btn btn-default metricChooser" role="button" data-toggle="dropdown" data-target="#" href="">
    <%=currentChartMetricChoice%> <span class="caret"></span>
  </a>
  <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
  <% var length = metricsList.length %>
  <% for (i = 0; i < length; i++) { %>
	  <li role="presentation"><a role="menuitem" tabindex="-1" href="#" class="<%=metricsList[i]%> metricChoice" id="<%=metricsList[i]%>"><%=metricsList[i]%></a></li>
	<% } %>
  </ul>
</div> <!-- /dropdown-menu -->
<div id="gviz"></div>
</div> <!-- /panel-body -->
</div> <!-- /panel-collapse -->
</div> <!-- /panel -->

<div>&nbsp</div>
