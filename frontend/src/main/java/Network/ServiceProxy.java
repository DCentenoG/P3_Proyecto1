package Network;

import Dto.PendingRequest;
import Dto.ReservationSummary;
import Dto.ResourceSlot;
import Dto.StatRow;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import Protocol.MessageCodec;
import Protocol.Request;
import Protocol.Response;
import Service.ExtractedReservationData;
import Service.IService;
import Service.ServiceException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/*
Patron Proxy: representa localmente al Service del Backend. Cada metodo arma un
Request con el nombre de la operacion y sus argumentos, lo envia por el socket y
espera la Response. Los Controladores lo reciben como IService y no saben que
detras hay una red.
*/
public final class ServiceProxy implements IService, AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    /* Mayor que el timeout de la IA en el Backend (20 s) para no cortar esa llamada. */
    private static final int READ_TIMEOUT_MS = 45_000;

    private final Socket socket = new Socket();
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Object ioLock = new Object();
    private boolean closed;

    public ServiceProxy(String host, int port) throws IOException {
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    /*
    Una peticion a la vez por conexion (la EDT y un SwingWorker pueden llamar al
    mismo tiempo). Si algo falla a mitad de camino la conexion queda
    desincronizada, asi que se cierra.
    Nota: un unico argumento null llega como arreglo null (los metodos de IService
    nunca lo reciben, pero por eso no se debe cambiar a un solo argumento nullable).
    */
    private Object call(String method, Object... args) throws ServiceException {
        Request request = new Request(method, args);
        synchronized (ioLock) {
            if (closed) {
                throw new ServiceException("La conexion con el servidor esta cerrada.");
            }
            Object reply;
            try {
                MessageCodec.writeFrame(out, MessageCodec.encode(request));
                reply = MessageCodec.decode(MessageCodec.readFrame(in));
            } catch (IOException | ClassNotFoundException e) {
                close();
                throw new ServiceException("Se perdio la comunicacion con el servidor: " + e.getMessage(), e);
            }
            if (!(reply instanceof Response response)) {
                close();
                throw new ServiceException("El servidor envio una respuesta no reconocida.");
            }
            if (!response.isSuccessful()) {
                throw new ServiceException(response.getErrorMessage());
            }
            return response.getPayload();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T callAs(String method, Object... args) throws ServiceException {
        return (T) call(method, args);
    }

    @Override
    public void close() {
        synchronized (ioLock) {
            closed = true;
            try {
                socket.close();
            } catch (IOException ignored) {
                // ya esta cerrada o no hay nada que hacer
            }
        }
    }

    //--- Autenticacion ---

    @Override
    public User login(int id, String password) throws ServiceException {
        return callAs("login", id, password);
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword) throws ServiceException {
        call("changePassword", userId, currentPassword, newPassword);
    }

    //--- Funcionarios ---

    @Override
    public List<Employee> listEmployees() throws ServiceException {
        return callAs("listEmployees");
    }

    @Override
    public Employee findEmployeeById(int id) throws ServiceException {
        return callAs("findEmployeeById", id);
    }

    @Override
    public Employee findEmployeeByName(String name) throws ServiceException {
        return callAs("findEmployeeByName", name);
    }

    @Override
    public Employee addEmployee(String name, String phoneText) throws ServiceException {
        return callAs("addEmployee", name, phoneText);
    }

    @Override
    public void updateEmployee(int id, String newName, String phoneText) throws ServiceException {
        call("updateEmployee", id, newName, phoneText);
    }

    @Override
    public void removeEmployee(String name, int phoneNumber) throws ServiceException {
        call("removeEmployee", name, phoneNumber);
    }

    //--- Categorias ---

    @Override
    public List<ResourceCategory> listCategories() throws ServiceException {
        return callAs("listCategories");
    }

    @Override
    public ResourceCategory findCategoryByDescription(String description) throws ServiceException {
        return callAs("findCategoryByDescription", description);
    }

    @Override
    public ResourceCategory addCategory(String description) throws ServiceException {
        return callAs("addCategory", description);
    }

    @Override
    public void updateCategory(String id, String currentDescription, String newDescription)
            throws ServiceException {
        call("updateCategory", id, currentDescription, newDescription);
    }

    @Override
    public void removeCategory(String id, String description) throws ServiceException {
        call("removeCategory", id, description);
    }

    //--- Recursos ---

    @Override
    public List<Resource> listResourcesByCategory(String categoryDescription) throws ServiceException {
        return callAs("listResourcesByCategory", categoryDescription);
    }

    @Override
    public Resource addResource(String categoryDescription, String idText, String description)
            throws ServiceException {
        return callAs("addResource", categoryDescription, idText, description);
    }

    @Override
    public void updateResource(String sourceCategoryId, int resourceId, String destinationCategoryId,
                               String idText, String newDescription) throws ServiceException {
        call("updateResource", sourceCategoryId, resourceId, destinationCategoryId, idText, newDescription);
    }

    @Override
    public void removeResource(String categoryDescription, int id, String description) throws ServiceException {
        call("removeResource", categoryDescription, id, description);
    }

    @Override
    public void transferResource(String sourceCategoryId, int resourceId, String destinationCategoryId)
            throws ServiceException {
        call("transferResource", sourceCategoryId, resourceId, destinationCategoryId);
    }

    //--- Reservas ---

    @Override
    public List<Reservation> listReservations(int employeeId) throws ServiceException {
        return callAs("listReservations", employeeId);
    }

    @Override
    public Reservation createReservation(int employeeId, String activity, LocalDate date,
                                         LocalTime startTime, LocalTime endTime,
                                         List<String> requiredCategoryDescriptions) throws ServiceException {
        return callAs("createReservation", employeeId, activity, date, startTime, endTime,
                requiredCategoryDescriptions);
    }

    @Override
    public void editReservation(int employeeId, int reservationId, String activity, LocalDate date,
                                LocalTime startTime, LocalTime endTime,
                                List<String> requiredCategoryDescriptions) throws ServiceException {
        call("editReservation", employeeId, reservationId, activity, date, startTime, endTime,
                requiredCategoryDescriptions);
    }

    @Override
    public void cancelReservation(int employeeId, int reservationId) throws ServiceException {
        call("cancelReservation", employeeId, reservationId);
    }

    //--- Solicitudes ---

    @Override
    public List<PendingRequest> listPendingRequests() throws ServiceException {
        return callAs("listPendingRequests");
    }

    @Override
    public void markRequestAsSeen(int employeeId, int reservationId) throws ServiceException {
        call("markRequestAsSeen", employeeId, reservationId);
    }

    @Override
    public Map<ResourceCategory, List<Resource>> availableResourcesFor(int employeeId, int reservationId)
            throws ServiceException {
        return callAs("availableResourcesFor", employeeId, reservationId);
    }

    @Override
    public void approveRequest(int employeeId, int reservationId, List<Resource> chosenResources)
            throws ServiceException {
        call("approveRequest", employeeId, reservationId, chosenResources);
    }

    @Override
    public void rejectRequest(int employeeId, int reservationId, String reason) throws ServiceException {
        call("rejectRequest", employeeId, reservationId, reason);
    }

    //--- Calendarizacion y estadisticas ---

    @Override
    public List<ResourceSlot> getResourceSchedule(LocalDate date, String categoryDescription)
            throws ServiceException {
        return callAs("getResourceSchedule", date, categoryDescription);
    }

    @Override
    public List<ReservationSummary> getActivitySchedule(LocalDate from, LocalDate to) throws ServiceException {
        return callAs("getActivitySchedule", from, to);
    }

    @Override
    public List<StatRow> getResourceStatistics(LocalDate from, LocalDate to) throws ServiceException {
        return callAs("getResourceStatistics", from, to);
    }

    @Override
    public List<StatRow> getWeeklyStatistics(LocalDate from, LocalDate to) throws ServiceException {
        return callAs("getWeeklyStatistics", from, to);
    }

    //--- Inteligencia artificial ---

    @Override
    public ExtractedReservationData extractReservationFromPhrase(String phrase) throws ServiceException {
        return callAs("extractReservationFromPhrase", phrase);
    }
}