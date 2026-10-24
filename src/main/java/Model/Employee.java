package Model;

import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ReservationNotFoundException;
import Model.exceptions.ResourceUnavailableException;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class Employee extends User{
    //Attributes
    private String name;
    private int phoneNumber;
    private ArrayList<Reservation> reservations;
    //End of Attributes

    //Default builder
    public Employee(){
        super();
    }

    //Parameterized builder
    public Employee(String name, int phoneNumber, int id, String password){
        super(id,password);
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.reservations = new ArrayList<Reservation>();
    }

    //Getter methods
    public String getName() {
        return name;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public ArrayList<Reservation> getReservations() {
        return reservations;
    }

    //Setter methods
    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setReservations(ArrayList<Reservation> reservations) {
        this.reservations = reservations;
    }


    /*
     * Crea una solicitud de reserva para este funcionario. La reserva nace
     * "En revision" (valor por defecto de Reservation) con las categorias que
     * pidio el funcionario, SIN recursos asignados: el recurso especifico de
     * cada categoria lo elige el administrador al aprobarla. Por eso queda de
     * inmediato en las solicitudes que revisa el administrador.
     *
     * Antes de crearla se verifica que cada categoria requerida tenga al menos
     * un recurso libre en ese horario (contra las reservas ya Aceptadas); si
     * CUALQUIERA no tiene, la solicitud NO se crea y se informan todas las
     * categorias sin disponibilidad, como pide el enunciado.
     */
    public void tryBook(String activity, LocalDate date, LocalTime startTime, LocalTime endTime,
                        List<ResourceCategory> requiredCategories, UserContainer users)
            throws ResourceUnavailableException {

        Reservation reservation = new Reservation(activity, date, startTime, endTime);
        List<String> unavailableCategories = categoriesWithoutAvailability(reservation, requiredCategories, users, null);

        if (!unavailableCategories.isEmpty()) {
            throw new ResourceUnavailableException(unavailableCategories);
        }

        reservation.setRequiredCategories(new ArrayList<>(requiredCategories));
        reservations.add(reservation);
    }

    /*
     * Edita una reserva existente de este funcionario con los nuevos datos. Sin
     * importar su estado anterior (En revision, Aceptada, Rechazada o Cancelada),
     * al guardarse vuelve a "En revision": libera los recursos y el horario que
     * ocupaba y debe revisarse de nuevo para volver a estar Aceptada.
     *
     * Igual que al crear, cada categoria requerida debe tener al menos un recurso
     * libre en el nuevo horario. Al revisarlo se ignora la reserva original (sus
     * recursos actuales quedan libres para la version editada). Si alguna falla,
     * la reserva queda exactamente igual.
     *
     * Se modifica el mismo objeto (no se reemplaza en la lista) para que la
     * reserva conserve su posicion en "Mis reservas".
     */
    public void tryEdit(Reservation target, String activity, LocalDate date, LocalTime startTime,
                        LocalTime endTime, List<ResourceCategory> requiredCategories, UserContainer users)
            throws ResourceUnavailableException, ReservationNotFoundException {

        requireOwnReservation(target);

        Reservation edited = new Reservation(activity, date, startTime, endTime);
        List<String> unavailableCategories = categoriesWithoutAvailability(edited, requiredCategories, users, target);

        if (!unavailableCategories.isEmpty()) {
            throw new ResourceUnavailableException(unavailableCategories);
        }

        target.setActivity(edited.getActivity());
        target.setDate(edited.getDate());
        target.setStartTime(edited.getStartTime());
        target.setEndTime(edited.getEndTime());
        target.setRequiredCategories(new ArrayList<>(requiredCategories));
        target.returnToReview();
    }

    /*
     * Cancela una reserva de este funcionario: pasa a "Cancelada" (ya no se borra
     * del sistema) y libera sus recursos. Ver Reservation.cancel() para las reglas
     * (una reserva Rechazada o ya Cancelada no se puede cancelar).
     */
    public void cancelReservation(Reservation target)
            throws ReservationNotFoundException, InvalidReservationOperationException {
        requireOwnReservation(target);
        target.cancel();
    }

    /* Indica si "reservation" (la misma instancia, no una igual) es una de las reservas de este funcionario. */
    public boolean ownsReservation(Reservation reservation) {
        for (Reservation own : reservations) {
            if (own == reservation) {
                return true;
            }
        }
        return false;
    }

    private void requireOwnReservation(Reservation reservation) throws ReservationNotFoundException {
        if (!ownsReservation(reservation)) {
            throw new ReservationNotFoundException("La reserva indicada no pertenece a este funcionario.");
        }
    }

    /*
     * Categorias (descripciones) que NO tienen ningun recurso libre en la
     * fecha/horario de "reservation", sin tomar en cuenta la reserva "ignored"
     * (null al crear; la reserva original al editar).
     */
    private static List<String> categoriesWithoutAvailability(Reservation reservation,
                                                              List<ResourceCategory> requiredCategories,
                                                              UserContainer users, Reservation ignored) {
        List<String> unavailableCategories = new ArrayList<>();
        for (ResourceCategory category : requiredCategories) {
            if (reservation.freeResourcesOf(category, users, ignored).isEmpty()) {
                unavailableCategories.add(category.getDescription());
            }
        }
        return unavailableCategories;
    }

}
