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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Contrato unico entre el Frontend y el Backend: todas las operaciones que la
 * capa de Presentacion puede pedirle al sistema.
 * <p>
 * Es la pieza central de la arquitectura distribuida del Proyecto 2. Tiene dos
 * implementaciones:
 * <ul>
 *   <li>{@code Service.LocalService}, en el modulo backend, que ejecuta la
 *       operacion de verdad contra la logica de dominio y la persistencia.</li>
 *   <li>El Proxy del modulo frontend, que empaqueta la llamada, la manda por
 *       socket al Backend y devuelve lo que este responda.</li>
 * </ul>
 * Los Controladores reciben un {@code IService} por constructor y <b>no saben
 * cual de las dos tienen enfrente</b>. Ese es exactamente el patron Proxy que
 * exige el enunciado: una clase que representa localmente al Service remoto.
 * <p>
 * <b>Reglas de esta interfaz:</b>
 * <ul>
 *   <li>Vive en {@code common}, el unico modulo que ven los otros dos.</li>
 *   <li>Toda operacion lanza {@link ServiceException} con un mensaje listo para
 *       mostrarle al usuario. Una llamada remota siempre puede fallar, asi que
 *       ninguna se declara sin excepcion.</li>
 *   <li>Las entidades se identifican por su id, nunca por referencia: lo que el
 *       Frontend tiene en la mano es una copia de lo que mando el Backend.</li>
 *   <li>No se declara ningun metodo con sobrecarga. Un nombre de metodo es la
 *       clave con la que el protocolo encontrara la operacion al otro lado del
 *       socket, asi que dos operaciones no pueden compartir nombre.</li>
 * </ul>
 */
public interface IService {

    //--- Autenticacion ---

    /** Usuario con ese id y clave, o {@code null} si las credenciales no son validas. */
    User login(int id, String password) throws ServiceException;

    /** Cambia la clave del usuario, verificando primero la actual. */
    void changePassword(int userId, String currentPassword, String newPassword) throws ServiceException;

    /**
     * Cierra la sesion del usuario autenticado en esta conexion: el servidor deja de
     * enviarle avisos dirigidos a el. La conexion sigue abierta para el proximo login.
     */
    void logout() throws ServiceException;
    //--- Funcionarios ---

    List<Employee> listEmployees() throws ServiceException;

    Employee findEmployeeById(int id) throws ServiceException;

    Employee findEmployeeByName(String name) throws ServiceException;

    /** Da de alta un funcionario. El telefono llega como texto, tal como lo escribio el usuario. */
    Employee addEmployee(String name, String phoneText) throws ServiceException;

    void updateEmployee(int id, String newName, String phoneText) throws ServiceException;

    void removeEmployee(String name, int phoneNumber) throws ServiceException;

    //--- Categorias de recursos ---

    List<ResourceCategory> listCategories() throws ServiceException;

    ResourceCategory findCategoryByDescription(String description) throws ServiceException;

    ResourceCategory addCategory(String description) throws ServiceException;

    void updateCategory(String id, String currentDescription, String newDescription) throws ServiceException;

    void removeCategory(String id, String description) throws ServiceException;

    //--- Recursos ---

    List<Resource> listResourcesByCategory(String categoryDescription) throws ServiceException;

    Resource addResource(String categoryDescription, String idText, String description) throws ServiceException;

    /**
     * Edita un recurso y, si {@code destinationCategoryId} es distinto de
     * {@code sourceCategoryId}, ademas lo mueve de categoria.
     */
    void updateResource(String sourceCategoryId, int resourceId, String destinationCategoryId,
                        String idText, String newDescription) throws ServiceException;

    void removeResource(String categoryDescription, int id, String description) throws ServiceException;

    void transferResource(String sourceCategoryId, int resourceId, String destinationCategoryId)
            throws ServiceException;

    //--- Reservas del funcionario ---

    List<Reservation> listReservations(int employeeId) throws ServiceException;

    /**
     * Crea una solicitud de reserva "En revision". Las categorias se indican por
     * su descripcion, que es lo que el funcionario eligio en el formulario.
     */
    Reservation createReservation(int employeeId, String activity, LocalDate date,
                                  LocalTime startTime, LocalTime endTime,
                                  List<String> requiredCategoryDescriptions) throws ServiceException;

    void editReservation(int employeeId, int reservationId, String activity, LocalDate date,
                         LocalTime startTime, LocalTime endTime,
                         List<String> requiredCategoryDescriptions) throws ServiceException;

    void cancelReservation(int employeeId, int reservationId) throws ServiceException;

    //--- Solicitudes (administrador) ---

    /** Solicitudes pendientes, ya ordenadas: no vistas primero, la mas reciente arriba. */
    List<PendingRequest> listPendingRequests() throws ServiceException;

    void markRequestAsSeen(int employeeId, int reservationId) throws ServiceException;

    /** Por cada categoria requerida por la solicitud, los recursos libres en su fecha y horario. */
    Map<ResourceCategory, List<Resource>> availableResourcesFor(int employeeId, int reservationId)
            throws ServiceException;

    void approveRequest(int employeeId, int reservationId, List<Resource> chosenResources)
            throws ServiceException;

    void rejectRequest(int employeeId, int reservationId, String reason) throws ServiceException;

    //--- Calendarizacion y estadisticas ---

    /** Franjas ocupadas de cada recurso ese dia. {@code categoryDescription} null = todas las categorias. */
    List<ResourceSlot> getResourceSchedule(LocalDate date, String categoryDescription) throws ServiceException;

    /** Reservas aceptadas en el rango, una por reserva (no una por recurso). */
    List<ReservationSummary> getActivitySchedule(LocalDate from, LocalDate to) throws ServiceException;

    /** Cuantas veces se uso un recurso de cada categoria en el rango. */
    List<StatRow> getResourceStatistics(LocalDate from, LocalDate to) throws ServiceException;

    /** Cuantas actividades hubo en cada semana del rango. */
    List<StatRow> getWeeklyStatistics(LocalDate from, LocalDate to) throws ServiceException;

    //--- Inteligencia artificial ---

    /**
     * Interpreta una frase en lenguaje natural y devuelve los datos de reserva
     * que logro extraer, para precargar el formulario. Lo que la IA no pueda
     * determinar queda vacio: nunca inventa un valor, y la validacion real la
     * sigue haciendo el resto de esta interfaz.
     * <p>
     * Corre en el Backend, no en el Frontend: es logica, y asi la clave de la
     * API vive en un unico servidor y no en cada maquina cliente.
     */
    ExtractedReservationData extractReservationFromPhrase(String phrase) throws ServiceException;
}
