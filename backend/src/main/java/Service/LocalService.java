package Service;

import Dto.PendingRequest;
import Dto.ReservationSummary;
import Dto.ResourceSlot;
import Dto.StatRow;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;

import Persistence.ConnectionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Implementacion real de {@link IService}: la que ejecuta las operaciones
 * contra la logica de dominio y la persistencia.
 * <p>
 * Es una fachada delgada. La logica sigue repartida entre {@link UserService},
 * {@link ResourceService} y {@link ReservationService}, que comparten la misma
 * instancia de {@link Service} para trabajar siempre sobre los mismos datos en
 * memoria. Esta clase solo junta las tres en la unica interfaz que ve el
 * Frontend, porque el enunciado pide que el Proxy represente a UN Service
 * remoto, no a tres.
 * <p>
 * Cuando exista la capa de sockets, es esta clase la que el {@code ClientHandler}
 * del servidor va a invocar por cada peticion que llegue. Mientras tanto, el
 * Frontend la usa directamente y la aplicacion sigue corriendo en un solo
 * proceso.
 */
public class LocalService implements IService {

    private final Service service;
    private final UserService userService;
    private final ResourceService resourceService;
    private final ReservationService reservationService;
    private final AIReservationExtractionService aiService;

    public LocalService(Service service) {
        this.service = service;
        this.userService = new UserService(service);
        this.resourceService = new ResourceService(service);
        this.reservationService = new ReservationService(service);
        this.aiService = new AIReservationExtractionService();
    }

    /** Acceso a la fachada de persistencia, para el arranque del servidor (carga y datos de prueba). */
    public Service getService() {
        return service;
    }

    //--- Arranque ---

    /*
    Crea el servicio con la base de datos lista para usarse: lee la
    configuracion de db/database.properties, carga el estado completo del
    sistema y siembra un administrador y un par de registros si la base esta
    vacia (primera ejecucion).

    La llama ServerMain al arrancar. Es la unica puerta de entrada a la
    persistencia: ni el Frontend ni el resto del codigo saben que hay detras.
    */
    public static LocalService bootstrapFromDatabase() throws ServiceException {
        Service service = new Service(ConnectionManager.fromProperties());
        service.load();
        LocalService localService = new LocalService(service);
        localService.seedDemoDataIfEmpty();
        return localService;
    }

    /*
    Datos minimos para poder entrar la primera vez. Se escriben por los mismos
    DAOs que el resto del sistema, no con sentencias aparte, para que no haya dos
    caminos distintos de escritura que puedan divergir.
    */
    private void seedDemoDataIfEmpty() throws ServiceException {
        if (!service.getUsers().getUsers().isEmpty()) {
            return;
        }
        userService.addAdmin("admin");                               // ID 1 - clave "admin"
        userService.addEmployee("Funcionario de prueba", "88889999"); // ID 2 - clave "2"
        resourceService.addCategory("Salas de reunion");
        resourceService.addCategory("Equipo audiovisual");

        // Se avisa con detalle porque este sembrado crea credenciales NUEVAS, que no
        // son las de los datos de prueba del Proyecto 1. Sin este mensaje es facil
        // creer que la base perdio informacion, cuando en realidad nunca la tuvo.
        System.out.println();
        System.out.println("=====================================================================");
        System.out.println(" La base estaba VACIA. Se sembraron datos minimos para poder entrar:");
        System.out.println("     administrador   id 1   clave \"admin\"");
        System.out.println("     funcionario     id 2   clave \"2\"");
        System.out.println();
        System.out.println(" Si lo que queria era recuperar los datos de src/Data/system.xml,");
        System.out.println(" detenga el servidor y ejecute:");
        System.out.println("     ImportarXmlAOracle --reemplazar");
        System.out.println("=====================================================================");
        System.out.println();
    }

    //--- Autenticacion ---

    @Override
    public User login(int id, String password) throws ServiceException {
        return userService.login(id, password);
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword) throws ServiceException {
        userService.changePassword(userId, currentPassword, newPassword);
    }

    @Override
    public void logout() {
        // Sin estado propio: quien esta conectado lo lleva el servidor de sockets.
    }

    //--- Funcionarios ---

    @Override
    public List<Employee> listEmployees() {
        return userService.listEmployees();
    }

    @Override
    public Employee findEmployeeById(int id) throws ServiceException {
        return userService.findEmployeeById(id);
    }

    @Override
    public Employee findEmployeeByName(String name) throws ServiceException {
        return userService.findEmployeeByName(name);
    }

    @Override
    public Employee addEmployee(String name, String phoneText) throws ServiceException {
        return userService.addEmployee(name, phoneText);
    }

    @Override
    public void updateEmployee(int id, String newName, String phoneText) throws ServiceException {
        userService.updateEmployee(id, newName, phoneText);
    }

    @Override
    public void removeEmployee(String name, int phoneNumber) throws ServiceException {
        userService.removeEmployee(name, phoneNumber);
    }

