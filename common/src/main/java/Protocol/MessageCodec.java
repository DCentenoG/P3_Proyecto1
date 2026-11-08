package Protocol;

import java.io.*;

/*
Convierte mensajes (Request/Response) a bytes y viceversa, y los envia con un
prefijo de longitud ("framing"): primero 4 bytes con el tamano, luego el contenido.

Cada mensaje usa un ObjectOutputStream NUEVO. Por eso nunca se acumula el cache
por identidad de objetos ya enviados (el bug del reset() olvidado), y si un objeto
no se puede serializar el error ocurre ANTES de escribir nada en el socket, asi que
la conexion no queda a medias.
*/
public final class MessageCodec {

    private static final int MAX_FRAME_SIZE = 32 * 1024 * 1024;

    private MessageCodec() {
    }

    public static byte[] encode(Object message) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(buffer)) {
            objects.writeObject(message);
        }
        return buffer.toByteArray();
    }

    public static Object decode(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objects = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return objects.readObject();
        }
    }

    public static void writeFrame(DataOutputStream out, byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    /** Lanza EOFException si el otro extremo cerro la conexion entre dos mensajes. */
    public static byte[] readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("Tamano de mensaje invalido: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }
}
