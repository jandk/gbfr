package be.twofold.gbfr.fastlz;

public final class FastLz {
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;

    private FastLz() {
    }

    public static int decompress(final byte[] in, final byte[] out) {
        final int level = (in[0] >> 5) + 1;
        if (level != LEVEL_1 && level != LEVEL_2) {
            throw new IllegalStateException("Invalid level: " + level);
        }

        int ip = 0;
        int op = 0;
        long ctrl = in[ip++] & 31;

        boolean loop = true;
        do {
            int ref = op;
            long len = ctrl >> 5;
            long ofs = (ctrl & 31) << 8;

            if (ctrl >= 32) {
                len--;
                ref -= ofs;

                int code;
                if (len == 6) {
                    if (level == LEVEL_1) {
                        len += Byte.toUnsignedInt(in[ip++]);
                    } else {
                        do {
                            code = Byte.toUnsignedInt(in[ip++]);
                            len += code;
                        } while (code == 255);
                    }
                }
                if (level == LEVEL_1) {
                    ref -= Byte.toUnsignedInt(in[ip++]);
                } else {
                    code = Byte.toUnsignedInt(in[ip++]);
                    ref -= code;

                    if (code == 255 && ofs == 31 << 8) {
                        ofs = Byte.toUnsignedInt(in[ip++]) << 8;
                        ofs += Byte.toUnsignedInt(in[ip++]);

                        ref = (int) (op - ofs - 8191);
                    }
                }

                if (op + len + 3 > out.length) {
                    return 0;
                }

                if (ref - 1 < 0) {
                    return 0;
                }

                if (ip < in.length) {
                    ctrl = Byte.toUnsignedInt(in[ip++]);
                } else {
                    loop = false;
                }

                if (ref == op) {
                    byte b = out[ref - 1];
                    out[op++] = b;
                    out[op++] = b;
                    out[op++] = b;
                    while (len != 0) {
                        out[op++] = b;
                        --len;
                    }
                } else {
                    ref--;

                    out[op++] = out[ref++];
                    out[op++] = out[ref++];
                    out[op++] = out[ref++];

                    while (len != 0) {
                        out[op++] = out[ref++];
                        --len;
                    }
                }
            } else {
                ctrl++;
                if (op + ctrl > out.length) {
                    return 0;
                }
                if (ip + ctrl > in.length) {
                    return 0;
                }

                out[op++] = in[ip++];
                for (--ctrl; ctrl != 0; ctrl--) {
                    out[op++] = in[ip++];
                }

                loop = ip < in.length;
                if (loop) {
                    ctrl = Byte.toUnsignedInt(in[ip++]);
                }
            }
        } while (loop);

        return op;
    }
}
