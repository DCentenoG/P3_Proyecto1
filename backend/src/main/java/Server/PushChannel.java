package Server;

import Protocol.MessageCodec;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/* Conexión de solo salida hacia un cliente. send() está sincronizado: varios hilos pueden hacer broadcast a la vez. */
final class PushChannel {

    private final Socket socket;
    private final DataOutputStream out;

    PushChannel(Socket socket, DataOutputStream out) {
        this.socket = socket;
        this.out = out;
    }

    synchronized void send(byte[] frame) throws IOException {
        MessageCodec.writeFrame(out, frame);
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // ya estaba cerrada
        }
    }
}