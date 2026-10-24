package Service;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.EmployeeNotFoundException;
import Model.exceptions.ReservationNotFoundException;
import Model.exceptions.ResourceUnavailableException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/*
Servicio de dominio de reservas (funcionalidad 2 del enunciado, la de mayor
peso). Orquesta la creacion de una reserva: por cada categoria requerida
busca el primer recurso disponible usando Reservation.addResource (que ya
trae la logica de choque de horarios contra TODAS las reservas del sistema) y
si alguna categoria no tiene disponibilidad, no se guarda nada y se informa
cuales categorias fallaron, tal como pide el enunciado. Esta es la misma
logica que ya usa ReservationsViewListener directamente; queda aqui
disponible para cuando el Controller se conecte a la capa Service.
*/
public class ReservationService {

    private final Service service;

    public ReservationService(Service service) {
        this.service = service;
    }

    public List<Reservation> listReservations(int employeeId) throws ServiceException {
        return requireEmployee(employeeId).getReservations();
    }

    /*
    Intenta crear la reserva asignando, para cada categoria requerida, el
    primer recurso de esa categoria que este libre en la fecha/horario
    indicados. Si alguna categoria no tiene ningun recurso disponible, no se
    agrega la reserva al funcionario (no queda nada a medias) y se lanza una
    excepcion indicando todas las categorias sin disponibilidad.
    */
    public Reservation createReservation(int employeeId, String activity, LocalDate date,
                                         LocalTime startTime, LocalTime endTime,
                                         List<String> requiredCategoryDescriptions) throws ServiceException {

        Employee employee = requireEmployee(employeeId);
        validateReservationData(activity, date, startTime, endTime, requiredCategoryDescriptions);

        List<ResourceCategory> categories = resolveCategories(requiredCategoryDescriptions);

        List<String> unavailableCategories = new ArrayList<>();
        try {
            employee.tryBook(activity.trim(), date, startTime, endTime, categories, service.getUsers());
        } catch (ResourceUnavailableException e) {
            unavailableCategories.addAll(e.getUnavailableCategories());
        }

        if (!unavailableCategories.isEmpty()) {
            throw new ServiceException("No hay disponibilidad para la(s) siguiente(s) categoria(s) en ese horario: "
                    + String.join(", ", unavailableCategories));
        }

        // Si llegamos aquí, allFailed está vacío: tryBook tuvo éxito y ya agregó
        // la reserva a employee.getReservations() internamente. La recuperamos
        // (es la última, porque tryBook la agrega al final de la lista) en vez
        // de retornar un objeto separado que nunca se agregó a ningún lado.
        Reservation reservation = employee.getReservations().get(employee.getReservations().size() - 1);
        service.save();
        return reservation;
    }

    /*
    Edita una reserva existente del funcionario. Aplica las mismas validaciones
    que createReservation (campos obligatorios, fecha no pasada, horario valido,
    categorias existentes) y vuelve a asignar, para cada categoria requerida, el
    primer recurso libre en el nuevo horario. Al revisar disponibilidad se ignora
    la propia reserva que se esta editando, asi que puede conservar su horario y
    sus recursos actuales si siguen libres.

    Es "todo o nada": si alguna categoria no tiene disponibilidad, la reserva
    queda exactamente como estaba y se informa cuales categorias fallaron.
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
            throw new ServiceException("No hay disponibilidad para la(s) siguiente(s) categoria(s) en ese horario: "
                    + String.join(", ", e.getUnavailableCategories()), e);
        }

        service.save();
    }

    public void cancelReservation(int employeeId, Reservation reservation) throws ServiceException {
        Employee employee = requireEmployee(employeeId);
        if (reservation == null) {
            throw new ServiceException("Debe indicar la reserva que desea cancelar.");
        }
        if (reservation.getDate().isBefore(LocalDate.now())) {
            throw new ServiceException("No es posible cancelar una reserva que ya paso.");
        }

        //Al remover la reserva de la lista, sus recursos quedan libres automaticamente: la
        //disponibilidad no se marca en el recurso, se calcula en el momento revisando las
        //reservas vigentes (por eso "liberar" no requiere tocar Resource ni ResourceCategory).
        boolean removed = employee.getReservations().remove(reservation);
        if (!removed) {
            throw new ServiceException("La reserva indicada no pertenece a este funcionario.");
        }

        service.save();
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
