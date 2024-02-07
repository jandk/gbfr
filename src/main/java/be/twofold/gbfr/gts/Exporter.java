package be.twofold.gbfr.gts;

import be.twofold.gbfr.decoder.*;
import be.twofold.gbfr.encoder.*;
import be.twofold.gbfr.fastlz.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class Exporter {
    private static final BCDecoder decoder = new BC5UDecoder(false);

    private final byte[] decodeBuffer = new byte[100 * 1024];

    private final Gts gts;
    private final Function<String, ByteBuffer> reader;

    public Exporter(Gts gts, Function<String, ByteBuffer> reader) {
        this.gts = gts;
        this.reader = reader;
    }

    public void export() throws IOException {
        var basePath = gts.name().substring(0, gts.name().lastIndexOf('.') - 1);
        var textures = mapTextures();


        int levelIndex = 0;
        GtsLevelInfo level = gts.levels().get(levelIndex);

        int ltw = 20;
        int lth = 20;

        var totalWidth = getTileWidth() * ltw;
        var totalHeight = getTileHeight() * lth;
        var total = new byte[totalWidth * totalHeight * 4];

        for (int y = 0; y < lth; y++) {
            int yy = y * getTileHeight();
            for (int x = 0; x < ltw; x++) {
                int xx = x * getTileWidth() * 4;

                int tileIndex = gts.tileIndices()[levelIndex][y * ltw + x];
                byte[] tile = readTile(tileIndex);

                for (int ty = 0; ty < getTileHeight(); ty++) {
                    int totalOffset = (yy + ty) * totalWidth * 4 + xx;
                    int tileOffset = ty * getTileWidth() * 4;

                    System.arraycopy(tile, tileOffset, total, totalOffset, getTileWidth() * 4);
                }
            }
        }

        PngFormat format = new PngFormat(totalWidth, totalHeight, PngColorType.RgbAlpha);
        try (var out = new PngOutputStream(Files.newOutputStream(Path.of("output.png")), format)) {
            out.writeImage(total);
        }
    }

    private int getTileHeight() {
        return gts.header().tileHeight();
    }

    private int getTileWidth() {
        return gts.header().tileWidth();
    }

    private final Map<String, Gtp> gtpCache = new HashMap<>();

    private byte[] readTile(int tileIndex) {
        GtsFlatTile tile = gts.flatTiles().get(tileIndex & 0xffffff);
        GtsPageFile pageFile = gts.pageFiles().get(tile.pageIndex());
        var basePath = gts.name().substring(0, gts.name().lastIndexOf('.') - 1);
        var path = basePath + pageFile.name();
        var gtp = gtpCache.computeIfAbsent(path, p -> {
            ByteBuffer apply = reader.apply(p);
            return Gtp.read(apply);
        });

        var chunk = gtp.chunks()
            .get(tile.pageChunk())
            .get(tile.pageSubChunk());
        return decodeTile(chunk);
    }

    private byte[] decodeTile(GtpChunk chunk) {
        if (chunk.compression() == 9) {
            FastLz.decompress(chunk.data(), decodeBuffer);
        }

        var bcBuffer = Arrays.copyOf(decodeBuffer, getTileWidth() * getTileHeight());
        return decoder.decode(bcBuffer, getTileWidth(), getTileHeight());
    }

    private List<Texture> mapTextures() {
        return gts.meta().asObject()
            .getOne(GdexType.ATLS).asObject()
            .getOne(GdexType.TXTS).asObject()
            .get(GdexType.TXTR).stream()
            .map(gdex -> mapTexture(gdex.asObject()))
            .toList();
    }

    private Texture mapTexture(GdexObject object) {
        var name = object.getOne(GdexType.NAME).asString();
        var width = object.getOne(GdexType.WDTH).asShort();
        var height = object.getOne(GdexType.HGHT).asShort();
        var offsetX = object.getOne(GdexType.XXXX).asShort();
        var offsetY = object.getOne(GdexType.YYYY).asShort();
        var address = object.getOne(GdexType.ADDR).asString();
        var srgb = object.getOne(GdexType.SRGB).asIntArray();
        var thumbnail = object.getOne(GdexType.THMB).asGUIDArray();
        return new Texture(name, width, height, offsetX, offsetY, address, srgb, thumbnail);
    }
}
