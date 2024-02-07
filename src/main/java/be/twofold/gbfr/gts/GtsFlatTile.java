package be.twofold.gbfr.gts;

import java.nio.*;

public record GtsFlatTile(
    short pageIndex,
    short pageChunk,
    short pageSubChunk,
    short unknown4,
    int unknown5
) {
    public static GtsFlatTile read(ByteBuffer buffer) {
        var pageIndex = buffer.getShort();
        var pageChunk = buffer.getShort();
        var pageSubChunk = buffer.getShort();
        var unknown4 = buffer.getShort();
        var unknown5 = buffer.getInt();

        return new GtsFlatTile(
            pageIndex,
            pageChunk,
            pageSubChunk,
            unknown4,
            unknown5
        );
    }
}
