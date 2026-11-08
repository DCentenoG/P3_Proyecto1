package Service;

import Model.CategoryContainer;
import Model.Reservation;
import Model.ResourceCategory;
import Model.User;
import Model.UserContainer;
import Persistence.ConnectionManager;
import Persistence.SystemData;
import Persistence.SystemOracleRepository;

import java.sql.Connection;
import java.sql.SQLException;

/*
Fachada de persistencia compartida por toda la capa de servicio: el unico punto
por el que se lee y se escribe la base de datos.

COMO FUNCIONA
-------------
load() corre una sola vez, al arrancar el servidor, y deja en memoria
(SystemData) los contenedores con todo el estado del sistema. UserService,
ResourceService y ReservationService reciben esta misma instancia por
constructor, asi que todos trabajan sobre los mismos UserContainer y
CategoryContainer: si se agrega una categoria, el resto la ve de inmediato.

De ahi en adelante, cada operacion que modifica algo hace dos cosas:
  1. muta los objetos en memoria (eso lo hace el Model, con sus reglas), y
  2. llama a UNO de los metodos de escritura de esta clase, que traduce ese
     cambio a un INSERT, UPDATE o DELETE puntual sobre su tabla.

Esto reemplazo al save() global del Proyecto 1, que reescribia el archivo XML
completo despues de cada cambio.

POR QUE ALCANZA CON LEER UNA VEZ
--------------------------------
Porque hay un unico proceso backend, que es la arquitectura que describe el
enunciado: el servidor es el unico que toca la base, asi que nadie mas puede
dejar su copia en memoria desactualizada. Si hubiera varios servidores contra
la misma base, habria que consultar en cada operacion.

TRANSACCIONES
-------------
Cada metodo de escritura abre su propia conexion, hace su trabajo y hace commit.
Si algo falla, hace rollback y lanza ServiceException: la base queda como
estaba. Las operaciones que tocan varias tablas -- una reserva se reparte en
RESERVA, RESERVA_CATEGORIA y RESERVA_RECURSO -- viajan en una sola transaccion,
de modo que nunca queda a medias.
*/
public class Service {

    private final SystemOracleRepository repository;
    private SystemData data;

    public Service(ConnectionManager connections) {
        this.repository = new SystemOracleRepository(connections);
        this.data = new SystemData();
    }

    /** Debe llamarse una unica vez, al arrancar el servidor. */
    public void load() throws ServiceException {
        try {
            data = repository.load();
        } catch (SQLException e) {
            throw new ServiceException("No fue posible cargar los datos de la base de datos:\n"
                    + e.getMessage(), e);
        }
    }

    public UserContainer getUsers() {
        return data.getUsers();
    }

    public CategoryContainer getCategories() {
        return data.getCategories();
    }

    public SystemData getData() {
        return data;
    }

    //=======================================================================
    //  Escrituras: usuarios
    //=======================================================================

    public void insertUser(User user) throws ServiceException {
        write("registrar el usuario", connection ->
                repository.getUserDAO().insert(connection, user));
    }

    public void updateEmployee(int userId, String name, int phoneNumber) throws ServiceException {
        write("actualizar el funcionario", connection ->
                repository.getUserDAO().updateEmployee(connection, userId, name, phoneNumber));
    }

    public void updatePassword(int userId, String password) throws ServiceException {
        write("cambiar la clave", connection ->
                repository.getUserDAO().updatePassword(connection, userId, password));
    }

    public void deleteUser(int userId) throws ServiceException {
        write("eliminar el usuario", connection ->
                repository.getUserDAO().delete(connection, userId));
    }

    //=======================================================================
    //  Escrituras: categorias
    //=======================================================================

    public void insertCategory(ResourceCategory category) throws ServiceException {
        write("registrar la categoria", connection ->
                repository.getCategoryDAO().insert(connection, category));
    }

    public void updateCategoryDescription(String categoryId, String description) throws ServiceException {
        write("actualizar la categoria", connection ->
                repository.getCategoryDAO().updateDescription(connection, categoryId, description));
    }

    public void deleteCategory(String categoryId) throws ServiceException {
        write("eliminar la categoria", connection ->
                repository.getCategoryDAO().delete(connection, categoryId));
    }

