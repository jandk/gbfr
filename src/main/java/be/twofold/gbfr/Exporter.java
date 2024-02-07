package be.twofold.gbfr;

import be.twofold.gbfr.decoder.*;
import be.twofold.gbfr.encoder.*;
import be.twofold.gbfr.fastlz.*;
import be.twofold.gbfr.gts.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class Exporter {
    private final byte[] decodeBuffer = new byte[100 * 1024];

    private final Gts gts;
    private final Function<String, ByteBuffer> reader;

    public Exporter(Gts gts, Function<String, ByteBuffer> reader) {
        this.gts = gts;
        this.reader = reader;
    }

    public void export(Path outPath) throws IOException {
        Files.createDirectories(outPath);
        var textures = mapTextures();

        for (Texture texture : textures) {
//            if(!texture.name().contains("ec352ea4742ad2719335cce23c7a4ebdb1dede431c226c3eaa05ef269390d6a6")){
//                continue;
//            }
            int usableX = gts.header().tileWidth() - 16;
            int usableY = gts.header().tileHeight() - 16;

            int tileX = texture.offsetX() / usableX;
            int tileY = texture.offsetY() / usableY;
            int tileWidth = Math.divideExact(texture.width(), usableX);
            int tileHeight = Math.divideExact(texture.height(), usableY);


            if (tileWidth != 0 && tileHeight != 0) {
                for (int layer = 0; layer < gts.header().numLayers(); layer++) {
                    var outputPath = outPath.resolve(texture.name().trim() + "." + layer + ".png");
                    if (Files.exists(outputPath)) {
                        continue;
                    }

                    extractTile(tileX, tileY, tileWidth, tileHeight, outputPath, layer);
                }
            }
        }
    }

    private void extractTile(int tileX, int tileY, int tileWidth, int tileHeight, Path outputPath, int layer) throws IOException {
        var numLayers = gts.header().numLayers();
        var level = gts.levels().getFirst();

        var usableX = gts.header().tileWidth() - 16;
        var usableY = gts.header().tileHeight() - 16;

        var totalWidth = usableX * tileWidth;
        var totalHeight = usableY * tileHeight;
        var total = new byte[totalWidth * totalHeight * 4];

        for (var y = 0; y < tileHeight; y++) {
            var yy = y * usableY;
            for (var x = 0; x < tileWidth; x++) {
                var xx = x * usableX * 4;

                var tileIndex = gts.tileIndices()[0][(tileY + y) * level.tileWidth() * numLayers + (tileX + x) * numLayers + layer];
                var tile = readTile(tileIndex);

                for (var ty = 0; ty < usableY; ty++) {
                    var totalOffset = (yy + ty) * totalWidth * 4 + xx;
                    var tileOffset = ((ty + 8) * getTileWidth() + 8) * 4;

                    System.arraycopy(tile, tileOffset, total, totalOffset, usableX * 4);
                }
            }
        }

        var format = new PngFormat(totalWidth, totalHeight, PngColorType.RgbAlpha);
        try (var out = new PngOutputStream(Files.newOutputStream(outputPath), format)) {
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
        var tile = gts.flatTiles().get(tileIndex & 0xffffff);
        var pageFile = gts.pageFiles().get(tile.pageIndex());
        var basePath = gts.name().substring(0, gts.name().lastIndexOf('.') - 1);
        var path = basePath + pageFile.name();

        if (gtpCache.size() > 100) {
            gtpCache.clear();
        }
        var gtp = gtpCache.computeIfAbsent(path, p -> {
            var apply = reader.apply(p);
            return Gtp.read(apply);
        });

        if(gtp == null){
            return new byte[getTileWidth() * getTileHeight() * 4];
        }

        var chunk = gtp.chunks()
            .get(tile.pageChunk())
            .get(tile.pageSubChunk());
        return decodeTile(chunk);
    }

    private byte[] decodeTile(GtpChunk chunk) {
        if (chunk.data().length <= 4) {
            return new byte[getTileWidth() * getTileHeight() * 4];
        }
        if (chunk.compression() == 9) {
            int done = FastLz.decompress(chunk.data(), decodeBuffer);
            // System.out.println(done);
        }

        var parameterBlockIndex = findParameterBlockIndex(chunk);
        var bytes = gts.parameterBlockData()[parameterBlockIndex];

        var codec = bytes.length < 48 ? "BC7" : new String(bytes, 44, 3);
        var bcDecoder = switch (codec) {
            case "BC5" -> new BC5UDecoder(false);
            case "BC7" -> new BC7Decoder();
            default -> throw new IllegalArgumentException("Unknown codec: " + codec);
        };

        var bcBuffer = Arrays.copyOf(decodeBuffer, getTileWidth() * getTileHeight());
        return bcDecoder.decode(bcBuffer, getTileWidth(), getTileHeight());
    }

    private int findParameterBlockIndex(GtpChunk chunk) {
        var gtsParameterBlocks = gts.parameterBlocks();
        for (var i = 0; i < gtsParameterBlocks.size(); i++) {
            if (gtsParameterBlocks.get(i).id() == chunk.parameterId()) {
                return i;
            }
        }
        return -1;
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
        var width = Short.toUnsignedInt(object.getOne(GdexType.WDTH).asShort());
        var height = Short.toUnsignedInt(object.getOne(GdexType.HGHT).asShort());
        var offsetX = Short.toUnsignedInt(object.getOne(GdexType.XXXX).asShort());
        var offsetY = Short.toUnsignedInt(object.getOne(GdexType.YYYY).asShort());
        var address = object.getOne(GdexType.ADDR).asString();
        var srgb = object.getOne(GdexType.SRGB).asIntArray();
        var thumbnail = object.getOne(GdexType.THMB).asGUIDArray();
        return new Texture(name, width, height, offsetX, offsetY, address, srgb, thumbnail);
    }
}
