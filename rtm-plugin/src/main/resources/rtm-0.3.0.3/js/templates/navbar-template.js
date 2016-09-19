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