    /** Cuantas reservas solicitaron esta categoria. Impide borrarla y dejar reservas apuntando a la nada. */
    public int countReservationsRequiring(String categoryId) throws ServiceException {
        return read("consultar el uso de la categoria", connection ->
                repository.getCategoryDAO().countReservationsRequiring(connection, categoryId));
    }

    //=======================================================================
    //  Escrituras: recursos
    //=======================================================================

    public void insertResource(String categoryId, int resourceId, String description) throws ServiceException {
        write("registrar el recurso", connection ->
                repository.getResourceDAO().insert(connection, categoryId, resourceId, description));
    }

    public void deleteResource(String categoryId, int resourceId) throws ServiceException {
        write("eliminar el recurso", connection ->
                repository.getResourceDAO().delete(connection, categoryId, resourceId));
    }

    /** Cuantas reservas tienen este recurso asignado. */
    public int countReservationsUsing(String categoryId, int resourceId) throws ServiceException {
        return read("consultar el uso del recurso", connection ->
                repository.getResourceDAO().countReservationsUsing(connection, categoryId, resourceId));
    }

    /**
     * Guarda un recurso que pudo cambiar de categoria, de id, de descripcion, o
     * de todo a la vez.
     * <p>
     * Hay que recibir la identidad ANTERIOR porque la identidad de un recurso es
     * la pareja (categoria, id): si cambia, las filas de RESERVA_RECURSO que lo
     * referencian quedan apuntando a algo que ya no existe. Las tres sentencias
     * van en una sola transaccion y en este orden, que es el unico que no viola
     * la clave foranea:
     * <ol>
     *   <li>crear la fila nueva en RECURSO,</li>
     *   <li>reapuntar las reservas a la identidad nueva,</li>
     *   <li>borrar la fila vieja.</li>
     * </ol>
     * Si la identidad no cambio, basta con actualizar la descripcion.
     */
    public void saveMovedResource(String oldCategoryId, int oldResourceId,
                                  String newCategoryId, int newResourceId,
                                  String description) throws ServiceException {
        boolean sameIdentity = oldCategoryId.equals(newCategoryId) && oldResourceId == newResourceId;
        write("guardar el recurso", connection -> {
            if (sameIdentity) {
                repository.getResourceDAO()
                        .updateDescription(connection, newCategoryId, newResourceId, description);
                return;
            }
            repository.getResourceDAO().insert(connection, newCategoryId, newResourceId, description);
            repository.getResourceDAO().repointReservationResources(connection,
                    oldCategoryId, oldResourceId, newCategoryId, newResourceId);
            repository.getResourceDAO().delete(connection, oldCategoryId, oldResourceId);
        });
    }

    //=======================================================================
    //  Escrituras: reservas
    //=======================================================================

    /** Inserta la reserva junto con sus categorias requeridas y recursos asignados. */
    public void insertReservation(int employeeId, Reservation reservation) throws ServiceException {
        write("registrar la reserva", connection ->
                repository.getReservationDAO().insert(connection, employeeId, reservation));
    }

    /** Reescribe la reserva y sus dos tablas puente a partir de como quedo en memoria. */
    public void updateReservation(Reservation reservation) throws ServiceException {
        write("actualizar la reserva", connection ->
                repository.getReservationDAO().update(connection, reservation));
    }

    //=======================================================================
    //  Plomeria de las transacciones
    //=======================================================================

    /* Lo que hace una escritura, para poder envolverla en una transaccion. */
    @FunctionalInterface
    private interface Write {
        void apply(Connection connection) throws SQLException;
    }

    /* Lo que hace una consulta puntual que devuelve un valor. */
    @FunctionalInterface
    private interface Read<T> {
        T apply(Connection connection) throws SQLException;
    }

    /*
    Abre la conexion, ejecuta, y hace commit o rollback. El rollback es lo que
    garantiza que una operacion de varias tablas no quede a medias: si la
    insercion en RESERVA_RECURSO falla, la fila de RESERVA tampoco queda.
    */
    private void write(String what, Write operation) throws ServiceException {
        try (Connection connection = repository.getConnections().open()) {
            try {
                operation.apply(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ServiceException("No fue posible " + what + " en la base de datos:\n"
                    + e.getMessage(), e);
        }
    }

    private <T> T read(String what, Read<T> operation) throws ServiceException {
        try (Connection connection = repository.getConnections().open()) {
            T result = operation.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            throw new ServiceException("No fue posible " + what + " en la base de datos:\n"
                    + e.getMessage(), e);
        }
    }
}
