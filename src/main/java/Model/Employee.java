package Model;

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
     * Intenta crear una reserva para este funcionario, asignando a cada
     * categoría requerida el primer recurso disponible en ese horario.
     * Si CUALQUIER categoría se queda sin recursos disponibles, la reserva
     * NO se agrega (todo o nada) y se retorna la lista de categorías sin
     * disponibilidad. Si todas las categorías tienen disponibilidad, la
     * reserva se agrega a este funcionario.
     */
    public void tryBook(String activity, LocalDate date, LocalTime startTime, LocalTime endTime,
                        List<ResourceCategory> requiredCategories, UserContainer users)
            throws ResourceUnavailableException {

        Reservation reservation = new Reservation(activity, date, startTime, endTime);
        List<String> unavailableCategories = assignResources(reservation, requiredCategories, users, null);

        if (!unavailableCategories.isEmpty()) {
            throw new ResourceUnavailableException(unavailableCategories);
        }

        reservations.add(reservation);
    }

    /*
     * Edita una reserva existente de este funcionario con los nuevos datos,
     * reasignando a cada categoría requerida el primer recurso disponible en
     * el nuevo horario (misma regla de "todo o nada" que tryBook).
     *
     * La versión nueva se arma primero en una reserva temporal: al revisar
     * disponibilidad se ignora la reserva original (sus recursos y horario
     * actuales quedan libres para la versión editada), y solo si TODAS las
     * categorías tienen disponibilidad se copian los datos a la reserva
     * original. Si alguna falla, la reserva original queda exactamente igual.
     *
     * Se modifica el mismo objeto (no se reemplaza en la lista) para que la
     * reserva conserve su posición en "Mis reservas" y cualquier referencia
     * que otras vistas tengan de ella.
     */
    public void tryEdit(Reservation target, String activity, LocalDate date, LocalTime startTime,
                        LocalTime endTime, List<ResourceCategory> requiredCategories, UserContainer users)
            throws ResourceUnavailableException, ReservationNotFoundException {

        if (!ownsReservation(target)) {
            throw new ReservationNotFoundException("La reserva indicada no pertenece a este funcionario.");
        }

        Reservation edited = new Reservation(activity, date, startTime, endTime);
        List<String> unavailableCategories = assignResources(edited, requiredCategories, users, target);

        if (!unavailableCategories.isEmpty()) {
            throw new ResourceUnavailableException(unavailableCategories);
        }

        target.setActivity(edited.getActivity());
        target.setDate(edited.getDate());
        target.setStartTime(edited.getStartTime());
        target.setEndTime(edited.getEndTime());
        target.setAssignedResources(edited.getAssignedResources());
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

    /*
     * Asigna a "reservation", por cada categoría requerida, el primer recurso
     * de esa categoría que esté libre en su fecha/horario, sin tomar en cuenta
     * la reserva "ignored" (null al crear; la reserva original al editar).
     * Retorna las descripciones de las categorías que se quedaron sin recurso.
     */
    private static List<String> assignResources(Reservation reservation, List<ResourceCategory> requiredCategories,
                                                UserContainer users, Reservation ignored) {
        List<String> unavailableCategories = new ArrayList<>();
        for (ResourceCategory category : requiredCategories) {
            boolean assigned = false;
            for (Resource resource : category.getResources()) {
                // addResource ya verifica, contra TODAS las reservas de TODOS los
                // funcionarios, que el recurso no esté ocupado en ese horario.
                if (reservation.addResource(resource, users, ignored)) {
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                unavailableCategories.add(category.getDescription());
            }
        }
        return unavailableCategories;
    }

}
