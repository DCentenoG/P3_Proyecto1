package View.Events;

import Model.CategoryContainer;
import Model.User;
import Model.UserContainer;

/**
 * Estado de la aplicación durante la sesión: los usuarios y las
 * categorías/recursos disponibles, y el usuario que inició sesión. Se
 * construye una única vez desde {@code Main} y se comparte entre todos
 * los controladores de eventos.
 * <p>
 * Mientras la capa Model no incorpore la carga/guardado en XML, esta
 * clase mantiene los datos únicamente en memoria durante la ejecución
 * (se pierden al cerrar el programa) y siembra un par de registros de
 * prueba para que la aplicación se pueda navegar e interactuar de
 * inmediato.
 */
public final class SessionContext {

    private final UserContainer users;
    private final CategoryContainer categories;
    private User currentUser;

    public SessionContext() {
        users = new UserContainer();
        categories = new CategoryContainer();
        seedDemoData();
    }

    private void seedDemoData() {
        users.addAdmin("admin");                                 // ID 1 · clave "admin"
        users.addEmployee("Funcionario de prueba", 88889999);     // ID 2 · clave "2"
        categories.addCategory("Salas de reunión");
        categories.addCategory("Equipo audiovisual");
    }

    public UserContainer getUsers() {
        return users;
    }

    public CategoryContainer getCategories() {
        return categories;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /** Busca, entre administradores y funcionarios, el usuario cuyo ID y clave coincidan; {@code null} si no existe. */
    public User authenticate(int id, String password) {
        for (User user : users.getUsers()) {
            if (user.getId() == id && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }
}
