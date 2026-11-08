package Service;

import Dto.PendingRequest;
import Dto.ReservationSummary;
import Dto.ResourceSlot;
import Dto.StatRow;
import Model.Employee;
import Model.RequestNotification;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.EmployeeNotFoundException;
import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ReservationNotFoundException;
import Model.exceptions.ResourceUnavailableException;
import Model.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
Servicio de dominio de reservas (funcionalidad 2 del enunciado, la de mayor
peso) y del flujo de solicitudes que revisa el administrador:

- El funcionario crea, edita y cancela sus reservas. Crear o editar deja la
  reserva "En revision" (una solicitud pendiente, sin recursos asignados);
  cancelar la deja "Cancelada" (ya no se borra).
- El administrador lista las solicitudes pendientes (con su notificacion
  vista / no vista), las marca como vistas, consulta que recursos estan libres
  para cada una y la aprueba (eligiendo un recurso por categoria requerida) o la
  rechaza (con un motivo obligatorio).
- Solo las reservas "Aceptadas" ocupan recursos y horarios en el sistema.

Las reglas de cada cambio de estado viven en el Model (Reservation/Employee);
aqui se validan los datos de entrada, se traducen las excepciones del Model a
ServiceException con un mensaje para el usuario y se guarda el XML.
*/
public class ReservationService {

    private final Service service;

    /*
    Par interno (funcionario, reserva) que se usa solo para poder ordenar las
    solicitudes por su notificacion antes de convertirlas al DTO que sale de
    esta capa. No se expone: hacia afuera viaja Dto.PendingRequest.
    */
    private record PendingPair(Employee employee, Reservation reservation) {
    }

    public ReservationService(Service service) {
        this.service = service;
    }

    public List<Reservation> listReservations(int employeeId) throws ServiceException {
        return requireEmployee(employeeId).getReservations();
    }

    //--- Funcionario: crear, editar y cancelar ---

    /*
    Crea una solicitud de reserva "En revision" con las categorias requeridas, que
    queda de inmediato en las solicitudes del administrador. Se valida que cada
    categoria tenga al menos un recurso libre en ese horario; si alguna no tiene, no
    se crea nada y se informan todas las categorias sin disponibilidad.
    */
    public Reservation createReservation(int employeeId, String activity, LocalDate date,
                                         LocalTime startTime, LocalTime endTime,
                                         List<String> requiredCategoryDescriptions) throws ServiceException {

        Employee employee = requireEmployee(employeeId);
        validateReservationData(activity, date, startTime, endTime, requiredCategoryDescriptions);
        List<ResourceCategory> categories = resolveCategories(requiredCategoryDescriptions);

        try {
            employee.tryBook(activity.trim(), date, startTime, endTime, categories, service.getUsers(),
                    LocalDateTime.now());
        } catch (ResourceUnavailableException e) {
            throw unavailable(e);
        }

        // tryBook agrega la reserva al final de la lista del funcionario.
        Reservation reservation = employee.getReservations().get(employee.getReservations().size() - 1);
        // Un INSERT en RESERVA mas sus categorias requeridas, todo en una transaccion.
        service.insertReservation(employeeId, reservation);
        return reservation;
    }

