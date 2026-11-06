package Model;

import java.io.Serializable;

public abstract class User implements Serializable {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

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

    /*
    La identidad de un usuario es su id, que UserContainer genera con un unico
    contador compartido entre administradores y funcionarios, asi que es unico en
    todo el sistema. Se compara ademas la clase concreta para que un Admin y un
    Employee nunca se consideren iguales.
    */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id == ((User) other).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
