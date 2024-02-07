package be.twofold.gbfr.gdex;

import be.twofold.gbfr.util.*;

import java.nio.*;
import java.util.*;

public sealed abstract class Gdex permits GdexObject, GdexString, GdexShort, GdexIntArray, GdexGUIDArray {
    private final GdexType type;

    public final boolean isObject() {
        return this instanceof GdexObject;
    }

    public final boolean isString() {
        return this instanceof GdexString;
    }

    public final boolean isShort() {
        return this instanceof GdexShort;
    }

    public final boolean isIntArray() {
        return this instanceof GdexIntArray;
    }

    public final boolean isGUIDArray() {
        return this instanceof GdexGUIDArray;
    }

    public GdexObject asObject() {
        throw new ClassCastException(getClass().getSimpleName());
    }

    public String asString() {
        throw new ClassCastException(getClass().getSimpleName());
    }

    public short asShort() {
        throw new ClassCastException(getClass().getSimpleName());
    }

    public int[] asIntArray() {
        throw new ClassCastException(getClass().getSimpleName());
    }

    public UUID[] asGUIDArray() {
        throw new ClassCastException(getClass().getSimpleName());
    }

    Gdex(GdexType type) {
        this.type = type;
    }

    public GdexType getType() {
        return type;
    }

    public static Gdex read(ByteBuffer buffer) {
        var type = GdexType.fromValue(buffer.getInt());
        var fieldType = GdexTagType.fromValue(buffer.get());
        var flags = buffer.get();

        int size = (flags & 1) != 0
            ? Math.toIntExact(IOUtils.readInt48(buffer))
            : buffer.getShort();

        var position = buffer.position();
        Gdex result = switch (fieldType) {
            case Object -> {
                ByteBuffer slice = buffer
                    .slice(buffer.position(), size)
                    .order(ByteOrder.LITTLE_ENDIAN);
                yield GdexObject.read(type, slice);
            }
            case String -> new GdexString(type, IOUtils.readWString(buffer, size));
            case Short -> new GdexShort(type, buffer.getShort());
            case IntArray -> new GdexIntArray(type, IOUtils.readIntArray(buffer, Math.divideExact(size, 4)));
            case GUIDArray -> new GdexGUIDArray(type, IOUtils.readGUIDArray(buffer, Math.divideExact(size, 16)));
        };

        buffer.position((position + size + 3) & ~3);
        return result;
    }

    @Override
    public String toString() {
        return type + "(" + getClass().getSimpleName() + ")";
    }
}
