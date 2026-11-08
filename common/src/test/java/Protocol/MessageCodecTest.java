package Protocol;

import Dto.ReservationSummary;
import Model.Employee;
import Model.Fixture;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Model.UserContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Serializacion de los mensajes que viajan por el socket.
 * <p>
 * Estas pruebas son de unidad aunque hablen de la red: no abren ningun socket,
 * serializan contra un arreglo de bytes en memoria. Lo que verifican es que el
 * grafo de objetos del dominio sobreviva el viaje, que es la condicion para que
 * la arquitectura distribuida funcione.
 */
@DisplayName("MessageCodec: los mensajes y el modelo sobreviven el viaje")
class MessageCodecTest {

    private UserContainer users;
    private Reservation aceptada;

    @BeforeEach
    void setUp() throws Exception {
        users = Fixture.acceptedScenario();
        aceptada = users.getListOfEmployees().get(0).getReservations().get(0);
    }

    /** Serializa y deserializa, que es lo que hace el socket pero sin socket. */
    private static Object roundTrip(Object message) throws IOException, ClassNotFoundException {
        return MessageCodec.decode(MessageCodec.encode(message));
    }

    // --- mensajes del protocolo ---

    @Test
    @DisplayName("Una peticion conserva el nombre del metodo y sus argumentos en orden")
    void requestIdaYVuelta() throws Exception {
        Request original = new Request("createReservation", Integer.valueOf(7), "Clase",
                LocalDate.of(2026, 11, 20));

        Request copia = (Request) roundTrip(original);

        assertEquals("createReservation", copia.getMethod());
        assertEquals(3, copia.getArgs().length);
        assertEquals(Integer.valueOf(7), copia.getArgs()[0]);
        assertEquals("Clase", copia.getArgs()[1]);
        assertEquals(LocalDate.of(2026, 11, 20), copia.getArgs()[2]);
    }

    @Test
    @DisplayName("Una peticion sin argumentos llega con un arreglo vacio, no null")
    void requestSinArgumentos() throws Exception {
        Request copia = (Request) roundTrip(new Request("listCategories"));

        assertEquals(0, copia.getArgs().length);
    }

    @Test
    @DisplayName("Una respuesta correcta lleva su resultado")
    void responseExitosaIdaYVuelta() throws Exception {
        Response copia = (Response) roundTrip(Response.success("sesion-42"));

        assertTrue(copia.isSuccessful());
        assertEquals("sesion-42", copia.getPayload());
        assertNull(copia.getErrorMessage());
    }

    @Test
    @DisplayName("El error viaja como texto, no como excepcion del Backend")
    void responseFallidaViajaComoTexto() throws Exception {
        Response copia = (Response) roundTrip(Response.failure("No hay disponibilidad para: Salas"));

        assertFalse(copia.isSuccessful());
        assertEquals("No hay disponibilidad para: Salas", copia.getErrorMessage());
        assertNull(copia.getPayload(), "una respuesta fallida no trae resultado");
    }

    @Test
    @DisplayName("El saludo conserva el canal y el id de sesion")
    void helloIdaYVuelta() throws Exception {
        Hello peticion = (Hello) roundTrip(Hello.request());
        assertEquals(Hello.Channel.REQUEST, peticion.getChannel());
        assertNull(peticion.getSessionId());

        Hello aviso = (Hello) roundTrip(Hello.push("sesion-42"));
        assertEquals(Hello.Channel.PUSH, aviso.getChannel());
        assertEquals("sesion-42", aviso.getSessionId());
    }

    @Test
    @DisplayName("Una notificacion de reserva nueva llega completa")
    void notificationIdaYVuelta() throws Exception {
        ReservationSummary resumen = new ReservationSummary(7, "Clase de redes", "Ana",
                Fixture.DAY, Fixture.at(8), Fixture.at(10), List.of("Salas 1", "Proyectores 1"));

        Notification copia = (Notification) roundTrip(Notification.newReservation(resumen));

        assertEquals(Notification.Type.NEW_RESERVATION, copia.getType());
        assertEquals(7, copia.getReservation().reservationId());
        assertEquals("Clase de redes - Ana", copia.getReservation().label());
        assertEquals(List.of("Salas 1", "Proyectores 1"), copia.getReservation().resourceDescriptions());
    }

    // --- el modelo del dominio ---

    @Test
    @DisplayName("Una reserva con referencias circulares se reconstruye sin duplicar objetos")
    void lasReferenciasCircularesSobreviven() throws Exception {
        Reservation copia = (Reservation) roundTrip(aceptada);

        Resource recurso = copia.getAssignedResources().get(0);
        ResourceCategory categoria = recurso.getResourceCategoryReference();

        assertSame(recurso, categoria.getResources().get(0),
                "la categoria debe apuntar al MISMO objeto recurso, no a una copia aparte");
        assertSame(categoria, copia.getRequiredCategories().get(0),
                "la categoria requerida y la del recurso deben ser el mismo objeto");
        assertNotSame(aceptada.getAssignedResources().get(0), recurso,
                "del otro lado del socket se recibe una copia, no el original");
    }

