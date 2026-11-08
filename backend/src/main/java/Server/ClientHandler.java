package Server;

import Dto.ReservationSummary;
import Model.Employee;
import Model.Reservation;
import Model.ResourceCategory;
import Protocol.MessageCodec;
import Protocol.Notification;
import Protocol.Request;
import Protocol.Response;
import Service.IService;
import Service.ServiceException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Atiende el canal de peticiones de UN cliente, en su propio hilo. */
final class ClientHandler implements Runnable {

    /* No tocan los datos compartidos: no deben frenar a los demas clientes. */
    private static final Set<String> LOCK_FREE_OPERATIONS = Set.of("extractReservationFromPhrase");

    /* Lo que sale de procesar una peticion: la respuesta y, si aplica, un aviso para los demas. */
    private record Outcome(byte[] reply, Notification notification) {
    }

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final String sessionId;
    private final IService service;
    private final Map<String, Method> operations;
    private final Object stateLock;
    private final ClientRegistry registry;

    ClientHandler(Socket socket, DataInputStream in, DataOutputStream out, String sessionId, IService service,
                  Map<String, Method> operations, Object stateLock, ClientRegistry registry) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.sessionId = sessionId;
        this.service = service;
        this.operations = operations;
        this.stateLock = stateLock;
        this.registry = registry;
    }

    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Cliente conectado: " + client + " (sesion " + sessionId + ")");
        try {
            while (true) {
                byte[] frame;
                try {
                    frame = MessageCodec.readFrame(in);
                } catch (EOFException clientLeft) {
                    break;
                }
                Outcome outcome = process(frame);
                MessageCodec.writeFrame(out, outcome.reply());
                // Fuera del candado: el broadcast escribe en sockets y no debe frenar a los demas hilos.
                if (outcome.notification() != null) {
                    registry.broadcastExcept(sessionId, outcome.notification());
                }
            }
        } catch (IOException e) {
            System.out.println("Conexion con " + client + " terminada: " + e.getMessage());
        } finally {
            registry.closeSession(sessionId);
            try {
                socket.close();
            } catch (IOException ignored) {
                // ya estaba cerrado
            }
            System.out.println("Cliente desconectado: " + client);
        }
    }

    private Outcome process(byte[] frame) {
        Object message;
        try {
            message = MessageCodec.decode(frame);
        } catch (IOException | ClassNotFoundException e) {
            return failure("No se pudo interpretar la peticion.");
        }
        if (!(message instanceof Request request)) {
            return failure("Mensaje no reconocido.");
        }

        Method operation = operations.get(request.getMethod());
        if (operation == null) {
            return failure("Operacion desconocida: " + request.getMethod());
        }
        if (LOCK_FREE_OPERATIONS.contains(request.getMethod())) {
            return invokeAndEncode(operation, request);
        }
        // Ejecutar, armar el aviso Y serializar dentro del candado: todo toca objetos vivos del servidor.
        synchronized (stateLock) {
            return invokeAndEncode(operation, request);
        }
    }

    private Outcome invokeAndEncode(Method operation, Request request) {
        Response response = invoke(operation, request);
        Notification notification = response.isSuccessful() ? alertFor(request, response) : null;
        try {
            return new Outcome(MessageCodec.encode(response), notification);
        } catch (IOException e) {
            e.printStackTrace();
            return failure("El servidor no pudo preparar la respuesta.");
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

    /*
    Aviso para los demas clientes cuando se registra una reserva nueva. Una reserva
    recien creada esta "En revision" y todavia no tiene recursos asignados, asi que
    en el lugar de los recursos se informan las categorias que se solicitaron.
    */
    private Notification alertFor(Request request, Response response) {
        if (!"createReservation".equals(request.getMethod())
                || !(response.getPayload() instanceof Reservation reservation)) {
            return null;
        }
        try {
            Employee employee = service.findEmployeeById((Integer) request.getArgs()[0]);
            List<String> categories = new ArrayList<>();
            for (ResourceCategory category : reservation.getRequiredCategories()) {
                categories.add(category.getDescription());
            }
            return Notification.newReservation(new ReservationSummary(reservation.getId(),
                    reservation.getActivity(), employee.getName(), reservation.getDate(),
                    reservation.getStartTime(), reservation.getEndTime(), categories));
        } catch (ServiceException | RuntimeException e) {
            System.err.println("No se pudo armar la alerta: " + e.getMessage());
            return null;
        }
    }

    /* Respuesta de error, que solo lleva texto y siempre se puede serializar. */
    private static Outcome failure(String message) {
        try {
            return new Outcome(MessageCodec.encode(Response.failure(message)), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}