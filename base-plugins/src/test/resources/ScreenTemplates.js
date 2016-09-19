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
{"testScreen1":[
  "Param1",
  {"id":"Param2","options":["Option1","Option2"]},
  {"type":"TEXT","id":"Param3","label":"LabelParam3","options":[{"value":"Option1"},{"value":"Option2"},{"value":"Option3", "activationExpression":"user=='user1'"}]},
  {"type":"TEXT","id":"Param4","label":"LabelParam4","options":[{"value":"Option1"},{"value":"Option2"}],"activationExpression":"user=='user1'"},
  {"id":"Param5","options":["Option1","Option2"],"activationExpression":"user=='user1'"}
]
}
