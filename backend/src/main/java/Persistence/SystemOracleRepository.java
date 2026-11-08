package Persistence;

import Model.CategoryContainer;
import Model.Employee;
import Model.Reservation;
import Model.ResourceCategory;
import Model.User;
import Model.UserContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reconstruye el estado del sistema desde la base de datos, usando los cuatro
 * DAOs. Es el reemplazo de {@link SystemXmlRepository}.
 *
 * <h2>Cuando se lee y cuando se escribe</h2>
 * {@link #load()} corre <b>una sola vez</b>, al arrancar el servidor. De ahi en
 * adelante los servicios trabajan sobre los contenedores en memoria, y cada
 * operacion que modifica algo escribe de inmediato en la base a traves del DAO
 * que corresponda.
 * <p>
 * Esto funciona porque hay <b>un unico proceso backend</b>, que es la
 * arquitectura que describe el enunciado: el servidor es el unico que toca la
 * base, asi que su copia en memoria no puede quedar desactualizada por nadie
 * mas. Si hubiera varios servidores contra la misma base, habria que leer en
 * cada operacion.
 *
 * <h2>Orden de carga</h2>
 * Importa, porque los objetos se referencian entre si:
 * <ol>
 *   <li>Categorias, que no dependen de nada.</li>
 *   <li>Recursos, que van dentro de su categoria.</li>
 *   <li>Usuarios.</li>
 *   <li>Reservas, que apuntan a un funcionario, a categorias y a recursos.</li>
 * </ol>
 */
public final class SystemOracleRepository {

    private final ConnectionManager connections;
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public SystemOracleRepository(ConnectionManager connections) {
        this.connections = connections;
    }

    public ConnectionManager getConnections() {
        return connections;
    }

    public CategoryDAO getCategoryDAO() {
        return categoryDAO;
    }

    public ResourceDAO getResourceDAO() {
        return resourceDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public ReservationDAO getReservationDAO() {
        return reservationDAO;
    }

    /** Lee la base completa y devuelve los contenedores listos para usar. */
    public SystemData load() throws SQLException {
        try (Connection connection = connections.open()) {
            List<ResourceCategory> categoryList = categoryDAO.findAll(connection);
            resourceDAO.attachAll(connection, categoryList);

            List<User> userList = userDAO.findAll(connection);
            reservationDAO.attachAll(connection, userList, categoryList);

            // La carga es de solo lectura, pero la conexion viene con autoCommit
            // apagado: se cierra la transaccion para no dejarla abierta.
            connection.commit();

            CategoryContainer categories = new CategoryContainer(new ArrayList<>(categoryList));
            categories.setNextId(highestCategoryNumber(categoryList) + 1);

            UserContainer users = new UserContainer(new ArrayList<>(userList));
            users.setNextId(highestUserId(userList) + 1);
            users.setNextReservationId(highestReservationId(userList) + 1);

            return new SystemData(users, categories);
        }
    }

    /*
    Los contadores se derivan del mayor id en uso, no de la cantidad de registros.
    Si se borro una categoria o un usuario intermedio, contar registros devolveria
    un id que ya existe. Es el mismo criterio que usaba el repositorio XML.

    Oracle 11g no tiene columnas IDENTITY (llegaron en 12c), asi que los ids se
    generan en Java igual que antes y aqui solo se retoma la cuenta.
    */
    private static int highestCategoryNumber(List<ResourceCategory> categories) {
        int highest = 0;
        for (ResourceCategory category : categories) {
            highest = Math.max(highest, extractNumber(category.getId()));
        }
        return highest;
    }

    /* "CAT-000007" -> 7 */
    private static int extractNumber(String categoryId) {
        if (categoryId == null) {
            return 0;
        }
        String digitsOnly = categoryId.replaceAll("\\D+", "");
        return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
    }

    private static int highestUserId(List<User> users) {
        int highest = 0;
        for (User user : users) {
            highest = Math.max(highest, user.getId());
        }
        return highest;
    }

    private static int highestReservationId(List<User> users) {
        int highest = 0;
        for (User user : users) {
            if (user instanceof Employee employee) {
                for (Reservation reservation : employee.getReservations()) {
                    highest = Math.max(highest, reservation.getId());
                }
            }
        }
        return highest;
    }
}
