package Model;

import java.util.ArrayList;

public class Employee extends User{
    //Attributes
    private String name;
    private int phoneNumber;
    private ArrayList<Reservation> Reservations;
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
        this.Reservations = new ArrayList<Reservation>();
    }

    //Getter methods
    public String getName() {
        return name;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public ArrayList<Reservation> getReservations() {
        return Reservations;
    }

    //Setter methods
    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setReservations(ArrayList<Reservation> Reservations) {}



}
