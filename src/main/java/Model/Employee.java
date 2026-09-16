package Model;

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
     * disponibilidad. Si la lista retornada está vacía, la reserva se creó
     * y ya quedó agregada a este funcionario.
     */
    public List<String> tryBook(String activity, LocalDate date, LocalTime startTime, LocalTime endTime,
                                List<ResourceCategory> requiredCategories, UserContainer users) {

        Reservation reservation = new Reservation(activity, date, startTime, endTime);
        List<String> unavailableCategories = new ArrayList<>();

        for (ResourceCategory category : requiredCategories) {
            boolean assigned = false;
            for (Resource resource : category.getResources()) {
                // addResource ya verifica, contra TODAS las reservas de TODOS los
                // funcionarios, que el recurso no esté ocupado en ese horario.
                if (reservation.addResource(resource, users)) {
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                unavailableCategories.add(category.getDescription());
            }
        }

        if (!unavailableCategories.isEmpty()) {
            return unavailableCategories; // se descarta todo el intento, nada queda a medias
        }

        reservations.add(reservation);
        return List.of(); // éxito: lista vacía
    }

}
