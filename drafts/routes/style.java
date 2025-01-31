package routes;

import fi.iki.elonen.NanoHTTPD;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Styles extends NanoHTTPD {
    private static final String STYLES_FILE_NAME = "assets/styles.json";

    public Styles(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Serve styles.json file
        if ("/style.json".equals(session.getUri())) {
            try {
                byte[] stylesJson = Files.readAllBytes(Paths.get(STYLES_FILE_NAME));
                return newFixedLengthResponse(Status.OK, "application/json", new ByteArrayInputStream(stylesJson), stylesJson.length);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading styles.json");
            }
        }
        return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }
}
