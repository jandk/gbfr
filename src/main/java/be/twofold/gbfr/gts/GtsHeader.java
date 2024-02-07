package be.twofold.gbfr.gts;

import be.twofold.gbfr.util.*;

import java.nio.*;

public record GtsHeader(
    int magic,
    int version,
    byte[] uuid,
    int numLayers,
    long layersOffset,
    int numLevels,
    long levelsOffset,
    int tileWidth,
    int tileHeight,
    int tileBorder,
    int tileMaxSize,
    int numFlatTiles,
    long flatTilesOffset,
    int numReverseTiles,
    long reverseTilesOffset,
    int customPageSize,
    int numPageFiles,
    long pagesFilesOffset,
    int metaSize,
    long metaOffset,
    int numParameterBlocks,
    long parameterBlocksOffset,
    long thumbnailOffset
) {
    public static GtsHeader read(ByteBuffer buffer) {
        int magic = buffer.getInt();
        int version = buffer.getInt();
        buffer.getInt(); // unknown
        byte[] uuid = IOUtils.readBytes(buffer, 16);
        int numLayers = buffer.getInt();
        long layersOffset = buffer.getLong();
        int numLevels = buffer.getInt();
        long levelsOffset = buffer.getLong();
        int tileWidth = buffer.getInt();
        int tileHeight = buffer.getInt();
        var tileBorder = buffer.getInt();
        var tileMaxSize = buffer.getInt();
        int numFlatTiles = buffer.getInt();
        long flatTilesOffset = buffer.getLong();
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        int numReverseTiles = buffer.getInt();
        long reverseTilesOffset = buffer.getLong();
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        buffer.getInt(); // unknown
        int customPageSize = buffer.getInt();
        int numPageFiles = buffer.getInt();
        long pagesFilesOffset = buffer.getLong();
        int metaSize = buffer.getInt();
        long metaOffset = buffer.getLong();
        int numParameterBlocks = buffer.getInt();
        long parameterBlocksOffset = buffer.getLong();
        long thumbnailOffset = buffer.getLong();

        return new GtsHeader(
            magic,
            version,
            uuid,
            numLayers,
            layersOffset,
            numLevels,
            levelsOffset,
            tileWidth,
            tileHeight,
            tileBorder,
            tileMaxSize,
            numFlatTiles,
            flatTilesOffset,
            numReverseTiles,
            reverseTilesOffset,
            customPageSize,
            numPageFiles,
            pagesFilesOffset,
            metaSize,
            metaOffset,
            numParameterBlocks,
            parameterBlocksOffset,
            thumbnailOffset
        );
    }
}
