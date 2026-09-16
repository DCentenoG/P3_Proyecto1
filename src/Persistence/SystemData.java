package Persistence;

import Model.CategoryContainer;
import Model.UserContainer;

/*
Contenedor en memoria de todo el estado del sistema que debe persistirse en el
archivo XML: los usuarios (administradores y funcionarios, con sus reservas ya
resueltas) y las categorias de recursos (con sus recursos).

Es el puente entre el dominio (paquete Model) y el repositorio XML: el
repositorio solo sabe leer y escribir un SystemData completo, y la capa
Service es quien decide que operaciones del Model se ejecutan sobre los
contenedores que esta clase expone.
*/
public class SystemData {

    private UserContainer users;
    private CategoryContainer categories;

    //Default builder: arranca con contenedores vacios (sistema sin archivo XML previo)
    public SystemData() {
        this.users = new UserContainer();
        this.categories = new CategoryContainer();
    }

    //Parameterized builder: se usa cuando el repositorio ya reconstruyo los contenedores desde el XML
    public SystemData(UserContainer users, CategoryContainer categories) {
        this.users = users;
        this.categories = categories;
    }

    public UserContainer getUsers() {
        return users;
    }

    public void setUsers(UserContainer users) {
        this.users = users;
    }

    public CategoryContainer getCategories() {
        return categories;
    }

    public void setCategories(CategoryContainer categories) {
        this.categories = categories;
    }
}
