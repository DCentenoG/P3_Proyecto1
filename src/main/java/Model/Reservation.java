package Model;
import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ResourceUnavailableException;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

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

    /* Solo una reserva Aceptada se muestra de forma global y ocupa recursos y horario. */
    public boolean isAccepted() {
        return state == State.ACCEPTED;
    }

    /*Indica si "resource" esta libre en la fecha/horario de esta reserva: ninguna OTRA reserva ACEPTADA que
    choque en horario lo tiene asignado. Las reservas En revision, Rechazadas o Canceladas no ocupan recursos.
    Se ignora la reserva "ignored" (null si no hay ninguna): al EDITAR, la version nueva no debe "chocar" con
    la reserva original que reemplaza. La comparacion es por identidad (==), no por contenido.*/
    public boolean isResourceFree(Resource resource, UserContainer users, Reservation ignored) {
        ArrayList<Employee> listOfEmployeesForCheckingReservations = users.getListOfEmployees();
        for (int i = 0; i < listOfEmployeesForCheckingReservations.size(); i++) {
            ArrayList<Reservation> toCheckList = listOfEmployeesForCheckingReservations.get(i).getReservations();
            for (int j = 0; j < toCheckList.size(); j++) {
                Reservation other = toCheckList.get(j);
                if (other == this || other == ignored || !other.isAccepted()) {
                    continue;
                }
                if (this.overlaps(other)) {
                    for (int k = 0; k < other.getAssignedResources().size(); k++) {
                        if (other.getAssignedResources().get(k).isSameResource(resource)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /* Recursos de "category" que estan libres en la fecha/horario de esta reserva (ver isResourceFree). */
    public List<Resource> freeResourcesOf(ResourceCategory category, UserContainer users, Reservation ignored) {
        List<Resource> free = new ArrayList<>();
        for (Resource resource : category.getResources()) {
            if (isResourceFree(resource, users, ignored)) {
                free.add(resource);
            }
        }
        return free;
    }

    /*Este metodo intenta agregar un recurso a la reserva verificando primero si no esta apartado en otra reserva
    aceptada que coincida en hora y fecha.

    Se decidio incluir en la reserva para evitar acoplamiento de clases innecesario.*/
    public boolean addResource(Resource resource, UserContainer users) {
        return addResource(resource, users, null);
    }

    /* Igual que addResource(resource, users), pero sin tomar en cuenta la reserva "ignored" al revisar choques. */
    public boolean addResource(Resource resource, UserContainer users, Reservation ignored) {
        if (!isResourceFree(resource, users, ignored)) {
            return false;
        }
        assignedResources.add(resource);
        return true;
    }

    //--- Cambios de estado (flujo de solicitudes) ---

    /*
    El funcionario cancela la reserva: pasa a Cancelada y libera los recursos y el
    horario que ocupaba (si estaba Aceptada). Ya no se borra del sistema. Una
    reserva Rechazada no se puede cancelar (no tiene sentido), ni una ya Cancelada.
    */
    public void cancel() throws InvalidReservationOperationException {
        ensureCancellable();
        state = State.CANCELLED;
        assignedResources.clear();
    }

    /* Verifica, sin cambiar nada, que la reserva se pueda cancelar en su estado actual. */
    public void ensureCancellable() throws InvalidReservationOperationException {
        if (state == State.REJECTED) {
            throw new InvalidReservationOperationException("No es posible cancelar una reserva rechazada.");
        }
        if (state == State.CANCELLED) {
            throw new InvalidReservationOperationException("La reserva ya se encuentra cancelada.");
        }
    }

    /*
    El administrador aprueba la solicitud asignandole "chosenResources": exactamente
    un recurso por cada categoria requerida, cada uno libre en la fecha/horario de la
    reserva. Si algo no cumple, la reserva queda exactamente igual. Al aprobarse, la
    reserva pasa a Aceptada y empieza a ocupar esos recursos en todo el sistema.
    */
    public void approve(List<Resource> chosenResources, UserContainer users)
            throws InvalidReservationOperationException, ResourceUnavailableException {
        requireInReview("aprobar");

        List<String> missingCategories = new ArrayList<>();
        ArrayList<Resource> assignment = new ArrayList<>();
        for (ResourceCategory category : requiredCategories) {
            Resource chosen = null;
            for (Resource resource : chosenResources) {
                if (resource != null && resource.getResourceCategoryReference() == category) {
                    if (chosen != null) {
                        throw new InvalidReservationOperationException(
                                "Debe elegir un solo recurso para la categoria '" + category.getDescription() + "'.");
                    }
                    chosen = resource;
                }
            }
            if (chosen == null) {
                missingCategories.add(category.getDescription());
            } else {
                assignment.add(chosen);
            }
        }
        if (!missingCategories.isEmpty()) {
            throw new InvalidReservationOperationException("Debe elegir un recurso para la(s) categoria(s): "
                    + String.join(", ", missingCategories));
        }
        if (assignment.size() != countNonNull(chosenResources)) {
            throw new InvalidReservationOperationException(
                    "Se eligio un recurso de una categoria que la solicitud no requiere.");
        }

        List<String> unavailableCategories = new ArrayList<>();
        for (Resource resource : assignment) {
            if (!isResourceFree(resource, users, null)) {
                unavailableCategories.add(resource.getResourceCategoryReference().getDescription());
            }
        }
        if (!unavailableCategories.isEmpty()) {
            throw new ResourceUnavailableException(unavailableCategories);
        }

        assignedResources = assignment;
        state = State.ACCEPTED;
        rejectionReason = null;
    }

    /* El administrador rechaza la solicitud con un motivo (obligatorio). No ocupa recursos. */
    public void reject(String reason) throws InvalidReservationOperationException {
        requireInReview("rechazar");
        if (reason == null || reason.isBlank()) {
            throw new InvalidReservationOperationException("Debe indicar el motivo del rechazo.");
        }
        state = State.REJECTED;
        rejectionReason = reason.trim();
        assignedResources.clear();
    }

    /*
    La reserva vuelve a ser una solicitud pendiente (se usa al EDITARLA, sin importar
    su estado anterior): queda En revision, libera los recursos que tuviera y se borra
    el motivo de un rechazo previo. Para volver a Aceptada debe revisarse de nuevo.
    */
    public void returnToReview() {
        state = State.IN_REVIEW;
        assignedResources.clear();
        rejectionReason = null;
    }

    private void requireInReview(String action) throws InvalidReservationOperationException {
        if (state != State.IN_REVIEW) {
            throw new InvalidReservationOperationException("Solo se puede " + action
                    + " una solicitud que esta En revision (esta reserva esta " + state.getLabel() + ").");
        }
    }

    private static int countNonNull(List<Resource> resources) {
        int count = 0;
        for (Resource resource : resources) {
            if (resource != null) {
                count++;
            }
        }
        return count;
    }
}
