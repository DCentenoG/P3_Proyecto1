package Server;

import Dto.ReservationSummary;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import Model.exceptions.ReservationNotFoundException;
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

    /* A quien va el aviso: a un usuario concreto o, si targetUserId es null, a todos menos a quien lo origino. */
    private record Delivery(Notification notification, Integer targetUserId) {
    }

    /* Lo que sale de procesar una peticion: la respuesta y, si aplica, un aviso para otros clientes. */
    private record Outcome(byte[] reply, Delivery delivery) {
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
                // Fuera del candado: el envio escribe en sockets y no debe frenar a los demas hilos.
                deliver(outcome.delivery());
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

    private void deliver(Delivery delivery) {
        if (delivery == null) {
            return;
        }
        if (delivery.targetUserId() == null) {
            registry.broadcastExcept(sessionId, delivery.notification());
        } else {
            registry.sendToUser(delivery.targetUserId(), delivery.notification());
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
        Delivery delivery = null;
        if (response.isSuccessful()) {
            updateSession(request, response);
            delivery = deliveryFor(request, response);
        }
        try {
            return new Outcome(MessageCodec.encode(response), delivery);
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

    /* Un login correcto asocia la sesion con su usuario; un logout la desasocia. */
    private void updateSession(Request request, Response response) {
        switch (request.getMethod()) {
            case "login" -> {
                if (response.getPayload() instanceof User user) {
                    registry.bindUser(sessionId, user.getId());
                }
            }
            case "logout" -> registry.unbindUser(sessionId);
            default -> {
                // las demas operaciones no cambian de quien es la sesion
            }
        }
    }

    /* Aviso que corresponde a una operacion que acaba de salir bien, o null si no genera ninguno. */
    private Delivery deliveryFor(Request request, Response response) {
        try {
            return switch (request.getMethod()) {
                case "createReservation" -> newReservationDelivery(request, response);
                case "approveRequest" -> reviewDelivery(request, true);
                case "rejectRequest" -> reviewDelivery(request, false);
                default -> null;
            };
        } catch (ServiceException | ReservationNotFoundException | RuntimeException e) {
            System.err.println("No se pudo armar el aviso: " + e.getMessage());
            return null;
        }
    }

    /*
    Reserva nueva: se avisa a todos los demas clientes. Una reserva recien creada esta
    "En revision" y todavia no tiene recursos asignados, asi que en el lugar de los
    recursos se informan las categorias que se solicitaron.
    */
    private Delivery newReservationDelivery(Request request, Response response) throws ServiceException {
        if (!(response.getPayload() instanceof Reservation reservation)) {
            return null;
        }
        Employee employee = service.findEmployeeById((Integer) request.getArgs()[0]);
        List<String> categories = new ArrayList<>();
        for (ResourceCategory category : reservation.getRequiredCategories()) {
            categories.add(category.getDescription());
        }
        return new Delivery(Notification.newReservation(summaryOf(employee, reservation, categories)), null);
    }

    /* Solicitud revisada: se avisa solo al funcionario duenio de la reserva. */
    private Delivery reviewDelivery(Request request, boolean approved)
            throws ServiceException, ReservationNotFoundException {
        int employeeId = (Integer) request.getArgs()[0];
        int reservationId = (Integer) request.getArgs()[1];
        Employee employee = service.findEmployeeById(employeeId);
        Reservation reservation = employee.getReservationById(reservationId);

        if (approved) {
            List<String> resources = new ArrayList<>();
            for (Resource resource : reservation.getAssignedResources()) {
                resources.add(resource.getDescription());
            }
            return new Delivery(Notification.reservationApproved(summaryOf(employee, reservation, resources)),
                    employeeId);
        }
        return new Delivery(Notification.reservationRejected(summaryOf(employee, reservation, List.of()),
                reservation.getRejectionReason()), employeeId);
    }

    private static ReservationSummary summaryOf(Employee employee, Reservation reservation, List<String> details) {
        return new ReservationSummary(reservation.getId(), reservation.getActivity(), employee.getName(),
                reservation.getDate(), reservation.getStartTime(), reservation.getEndTime(), details);
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