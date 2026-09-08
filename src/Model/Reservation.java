package Model;
import java.time.*;
import java.util.ArrayList;

public class Reservation {
    //Attributes
    private String activity;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private ArrayList<Resource> assignedResources;
    //End of attributes

    //Default Builder
    public Reservation() {
        activity = null;
        date = null;
        startTime = null;
        endTime = null;
        assignedResources = new ArrayList<>();
    }

    //Parameterized Builder
    public Reservation(String activity, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.activity = activity;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        assignedResources = new ArrayList<>(); //PENDIENTE VERIFICAR SI RECIBE LOS RECURSOS O SE AGREGAN CON LA RESERVACION YA CREADA
    }

    //basic getters
    public String getActivity() {
        return activity;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public ArrayList<Resource> getAssignedResources() {
        return assignedResources;
    }

    //basic setters
    public void setActivity(String Activity) {
        this.activity = Activity;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStartTime(LocalTime StartTime) {
        this.startTime = StartTime;
    }

    public void setEndTime(LocalTime EndTime) {
        this.endTime = EndTime;
    }

    public void setAssignedResources(ArrayList<Resource> AssignedResources) {
        this.assignedResources = AssignedResources;
    }

    //logic and calculus methods

    //Este metodo chequea si dos reservas chocan en horario al menos un momento.
    public boolean overlaps(Reservation other) {
        return this.date.equals(other.date)
                && this.startTime.isBefore(other.endTime)
                && other.startTime.isBefore(this.endTime);
    }

    //Este metodo intenta agregar un recurso a la reserva verificando primero si no esta apartado en otra reserva que coincida en hora
    /*Se decidio incluir en la reserva para evitar acoplamiento de clases innecesario, esto porque Employee igual se le implementara un
    metodo que es llama a addResource al crear la reserva n veces segun la cantidad de categorias de recursos seleccionadas en la capa
    de la vista*/
    public boolean addResource(Resource resource, UserContainer users) {

        //Certificates first if the resource is already on another reservation at the same time
        ArrayList<Employee> listOfEmployeesForCheckingReservations = users.getListOfEmployees();
        for (int i = 0; i < listOfEmployeesForCheckingReservations.size(); i++) {
            Employee aux = listOfEmployeesForCheckingReservations.get(i);
            ArrayList<Reservation> toCheckList = aux.getReservations();
            for (int j = 0; j < toCheckList.size(); j++) {
                if (this.overlaps(toCheckList.get(j))) {
                    for (int k = 0; k < toCheckList.get(j).getAssignedResources().size(); k++) {
                        if (toCheckList.get(j).getAssignedResources().get(k).isSameResource(resource)) {
                            return false;
                        }
                    }
                }
            }
        }

        /*if all the loop get to the last iteration it means that the resource is available at that moment
        so you can add it and then returns true to clarify that it has been added*/
        assignedResources.add(resource);
        return true;
    }
}
