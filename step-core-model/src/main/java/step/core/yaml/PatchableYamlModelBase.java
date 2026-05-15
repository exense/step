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
 ******************************************************************************/
package step.core.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchableYamlModelBase extends AbstractYamlModel implements PatchableYamlModel {
    private static final Logger logger = LoggerFactory.getLogger(PatchableYamlModelBase.class);

    @JsonIgnore
    private PatchingContext context;

    public PatchableYamlModelBase(PatchingContext context) {
        this.context = context;
    }

    @Override
    @JsonIgnore
    public final PatchingContext getPatchingContext() {
        return context;
    }

    @JsonIgnore
    private boolean modified = false;

    public void setModified() {
        this.modified = true;
    }

    @Override
    @JsonIgnore
    public final void setPatchingContext(PatchingContext context) {
        this.context = context;
    }

    @JsonIgnore
    public String getCurrentYaml(String contextIndent) {
        if (!modified) {
            String chunk = context.getChunk(this);
            if (chunk != null) {
                // use the original chunk, but still re-indent appropriately if needed
                return context.reindent(chunk, contextIndent);
            }
            // fallthrough in case of any problem?
            throw new IllegalStateException();
        }
        return context.serialize(this, contextIndent);
    }

    @Override
    @JsonIgnore
    public StartingLineDeterminationStrategy getStartingLineDeterminationStrategy() {
        // Let's hope that this really is true for all subclasses :-)
        return StartingLineDeterminationStrategy.SAME_LINE;
    }

    @Override
    public void onParsed(JsonLocation startLocation, JsonLocation endLocation) {
        context.claimChunk(startLocation, endLocation, this);
    }

}
