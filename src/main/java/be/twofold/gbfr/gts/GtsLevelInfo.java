package be.twofold.gbfr.gts;

import java.nio.*;

public record GtsLevelInfo(
    int tileWidth,
    int tileHeight,
    int indicesOffset,
    int pad
) {
    public static GtsLevelInfo read(ByteBuffer buffer) {
        var tileWidth = buffer.getInt();
        var tileHeight = buffer.getInt();
        var indicesOffset = buffer.getInt();
        var pad = buffer.getInt();

        return new GtsLevelInfo(
            tileWidth,
            tileHeight,
            indicesOffset,
            pad
        );
    }
}
