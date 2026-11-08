package Model;

import Model.exceptions.ReservationNotFoundException;
import Model.exceptions.ResourceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Solicitar, editar y cancelar reservas de un funcionario.
 * <p>
 * Lo mas importante que se verifica aqui es la estabilidad del id: una reserva
 * conserva el mismo id durante toda su vida, aunque se edite o se cancele. De
 * eso depende que el Frontend pueda identificarla despues de que cruzo el
 * socket, porque lo que recibe es una copia y la comparacion por referencia ya
 * no sirve.
 */
@DisplayName("Employee: solicitudes de reserva y estabilidad del id")
class EmployeeTest {

    private UserContainer users;
    private ResourceCategory salas;
    private ResourceCategory proyectores;
    private Employee ana;
    private Employee luis;
    private Resource salaA;
    private Resource proyectorX;

    @BeforeEach
    void setUp() throws Exception {
        users = Fixture.usersNamed("Ana", "Luis");
        ana = users.getEmployeeById(1);
        luis = users.getEmployeeById(2);

        CategoryContainer categories = new CategoryContainer();
        salas = Fixture.categoryWith(categories, "Salas", 2);
        proyectores = Fixture.categoryWith(categories, "Proyectores", 1);

        salaA = salas.getResourceById(1);
        proyectorX = proyectores.getResourceById(1);
    }

    // --- tryBook ---

