package Model;

public abstract class User {
    //Attributes
    private int id;
    private String password;
    //End of attributes

    //Default builder
    public User(){}
    //Parameterized Builder
    public User(int id, String password){
        this.id = id;
        this.password = password;
    }

    //Getter methods
    public int getId(){return id;}
    public String getPassword(){return password;}

    //Setter methods
    public void setId(int id){this.id = id;}
    public void setPassword(String password){this.password = password;}
}
