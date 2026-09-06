package Model;

public class Employee extends User{
    //Attributes
    private String name;
    private int phoneNumber;
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
    }

    //Getter methods
    public String getName() {
        return name;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    //Setter methods
    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }



}
