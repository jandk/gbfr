package be.twofold.gbfr.gdex;

import java.util.*;

public final class GdexGUIDArray extends Gdex {
    private final UUID[] values;

    public GdexGUIDArray(GdexType type, UUID[] values) {
        super(type);
        this.values = values;
    }

    public UUID[] getValues() {
        return values;
    }

    @Override
    public UUID[] asGUIDArray() {
        return values;
    }
}
