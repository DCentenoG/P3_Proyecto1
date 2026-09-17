package Controller;

import Model.CategoryContainer;
import Model.User;
import Model.UserContainer;
import Service.ReservationService;
import Service.ResourceService;
import Service.Service;
import Service.ServiceException;
import Service.UserService;

import java.awt.Component;

/**
 * Estado de la aplicación durante la sesión: los usuarios y las
 * categorías/recursos disponibles, y el usuario que inició sesión. Se
 * construye una única vez desde {@code Main} y se comparte entre todos
 * los controladores de eventos.
 * <p>
 * Los datos ahora se cargan y se guardan en el archivo XML de
 * {@code src/Data/system.xml} a través de la capa {@link Service}: al
 * construirse se carga el archivo (o se siembran un par de registros de
 * prueba si el archivo todavía no existe o está vacío), y cada controlador
 * que modifique usuarios, categorías, recursos o reservas debe llamar a
 * {@link #save(Component)} inmediatamente después del cambio para que quede
 * persistido.
 */
public final class SessionContext {

    private static final String XML_PATH = "src/Data/system.xml";

    private final Service service;
    private User currentUser;

    public SessionContext() {
        this.service = new Service(XML_PATH);
        try {
            service.load();
        } catch (ServiceException e) {
            // El archivo no existe todavía o está corrupto: se avisa por consola y se
            // arranca con contenedores vacíos para que la aplicación se pueda seguir
            // usando (se completan con datos de prueba más abajo).
            System.err.println(e.getMessage());
        }
        seedDemoDataIfEmpty();
    }

    private void seedDemoDataIfEmpty() {
        if (!service.getUsers().getUsers().isEmpty()) {
            return;
        }
        service.getUsers().addAdmin("admin");                                 // ID 1 · clave "admin"
        service.getUsers().addEmployee("Funcionario de prueba", 88889999);    // ID 2 · clave "2"
        service.getCategories().addCategory("Salas de reunión");
        service.getCategories().addCategory("Equipo audiovisual");
        try {
            service.save();
        } catch (ServiceException e) {
            System.err.println(e.getMessage());
        }
    }

    public UserContainer getUsers() {
        return service.getUsers();
    }

    public CategoryContainer getCategories() {
        return service.getCategories();
    }

    /**
     * Servicio de dominio de reservas (ver {@link ReservationService}):
     * comparte la misma instancia de {@link Service} que ya usan
     * {@link #getUsers()}/{@link #getCategories()}, para que una reserva
     * creada a través de él quede reflejada de inmediato en los mismos
     * contenedores en memoria que usa el resto de la aplicación.
     */
    public ReservationService getReservationService() {
        return new ReservationService(service);
    }

    public UserService getUserService() {
        return new UserService(service);
    }

    public ResourceService getResourceService() {
        return new ResourceService(service);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /** Busca, entre administradores y funcionarios, el usuario cuyo ID y clave coincidan; {@code null} si no existe. */
    public User authenticate(int id, String password) {
        for (User user : service.getUsers().getUsers()) {
            if (user.getId() == id && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Persiste en el XML el estado actual de usuarios/categorías/recursos.
     * Debe llamarse después de cualquier alta, edición, borrado o reserva
     * nueva/cancelada. Si falla, muestra el error sobre {@code parent} y
     * retorna {@code false} para que el controlador que llamó pueda
     * cancelar el resto de su flujo (no mostrar el mensaje de éxito, no
     * cerrar el diálogo, etc.).
     */
    public boolean save(Component parent) {
        try {
            service.save();
            return true;
        } catch (ServiceException e) {
            DialogHelper.error(parent, e.getMessage());
            return false;
        }
    }
}
