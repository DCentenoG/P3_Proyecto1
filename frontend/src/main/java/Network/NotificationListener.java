package Network;

import Protocol.Hello;
import Protocol.MessageCodec;
import Protocol.Notification;
import Protocol.Response;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/*
Segunda conexion al Backend, solo para recibir notificaciones. Un hilo propio las
lee y se las entrega al handler. IMPORTANTE: ese hilo NO es el EDT de Swing; quien
use el handler debe pasar a la interfaz con SwingUtilities.invokeLater.
*/
public final class NotificationListener implements AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final Socket socket = new Socket();
    private final DataInputStream in;
    private final Consumer<Notification> handler;
    private volatile boolean closed;

    public NotificationListener(String host, int port, String sessionId, Consumer<Notification> handler)
            throws IOException {
        this.handler = handler;
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        try {
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            MessageCodec.writeFrame(out, MessageCodec.encode(Hello.push(sessionId)));
            Object reply = MessageCodec.decode(MessageCodec.readFrame(in));
            if (!(reply instanceof Response response) || !response.isSuccessful()) {
                throw new IOException("El servidor rechazo el canal de notificaciones.");
            }
            socket.setSoTimeout(0);   // despues del saludo se espera sin limite de tiempo
        } catch (IOException | ClassNotFoundException e) {
            close();
            throw (e instanceof IOException io) ? io : new IOException(e);
        }

        Thread thread = new Thread(this::listen, "notificaciones");
        thread.setDaemon(true);
        thread.start();
    }

    private void listen() {
        try {
            while (!closed) {
                Object message = MessageCodec.decode(MessageCodec.readFrame(in));
                if (message instanceof Notification notification) {
                    handler.accept(notification);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!closed) {
                System.err.println("Canal de notificaciones cerrado: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // ya estaba cerrado
        }
    }
}