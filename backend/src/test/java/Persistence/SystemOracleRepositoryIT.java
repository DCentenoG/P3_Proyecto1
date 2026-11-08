package Persistence;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Camino de LECTURA completo contra la base real: configuracion, conexion, las
 * seis tablas, los cuatro DAOs y la reconstruccion de los contenedores en
 * memoria.
 * <p>
 * Prueba de integracion: la corre el Failsafe Plugin por terminar el nombre en
 * {@code IT}, no Surefire. Es de solo lectura, no escribe nada.
 * <p>
 * Las verificaciones no dependen de CUANTOS datos haya en la base: funcionan con
 * la base vacia y con la base cargada. Lo que se comprueba es que el camino
 * entero responda y que el grafo de objetos quede bien armado.
 */
@DisplayName("Integracion: lectura completa de la base de datos Oracle")
class SystemOracleRepositoryIT {

    private SystemOracleRepository repository;

    @BeforeEach
    void setUp() {
        OracleFixture.requireDatabase();
        repository = new SystemOracleRepository(OracleFixture.connections());
    }

    @Test
    @DisplayName("Las seis tablas del esquema existen y responden")
    void lasSeisTablasExisten() throws Exception {
        String[] tables = {"USUARIO", "CATEGORIA", "RECURSO", "RESERVA",
                "RESERVA_CATEGORIA", "RESERVA_RECURSO"};

        OracleFixture.inRollback(connection -> {
            for (String table : tables) {
                assertTrue(countOf(connection, table) >= 0,
                        "la tabla " + table + " debe existir (si no, falta ejecutar db/schema.sql)");
            }
        });
    }

    @Test
    @DisplayName("load() devuelve los contenedores armados sin lanzar nada")
    void loadDevuelveLosContenedores() throws Exception {
        SystemData data = repository.load();

        assertNotNull(data);
        assertNotNull(data.getUsers());
        assertNotNull(data.getCategories());
    }

    @Test
    @DisplayName("Los contadores retoman desde el mayor id en uso, no desde la cantidad de filas")
    void losContadoresRetomanDesdeElMayorId() throws Exception {
        SystemData data = repository.load();

        int mayorIdDeUsuario = 0;
        int mayorIdDeReserva = 0;
        for (User user : data.getUsers().getUsers()) {
            mayorIdDeUsuario = Math.max(mayorIdDeUsuario, user.getId());
        }
        for (Employee employee : data.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                mayorIdDeReserva = Math.max(mayorIdDeReserva, reservation.getId());
            }
        }

        assertTrue(data.getUsers().getNextId() > mayorIdDeUsuario,
                "el siguiente id de usuario no puede repetir uno existente");
        assertTrue(data.getUsers().getNextReservationId() > mayorIdDeReserva,
                "el siguiente id de reserva no puede repetir uno existente");
        assertTrue(data.getCategories().getNextId() >= 1);
    }

    @Test
    @DisplayName("Cada recurso asignado a una reserva es la MISMA instancia que vive en su categoria")
    void losRecursosAsignadosSonLaMismaInstancia() throws Exception {
        SystemData data = repository.load();

        for (Employee employee : data.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                for (Resource assigned : reservation.getAssignedResources()) {
                    ResourceCategory owner = findCategory(data, assigned.getCategoryId());
                    assertNotNull(owner, "el recurso " + assigned + " apunta a una categoria inexistente");
                    assertSame(owner.getResourceById(assigned.getId()), assigned,
                            "si fueran copias distintas, liberar un recurso en una reserva no se"
                                    + " reflejaria en el catalogo");
                }
            }
        }
    }

    @Test
    @DisplayName("Cada categoria requerida por una reserva es la MISMA instancia del catalogo")
    void lasCategoriasRequeridasSonLaMismaInstancia() throws Exception {
        SystemData data = repository.load();

        for (Employee employee : data.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                for (ResourceCategory required : reservation.getRequiredCategories()) {
                    assertSame(findCategory(data, required.getId()), required);
                }
            }
        }
    }

    @Test
    @DisplayName("Toda reserva leida trae un id asignado")
    void todaReservaLeidaTieneId() throws Exception {
        SystemData data = repository.load();

        for (Employee employee : data.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                assertTrue(reservation.hasId(),
                        "una reserva sin id no se puede identificar desde el Frontend");
                assertNotNull(reservation.getState());
                assertNotNull(reservation.getDate());
            }
        }
    }

    //--- apoyo ---

    private static int countOf(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static ResourceCategory findCategory(SystemData data, String categoryId) {
        for (ResourceCategory category : data.getCategories().getCategories()) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }
}
