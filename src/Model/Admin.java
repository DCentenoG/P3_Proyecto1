package Model;

public class Admin extends User{
    //This class doesnt have more attributes

    //Default builder
    public Admin(){super();}

    //Parameterized builder
    public Admin(int id, String password){super(id,password);}

}
