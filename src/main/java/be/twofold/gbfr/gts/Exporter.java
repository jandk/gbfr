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
    private final byte[] decodeBuffer = new byte[100 * 1024];

    private final Gts gts;
    private final Function<String, ByteBuffer> reader;

    public Exporter(Gts gts, Function<String, ByteBuffer> reader) {
        this.gts = gts;
        this.reader = reader;
    }

    public void export() throws IOException {
        var destination = Path.of("/home/jan/output/");
        Files.createDirectories(destination);
        var textures = mapTextures();

        for (Texture texture : textures) {
            int tileX = texture.offsetX() / 128;
            int tileY = texture.offsetY() / 128;
            int tileWidth = Math.divideExact(texture.width(), 128);
            int tileHeight = Math.divideExact(texture.height(), 128);

            var outputPath = destination.resolve(texture.name().trim() + ".png");
            if(Files.exists(outputPath)) {
                continue;
            }

            if (tileWidth != 0 && tileHeight != 0) {
                extractTile(tileX, tileY, tileWidth, tileHeight, outputPath);
            }
        }
    }

    private void extractTile(int tileX, int tileY, int tileWidth, int tileHeight, Path outputPath) throws IOException {
        var numLayers = gts.header().numLayers();
        var level = gts.levels().getFirst();

        var tw = 128;
        var th = 128;

        var totalWidth = tw * tileWidth;
        var totalHeight = th * tileHeight;
        var total = new byte[totalWidth * totalHeight * 4];

        for (var y = 0; y < tileHeight; y++) {
            var yy = y * th;
            for (var x = 0; x < tileWidth; x++) {
                var xx = x * tw * 4;

                var tileIndex = gts.tileIndices()[0][(tileY + y) * level.tileWidth() * numLayers + (tileX + x) * numLayers];
                var tile = readTile(tileIndex);

                for (var ty = 0; ty < th; ty++) {
                    var totalOffset = (yy + ty) * totalWidth * 4 + xx;
                    var tileOffset = ((ty + 8) * getTileWidth() + 8) * 4;

                    System.arraycopy(tile, tileOffset, total, totalOffset, tw * 4);
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
        var gtp = gtpCache.computeIfAbsent(path, p -> {
            var apply = reader.apply(p);
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
