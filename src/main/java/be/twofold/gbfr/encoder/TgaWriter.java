package be.twofold.gbfr.encoder;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

public final class TgaWriter {
    public static void writeTga(Path path, byte[] data, int width, int height) {
        ByteBuffer header = ByteBuffer
            .allocate(18)
            .order(ByteOrder.LITTLE_ENDIAN);

        header.put((byte) 0); // idlength
        header.put((byte) 0); // colourmaptype
        header.put((byte) 2); // datatypecode
        header.putShort((short) 0); // colourmaporigin
        header.putShort((short) 0); // colourmaplength
        header.put((byte) 0); // colourmapdepth
        header.putShort((short) 0); // x_origin
        header.putShort((short) 0); // y_origin
        header.putShort((short) width);
        header.putShort((short) height);
        header.put((byte) 32); // bitsperpixel
        header.put((byte) 0x20); // imagedescriptor
        header.flip();

        for (int i = 0; i < data.length; i += 4) {
            byte r = data[i];
            data[i] = data[i + 2];
            data[i + 2] = r;
        }

        try (var out = new FileOutputStream(path.toFile())) {
            out.write(header.array());
            out.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
