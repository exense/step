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
<div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
  <div class="container">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="#">RTM v<%= rtmVersion %></a>
    </div>
    <div class="navbar-collapse collapse">
      <ul class="nav navbar-nav">
      <% %>
      <% _.each(contextKeys, function(key) { %>
        <% var inactive = "menu-choice" %>
        <% var active = "menu-choice active" %>
        <% if(contexts[key].active){%>
        <li class="<%= active %>"><a href="#<%= key %>"><%= key %></a></li>
        <% }else{ %>
          <li class="<%= inactive %>"><a href="#<%= key %>"><%= key %></a></li>
        <% } %>
      <%});%>
      </ul>
    </div><!--/.nav-collapse -->
  </div>
</div>