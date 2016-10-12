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
