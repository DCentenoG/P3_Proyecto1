package Server;

import Protocol.MessageCodec;
import Protocol.Request;
import Protocol.Response;
import Service.IService;
import Service.ServiceException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/*
Atiende a UN cliente conectado, en su propio hilo: lee una peticion, la ejecuta
contra el IService real y escribe la respuesta, hasta que el cliente se desconecta.
*/
final class ClientHandler implements Runnable {

    /* No tocan los datos compartidos (la IA solo hace una llamada HTTP): no deben frenar a los demas clientes. */
    private static final Set<String> LOCK_FREE_OPERATIONS = Set.of("extractReservationFromPhrase");

    private final Socket socket;
    private final IService service;
    private final Map<String, Method> operations;
    private final Object stateLock;

    ClientHandler(Socket socket, IService service, Map<String, Method> operations, Object stateLock) {
        this.socket = socket;
        this.service = service;
        this.operations = operations;
        this.stateLock = stateLock;
    }

    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Cliente conectado: " + client);
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            while (true) {
                byte[] frame;
                try {
                    frame = MessageCodec.readFrame(in);
                } catch (EOFException clientLeft) {
                    break;
                }
                MessageCodec.writeFrame(out, process(frame));
            }
        } catch (IOException e) {
            System.out.println("Conexion con " + client + " terminada: " + e.getMessage());
        } finally {
            System.out.println("Cliente desconectado: " + client);
        }
    }

    private byte[] process(byte[] frame) {
        Object message;
        try {
            message = MessageCodec.decode(frame);
        } catch (IOException | ClassNotFoundException e) {
            return encode(Response.failure("No se pudo interpretar la peticion."));
        }
        if (!(message instanceof Request request)) {
            return encode(Response.failure("Mensaje no reconocido."));
        }

        Method operation = operations.get(request.getMethod());
        if (operation == null) {
            return encode(Response.failure("Operacion desconocida: " + request.getMethod()));
        }
        if (LOCK_FREE_OPERATIONS.contains(request.getMethod())) {
            return invokeAndEncode(operation, request);
        }
        // Ejecutar Y serializar dentro del candado: la respuesta contiene objetos vivos del
        // servidor, que otro hilo podria modificar mientras se estan escribiendo.
        synchronized (stateLock) {
            return invokeAndEncode(operation, request);
        }
    }

    private byte[] invokeAndEncode(Method operation, Request request) {
        Response response = invoke(operation, request);
        try {
            return MessageCodec.encode(response);
        } catch (IOException e) {
            e.printStackTrace();
            return encode(Response.failure("El servidor no pudo preparar la respuesta."));
        }
    }

    private Response invoke(Method operation, Request request) {
        try {
            return Response.success(operation.invoke(service, request.getArgs()));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ServiceException serviceException) {
                return Response.failure(serviceException.getMessage());
            }
            e.getCause().printStackTrace();
            return Response.failure("Error interno del servidor. Intente de nuevo.");
        } catch (IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
            return Response.failure("Peticion invalida: " + request.getMethod());
        }
    }

    /* Para respuestas de error, que solo llevan texto y siempre se pueden serializar. */
    private static byte[] encode(Response response) {
        try {
            return MessageCodec.encode(response);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}