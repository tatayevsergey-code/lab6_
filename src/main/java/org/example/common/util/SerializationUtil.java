package org.example.common.util;

import java.io.*;
import java.nio.ByteBuffer;

public class SerializationUtil {
    public static ByteBuffer serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();

        byte[] bytes = baos.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocate(4 + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    public static Object deserialize(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    public static void serializeToStream(Object obj, OutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] bytes = baos.toByteArray();

        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
        out.flush();
    }

    public static Object deserializeFromStream(InputStream in) throws IOException, ClassNotFoundException {

        byte[] lenBytes = new byte[4];
        int read = 0;
        while (read < 4) {
            int r = in.read(lenBytes, read, 4 - read);
            if (r == -1) throw new IOException("Соединение закрыто");
            read += r;
        }
        int length = ByteBuffer.wrap(lenBytes).getInt();

        byte[] data = new byte[length];
        read = 0;
        while (read < length) {
            int r = in.read(data, read, length - read);
            if (r == -1) throw new IOException("Соединение закрыто");
            read += r;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }
}
