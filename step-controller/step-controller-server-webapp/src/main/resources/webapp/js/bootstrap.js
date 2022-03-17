/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
console.log("Initializing bootstrapper")
var step = {
    bootstrap: function() {
      $.get("rest/app/plugins",function(plugins) {
        console.log("Loading plugins")
        var scripts = [];
        var angularModules = [];
        _.each(plugins,function(plugin) {
          angularModules = angularModules.concat(plugin.angularModules);
          scripts = scripts.concat(plugin.scripts);   
        })

        var deferred = new $.Deferred(), pipe = deferred;

        $.each(scripts, function(i, val) {
          pipe = pipe.pipe(function() {
            return loadScript(val);
          });
        });

        pipe.pipe(function() {
          _.each(angularModules, function(module) {
            angular.module('tecAdminApp').requires.push(module);      
          })
          console.log("Bootstrapping angular");
          angular.bootstrap(document,['tecAdminApp'])
        })
        
        deferred.resolve();
      })      
    }
}

function loadScript(filename){
    var d = $.Deferred()
    var callback = function() {
      console.log('Loaded plugin script ' + filename);
      d.resolve()
    };
    var script=document.createElement('script')
    script.setAttribute("type","text/javascript")
    script.setAttribute("src", filename)
    script.onreadystatechange = callback;
    script.onload = callback;
    document.getElementsByTagName("head")[0].appendChild(script)
    return d;
}
