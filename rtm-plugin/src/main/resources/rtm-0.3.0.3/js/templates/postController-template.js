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
<a data-toggle="collapse" data-parent="#accordion" href="#collapseSelectors">
Selectors
</a>
</h4>
</div>
<div id="collapseSelectors" class="panel-collapse collapse in">
<div class="panel-body">
<span><button type="submit" id="addSelector" class="btn btn-default">Add Selector</button></span><span>&nbsp</span>
<span><button type="submit" id="clearAll" class="btn btn-default">Clear All</button></span><span>&nbsp</span>
<span><button type="submit" id="sendSearch" class="btn btn-success">Search</button></span><span>&nbsp</span>

<div>&nbsp</div>

<form class="controller-form form-inline">
<table class="table table-striped table-bordered dataTable no-footer">
<thead>
<tr>
<!--<th>Selectors</th>-->
<th>Operand</th>
<th>Filters</th>
<th>Del</th>
</tr>
</thead>
<% var selectorIndex = 0%>
<% _.each(selectors, function(sel){ %>
  <tr>
  <!--<th><span>Selector <%= selectorIndex %> </span></th>-->


  <th style="text-align : left;">OR</th>

  <th>
  <span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>" class="btn at btn-warning">Add Text Filter</button></span>
  <span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>" class="btn an btn-warning">Add Numerical Filter</button></span>
  <span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>" class="btn ad btn-warning">Add Date Filter</button></span>
  </th>
  <th><span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>" class="btn rs btn-danger">X</button></span></th>
  </tr>


  <% var fIndex = 0 %>

  <% _.each(sel.getFilters(), function(f){ %>
    <tr>
    <!--<th>Filter <%= selectorIndex %>:<%= fIndex %> </th> -->

    <th style="text-align : right;">AND</th>

    <th>

    <% if(f.getValueGeneric('type') === 'text'){%>

      <div class="date col-md-3" style="padding: 0; margin: 0;">
        <input type="text" class="form-control tkey sinp" style="height: 20 px;" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="key" value="<%= f.getValueGeneric('key') %>">
      </div>

      <div class="col-md-1"  style="padding: 0; margin: 0;" >
      <a  id="dLabel" class="btn btn-default metricChooser" role="button" data-toggle="dropdown" data-target="#" href="" >
        <span class="caret"></span>
      </a>
         <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel" >
            <% var length = defaultTextKeys.length %>
            <% for (i = 0; i < length; i++) { %>
            <li role="presentation" ><a  role="menuitem" tabindex="-1" href="#" class="<%=defaultTextKeys[i]%> defaultKey" id="<%= selectorIndex %>_<%= fIndex %>_<%=defaultTextKeys[i]%>"><%=defaultTextKeys[i]%></a></li>
            <% } %>
         </ul>

      </div>

      <div class="date col-md-3"><input type="text" class="form-control tval sinp" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="value" value="<%= f.getValueGeneric('value') %>"></div>

      <% if(f.getValueGeneric('regex') === 'regex') {%>
        <div class="date col-md-3"><input type="checkbox" class="treg sinp" id="<%= selectorIndex %>_<%= fIndex %>" value="<%= f.getValueGeneric('regex') %>" checked="checked"><label>regex</label></div>
      <% }else{%>

          <div class="date col-md-3">
          <input type="checkbox" class="treg sinp" id="<%= selectorIndex %>_<%= fIndex %>" value="<%= f.getValueGeneric('regex') %>">
          <label>regex</label>
          </div>

      <% }%>


      </th><th><span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>_<%= fIndex %>" class="btn rtf btn-danger">X</button></span></th>
      
    <%} if(f.getValueGeneric('type') === 'numerical') {%>

            <div class="date col-md-3" style="padding: 0; margin: 0;">
            <input type="text" class="form-control nkey sinp" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="key" value="<%= f.getValueGeneric('key') %>">
            </div>
            <div class="col-md-1"  style="padding: 0; margin: 0;" >
            <a  id="dLabel" class="btn btn-default metricChooser" role="button" data-toggle="dropdown" data-target="#" href="" >
              <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel" >
              <% var length = defaultNumericalKeys.length %>
              <% for (i = 0; i < length; i++) { %>
              <li role="presentation" ><a  role="menuitem" tabindex="-1" href="#" class="<%=defaultNumericalKeys[i]%> defaultKey" id="<%= selectorIndex %>_<%= fIndex %>_<%=defaultNumericalKeys[i]%>"><%=defaultNumericalKeys[i]%></a></li>
              <% } %>
            </ul>
            </div>

            <div class="input-group date col-md-3">
            <input type="text" class="form-control nmin sinp" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="minValue" value="<%= f.getValueGeneric('minValue') %>">
            </div><div class="input-group date col-md-3">
            <input type="text" class="form-control nmax sinp" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="maxValue" value="<%= f.getValueGeneric('maxValue') %>">
            </div
            </th><th><span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>_<%= fIndex %>" class="btn rnf btn-danger">X</button></span></th>

    <%} if(f.getValueGeneric('type') === 'date') {%>
              <div class="date col-md-3" style="padding: 0; margin: 0;">
              <input type="text" class="form-control dkey sinp" id="<%= selectorIndex %>_<%= fIndex %>" placeholder="key" value="<%= f.getValueGeneric('key') %>">
              </div>
              <div class="col-md-1"  style="padding: 0; margin: 0;" >
                <a  id="dLabel" class="btn btn-default metricChooser" role="button" data-toggle="dropdown" data-target="#" href="" >
                <span class="caret"></span>
                </a>
              <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel" >
                <% var length = defaultDateKeys.length %>
                <% for (i = 0; i < length; i++) { %>
                <li role="presentation" ><a  role="menuitem" tabindex="-1" href="#" class="<%=defaultDateKeys[i]%> defaultKey" id="<%= selectorIndex %>_<%= fIndex %>_<%=defaultDateKeys[i]%>"><%=defaultDateKeys[i]%></a></li>
                <% } %>
              </ul>
              </div>

              <div class="input-group date form_datetime col-md-4" data-date-format="yyyy.mm.dd hh:ii:ss" data-link-field="dtp_input1">
              <input class="form-control dmin sinp ptBtn" size="16" type="text" value="<%= f.getValueGeneric('minDate') %>" placeholder="minDate" id="<%= selectorIndex %>_<%= fIndex %>">
              <span class="input-group-addon"><span class="glyphicon glyphicon-th"></span></span>
              </div>
              <div class="input-group date form_datetime col-md-4" data-date-format="yyyy.mm.dd hh:ii:ss" data-link-field="dtp_input1">
              <input class="form-control dmax sinp ptBtn" size="16" type="text" value="<%= f.getValueGeneric('maxDate') %>" placeholder="maxDate" id="<%= selectorIndex %>_<%= fIndex %>">
              <span class="input-group-addon"><span class="glyphicon glyphicon-th"></span></span>
              </div>
              </th><th><span class="btn-group-xs"><button type="submit" id="<%= selectorIndex %>_<%= fIndex %>" class="btn rdf btn-danger">X</button></span></th>
              <%}%>

              <% fIndex = fIndex + 1%>

              <% }); %>
</tr>
<%  selectorIndex = selectorIndex + 1; %>
<% }); %>
</table>

</div> <!-- /panel-body -->
</div> <!-- /panel-collapse -->
</div> <!-- /panel -->
<div>&nbsp</div>