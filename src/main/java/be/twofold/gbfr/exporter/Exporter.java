package be.twofold.gbfr.exporter;

import be.twofold.gbfr.decoder.*;
import be.twofold.gbfr.encoder.*;
import be.twofold.gbfr.fastlz.*;
import be.twofold.gbfr.gdex.*;
import be.twofold.gbfr.gtp.*;
import be.twofold.gbfr.gts.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

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

        Map<String, Integer> fileNameCount = new HashMap<>();
        for (var texture : textures) {
//            if (!texture.name().equals("8bf26938a07d150fd35766b09560b70ea4671cdb02c8575fde8727e62ee475f3")) {
//                continue;
//            }
            var usableX = gts.header().tileWidth() - 16;
            var usableY = gts.header().tileHeight() - 16;

            var tileX = texture.offsetX() / usableX;
            var tileY = texture.offsetY() / usableY;
            var tileWidth = Math.divideExact(texture.width(), usableX);
            var tileHeight = Math.divideExact(texture.height(), usableY);

            if (tileWidth != 0 && tileHeight != 0) {
                for (var layer = 0; layer < gts.header().numLayers(); layer++) {
                    XmlTexture xmlTexture = texture.textures().get(layer);
                    var fileName = xmlTexture.src();
                    fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));

                    var outputPath = outPath.resolve(fileName + ".tga");
                    if (!Files.exists(outputPath.getParent())) {
                        Files.createDirectories(outputPath.getParent());
                    }
                    if (Files.exists(outputPath)) {
                        continue;
                    }

                    int tileStepX = Math.max(1, Math.divideExact(texture.width(), xmlTexture.width()));
                    int tileStepY = Math.max(1, Math.divideExact(texture.height(), xmlTexture.height()));

                    extractTile(tileX, tileY, tileWidth, tileHeight, tileStepX, tileStepY, outputPath, layer);
                }
            }
        }
    }

    private void extractTile(
        int tileX,
        int tileY,
        int tileWidth,
        int tileHeight,
        int tileStepX,
        int tileStepY,
        Path outputPath,
        int layer
    ) throws IOException {
        var numLayers = gts.header().numLayers();
        var level = gts.levels().getFirst();

        var usableX = gts.header().tileWidth() - 16;
        var usableY = gts.header().tileHeight() - 16;

        var numTilesX = Math.max(1, tileWidth / tileStepX);
        var numTilesY = Math.max(1, tileHeight / tileStepY);

        var totalWidth = usableX * numTilesX;
        var totalHeight = usableY * numTilesY;
        var total = new byte[totalWidth * totalHeight * 4];

        for (var y = 0; y < numTilesY; y++) {
            var yy = y * usableY;
            for (var x = 0; x < numTilesX; x++) {
                var xx = x * usableX * 4;

                var tileIndex = gts.tileIndices()[0][(tileY + ((tileStepY * y))) * level.tileWidth() * numLayers + (tileX + (tileStepX * x)) * numLayers + layer];
                var tile = readTile(tileIndex);

                for (var ty = 0; ty < usableY; ty++) {
                    var totalOffset = (yy + ty) * totalWidth * 4 + xx;
                    var tileOffset = ((ty + 8) * getTileWidth() + 8) * 4;

                    System.arraycopy(tile, tileOffset, total, totalOffset, usableX * 4);
                }
            }
        }

        TgaWriter.writeTga(outputPath, total, totalWidth, totalHeight);
//        var format = new PngFormat(totalWidth, totalHeight, PngColorType.RgbAlpha);
//        try (var out = new PngOutputStream(Files.newOutputStream(outputPath), format)) {
//            out.writeImage(total);
//        }
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

        if (gtp == null) {
            return new byte[getTileWidth() * getTileHeight() * 4];
        }

        var chunk = gtp.chunks()
            .get(tile.pageChunk())
            .get(tile.pageSubChunk());
        return decodeTile(chunk);
    }

    private byte[] decodeTile(GtpChunk chunk) {
        if (chunk.data().length == 2) {
            var result = new byte[getTileWidth() * getTileHeight() * 4];
            for (var i = 0; i < result.length; i += 4) {
                result[i] = chunk.data()[0];
                result[i + 1] = chunk.data()[1];
                result[i + 3] = (byte) 0xff;
            }
            return result;
        }
        if (chunk.data().length == 4) {
            var result = new byte[getTileWidth() * getTileHeight() * 4];
            for (var i = 0; i < result.length; i += 4) {
                result[i] = chunk.data()[0];
                result[i + 1] = chunk.data()[1];
                result[i + 2] = chunk.data()[2];
                result[i + 3] = chunk.data()[3];
            }
            return result;
        }
        if (chunk.compression() == 9) {
            var done = FastLz.decompress(chunk.data(), decodeBuffer);
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
        var assets = mapXml();

        return gts.meta().asObject()
            .getOne(GdexType.ATLS).asObject()
            .getOne(GdexType.TXTS).asObject()
            .get(GdexType.TXTR).stream()
            .map(gdex -> mapTexture(gdex.asObject(), assets))
            .toList();
    }

    private Texture mapTexture(GdexObject object, List<XmlAsset> assets) {
        var name = object.getOne(GdexType.NAME).asString().trim();
        var width = Short.toUnsignedInt(object.getOne(GdexType.WDTH).asShort());
        var height = Short.toUnsignedInt(object.getOne(GdexType.HGHT).asShort());
        var offsetX = Short.toUnsignedInt(object.getOne(GdexType.XXXX).asShort());
        var offsetY = Short.toUnsignedInt(object.getOne(GdexType.YYYY).asShort());
        var address = object.getOne(GdexType.ADDR).asString().trim();
        var srgb = object.getOne(GdexType.SRGB).asIntArray();
        var thumbnail = object.getOne(GdexType.THMB).asGUIDArray();

        var asset = assets.stream()
            .filter(a -> a.name().equals(name))
            .findFirst().orElseThrow();

        return new Texture(name, width, height, offsetX, offsetY, address, srgb, thumbnail, asset.textures());
    }

    private List<XmlAsset> mapXml() {
        var rawXml = gts.meta().asObject()
            .getOne(GdexType.PROJ).asString()
            .trim();

        try {
            var document = DocumentBuilderFactory
                .newDefaultInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(rawXml)));

            var root = document.getDocumentElement();
            var importedAssets = findElement(root, "ImportedAssets");
            return findElements(importedAssets, "Asset").stream()
                .map(this::mapAsset)
                .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XmlAsset mapAsset(Element asset) {
        var name = asset.getAttribute("Name");
        var guid = UUID.fromString(asset.getAttribute("GUID").substring(1, 37));
        var width = Integer.parseInt(asset.getAttribute("Width"));
        var height = Integer.parseInt(asset.getAttribute("Height"));

        var layers = findElement(asset, "Layers");
        var textures = findElements(layers, "Layer").stream()
            .map(this::mapLayer)
            .toList();

        return new XmlAsset(name, guid, width, height, textures);
    }

    private XmlTexture mapLayer(Element layer) {
        var textures = findElement(layer, "Textures");
        var textureList = findElements(textures, "Texture");
        if (textureList.size() != 1) {
            throw new RuntimeException("Expected 1 texture, got " + textureList.size());
        }

        var texture = textureList.get(0);
        var src = texture.getAttribute("Src");
        var subIndex = Integer.parseInt(texture.getAttribute("SubIndex"));
        var width = Integer.parseInt(texture.getAttribute("Width"));
        var height = Integer.parseInt(texture.getAttribute("Height"));
        var numChannels = Integer.parseInt(texture.getAttribute("NumChannels"));

        return new XmlTexture(src, subIndex, width, height, numChannels);
    }

    private Element findElement(Element element, String name) {
        return stream(element.getChildNodes())
            .filter(e -> e.getTagName().equals(name))
            .findFirst().orElseThrow();
    }

    private List<Element> findElements(Element element, String name) {
        return stream(element.getChildNodes())
            .filter(e -> e.getTagName().equals(name))
            .toList();
    }

    private static Stream<Element> stream(NodeList elements) {
        return IntStream.range(0, elements.getLength())
            .mapToObj(elements::item)
            .filter(Element.class::isInstance)
            .map(Element.class::cast);
    }
}
