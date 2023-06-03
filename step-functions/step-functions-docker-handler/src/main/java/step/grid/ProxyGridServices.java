package step.grid;

import ch.exense.commons.io.FileHelper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Path("/proxy/grid")
public class ProxyGridServices {

    public static FileManagerClient fileManagerClient;

    @GET
    @Path("/file/{id}/{version}")
    public Response getFile(@PathParam("id") String id, @PathParam("version") String version) throws IOException, FileManagerException {
        FileVersionId versionId = new FileVersionId(id, version);
        FileVersion fileVersion = fileManagerClient.requestFileVersion(versionId);
        File file = fileVersion.getFile();

        final FileInputStream inputStream = new FileInputStream(file);
        StreamingOutput fileStream = output -> {
            try {
                FileHelper.copy(inputStream, output, 2048);
                output.flush();
            } finally {
                inputStream.close();
            }

        };
        Response.ResponseBuilder var10000 = Response.ok(fileStream, "application/octet-stream");
        String var10002 = file.getName();
        return var10000.header("content-disposition", "attachment; filename = " + var10002 + "; type = " + (fileVersion.isDirectory() ? "dir" : "file")).build();
    }
}