    /*
    Edita una reserva del funcionario (en cualquier estado). Aplica las mismas
    validaciones que createReservation y, al guardarse, la reserva vuelve a "En
    revision": libera sus recursos y debe aprobarse de nuevo. Es "todo o nada": si
    algo falla, la reserva queda exactamente como estaba.
    */
    public void editReservation(int employeeId, int reservationId, String activity, LocalDate date,
                                LocalTime startTime, LocalTime endTime,
                                List<String> requiredCategoryDescriptions) throws ServiceException {

        Employee employee = requireEmployee(employeeId);
        validateReservationData(activity, date, startTime, endTime, requiredCategoryDescriptions);
        List<ResourceCategory> categories = resolveCategories(requiredCategoryDescriptions);

        try {
            employee.tryEdit(reservationId, activity.trim(), date, startTime, endTime, categories, service.getUsers(),
                    LocalDateTime.now());
        } catch (ReservationNotFoundException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (ResourceUnavailableException e) {
            throw unavailable(e);
        }

        // tryEdit modifica el MISMO objeto, asi que el id no cambio: es un UPDATE.
        service.updateReservation(requireReservationOf(employee, reservationId));
    }

    /*
    Cancela una reserva del funcionario: queda "Cancelada" (ya no se borra) y libera
    sus recursos. No se puede cancelar una reserva Rechazada, una ya Cancelada ni una
    cuya fecha ya paso.
    */
    public void cancelReservation(int employeeId, int reservationId) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        Reservation reservation = requireReservationOf(employee, reservationId);

        // Primero las reglas del estado (p. ej. una Rechazada nunca se cancela), para
        // que el mensaje explique el motivo real; luego la regla de la fecha.
        try {
            reservation.ensureCancellable();
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        if (reservation.getDate().isBefore(LocalDate.now())) {
            throw new ServiceException("No es posible cancelar una reserva que ya paso.");
        }

        try {
            employee.cancelReservation(reservationId);
        } catch (ReservationNotFoundException | InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        service.updateReservation(reservation);
    }

    //--- Administrador: revision de solicitudes ---

    /*
    Solicitudes "En revision" de todos los funcionarios, en el orden en que se
    muestran en el sidebar del administrador:
      1. Primero las NO vistas, de la que entro mas recientemente a la mas antigua
         (una solicitud nueva entrante queda arriba de todas).
      2. Despues las ya vistas, de la marcada como vista mas recientemente a la mas
         antigua (al marcar una, queda primera entre las vistas, justo debajo de la
         ultima no vista).
    Las de archivos anteriores a este flujo, sin fecha de entrada, quedan al final
    de su grupo.
    */
    public List<PendingRequest> listPendingRequests() {
        List<PendingPair> pending = new ArrayList<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (reservation.getState() == State.IN_REVIEW) {
                    pending.add(new PendingPair(employee, reservation));
                }
            }
        }
        pending.sort(NOTIFICATION_ORDER);

        // El orden ya quedo resuelto aqui: la Vista pinta la lista tal cual.
        List<PendingRequest> requests = new ArrayList<>();
        for (PendingPair pair : pending) {
            requests.add(new PendingRequest(pair.employee().getId(),
                    summaryOf(pair.employee(), pair.reservation()),
                    pair.reservation().getNotification().isSeen()));
        }
        return requests;
    }

    /*
    Marca como vista la notificacion de la solicitud. NO cambia el estado de la
    reserva (sigue "En revision"); solo cambia su lugar en el sidebar y deja de
    contarse como nueva. Una solicitud ya marcada como vista no se puede marcar otra vez.
    */
    public void markRequestAsSeen(int employeeId, int reservationId) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        Reservation request = requireReservationOf(employee, reservationId);

        try {
            request.markRequestSeen(LocalDateTime.now());
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        service.updateReservation(request);
    }

    /*
    Para cada categoria requerida por la solicitud (en su orden), los recursos de esa
    categoria que estan libres en su fecha/horario, es decir, que no estan asignados
    a otra reserva ya Aceptada que choque en horario. Son las opciones que el
    administrador puede elegir al aprobarla (una lista vacia: no hay disponibilidad).
    */
    public Map<ResourceCategory, List<Resource>> availableResourcesFor(int employeeId, int reservationId)
            throws ServiceException {
        Reservation request = requireReservationOf(requireEmployee(employeeId), reservationId);
        Map<ResourceCategory, List<Resource>> available = new LinkedHashMap<>();
        for (ResourceCategory category : request.getRequiredCategories()) {
            available.put(category, request.freeResourcesOf(category, service.getUsers(), null));
        }
        return available;
    }

    /*
    Aprueba la solicitud asignandole "chosenResources" (un recurso libre por cada
    categoria requerida). La reserva pasa a "Aceptada", deja de estar en las
    solicitudes y empieza a ocupar esos recursos y su horario en todo el sistema.
    */
    public void approveRequest(int employeeId, int reservationId, List<Resource> chosenResources)
            throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        Reservation request = requireReservationOf(employee, reservationId);
        if (chosenResources == null) {
            throw new ServiceException("Debe elegir un recurso por cada categoria requerida.");
        }

        // NUEVO: los recursos que llegan por la red son copias; se cambian por los reales del servidor.
        List<Resource> canonicalResources = new ArrayList<>();
        for (Resource received : chosenResources) {
            canonicalResources.add(resolveResource(received));
        }

        try {
            request.approve(canonicalResources, service.getUsers());   // antes: chosenResources
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (ResourceUnavailableException e) {
            throw new ServiceException("El recurso elegido ya no esta disponible en ese horario para la(s) "
                    + "categoria(s): " + String.join(", ", e.getUnavailableCategories()), e);
        }

        // Cambio el estado y se llenaron los recursos asignados: el UPDATE reescribe
        // tambien RESERVA_RECURSO.
        service.updateReservation(request);
    }

