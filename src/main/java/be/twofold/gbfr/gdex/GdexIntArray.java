package be.twofold.gbfr.gdex;

public final class GdexIntArray extends Gdex {
    private final int[] values;

    public GdexIntArray(GdexType type, int[] values) {
        super(type);
        this.values = values;
    }

    public int[] getValue() {
        return values;
    }

    @Override
    public int[] asIntArray() {
        return values;
    }
}
