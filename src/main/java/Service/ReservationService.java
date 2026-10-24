package Service;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.EmployeeNotFoundException;
import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ReservationNotFoundException;
import Model.exceptions.ResourceUnavailableException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
Servicio de dominio de reservas (funcionalidad 2 del enunciado, la de mayor
peso) y del flujo de solicitudes que revisa el administrador:

- El funcionario crea, edita y cancela sus reservas. Crear o editar deja la
  reserva "En revision" (una solicitud pendiente, sin recursos asignados);
  cancelar la deja "Cancelada" (ya no se borra).
- El administrador lista las solicitudes pendientes, consulta que recursos
  estan libres para cada una y la aprueba (eligiendo un recurso por categoria
  requerida) o la rechaza (con un motivo obligatorio).
- Solo las reservas "Aceptadas" ocupan recursos y horarios en el sistema.

Las reglas de cada cambio de estado viven en el Model (Reservation/Employee);
aqui se validan los datos de entrada, se traducen las excepciones del Model a
ServiceException con un mensaje para el usuario y se guarda el XML.
*/
public class ReservationService {

    private final Service service;

    /* Una solicitud pendiente de revision junto con el funcionario que la hizo. */
    public record PendingRequest(Employee employee, Reservation reservation) {
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
            employee.tryBook(activity.trim(), date, startTime, endTime, categories, service.getUsers());
        } catch (ResourceUnavailableException e) {
            throw unavailable(e);
        }

        // tryBook agrega la reserva al final de la lista del funcionario.
        Reservation reservation = employee.getReservations().get(employee.getReservations().size() - 1);
        service.save();
        return reservation;
    }

    /*
    Edita una reserva del funcionario (en cualquier estado). Aplica las mismas
    validaciones que createReservation y, al guardarse, la reserva vuelve a "En
    revision": libera sus recursos y debe aprobarse de nuevo. Es "todo o nada": si
    algo falla, la reserva queda exactamente como estaba.
    */
    public void editReservation(int employeeId, Reservation reservation, String activity, LocalDate date,
                                LocalTime startTime, LocalTime endTime,
                                List<String> requiredCategoryDescriptions) throws ServiceException {

        Employee employee = requireEmployee(employeeId);
        if (reservation == null) {
            throw new ServiceException("Debe indicar la reserva que desea editar.");
        }
        validateReservationData(activity, date, startTime, endTime, requiredCategoryDescriptions);
        List<ResourceCategory> categories = resolveCategories(requiredCategoryDescriptions);

        try {
            employee.tryEdit(reservation, activity.trim(), date, startTime, endTime, categories, service.getUsers());
        } catch (ReservationNotFoundException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (ResourceUnavailableException e) {
            throw unavailable(e);
        }

        service.save();
    }

    /*
    Cancela una reserva del funcionario: queda "Cancelada" (ya no se borra) y libera
    sus recursos. No se puede cancelar una reserva Rechazada, una ya Cancelada ni una
    cuya fecha ya paso.
    */
    public void cancelReservation(int employeeId, Reservation reservation) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        if (reservation == null) {
            throw new ServiceException("Debe indicar la reserva que desea cancelar.");
        }
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
            employee.cancelReservation(reservation);
        } catch (ReservationNotFoundException | InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        service.save();
    }

    //--- Administrador: revision de solicitudes ---

    /* Solicitudes "En revision" de todos los funcionarios, ordenadas por fecha y hora de inicio. */
    public List<PendingRequest> listPendingRequests() {
        List<PendingRequest> pending = new ArrayList<>();
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (reservation.getState() == State.IN_REVIEW) {
                    pending.add(new PendingRequest(employee, reservation));
                }
            }
        }
        pending.sort(Comparator
                .comparing((PendingRequest request) -> request.reservation().getDate())
                .thenComparing(request -> request.reservation().getStartTime()));
        return pending;
    }

    /*
    Para cada categoria requerida por la solicitud (en su orden), los recursos de esa
    categoria que estan libres en su fecha/horario, es decir, que no estan asignados
    a otra reserva ya Aceptada que choque en horario. Son las opciones que el
    administrador puede elegir al aprobarla (una lista vacia: no hay disponibilidad).
    */
    public Map<ResourceCategory, List<Resource>> availableResourcesFor(Reservation request) throws ServiceException {
        if (request == null) {
            throw new ServiceException("Debe indicar la solicitud a revisar.");
        }
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
    public void approveRequest(int employeeId, Reservation request, List<Resource> chosenResources)
            throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        requireRequestOf(employee, request);
        if (chosenResources == null) {
            throw new ServiceException("Debe elegir un recurso por cada categoria requerida.");
        }

        try {
            request.approve(chosenResources, service.getUsers());
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        } catch (ResourceUnavailableException e) {
            throw new ServiceException("El recurso elegido ya no esta disponible en ese horario para la(s) "
                    + "categoria(s): " + String.join(", ", e.getUnavailableCategories()), e);
        }

        service.save();
    }

    /*
    Rechaza la solicitud con un motivo, que es obligatorio. La reserva pasa a
    "Rechazada", deja de estar en las solicitudes y el funcionario puede consultar
    el motivo (y editarla para volver a solicitarla).
    */
    public void rejectRequest(int employeeId, Reservation request, String reason) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        requireRequestOf(employee, request);
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("Debe indicar el motivo del rechazo.");
        }

        try {
            request.reject(reason);
        } catch (InvalidReservationOperationException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        service.save();
    }

    //--- Utilidades ---

    private void requireRequestOf(Employee employee, Reservation request) throws ServiceException {
        if (request == null) {
            throw new ServiceException("Debe indicar la solicitud a revisar.");
        }
        if (!employee.ownsReservation(request)) {
            throw new ServiceException("La solicitud indicada no pertenece a ese funcionario.");
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
}
