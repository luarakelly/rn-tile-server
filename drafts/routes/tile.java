package routes;

import fi.iki.elonen.NanoHTTPD;
import db.GetTiles;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Tile extends NanoHTTPD {
    public Tile(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // Handle tile requests (e.g., /tile/zoom/x/y.pbf)
        if (uri.matches("/tile/\\d+/\\d+/\\d+\\.pbf")) {
            String[] parts = uri.split("/");
            int z = Integer.parseInt(parts[2]);
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4].split("\\.")[0]);

            byte[] tileData = GetTiles.getTile(z, x, y);
            if (tileData != null) {
                return newFixedLengthResponse(Status.OK, "application/x-protobuf", new ByteArrayInputStream(tileData), tileData.length);
            } else {
                return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Tile not found");
            }
        }
        return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }
}
