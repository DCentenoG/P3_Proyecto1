package Server;

import Service.IService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/* Acepta conexiones en un puerto y le da un hilo a cada cliente. */
public final class ReservationServer {

    private final int port;
    private final IService service;
    private final Object stateLock = new Object();
    private final Map<String, Method> operations = indexOperations();

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
                Thread thread = new Thread(new ClientHandler(client, service, operations, stateLock),
                        "cliente-" + (++counter));
                thread.start();
            }
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