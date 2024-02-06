package be.twofold.gbfr.gts;

import java.nio.*;
import java.util.*;

public record Gts(
    GtsHeader header,
    Gdex meta,
    List<GtsPageFile> pageFiles
) {
    public static Gts read(ByteBuffer buffer) {
        var header = GtsHeader.read(buffer);

        var meta = Gdex.read(buffer.slice(
            Math.toIntExact(header.metaOffset()),
            header.metaSize()
        ).order(ByteOrder.LITTLE_ENDIAN));

        buffer.position(Math.toIntExact(header.pagesFilesOffset()));
        var pageFiles = readPageFiles(buffer, header.numPageFiles());

        return new Gts(header, meta, pageFiles);
    }

    private static List<GtsPageFile> readPageFiles(ByteBuffer buffer, int count) {
        List<GtsPageFile> pageFiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pageFiles.add(GtsPageFile.read(buffer));
        }
        return pageFiles;
    }


}
