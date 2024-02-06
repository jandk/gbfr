package be.twofold.gbfr.gts;

import java.nio.*;
import java.util.*;
import java.util.function.*;

public final class Exporter {
    private final Gts gts;
    private final Function<String, ByteBuffer> reader;

    public Exporter(Gts gts, Function<String, ByteBuffer> reader) {
        this.gts = gts;
        this.reader = reader;
    }

    public void export() {
        var textures = mapTextures();
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
