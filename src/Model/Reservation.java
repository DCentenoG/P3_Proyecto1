package Model;
import java.time.*;
import java.util.ArrayList;

public class Reservation {
    //Attributes
    private String Activity;
    private LocalDate date;
    private LocalTime StartTime;
    private LocalTime EndTime;
    private ArrayList<Resource> AssignedResources;
    //End of attributes

    //Default Builder
    public Reservation() {
        Activity = null;
        date = null;
        StartTime = null;
        EndTime = null;
        AssignedResources = new ArrayList<>();
    }

    //Parameterized Builder
    public Reservation(String Activity, LocalDate date, LocalTime StartTime, LocalTime EndTime) {
        this.Activity = Activity;
        this.date = date;
        this.StartTime = StartTime;
        this.EndTime = EndTime;
        AssignedResources = new ArrayList<>(); //PENDIENTE VERIFICAR SI RECIBE LOS RECURSOS O SE AGREGAN CON LA RESERVACION YA CREADA
    }

    //basic getters
    public String getActivity() {
        return Activity;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return StartTime;
    }

    public LocalTime getEndTime() {
        return EndTime;
    }

    public ArrayList<Resource> getAssignedResources() {
        return AssignedResources;
    }

    //basic setters
    public void setActivity(String Activity) {
        this.Activity = Activity;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStartTime(LocalTime StartTime) {
        this.StartTime = StartTime;
    }

    public void setEndTime(LocalTime EndTime) {
        this.EndTime = EndTime;
    }

    public void setAssignedResources(ArrayList<Resource> AssignedResources) {
        this.AssignedResources = AssignedResources;
    }

    //logic and calculus methods


}