    @Test
    @DisplayName("La reserva conserva su id al cruzar el socket, asi que sigue siendo la misma")
    void laIdentidadSobreviveElViaje() throws Exception {
        Reservation copia = (Reservation) roundTrip(aceptada);

        assertNotSame(aceptada, copia);
        assertEquals(aceptada.getId(), copia.getId());
        assertEquals(aceptada, copia, "igualdad por id: es la misma reserva vista del otro lado");
        assertEquals(aceptada.hashCode(), copia.hashCode());
    }

    @Test
    @DisplayName("El estado, el horario y los recursos de la reserva llegan intactos")
    void elContenidoDeLaReservaLlegaIntacto() throws Exception {
        Reservation copia = (Reservation) roundTrip(aceptada);

        assertEquals(State.ACCEPTED, copia.getState());
        assertEquals(Fixture.DAY, copia.getDate());
        assertEquals(Fixture.at(8), copia.getStartTime());
        assertEquals(Fixture.at(10), copia.getEndTime());
        assertEquals(1, copia.getAssignedResources().size());
        assertEquals("Salas 1", copia.getAssignedResources().get(0).getDescription());
        assertEquals(Fixture.NOW, copia.getNotification().getReceivedAt());
    }

    @Test
    @DisplayName("El contenedor de usuarios completo viaja con sus reservas")
    void elContenedorCompletoViaja() throws Exception {
        UserContainer copia = (UserContainer) roundTrip(users);

        assertEquals(2, copia.getListOfEmployees().size());
        Employee ana = copia.getListOfEmployees().get(0);
        assertEquals("Ana", ana.getName());
        assertEquals(1, ana.getReservations().size());
        assertEquals(users.getNextReservationId(), copia.getNextReservationId(),
                "el contador de ids tambien es parte del estado");
    }

    @Test
    @DisplayName("Cada mensaje se serializa de cero: un objeto modificado viaja con su estado nuevo")
    void noSeArrastraElEstadoViejo() throws Exception {
        aceptada.setActivity("Primera version");
        Reservation primera = (Reservation) roundTrip(aceptada);

        aceptada.setActivity("Segunda version");
        Reservation segunda = (Reservation) roundTrip(aceptada);

        assertEquals("Primera version", primera.getActivity());
        assertEquals("Segunda version", segunda.getActivity(),
                "si el ObjectOutputStream se reusara sin reset(), aqui llegaria la version vieja");
    }

    // --- framing (prefijo de longitud) ---

    @Test
    @DisplayName("Un mensaje se escribe y se lee completo con su prefijo de longitud")
    void elMarcoSeEscribeYSeLee() throws Exception {
        byte[] contenido = MessageCodec.encode(new Request("listCategories"));
        ByteArrayOutputStream crudo = new ByteArrayOutputStream();

        MessageCodec.writeFrame(new DataOutputStream(crudo), contenido);

        DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(crudo.toByteArray()));
        assertArrayEquals(contenido, MessageCodec.readFrame(entrada));
    }

    @Test
    @DisplayName("Dos mensajes seguidos no se mezclan")
    void dosMensajesSeguidosNoSeMezclan() throws Exception {
        ByteArrayOutputStream crudo = new ByteArrayOutputStream();
        DataOutputStream salida = new DataOutputStream(crudo);
        MessageCodec.writeFrame(salida, MessageCodec.encode(new Request("primero")));
        MessageCodec.writeFrame(salida, MessageCodec.encode(new Request("segundo")));

        DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(crudo.toByteArray()));
        Request primero = (Request) MessageCodec.decode(MessageCodec.readFrame(entrada));
        Request segundo = (Request) MessageCodec.decode(MessageCodec.readFrame(entrada));

        assertEquals("primero", primero.getMethod());
        assertEquals("segundo", segundo.getMethod());
    }

    @Test
    @DisplayName("Un tamano de mensaje absurdo se rechaza antes de reservar memoria")
    void seRechazaUnTamanoInvalido() throws Exception {
        ByteArrayOutputStream crudo = new ByteArrayOutputStream();
        new DataOutputStream(crudo).writeInt(64 * 1024 * 1024);
        DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(crudo.toByteArray()));

        assertThrows(IOException.class, () -> MessageCodec.readFrame(entrada));
    }

    @Test
    @DisplayName("Si el otro extremo cierra entre dos mensajes se reporta EOF")
    void sinDatosSeReportaEof() {
        DataInputStream vacio = new DataInputStream(new ByteArrayInputStream(new byte[0]));

        assertThrows(EOFException.class, () -> MessageCodec.readFrame(vacio));
    }
}
