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
    //Categorias que el funcionario pidio para la reserva. Se guardan aparte de los recursos
    //porque, con el flujo de solicitudes, el recurso especifico de cada categoria lo asigna el
    //administrador al aprobar: una reserva "En revision" tiene categorias pero aun no recursos.
    private ArrayList<ResourceCategory> requiredCategories;
    //Estado de revision de la reserva (ver State). Toda reserva empieza "En revision".
    private State state;
    //Motivo indicado por el administrador al rechazar la solicitud (null si no esta rechazada).
    private String rejectionReason;
    //End of attributes

    //Default Builder
    public Reservation() {
        activity = null;
        date = null;
        startTime = null;
        endTime = null;
        assignedResources = new ArrayList<>();
        requiredCategories = new ArrayList<>();
        state = State.IN_REVIEW;
        rejectionReason = null;
    }

    //Parameterized Builder
    public Reservation(String activity, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.activity = activity;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        assignedResources = new ArrayList<>(); //PENDIENTE VERIFICAR SI RECIBE LOS RECURSOS O SE AGREGAN CON LA RESERVACION YA CREADA
        requiredCategories = new ArrayList<>();
        state = State.IN_REVIEW;
        rejectionReason = null;
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

    public ArrayList<ResourceCategory> getRequiredCategories() {
        return requiredCategories;
    }

    public State getState() {
        return state;
    }

    public String getRejectionReason() {
        return rejectionReason;
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

    public void setRequiredCategories(ArrayList<ResourceCategory> requiredCategories) {
        this.requiredCategories = requiredCategories;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    //logic and calculus methods

    //Este metodo chequea si dos reservas chocan en horario al menos un momento.
    public boolean overlaps(Reservation other) {
        return this.date.equals(other.date)
                && this.startTime.isBefore(other.endTime)
                && other.startTime.isBefore(this.endTime);
    }

    /*Este metodo intenta agregar un recurso a la reserva verificando primero si no esta apartado en otra reserva
    que coincida en hora y fecha.
    
    Se decidio incluir en la reserva para evitar acoplamiento de clases innecesario, esto porque Employee igual se le implementara un
    metodo que llama a addResource al crear la reserva y llama a este metodo n veces segun la cantidad de categorias de
    recursos seleccionadas en la capa de la vista*/
    public boolean addResource(Resource resource, UserContainer users) {
        return addResource(resource, users, null);
    }

    /*Igual que addResource(resource, users), pero sin tomar en cuenta la reserva "ignored" al revisar
    choques de horario. Se usa al EDITAR una reserva: la version nueva se arma en una reserva temporal y no
    debe "chocar" con la reserva original que va a reemplazar (cuyos recursos se liberan al aplicar la
    edicion). La comparacion es por identidad (==), no por contenido.*/
    public boolean addResource(Resource resource, UserContainer users, Reservation ignored) {

        //Certificates first if the resource is already on another reservation at the same time
        ArrayList<Employee> listOfEmployeesForCheckingReservations = users.getListOfEmployees();
        for (int i = 0; i < listOfEmployeesForCheckingReservations.size(); i++) {
            Employee aux = listOfEmployeesForCheckingReservations.get(i);
            ArrayList<Reservation> toCheckList = aux.getReservations();
            for (int j = 0; j < toCheckList.size(); j++) {
                if (toCheckList.get(j) == ignored) {
                    continue;
                }
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
