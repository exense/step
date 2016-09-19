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
<a data-toggle="collapse" data-parent="#accordion" href="#collapseASP">Service Params</a>
</h4>
</div>

<div id="collapseASP" class="panel-collapse collapse in">
<div class="panel-body">
<form class="form-inline">
<div class="form-group">
<div class="form-group row">
<label class="col-md-2 control-label">granularity</label>
<div class="col-md-3">
<input type="text" class="form-control granularity aggserviceparams" id="granularity" value="<%= content.granularity %>">
</div>
<label class="col-md-2 control-label">groupby</label>
<div class="col-md-3">
<input type="text" class="form-control groupby aggserviceparams" id="groupby" value="<%= content.groupby %>">
</div>
</div>

</div>

</form>
</div> <!-- /panel-body -->
</div> <!-- /panel-collapse -->
</div> <!-- /panel -->

<div>&nbsp</div>