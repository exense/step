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
package step.automation.packages;

import ch.exense.commons.io.FileHelper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.net.URLConnection;

/**
 * Serves the content of a single automation package entry for
 * {@code /automation-packages/ap-resources/content}, whether the entry comes from a deployed archive
 * or from the exploded directory of the package open in the editor.
 */
public class ApResourceContentResponse {

    private ApResourceContentResponse() {
    }

    /**
     * @param apResource the entry to serve. Its underlying archive handle is closed once the content
     *                   has been streamed, so the caller must not close it itself
     * @param inline     whether the content should be served as {@code inline} rather than as an
     *                   attachment
     */
    public static Response of(ApResourceBrowser.ApResourceStream apResource, boolean inline) {
        StreamingOutput fileStream = output -> {
            try (ApResourceBrowser.ApResourceStream stream = apResource) {
                FileHelper.copy(stream.getInputStream(), output, 2048);
            }
        };
        String contentDisposition = inline ? "inline" : "attachment";
        return Response.ok(fileStream, getMimeType(apResource.getName()))
            .header("content-disposition", String.format(contentDisposition + "; filename=\"%s\"", apResource.getName()))
            .build();
    }

    private static String getMimeType(String fileName) {
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) {
            mimeType = fileName.endsWith(".log") ? "text/plain; charset=utf-8" : MediaType.APPLICATION_OCTET_STREAM;
        }
        return mimeType;
    }
}