    /*
    Rechaza la solicitud con un motivo, que es obligatorio. La reserva pasa a
    "Rechazada", deja de estar en las solicitudes y el funcionario puede consultar
    el motivo (y editarla para volver a solicitarla).
    */
    public void rejectRequest(int employeeId, int reservationId, String reason) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        Reservation request = requireReservationOf(employee, reservationId);
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("Debe indicar el motivo del rechazo.");
        }

        try {
            request.reject(reason);
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        service.updateReservation(request);
    }

    //--- Consultas de calendarizacion y estadisticas ---

    /*
    Estas cuatro operaciones son las que antes resolvian los Controladores
    recorriendo en memoria todos los funcionarios y todas sus reservas
    (StatisticsViewListener, CalendarViewListener y ActivitySchedulingViewListener).

    Son reglas de negocio -- "solo cuentan las reservas Aceptadas", "la semana va
    de domingo a sabado" -- asi que su lugar es esta capa, no la de Presentacion.
    Ademas, con el Backend remoto no hay alternativa: traerse por el socket todos
    los funcionarios con todas sus reservas para que el Frontend sume o filtre
    seria inviable. Cuando la persistencia pase a Oracle, cada una de estas
    consultas se convierte en una sentencia SQL.

    Todas devuelven DTOs del paquete Dto, nunca entidades del dominio.
    */

    /*
    Franjas ocupadas de cada recurso en un dia (pantalla Calendarizacion). Una
    reserva con varios recursos produce una franja por recurso. Si
    categoryDescription no es null, solo se devuelven los recursos de esa
    categoria.
    */
    public List<ResourceSlot> getResourceSchedule(LocalDate date, String categoryDescription)
            throws ServiceException {
        if (date == null) {
            throw new ServiceException("Debe indicar la fecha de la calendarizacion.");
        }
        List<ResourceSlot> slots = new ArrayList<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                // Solo las reservas Aceptadas ocupan recursos y se muestran de forma global.
                if (!reservation.isAccepted() || !date.equals(reservation.getDate())) {
                    continue;
                }
                ReservationSummary summary = summaryOf(employee, reservation);
                for (Resource resource : reservation.getAssignedResources()) {
                    ResourceCategory category = resource.getResourceCategoryReference();
                    if (categoryDescription != null
                            && (category == null || !categoryDescription.equals(category.getDescription()))) {
                        continue;
                    }
                    slots.add(new ResourceSlot(resource.getCategoryId(), resource.getId(),
                            resource.getDescription(), summary));
                }
            }
        }
        return slots;
    }

    /*
    Reservas Aceptadas cuya fecha cae dentro del rango [from, to], ambos
    inclusive (pantalla Actividades, que muestra una semana completa). A
    diferencia de getResourceSchedule, aqui cada reserva aparece una sola vez,
    sin importar cuantos recursos tenga asignados.
    */
    public List<ReservationSummary> getActivitySchedule(LocalDate from, LocalDate to) throws ServiceException {
        if (from == null || to == null) {
            throw new ServiceException("Debe indicar el rango de fechas de las actividades.");
        }
        List<ReservationSummary> activities = new ArrayList<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (!reservation.isAccepted() || !isWithin(reservation.getDate(), from, to)) {
                    continue;
                }
                activities.add(summaryOf(employee, reservation));
            }
        }
        return activities;
    }

    /*
    Cuantas veces se uso un recurso de cada categoria, entre las reservas
    Aceptadas del rango. Una reserva con tres recursos suma uno a la categoria de
    cada uno. Ordenado alfabeticamente por categoria.
    */
    public List<StatRow> getResourceStatistics(LocalDate from, LocalDate to) throws ServiceException {
        Map<String, Integer> counts = new TreeMap<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (!reservation.isAccepted() || !isWithin(reservation.getDate(), from, to)) {
                    continue;
                }
                for (Resource resource : reservation.getAssignedResources()) {
                    ResourceCategory category = resource.getResourceCategoryReference();
                    if (category != null) {
                        counts.merge(category.getDescription(), 1, Integer::sum);
                    }
                }
            }
        }
        return toStatRows(counts);
    }

    /*
    Cuantas actividades hubo en cada semana del rango, entre las reservas
    Aceptadas. La etiqueta es "Semana <numero>-<anio>" segun la norma ISO-8601.
    Ordenado alfabeticamente por etiqueta, igual que antes.
    */
    public List<StatRow> getWeeklyStatistics(LocalDate from, LocalDate to) throws ServiceException {
        Map<String, Integer> counts = new TreeMap<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (!reservation.isAccepted() || !isWithin(reservation.getDate(), from, to)) {
                    continue;
                }
                LocalDate date = reservation.getDate();
                counts.merge("Semana " + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + "-" + date.getYear(),
                        1, Integer::sum);
            }
        }
        return toStatRows(counts);
    }

    //--- Utilidades ---

    /* Arma el resumen de una reserva: lo unico que la capa de Presentacion necesita ver de ella. */
    private static ReservationSummary summaryOf(Employee employee, Reservation reservation) {
        List<String> resourceDescriptions = new ArrayList<>();
        for (Resource resource : reservation.getAssignedResources()) {
            resourceDescriptions.add(resource.getDescription());
        }
        return new ReservationSummary(reservation.getId(), reservation.getActivity(), employee.getName(),
                reservation.getDate(), reservation.getStartTime(), reservation.getEndTime(),
                resourceDescriptions);
    }

    /* Un limite null significa "sin limite por ese lado". */
    private static boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static List<StatRow> toStatRows(Map<String, Integer> counts) {
        List<StatRow> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            rows.add(new StatRow(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    /* Orden del sidebar: no vistas (mas reciente primero), luego vistas (marcada mas recientemente primero). */
    private static final Comparator<PendingPair> NOTIFICATION_ORDER = Comparator
            .comparing((PendingPair pair) -> pair.reservation().getNotification().isSeen())
            .thenComparing(pair -> sortTimeOf(pair.reservation().getNotification()),
                    Comparator.nullsLast(Comparator.reverseOrder()));

    /* Momento que ordena una notificacion dentro de su grupo: cuando entro (no vista) o cuando se vio (vista). */
    private static LocalDateTime sortTimeOf(RequestNotification notification) {
        return notification.isSeen() ? notification.getSeenAt() : notification.getReceivedAt();
    }

    /*
    Localiza la reserva del funcionario por su id. Es el unico punto por el que
    este servicio resuelve una reserva: el Frontend solo manda el id, nunca el
    objeto, porque al otro lado del socket lo que llegaria seria una copia.
    */
    private Reservation requireReservationOf(Employee employee, int reservationId) throws ServiceException {
        try {
            return employee.getReservationById(reservationId);
        } catch (ReservationNotFoundException e) {
            throw new ServiceException("La reserva indicada no pertenece a ese funcionario.", e);
        }
    }

    private static ServiceException unavailable(ResourceUnavailableException e) {
        return new ServiceException("No hay recursos disponibles para la(s) siguiente(s) categoria(s) en ese horario: "
                + String.join(", ", e.getUnavailableCategories()), e);
    }

    private void validateReservationData(String activity, LocalDate date, LocalTime startTime, LocalTime endTime,
                                          List<String> requiredCategories) throws ServiceException {
        if (activity == null || activity.isBlank()) {
            throw new ServiceException("Debe indicar la actividad que se realizara.");
        }
        if (date == null || startTime == null || endTime == null) {
            throw new ServiceException("Debe indicar fecha, hora de inicio y hora de fin de la reserva.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new ServiceException("No se puede reservar en una fecha que ya paso.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ServiceException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        if (requiredCategories == null || requiredCategories.isEmpty()) {
            throw new ServiceException("Debe seleccionar al menos una categoria de recurso requerida.");
        }
    }

    /*
    Traduce las descripciones elegidas en el formulario a las categorias reales
    del sistema. Si alguna no existe, se informan todas las que fallaron juntas.
    */
    private List<ResourceCategory> resolveCategories(List<String> descriptions) throws ServiceException {
        List<ResourceCategory> categories = new ArrayList<>();
        List<String> unresolvedCategories = new ArrayList<>();
        for (String description : descriptions) {
            try {
                categories.add(service.getCategories().getCategorybyDescription(description));
            } catch (CategoryNotFoundException e) {
                unresolvedCategories.add(description);
            }
        }

        if (!unresolvedCategories.isEmpty()) {
            throw new ServiceException("No existe(n) la(s) siguiente(s) categoria(s): "
                    + String.join(", ", unresolvedCategories));
        }
        return categories;
    }

    private Employee requireEmployee(int employeeId) throws ServiceException {
        try {
            return service.getUsers().getEmployeeById(employeeId);
        } catch (EmployeeNotFoundException e) {
            throw new ServiceException(
                    "No existe ningun funcionario con el id '" + employeeId + "'.", e);
        }
    }

    /*
Con el Backend remoto, los recursos que llegan del Frontend son copias. Se busca
la instancia real (la que vive dentro de su categoria) para que la reserva y la
categoria sigan compartiendo el mismo objeto Resource.
*/
    private Resource resolveResource(Resource received) throws ServiceException {
        if (received == null) {
            return null;   // approve() ya reporta que falta un recurso
        }
        for (ResourceCategory category : service.getCategories().getCategories()) {
            if (category.getId().equals(received.getCategoryId())) {
                try {
                    return category.getResourceById(received.getId());
                } catch (ResourceNotFoundException e) {
                    throw new ServiceException("El recurso elegido ya no existe.", e);
                }
            }
        }
        throw new ServiceException("La categoria del recurso elegido ya no existe.");
    }
}
