package Persistence;

import Model.Admin;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Camino de ESCRITURA de las cuatro DAOs contra la base real: insertar,
 * actualizar, borrar y volver a leer lo escrito.
 * <p>
 * Prueba de integracion (Failsafe Plugin). Cada prueba corre dentro de una
 * transaccion que se revierte al terminar, asi que <b>no deja ni un registro en
 * la base</b>; ver {@link OracleFixture} para el detalle de esa decision. La
 * ultima prueba de esta clase comprueba justamente eso.
 */
@DisplayName("Integracion: escritura de las DAOs contra Oracle")
class OracleDaoWriteIT {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);
    private static final String CATEGORY_ID = OracleFixture.CATEGORY_ID;
    private static final String OTHER_CATEGORY_ID = "CAT-900002";
    private static final int USER_ID = OracleFixture.USER_ID;
    private static final int RESERVATION_ID = OracleFixture.RESERVATION_ID;

    private CategoryDAO categoryDAO;
    private ResourceDAO resourceDAO;
    private UserDAO userDAO;
    private ReservationDAO reservationDAO;

    @BeforeEach
    void setUp() {
        OracleFixture.requireDatabase();
        categoryDAO = new CategoryDAO();
        resourceDAO = new ResourceDAO();
        userDAO = new UserDAO();
        reservationDAO = new ReservationDAO();
    }

    // --- CategoryDAO ---

    @Test
    @DisplayName("CategoryDAO: insertar, leer, renombrar y borrar una categoria")
    void categoryDAO_cicloCompleto() throws Exception {
        OracleFixture.inRollback(connection -> {
            categoryDAO.insert(connection, new ResourceCategory(CATEGORY_ID, "IT Categoria"));

            assertEquals("IT Categoria", descriptionOf(categoryDAO.findAll(connection), CATEGORY_ID));

            categoryDAO.updateDescription(connection, CATEGORY_ID, "IT Categoria renombrada");

            assertEquals("IT Categoria renombrada",
                    descriptionOf(categoryDAO.findAll(connection), CATEGORY_ID),
                    "renombrar no cambia la identidad: el id sigue siendo el mismo");

            categoryDAO.delete(connection, CATEGORY_ID);

            assertNull(findCategory(categoryDAO.findAll(connection), CATEGORY_ID));
        });
    }

    // --- ResourceDAO ---

    @Test
    @DisplayName("ResourceDAO: insertar, leer, renombrar y borrar un recurso")
    void resourceDAO_cicloCompleto() throws Exception {
        OracleFixture.inRollback(connection -> {
            categoryDAO.insert(connection, new ResourceCategory(CATEGORY_ID, "IT Categoria"));
            resourceDAO.insert(connection, CATEGORY_ID, 1, "IT Recurso 1");

            ResourceCategory leida = readCategoryWithResources(connection, CATEGORY_ID);
            assertEquals(1, leida.getSize());
            assertEquals("IT Recurso 1", leida.getResourceById(1).getDescription());
            assertEquals(CATEGORY_ID, leida.getResourceById(1).getCategoryId(),
                    "attachAll debe dejar la relacion en los dos sentidos");

            resourceDAO.updateDescription(connection, CATEGORY_ID, 1, "IT Recurso renombrado");

            assertEquals("IT Recurso renombrado",
                    readCategoryWithResources(connection, CATEGORY_ID).getResourceById(1).getDescription());

            resourceDAO.delete(connection, CATEGORY_ID, 1);

            assertEquals(0, readCategoryWithResources(connection, CATEGORY_ID).getSize());
        });
    }

    @Test
    @DisplayName("ResourceDAO: el mismo id de recurso puede existir en dos categorias distintas")
    void resourceDAO_elIdSoloEsUnicoDentroDeLaCategoria() throws Exception {
        OracleFixture.inRollback(connection -> {
            categoryDAO.insert(connection, new ResourceCategory(CATEGORY_ID, "IT Salas"));
            categoryDAO.insert(connection, new ResourceCategory(OTHER_CATEGORY_ID, "IT Proyectores"));

            resourceDAO.insert(connection, CATEGORY_ID, 1, "IT Sala 1");
            resourceDAO.insert(connection, OTHER_CATEGORY_ID, 1, "IT Proyector 1");

            Resource sala = readCategoryWithResources(connection, CATEGORY_ID).getResourceById(1);
            Resource proyector = readCategoryWithResources(connection, OTHER_CATEGORY_ID).getResourceById(1);

            assertEquals(1, sala.getId());
            assertEquals(1, proyector.getId());
            assertFalse(sala.isSameResource(proyector),
                    "la llave primaria compuesta (categoria, id) los mantiene separados");
        });
    }

    // --- UserDAO ---

    @Test
    @DisplayName("UserDAO: insertar, leer, editar y borrar un funcionario")
    void userDAO_cicloCompletoDeUnFuncionario() throws Exception {
        OracleFixture.inRollback(connection -> {
            userDAO.insert(connection, new Employee("IT Funcionario", 55550000, USER_ID, "clave-it"));

            Employee leido = (Employee) findUser(userDAO.findAll(connection), USER_ID);
            assertNotNull(leido);
            assertEquals("IT Funcionario", leido.getName());
            assertEquals(55550000, leido.getPhoneNumber());
            assertEquals("clave-it", leido.getPassword());

            userDAO.updateEmployee(connection, USER_ID, "IT Renombrado", 55559999);
            userDAO.updatePassword(connection, USER_ID, "clave-nueva");

            Employee actualizado = (Employee) findUser(userDAO.findAll(connection), USER_ID);
            assertEquals("IT Renombrado", actualizado.getName());
            assertEquals(55559999, actualizado.getPhoneNumber());
            assertEquals("clave-nueva", actualizado.getPassword());

            userDAO.delete(connection, USER_ID);

            assertNull(findUser(userDAO.findAll(connection), USER_ID));
        });
    }

    @Test
    @DisplayName("UserDAO: un administrador se guarda sin nombre ni telefono y se lee como Admin")
    void userDAO_administrador() throws Exception {
        OracleFixture.inRollback(connection -> {
            userDAO.insert(connection, new Admin(USER_ID, "clave-admin"));

            User leido = findUser(userDAO.findAll(connection), USER_ID);

            assertNotNull(leido);
            assertTrue(leido instanceof Admin, "el tipo guardado en la columna decide la clase");
            assertEquals("clave-admin", leido.getPassword());
        });
    }

    // --- ReservationDAO ---

    @Test
    @DisplayName("ReservationDAO: la reserva se guarda con sus dos tablas puente y se lee completa")
    void reservationDAO_insertaYLeeLaReservaCompleta() throws Exception {
        OracleFixture.inRollback(connection -> {
            Reservation original = seedReservation(connection);

            Reservation leida = readReservation(connection);

            assertEquals(original.getId(), leida.getId());
            assertEquals("IT Actividad", leida.getActivity());
            assertEquals(DAY, leida.getDate());
            assertEquals(LocalTime.of(8, 0), leida.getStartTime());
            assertEquals(LocalTime.of(10, 0), leida.getEndTime());
            assertEquals(State.ACCEPTED, leida.getState());
            assertEquals(1, leida.getRequiredCategories().size(), "RESERVA_CATEGORIA");
            assertEquals(1, leida.getAssignedResources().size(), "RESERVA_RECURSO");
            assertNotNull(leida.getNotification().getReceivedAt());
            assertFalse(leida.getNotification().isSeen());
        });
    }

    @Test
    @DisplayName("ReservationDAO: el recurso leido es la MISMA instancia que vive en su categoria")
    void reservationDAO_elRecursoEsLaMismaInstancia() throws Exception {
        OracleFixture.inRollback(connection -> {
            seedReservation(connection);

            List<ResourceCategory> categorias = categoryDAO.findAll(connection);
            resourceDAO.attachAll(connection, categorias);
            List<User> usuarios = userDAO.findAll(connection);
            reservationDAO.attachAll(connection, usuarios, categorias);

            Employee empleado = (Employee) findUser(usuarios, USER_ID);
            Reservation leida = empleado.getReservationById(RESERVATION_ID);
            ResourceCategory categoria = findCategory(categorias, CATEGORY_ID);

            assertSame(categoria.getResourceById(1), leida.getAssignedResources().get(0),
                    "si fueran copias, liberar el recurso al cancelar no se veria en el catalogo");
            assertSame(categoria, leida.getRequiredCategories().get(0));
        });
    }

    @Test
    @DisplayName("ReservationDAO: actualizar reescribe la reserva y sus tablas puente")
    void reservationDAO_actualiza() throws Exception {
        OracleFixture.inRollback(connection -> {
            Reservation reserva = seedReservation(connection);

            reserva.setActivity("IT Actividad editada");
            reserva.setStartTime(LocalTime.of(14, 0));
            reserva.setEndTime(LocalTime.of(16, 0));
            reserva.setState(State.CANCELLED);
            reserva.setAssignedResources(new ArrayList<>());
            reservationDAO.update(connection, reserva);

            Reservation leida = readReservation(connection);

            assertEquals("IT Actividad editada", leida.getActivity());
            assertEquals(LocalTime.of(14, 0), leida.getStartTime());
            assertEquals(State.CANCELLED, leida.getState());
            assertTrue(leida.getAssignedResources().isEmpty(),
                    "al cancelar se liberan los recursos tambien en la base");
            assertEquals(1, leida.getRequiredCategories().size(),
                    "las categorias pedidas se conservan");
        });
    }

    @Test
    @DisplayName("ReservationDAO: borrar la reserva borra en cascada sus filas puente")
    void reservationDAO_borrarArrastraLasFilasPuente() throws Exception {
        OracleFixture.inRollback(connection -> {
            seedReservation(connection);
            assertEquals(1, countWhereReservation(connection, "RESERVA_CATEGORIA"));
            assertEquals(1, countWhereReservation(connection, "RESERVA_RECURSO"));

            reservationDAO.delete(connection, RESERVATION_ID);

            assertEquals(0, countWhereReservation(connection, "RESERVA_CATEGORIA"));
            assertEquals(0, countWhereReservation(connection, "RESERVA_RECURSO"));
        });
    }

    @Test
    @DisplayName("ResourceDAO: contar y reapuntar las reservas que usan un recurso")
    void resourceDAO_contarYReapuntar() throws Exception {
        OracleFixture.inRollback(connection -> {
            seedReservation(connection);
            assertEquals(1, resourceDAO.countReservationsUsing(connection, CATEGORY_ID, 1));

            // Mover el recurso 1 al id 2 de la misma categoria: primero se crea el
            // destino, despues se reapunta y por ultimo se borra el origen. En otro
            // orden la clave foranea lo rechaza.
            resourceDAO.insert(connection, CATEGORY_ID, 2, "IT Recurso movido");
            resourceDAO.repointReservationResources(connection, CATEGORY_ID, 1, CATEGORY_ID, 2);
            resourceDAO.delete(connection, CATEGORY_ID, 1);

            assertEquals(0, resourceDAO.countReservationsUsing(connection, CATEGORY_ID, 1));
            assertEquals(1, resourceDAO.countReservationsUsing(connection, CATEGORY_ID, 2));
            assertEquals(2, readReservation(connection).getAssignedResources().get(0).getId(),
                    "la reserva debe seguir teniendo un recurso, ahora el nuevo");
        });
    }

    @Test
    @DisplayName("CategoryDAO: contar las reservas que requieren una categoria")
    void categoryDAO_contarReservasQueLaRequieren() throws Exception {
        OracleFixture.inRollback(connection -> {
            seedReservation(connection);

            assertEquals(1, categoryDAO.countReservationsRequiring(connection, CATEGORY_ID));
            assertEquals(0, categoryDAO.countReservationsRequiring(connection, "CAT-999999"));
        });
    }

    // --- la garantia de que estas pruebas no dejan basura ---

    @Test
    @DisplayName("Lo que escribe una prueba se revierte: la base queda igual que antes")
    void laTransaccionSiempreSeRevierte() throws Exception {
        int antes = countCategoriesInNewTransaction();

        OracleFixture.inRollback(connection -> {
            categoryDAO.insert(connection, new ResourceCategory(CATEGORY_ID, "IT Categoria"));
            assertEquals(antes + 1, countCategories(connection),
                    "dentro de la transaccion lo escrito ya se ve");
        });

        assertEquals(antes, countCategoriesInNewTransaction(),
                "despues del rollback no debe quedar ni un registro de prueba");
    }

    //--- apoyo ---

    /**
     * Deja en la base un funcionario, una categoria con un recurso y una reserva
     * Aceptada que los usa. Devuelve el objeto en memoria que se escribio.
     */
    private Reservation seedReservation(Connection connection) throws Exception {
        userDAO.insert(connection, new Employee("IT Funcionario", 55550000, USER_ID, "clave-it"));

        ResourceCategory categoria = new ResourceCategory(CATEGORY_ID, "IT Categoria");
        categoryDAO.insert(connection, categoria);
        resourceDAO.insert(connection, CATEGORY_ID, 1, "IT Recurso 1");
        categoria.addResource(1, "IT Recurso 1");

        Reservation reserva = new Reservation("IT Actividad", DAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
        reserva.setId(RESERVATION_ID);
        reserva.setRequiredCategories(new ArrayList<>(List.of(categoria)));
        reserva.setAssignedResources(new ArrayList<>(List.of(categoria.getResourceById(1))));
        reserva.setState(State.ACCEPTED);
        reserva.getNotification().receive(LocalDateTime.of(2026, 11, 8, 9, 0));

        reservationDAO.insert(connection, USER_ID, reserva);
        return reserva;
    }

    /** Vuelve a leer la reserva de prueba por el camino real de los DAOs. */
    private Reservation readReservation(Connection connection) throws Exception {
        List<ResourceCategory> categorias = categoryDAO.findAll(connection);
        resourceDAO.attachAll(connection, categorias);
        List<User> usuarios = userDAO.findAll(connection);
        reservationDAO.attachAll(connection, usuarios, categorias);
        return ((Employee) findUser(usuarios, USER_ID)).getReservationById(RESERVATION_ID);
    }

    private ResourceCategory readCategoryWithResources(Connection connection, String categoryId)
            throws Exception {
        List<ResourceCategory> categorias = categoryDAO.findAll(connection);
        resourceDAO.attachAll(connection, categorias);
        return findCategory(categorias, categoryId);
    }

    private static ResourceCategory findCategory(List<ResourceCategory> categories, String categoryId) {
        for (ResourceCategory category : categories) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }

    private static String descriptionOf(List<ResourceCategory> categories, String categoryId) {
        ResourceCategory found = findCategory(categories, categoryId);
        return found == null ? null : found.getDescription();
    }

    private static User findUser(List<User> users, int userId) {
        for (User user : users) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }

    private static int countWhereReservation(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE reserva_id = ?")) {
            statement.setInt(1, RESERVATION_ID);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static int countCategories(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM CATEGORIA");
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static int countCategoriesInNewTransaction() throws Exception {
        try (Connection connection = OracleFixture.connections().open()) {
            int total = countCategories(connection);
            connection.rollback();
            return total;
        }
    }
}
