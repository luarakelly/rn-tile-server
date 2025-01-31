package db;

import java.sql.*;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;

public class GetTiles {
    private static final String TILE_FILE_NAME = "tiles.mbtiles";

    public static byte[] getTile(int z, int x, int y) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + TILE_FILE_NAME)) {
            // Reverse Y as required by certain tile formats
            y = (1 << z) - 1 - y;

            String query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, z);
                stmt.setInt(2, x);
                stmt.setInt(3, y);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        byte[] compressedData = rs.getBytes("tile_data");
                        return decompress(compressedData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
