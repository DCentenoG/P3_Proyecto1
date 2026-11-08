package Server;

import Protocol.Hello;
import Protocol.MessageCodec;
import Protocol.Response;
import Service.IService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/*
Acepta conexiones y le da un hilo a cada una. El primer mensaje de cada conexion
(Hello) indica si es un canal de peticiones o un canal de notificaciones.
*/
public final class ReservationServer {

    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;

    private final int port;
    private final IService service;
    private final Object stateLock = new Object();
    private final Map<String, Method> operations = indexOperations();
    private final ClientRegistry registry = new ClientRegistry();

    public ReservationServer(int port, IService service) {
        this.port = port;
        this.service = service;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor escuchando en el puerto " + port);
            int counter = 0;
            while (true) {
                Socket client = serverSocket.accept();
                int number = ++counter;
                new Thread(() -> serve(client), "conexion-" + number).start();
            }
        }
    }

    private void serve(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            Object first = MessageCodec.decode(MessageCodec.readFrame(in));
            socket.setSoTimeout(0);

            if (!(first instanceof Hello hello)) {
                MessageCodec.writeFrame(out, MessageCodec.encode(Response.failure("Saludo invalido.")));
                socket.close();
                return;
            }

            if (hello.getChannel() == Hello.Channel.REQUEST) {
                String sessionId = registry.openSession();
                MessageCodec.writeFrame(out, MessageCodec.encode(Response.success(sessionId)));
                new ClientHandler(socket, in, out, sessionId, service, operations, stateLock, registry).run();
            } else {
                servePush(socket, in, out, hello.getSessionId());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Conexion rechazada: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {
                // nada que hacer
            }
        }
    }

    /* Canal de notificaciones: el cliente no envia nada; solo se espera a que se desconecte. */
    private void servePush(Socket socket, DataInputStream in, DataOutputStream out, String sessionId)
            throws IOException {
        PushChannel channel = new PushChannel(socket, out);
        if (!registry.isOpen(sessionId)) {
            channel.send(MessageCodec.encode(Response.failure("Sesion desconocida.")));
            channel.close();
            return;
        }
        // La respuesta del saludo se envia ANTES de registrar el canal, para que ninguna
        // notificacion pueda llegar antes que ella.
        channel.send(MessageCodec.encode(Response.success(null)));
        if (!registry.attachPush(sessionId, channel)) {
            channel.close();
            return;
        }
        System.out.println("Canal de notificaciones abierto para la sesion " + sessionId);
        try {
            while (in.read() != -1) {
                // ignorado
            }
        } catch (IOException ignored) {
            // el cliente se desconecto
        } finally {
            registry.detachPush(sessionId, channel);
            channel.close();
        }
    }

    /* Nombre de operacion -> metodo de IService. Solo estas operaciones se pueden pedir por la red. */
    private static Map<String, Method> indexOperations() {
        Map<String, Method> index = new HashMap<>();
        for (Method method : IService.class.getMethods()) {
            if (index.put(method.getName(), method) != null) {
                throw new IllegalStateException("IService no puede tener sobrecargas: " + method.getName());
            }
        }
        return index;
    }
}