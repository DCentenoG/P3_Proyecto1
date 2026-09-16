package Service;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;

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

        List<ResourceCategory> categories = new ArrayList<>();
        List<String> unresolvedCategories = new ArrayList<>();
        for (String description : requiredCategoryDescriptions) {
            ResourceCategory category = service.getCategories().getCategorybyDescription(description);
            if (category == null) {
                unresolvedCategories.add(description); // la categoría ni siquiera existe
            } else {
                categories.add(category);
            }
        }

        List<String> unavailableCategories = employee.tryBook(activity.trim(), date, startTime, endTime, categories, service.getUsers());

        List<String> allFailed = new ArrayList<>(unresolvedCategories);
        allFailed.addAll(unavailableCategories);
        if (!allFailed.isEmpty()) {
            throw new ServiceException("No hay disponibilidad para la(s) siguiente(s) categoria(s) en ese horario: "
                    + String.join(", ", allFailed));
        }

        // Si llegamos aquí, allFailed está vacío: tryBook tuvo éxito y ya agregó
        // la reserva a employee.getReservations() internamente. La recuperamos
        // (es la última, porque tryBook la agrega al final de la lista) en vez
        // de retornar un objeto separado que nunca se agregó a ningún lado.
        Reservation reservation = employee.getReservations().get(employee.getReservations().size() - 1);
        service.save();
        return reservation;
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

    private Employee requireEmployee(int employeeId) throws ServiceException {
        Employee employee = service.getUsers().getEmployeeById(employeeId);
        if (employee == null) {
            throw new ServiceException("No existe ningun funcionario con el id '" + employeeId + "'.");
        }
        return employee;
    }
}
