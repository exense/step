/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages.client.model;

public class AutomationPackageFromMavenRequest {

    private String apMavenSnippetXml;
    private String keywordLibraryMavenSnippetXml;

    public AutomationPackageFromMavenRequest() {
    }

    public String getApMavenSnippetXml() {
        return apMavenSnippetXml;
    }

    public void setApMavenSnippetXml(String apMavenSnippetXml) {
        this.apMavenSnippetXml = apMavenSnippetXml;
    }

    public String getKeywordLibraryMavenSnippetXml() {
        return keywordLibraryMavenSnippetXml;
    }

    public void setKeywordLibraryMavenSnippetXml(String keywordLibraryMavenSnippetXml) {
        this.keywordLibraryMavenSnippetXml = keywordLibraryMavenSnippetXml;
    }
}
