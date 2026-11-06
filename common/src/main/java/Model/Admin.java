package Model;

public class Admin extends User{
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    //This class doesnt have more attributes

    //Default builder
    public Admin(){super();}

    //Parameterized builder
    public Admin(int id, String password){super(id,password);}

}
