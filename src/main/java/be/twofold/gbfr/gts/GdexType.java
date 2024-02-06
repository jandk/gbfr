package be.twofold.gbfr.gts;

import java.nio.*;

public enum GdexType {
    ADDR(0x52444441),
    ATLS(0x534c5441),
    BDPR(0x52504442),
    BINF(0x464e4942),
    BLDV(0x56444c42),
    BLKS(0x534b4c42),
    CMPW(0x57504d43),
    COMP(0x504d4f43),
    DATE(0x45544144),
    HGHT(0x54484748),
    INDX(0x58444e49),
    INFO(0x4f464e49),
    LAYR(0x5259414c),
    LINF(0x464e494c),
    LTMP(0x504d544c),
    MAJR(0x524a414d),
    META(0x4154454d),
    MINR(0x524e494d),
    NAME(0x454d414e),
    PROJ(0x4a4f5250),
    SRGB(0x42475253),
    THMB(0x424d4854),
    TILE(0x454c4954),
    TXTR(0x52545854),
    TXTS(0x53545854),
    TYPE(0x45505954),
    WDTH(0x48544457),
    XXXX(0x58585858),
    YYYY(0x59595959),
    ;

    private final int value;

    GdexType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static GdexType fromValue(int value) {
        for (GdexType type : GdexType.values()) {
            if (type.value == value) {
                return type;
            }
        }

        String s = new String(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
        throw new IllegalArgumentException("Unknown GdexType value: " + s + "(0x" + Integer.toHexString(value) + ")");
    }
}
