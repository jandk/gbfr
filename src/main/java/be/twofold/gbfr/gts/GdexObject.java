package be.twofold.gbfr.gts;

import java.nio.*;
import java.util.*;

public final class GdexObject extends Gdex {
    private final List<Gdex> children;

    GdexObject(GdexType type, List<Gdex> children) {
        super(type);
        this.children = children;
    }

    public List<Gdex> get(GdexType type) {
        return children.stream()
            .filter(c -> c.getType() == type)
            .toList();
    }

    public Gdex getOne(GdexType type) {
        List<Gdex> list = get(type);
        if (list.size() != 1) {
            throw new IllegalStateException("Expected exactly one " + type + ", but got " + list.size());
        }
        return list.getFirst();
    }

    public List<Gdex> getChildren() {
        return children;
    }

    @Override
    public GdexObject asObject() {
        return this;
    }

    public static GdexObject read(GdexType type, ByteBuffer buffer) {
        List<Gdex> children = new ArrayList<>();
        while (buffer.hasRemaining()) {
            children.add(read(buffer));
        }
        return new GdexObject(type, children);
    }
}
