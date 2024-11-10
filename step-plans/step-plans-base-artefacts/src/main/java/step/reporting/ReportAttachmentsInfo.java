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
package step.reporting;

import step.attachments.AttachmentMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportAttachmentsInfo {
    private Map<String, List<AttachmentMeta>> attachmentsPerTestCase = new HashMap<>();

    public Map<String, List<AttachmentMeta>> getAttachmentsPerTestCase() {
        return attachmentsPerTestCase;
    }

    public void add(String testCaseId, List<AttachmentMeta> attachmentMetas) {
        attachmentsPerTestCase.computeIfAbsent(testCaseId, k -> new ArrayList<>());
        attachmentsPerTestCase.get(testCaseId).addAll(attachmentMetas);
    }
}