    //--- Categorias ---

    @Override
    public List<ResourceCategory> listCategories() {
        return resourceService.listCategories();
    }

    @Override
    public ResourceCategory findCategoryByDescription(String description) throws ServiceException {
        return resourceService.findCategoryByDescription(description);
    }

    @Override
    public ResourceCategory addCategory(String description) throws ServiceException {
        return resourceService.addCategory(description);
    }

    @Override
    public void updateCategory(String id, String currentDescription, String newDescription)
            throws ServiceException {
        resourceService.updateCategory(id, currentDescription, newDescription);
    }

    @Override
    public void removeCategory(String id, String description) throws ServiceException {
        resourceService.removeCategory(id, description);
    }

    //--- Recursos ---

    @Override
    public List<Resource> listResourcesByCategory(String categoryDescription) throws ServiceException {
        return resourceService.listResourcesByCategory(categoryDescription);
    }

    @Override
    public Resource addResource(String categoryDescription, String idText, String description)
            throws ServiceException {
        return resourceService.addResource(categoryDescription, idText, description);
    }

    @Override
    public void updateResource(String sourceCategoryId, int resourceId, String destinationCategoryId,
                               String idText, String newDescription) throws ServiceException {
        resourceService.updateResource(sourceCategoryId, resourceId, destinationCategoryId, idText, newDescription);
    }

    @Override
    public void removeResource(String categoryDescription, int id, String description) throws ServiceException {
        resourceService.removeResource(categoryDescription, id, description);
    }

    @Override
    public void transferResource(String sourceCategoryId, int resourceId, String destinationCategoryId)
            throws ServiceException {
        resourceService.transferResource(sourceCategoryId, resourceId, destinationCategoryId);
    }

    //--- Reservas ---

    @Override
    public List<Reservation> listReservations(int employeeId) throws ServiceException {
        return reservationService.listReservations(employeeId);
    }

    @Override
    public Reservation createReservation(int employeeId, String activity, LocalDate date,
                                         LocalTime startTime, LocalTime endTime,
                                         List<String> requiredCategoryDescriptions) throws ServiceException {
        return reservationService.createReservation(employeeId, activity, date, startTime, endTime,
                requiredCategoryDescriptions);
    }

    @Override
    public void editReservation(int employeeId, int reservationId, String activity, LocalDate date,
                                LocalTime startTime, LocalTime endTime,
                                List<String> requiredCategoryDescriptions) throws ServiceException {
        reservationService.editReservation(employeeId, reservationId, activity, date, startTime, endTime,
                requiredCategoryDescriptions);
    }

    @Override
    public void cancelReservation(int employeeId, int reservationId) throws ServiceException {
        reservationService.cancelReservation(employeeId, reservationId);
    }

    //--- Solicitudes ---

    @Override
    public List<PendingRequest> listPendingRequests() {
        return reservationService.listPendingRequests();
    }

    @Override
    public void markRequestAsSeen(int employeeId, int reservationId) throws ServiceException {
        reservationService.markRequestAsSeen(employeeId, reservationId);
    }

    @Override
    public Map<ResourceCategory, List<Resource>> availableResourcesFor(int employeeId, int reservationId)
            throws ServiceException {
        return reservationService.availableResourcesFor(employeeId, reservationId);
    }

    @Override
    public void approveRequest(int employeeId, int reservationId, List<Resource> chosenResources)
            throws ServiceException {
        reservationService.approveRequest(employeeId, reservationId, chosenResources);
    }

    @Override
    public void rejectRequest(int employeeId, int reservationId, String reason) throws ServiceException {
        reservationService.rejectRequest(employeeId, reservationId, reason);
    }

    //--- Calendarizacion y estadisticas ---

    @Override
    public List<ResourceSlot> getResourceSchedule(LocalDate date, String categoryDescription)
            throws ServiceException {
        return reservationService.getResourceSchedule(date, categoryDescription);
    }

    @Override
    public List<ReservationSummary> getActivitySchedule(LocalDate from, LocalDate to) throws ServiceException {
        return reservationService.getActivitySchedule(from, to);
    }

    @Override
    public List<StatRow> getResourceStatistics(LocalDate from, LocalDate to) throws ServiceException {
        return reservationService.getResourceStatistics(from, to);
    }

    @Override
    public List<StatRow> getWeeklyStatistics(LocalDate from, LocalDate to) throws ServiceException {
        return reservationService.getWeeklyStatistics(from, to);
    }

    //--- Inteligencia artificial ---

    /*
    La excepcion propia de la IA se traduce aqui a ServiceException: IService
    declara un unico tipo de error para que el Frontend no tenga que conocer
    los detalles de como esta implementado el Backend.
    */
    @Override
    public ExtractedReservationData extractReservationFromPhrase(String phrase) throws ServiceException {
        try {
            return aiService.extractFromPhrase(phrase);
        } catch (AIExtractionException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