    @Test
    @DisplayName("Una solicitud nueva nace En revision, sin recursos y como notificacion no vista")
    void tryBookCreaUnaSolicitudPendiente() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10),
                List.of(salas), users, Fixture.NOW);

        assertEquals(1, ana.getReservations().size());
        Reservation solicitud = ana.getReservations().get(0);
        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty(),
                "el recurso lo asigna el administrador al aprobar");
        assertEquals(List.of(salas), solicitud.getRequiredCategories());
        assertFalse(solicitud.getNotification().isSeen());
        assertEquals(Fixture.NOW, solicitud.getNotification().getReceivedAt());
    }

    @Test
    @DisplayName("Cada solicitud recibe un id propio")
    void tryBookAsignaUnIdNuevo() throws Exception {
        ana.tryBook("Una", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        ana.tryBook("Otra", Fixture.DAY, Fixture.at(14), Fixture.at(16), List.of(salas), users, Fixture.NOW);

        int primera = ana.getReservations().get(0).getId();
        int segunda = ana.getReservations().get(1).getId();

        assertTrue(primera > Reservation.UNASSIGNED_ID);
        assertTrue(segunda > Reservation.UNASSIGNED_ID);
        assertNotEquals(primera, segunda);
    }

    @Test
    @DisplayName("Los ids no se repiten entre funcionarios distintos")
    void losIdsSonUnicosEnTodoElSistema() throws Exception {
        ana.tryBook("Una", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        luis.tryBook("Otra", Fixture.DAY, Fixture.at(14), Fixture.at(16), List.of(salas), users, Fixture.NOW);

        assertNotEquals(ana.getReservations().get(0).getId(),
                luis.getReservations().get(0).getId());
    }

    @Test
    @DisplayName("Si una categoria no tiene disponibilidad la solicitud NO se crea")
    void tryBookEsTodoONada() throws Exception {
        Fixture.accepted(luis, users, proyectores, proyectorX, 8, 10);

        ResourceUnavailableException error = assertThrows(ResourceUnavailableException.class,
                () -> ana.tryBook("Clase", Fixture.DAY, Fixture.at(9), Fixture.at(11),
                        List.of(salas, proyectores), users, Fixture.NOW));

        assertEquals(List.of("Proyectores"), error.getUnavailableCategories());
        assertTrue(ana.getReservations().isEmpty(),
                "no debe quedar una solicitud a medias");
    }

    // --- estabilidad del id ---

    @Test
    @DisplayName("El id de una reserva no cambia al editarla")
    void elIdSobreviveLaEdicion() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        Reservation solicitud = ana.getReservations().get(0);
        int id = solicitud.getId();

        ana.tryEdit(id, "Clase movida", Fixture.DAY, Fixture.at(14), Fixture.at(16),
                List.of(salas), users, Fixture.NOW.plusHours(1));

        assertEquals(1, ana.getReservations().size(), "editar no agrega una reserva nueva");
        assertSame(solicitud, ana.getReservations().get(0), "se modifica el mismo objeto");
        assertEquals(id, solicitud.getId());
        assertEquals("Clase movida", solicitud.getActivity());
        assertEquals(Fixture.at(14), solicitud.getStartTime());
    }

    @Test
    @DisplayName("El id de una reserva no cambia al cancelarla")
    void elIdSobreviveLaCancelacion() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        Reservation solicitud = ana.getReservations().get(0);
        int id = solicitud.getId();

        ana.cancelReservation(id);

        assertEquals(id, solicitud.getId());
        assertEquals(State.CANCELLED, solicitud.getState());
        assertEquals(1, ana.getReservations().size(), "cancelar no borra la reserva del sistema");
    }

    // --- tryEdit ---

    @Test
    @DisplayName("Editar una reserva Aceptada la devuelve a revision y libera sus recursos")
    void editarDevuelveARevision() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        Reservation solicitud = ana.getReservations().get(0);
        solicitud.approve(List.of(salaA), users);
        assertEquals(State.ACCEPTED, solicitud.getState());

        ana.tryEdit(solicitud.getId(), "Clase", Fixture.DAY, Fixture.at(14), Fixture.at(16),
                List.of(salas), users, Fixture.NOW.plusHours(1));

        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty());
        assertFalse(solicitud.getNotification().isSeen());
    }

    @Test
    @DisplayName("Al editar se puede reusar el unico recurso que la propia reserva ya tenia")
    void editarPuedeReusarSuPropioRecurso() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10),
                List.of(proyectores), users, Fixture.NOW);
        Reservation solicitud = ana.getReservations().get(0);
        solicitud.approve(List.of(proyectorX), users);

        // El horario nuevo se traslapa con el actual y el unico proyector del
        // sistema es el que ella misma tiene asignado: aun asi debe poder guardar.
        ana.tryEdit(solicitud.getId(), "Clase", Fixture.DAY, Fixture.at(9), Fixture.at(11),
                List.of(proyectores), users, Fixture.NOW.plusHours(1));

        assertEquals(Fixture.at(9), solicitud.getStartTime());
        assertEquals(State.IN_REVIEW, solicitud.getState());
    }

    @Test
    @DisplayName("Si la edicion no tiene disponibilidad la reserva queda intacta")
    void editarEsTodoONada() throws Exception {
        ana.tryBook("Original", Fixture.DAY, Fixture.at(14), Fixture.at(16),
                List.of(salas), users, Fixture.NOW);
        Reservation solicitud = ana.getReservations().get(0);
        Fixture.accepted(luis, users, proyectores, proyectorX, 8, 10);

        assertThrows(ResourceUnavailableException.class,
                () -> ana.tryEdit(solicitud.getId(), "Cambiada", Fixture.DAY,
                        Fixture.at(9), Fixture.at(11), List.of(proyectores), users,
                        Fixture.NOW.plusHours(1)));

        assertEquals("Original", solicitud.getActivity());
        assertEquals(Fixture.at(14), solicitud.getStartTime());
        assertEquals(List.of(salas), solicitud.getRequiredCategories());
    }

    @Test
    @DisplayName("Un funcionario no puede editar una reserva que no es suya")
    void noSeEditaUnaReservaAjena() throws Exception {
        luis.tryBook("De Luis", Fixture.DAY, Fixture.at(8), Fixture.at(10),
                List.of(salas), users, Fixture.NOW);
        int idDeLuis = luis.getReservations().get(0).getId();

        assertThrows(ReservationNotFoundException.class,
                () -> ana.tryEdit(idDeLuis, "Robada", Fixture.DAY, Fixture.at(8), Fixture.at(10),
                        List.of(salas), users, Fixture.NOW));
    }

    // --- busqueda por id ---

    @Test
    @DisplayName("ownsReservation distingue las reservas propias de las ajenas")
    void ownsReservation() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        int id = ana.getReservations().get(0).getId();

        assertTrue(ana.ownsReservation(id));
        assertFalse(luis.ownsReservation(id));
        assertFalse(ana.ownsReservation(id + 100));
    }

    @Test
    @DisplayName("getReservationById devuelve el mismo objeto de la lista")
    void getReservationByIdDevuelveLaMisma() throws Exception {
        ana.tryBook("Clase", Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of(salas), users, Fixture.NOW);
        Reservation esperada = ana.getReservations().get(0);

        assertSame(esperada, ana.getReservationById(esperada.getId()));
    }

    @Test
    @DisplayName("Un id desconocido falla con ReservationNotFoundException")
    void idDesconocidoFalla() {
        assertThrows(ReservationNotFoundException.class, () -> ana.getReservationById(404));
        assertThrows(ReservationNotFoundException.class, () -> ana.cancelReservation(404));
    }
}
