package Model;

import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ResourceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de negocio de una reserva: cuando dos reservas chocan, cuando un
 * recurso esta libre, y el flujo de estados (En revision -> Aceptada /
 * Rechazada / Cancelada).
 * <p>
 * Pruebas de unidad: no tocan base de datos, ni sockets, ni archivos. Las corre
 * el Surefire Plugin por terminar el nombre en {@code Test}.
 */
@DisplayName("Reservation: disponibilidad de recursos y flujo de estados")
class ReservationTest {

    private UserContainer users;
    private ResourceCategory salas;
    private ResourceCategory proyectores;
    private Employee ana;
    private Employee luis;
    private Resource salaA;
    private Resource salaB;
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
        salaB = salas.getResourceById(2);
        proyectorX = proyectores.getResourceById(1);
    }

    /** Solicitud de Ana de 1 a 3 de la tarde con las categorias indicadas. */
    private Reservation requestFromAna(ResourceCategory... required) throws Exception {
        ana.tryBook("Reunion", Fixture.DAY, Fixture.at(13), Fixture.at(15),
                List.of(required), users, Fixture.NOW);
        return ana.getReservations().get(ana.getReservations().size() - 1);
    }

    // --- overlaps ---

    @Test
    @DisplayName("Dos reservas del mismo dia que comparten aunque sea un rato se traslapan")
    void overlapsCuandoComparten() {
        Reservation temprano = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        Reservation cruzada = new Reservation("B", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        assertTrue(temprano.overlaps(cruzada));
        assertTrue(cruzada.overlaps(temprano), "el traslape debe ser simetrico");
    }

    @Test
    @DisplayName("Una reserva que termina justo cuando empieza la otra NO se traslapa")
    void noOverlapsCuandoSeTocan() {
        Reservation primera = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        Reservation segunda = new Reservation("B", Fixture.DAY, Fixture.at(10), Fixture.at(12));

        assertFalse(primera.overlaps(segunda));
        assertFalse(segunda.overlaps(primera));
    }

    @Test
    @DisplayName("El mismo horario en dias distintos no se traslapa")
    void noOverlapsEnDiasDistintos() {
        Reservation hoy = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        Reservation manana = new Reservation("B", Fixture.DAY.plusDays(1), Fixture.at(8), Fixture.at(10));

        assertFalse(hoy.overlaps(manana));
    }

    // --- isResourceFree ---

    @Test
    @DisplayName("Sin reservas en el sistema todo recurso esta libre")
    void recursoLibreSinReservas() {
        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(8), Fixture.at(10));

        assertTrue(nueva.isResourceFree(salaA, users, null));
    }

    @Test
    @DisplayName("Una reserva Aceptada ocupa su recurso en el horario que se traslapa")
    void recursoOcupadoPorReservaAceptada() throws Exception {
        Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        assertFalse(nueva.isResourceFree(salaA, users, null));
        assertTrue(nueva.isResourceFree(salaB, users, null), "la otra sala sigue libre");
    }

    @Test
    @DisplayName("Una reserva Aceptada en otro horario no ocupa nada")
    void recursoLibreFueraDelHorario() throws Exception {
        Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation tarde = new Reservation("Taller", Fixture.DAY, Fixture.at(14), Fixture.at(16));

        assertTrue(tarde.isResourceFree(salaA, users, null));
    }

    @Test
    @DisplayName("Una solicitud En revision NO ocupa recursos, aunque los tenga asignados")
    void enRevisionNoOcupa() {
        Reservation enRevision = new Reservation("Pendiente", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        enRevision.setId(99);
        assertTrue(enRevision.addResource(salaA, users));
        luis.getReservations().add(enRevision);
        assertEquals(State.IN_REVIEW, enRevision.getState());

        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        assertTrue(nueva.isResourceFree(salaA, users, null),
                "solo las reservas Aceptadas bloquean un recurso");
    }

    @Test
    @DisplayName("Al editar, la reserva original no se cuenta como conflicto de si misma")
    void laReservaIgnoradaNoBloquea() throws Exception {
        Reservation original = Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation editada = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        assertFalse(editada.isResourceFree(salaA, users, null));
        assertTrue(editada.isResourceFree(salaA, users, original),
                "la version editada debe poder quedarse con el recurso que ya tenia");
    }

    @Test
    @DisplayName("freeResourcesOf deja fuera el recurso ocupado")
    void freeResourcesOfFiltra() throws Exception {
        Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        List<Resource> libres = nueva.freeResourcesOf(salas, users, null);

        assertEquals(1, libres.size());
        assertEquals(salaB, libres.get(0));
    }

    @Test
    @DisplayName("addResource no agrega un recurso ocupado")
    void addResourceRechazaElOcupado() throws Exception {
        Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));

        assertFalse(nueva.addResource(salaA, users));
        assertTrue(nueva.getAssignedResources().isEmpty());
        assertTrue(nueva.addResource(salaB, users));
        assertEquals(1, nueva.getAssignedResources().size());
    }

    // --- approve ---

    @Test
    @DisplayName("Aprobar asigna un recurso por categoria y deja la reserva Aceptada")
    void aprobarAsignaUnRecursoPorCategoria() throws Exception {
        Reservation solicitud = requestFromAna(salas, proyectores);

        solicitud.approve(List.of(salaA, proyectorX), users);

        assertEquals(State.ACCEPTED, solicitud.getState());
        assertEquals(2, solicitud.getAssignedResources().size());
        assertTrue(solicitud.getAssignedResources().contains(salaA));
        assertTrue(solicitud.getAssignedResources().contains(proyectorX));
        assertNull(solicitud.getRejectionReason());
    }

    @Test
    @DisplayName("Si falta el recurso de una categoria no se asigna NADA")
    void aprobarEsTodoONadaSiFaltaUnaCategoria() throws Exception {
        Reservation solicitud = requestFromAna(salas, proyectores);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.approve(List.of(salaA), users));

        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty());
    }

    @Test
    @DisplayName("No se pueden elegir dos recursos de la misma categoria")
    void aprobarRechazaDosDeLaMismaCategoria() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.approve(List.of(salaA, salaB), users));

        assertEquals(State.IN_REVIEW, solicitud.getState());
    }

    @Test
    @DisplayName("No se puede colar un recurso de una categoria que la solicitud no pidio")
    void aprobarRechazaCategoriaNoPedida() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.approve(List.of(salaA, proyectorX), users));

        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty());
    }

    @Test
    @DisplayName("Aprobar con un recurso ya ocupado informa la categoria y no cambia la reserva")
    void aprobarConRecursoOcupado() throws Exception {
        Fixture.accepted(luis, users, salas, salaA, 13, 15);
        Reservation solicitud = requestFromAna(salas);

        ResourceUnavailableException error = assertThrows(ResourceUnavailableException.class,
                () -> solicitud.approve(List.of(salaA), users));

        assertEquals(List.of("Salas"), error.getUnavailableCategories());
        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty());
    }

    @Test
    @DisplayName("Una reserva ya Aceptada no se puede volver a aprobar")
    void noSePuedeAprobarDosVeces() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.approve(List.of(salaA), users);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.approve(List.of(salaB), users));
    }

    // --- reject ---

    @Test
    @DisplayName("Rechazar exige un motivo")
    void rechazarExigeMotivo() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        assertThrows(InvalidReservationOperationException.class, () -> solicitud.reject(null));
        assertThrows(InvalidReservationOperationException.class, () -> solicitud.reject("   "));

        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertNull(solicitud.getRejectionReason());
    }

    @Test
    @DisplayName("Rechazar guarda el motivo sin espacios sobrantes y libera los recursos")
    void rechazarGuardaElMotivo() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        solicitud.reject("  No hay presupuesto  ");

        assertEquals(State.REJECTED, solicitud.getState());
        assertEquals("No hay presupuesto", solicitud.getRejectionReason());
        assertTrue(solicitud.getAssignedResources().isEmpty());
    }

    @Test
    @DisplayName("Solo se rechaza una solicitud que esta En revision")
    void noSeRechazaUnaAceptada() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.approve(List.of(salaA), users);

        assertThrows(InvalidReservationOperationException.class, () -> solicitud.reject("Tarde"));
    }

    // --- cancel ---

    @Test
    @DisplayName("Cancelar una reserva Aceptada libera su recurso para los demas")
    void cancelarLiberaElRecurso() throws Exception {
        Reservation ocupada = Fixture.accepted(luis, users, salas, salaA, 8, 10);
        Reservation nueva = new Reservation("Taller", Fixture.DAY, Fixture.at(9), Fixture.at(11));
        assertFalse(nueva.isResourceFree(salaA, users, null));

        ocupada.cancel();

        assertEquals(State.CANCELLED, ocupada.getState());
        assertTrue(ocupada.getAssignedResources().isEmpty());
        assertTrue(nueva.isResourceFree(salaA, users, null));
    }

    @Test
    @DisplayName("Una reserva Rechazada no se puede cancelar")
    void noSeCancelaUnaRechazada() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.reject("Motivo");

        assertThrows(InvalidReservationOperationException.class, solicitud::cancel);
        assertEquals(State.REJECTED, solicitud.getState());
    }

    @Test
    @DisplayName("Una reserva ya Cancelada no se puede cancelar de nuevo")
    void noSeCancelaDosVeces() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.cancel();

        assertThrows(InvalidReservationOperationException.class, solicitud::cancel);
    }

    @Test
    @DisplayName("ensureCancellable no cambia nada: solo avisa si se puede o no")
    void ensureCancellableNoCambiaElEstado() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        solicitud.ensureCancellable();

        assertEquals(State.IN_REVIEW, solicitud.getState());
    }

    // --- returnToReview y notificaciones ---

    @Test
    @DisplayName("Volver a revision libera recursos, borra el motivo y entra como notificacion nueva")
    void volverARevision() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.markRequestSeen(Fixture.NOW.plusHours(1));
        solicitud.approve(List.of(salaA), users);

        LocalDateTime reentrada = Fixture.NOW.plusHours(2);
        solicitud.returnToReview(reentrada);

        assertEquals(State.IN_REVIEW, solicitud.getState());
        assertTrue(solicitud.getAssignedResources().isEmpty());
        assertNull(solicitud.getRejectionReason());
        assertFalse(solicitud.getNotification().isSeen(), "debe volver a aparecer como no vista");
        assertEquals(reentrada, solicitud.getNotification().getReceivedAt());
    }

    @Test
    @DisplayName("Marcar una solicitud como vista no cambia su estado")
    void marcarVistaNoCambiaElEstado() throws Exception {
        Reservation solicitud = requestFromAna(salas);

        solicitud.markRequestSeen(Fixture.NOW.plusMinutes(5));

        assertTrue(solicitud.getNotification().isSeen());
        assertEquals(Fixture.NOW.plusMinutes(5), solicitud.getNotification().getSeenAt());
        assertEquals(State.IN_REVIEW, solicitud.getState());
    }

    @Test
    @DisplayName("Una solicitud ya vista no se marca otra vez")
    void noSeMarcaVistaDosVeces() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.markRequestSeen(Fixture.NOW);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.markRequestSeen(Fixture.NOW.plusMinutes(1)));
    }

    @Test
    @DisplayName("Solo se marca como vista una solicitud que esta En revision")
    void soloSeMarcaVistaEnRevision() throws Exception {
        Reservation solicitud = requestFromAna(salas);
        solicitud.approve(List.of(salaA), users);

        assertThrows(InvalidReservationOperationException.class,
                () -> solicitud.markRequestSeen(Fixture.NOW));
    }

    // --- identidad ---

    @Test
    @DisplayName("Dos reservas con el mismo id son la misma, aunque sean objetos distintos")
    void igualdadPorId() {
        Reservation original = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        original.setId(7);
        Reservation copia = new Reservation("Otra actividad", Fixture.DAY.plusDays(3),
                Fixture.at(14), Fixture.at(16));
        copia.setId(7);

        assertEquals(original, copia, "la identidad es el id, no el contenido");
        assertEquals(original.hashCode(), copia.hashCode());
    }

    @Test
    @DisplayName("Reservas con id distinto no son iguales")
    void idsDistintosNoSonIguales() {
        Reservation una = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        una.setId(1);
        Reservation otra = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        otra.setId(2);

        assertNotEquals(una, otra);
    }

    @Test
    @DisplayName("Una reserva sin id asignado solo es igual a si misma")
    void sinIdSoloEsIgualASiMisma() {
        Reservation sinId = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        Reservation otraSinId = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));

        assertFalse(sinId.hasId());
        assertEquals(sinId, sinId);
        assertNotEquals(sinId, otraSinId, "dos reservas sin id no deben confundirse");
    }

    @Test
    @DisplayName("Una reserva no es igual a un objeto de otro tipo")
    void noEsIgualAOtroTipo() {
        Reservation reserva = new Reservation("A", Fixture.DAY, Fixture.at(8), Fixture.at(10));
        reserva.setId(1);

        assertNotEquals(reserva, "1");
    }
}
