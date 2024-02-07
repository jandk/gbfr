package be.twofold.gbfr.gts;

import be.twofold.gbfr.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.stream.*;

public record Gts(
    String name,
    GtsHeader header,
    Gdex meta,
    List<GtsLevelInfo> levels,
    List<GtsPageFile> pageFiles,
    List<GtsFlatTile> flatTiles,
    int[][] tileIndices,
    List<GtsParameterBlock> parameterBlocks,
    byte[][] parameterBlockData
) {
    public static Gts read(String name, ByteBuffer buffer) throws IOException {
        var header = GtsHeader.read(buffer);

        var meta = Gdex.read(buffer.slice(
            Math.toIntExact(header.metaOffset()),
            header.metaSize()
        ).order(ByteOrder.LITTLE_ENDIAN));

        buffer.position(Math.toIntExact(header.levelsOffset()));
        var levels = IOUtils.readStructs(buffer, header.numLevels(), GtsLevelInfo::read);

        var tileIndices = new int[levels.size()][];
        for (int i = 0; i < levels.size(); i++) {
            GtsLevelInfo level = levels.get(i);
            buffer.position(level.indicesOffset());
            tileIndices[i] = IOUtils.readIntArray(buffer, level.tileWidth() * level.tileHeight() * header.numLayers());
        }

        buffer.position(Math.toIntExact(header.pagesFilesOffset()));
        var pageFiles = IOUtils.readStructs(buffer, header.numPageFiles(), GtsPageFile::read);

        buffer.position(Math.toIntExact(header.flatTilesOffset()));
        var flatTiles = IOUtils.readStructs(buffer, header.numFlatTiles(), GtsFlatTile::read);

        buffer.position(Math.toIntExact(header.parameterBlocksOffset()));
        var parameterBlocks = IOUtils.readStructs(buffer, header.numParameterBlocks(), GtsParameterBlock::read);

        var parameterBlockData = new byte[parameterBlocks.size()][];
        for (int i = 0; i < parameterBlocks.size(); i++) {
            GtsParameterBlock parameterBlock = parameterBlocks.get(i);
            buffer.position(Math.toIntExact(parameterBlock.offset()));
            parameterBlockData[i] = IOUtils.readBytes(buffer, parameterBlock.size());
        }

        // Files.write(Path.of("/home/jan/flatTiles.csv"), toCsv(GtsFlatTile.class, flatTiles).getBytes());

        var group = Arrays.stream(parameterBlockData)
            .collect(Collectors.groupingBy(a -> a.length, Collectors.toList()));

        Set<ByteArray> unique = new HashSet<>();
        for (List<byte[]> value : group.values()) {
            for (byte[] bytes : value) {
                unique.add(new ByteArray(bytes));
            }
        }

        return new Gts(name, header, meta, levels, pageFiles, flatTiles, tileIndices, parameterBlocks, parameterBlockData);
    }

    private static final class ByteArray{
        private final byte[] array;

        private ByteArray(byte[] array) {
            this.array = array;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ByteArray byteArray
                && Arrays.equals(array, byteArray.array);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    public static <T> String toCsv(Class<T> clazz, List<T> list) {
        RecordComponent[] components = clazz.getRecordComponents();
        StringBuilder builder = new StringBuilder();
        for (RecordComponent component : components) {
            builder.append(component.getName()).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("\n");
        for (T t : list) {
            for (RecordComponent component : components) {
                try {
                    Field field = clazz.getDeclaredField(component.getName());
                    field.setAccessible(true);
                    builder.append(field.get(t)).append(",");
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Gts(" + name + ")";
    }
}
