package Server;

import Model.Employee;
import Model.Reservation;
import Model.ResourceCategory;
import Protocol.Hello;
import Protocol.MessageCodec;
import Protocol.Notification;
import Protocol.Request;
import Protocol.Response;
import Service.IService;
import Service.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El servidor real, por un socket real, contra un cliente crudo.
 * <p>
 * Prueba de integracion (Failsafe Plugin): levanta un {@link ReservationServer}
 * en un puerto libre y le habla con el mismo {@link MessageCodec} que usa el
 * Proxy del Frontend. Lo que se verifica es el contrato de la red: el saludo, el
 * prefijo de longitud, el despacho por reflexion, como viajan los errores, que
 * cada cliente se atienda en su propio hilo y que las alertas se empujen a los
 * demas clientes y no a quien las origino.
 * <p>
 * No necesita base de datos: el {@code IService} del servidor es un doble de
 * prueba hecho con {@link Proxy}, que devuelve valores fijos. Asi la prueba mide
 * la red, no la persistencia, y no se rompe cuando {@code IService} gane
 * operaciones nuevas.
 * <p>
 * Esta clase vive en el modulo backend y el {@code ServiceProxy} se prueba
 * aparte, en el frontend: hacer que un modulo dependa del otro solo para probar
 * metaria el driver de Oracle en el classpath del Frontend y echaria a perder la
 * separacion que exige el enunciado.
 */
@DisplayName("Integracion: el servidor de reservas por socket")
class ReservationServerIT {

    private static final int READ_TIMEOUT_MS = 5_000;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    private static int port;

    /** Argumentos con que el servidor llamo a createReservation. */
    private static final AtomicReference<Object[]> lastCreateArgs = new AtomicReference<>();

    @BeforeAll
    static void startServer() throws Exception {
        port = freePort();
        ReservationServer server = new ReservationServer(port, fakeService());
        Thread thread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // El hilo muere al terminar las pruebas; no hay nada que reportar.
            }
        }, "servidor-de-prueba");
        thread.setDaemon(true);
        thread.start();
        waitUntilListening();
    }

    // --- saludo ---

    @Test
    @DisplayName("El saludo del canal de peticiones devuelve un id de sesion")
    void elSaludoDevuelveUnIdDeSesion() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            assertFalse(client.sessionId().isBlank());
        }
    }

    @Test
    @DisplayName("Dos clientes reciben sesiones distintas y se atienden a la vez")
    void dosClientesSeAtiendenEnParalelo() throws Exception {
        try (RawClient primero = RawClient.requestChannel();
             RawClient segundo = RawClient.requestChannel()) {

            assertNotEquals(primero.sessionId(), segundo.sessionId());

            // Intercalados a proposito: cada conexion tiene su propio hilo en el
            // servidor, asi que una no debe quedar esperando a la otra.
            Response unoA = primero.call(new Request("listCategories"));
            Response dosA = segundo.call(new Request("listCategories"));
            Response unoB = primero.call(new Request("listCategories"));

            assertTrue(unoA.isSuccessful());
            assertTrue(dosA.isSuccessful());
            assertTrue(unoB.isSuccessful());
        }
    }

    @Test
    @DisplayName("Si el primer mensaje no es un saludo, el servidor rechaza y cierra")
    void unSaludoInvalidoCierraLaConexion() throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), READ_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            MessageCodec.writeFrame(out, MessageCodec.encode(new Request("listCategories")));
            Response respuesta = (Response) MessageCodec.decode(MessageCodec.readFrame(in));

            assertFalse(respuesta.isSuccessful());
            assertEquals("Saludo invalido.", respuesta.getErrorMessage());
            assertThrows(EOFException.class, () -> MessageCodec.readFrame(in),
                    "el servidor debe cerrar la conexion despues de rechazarla");
        }
    }

    @Test
    @DisplayName("Un canal de notificaciones con una sesion inventada se rechaza")
    void unaSesionDesconocidaSeRechaza() throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), READ_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            MessageCodec.writeFrame(out, MessageCodec.encode(Hello.push("sesion-que-no-existe")));
            Response respuesta = (Response) MessageCodec.decode(MessageCodec.readFrame(in));

            assertFalse(respuesta.isSuccessful());
            assertEquals("Sesion desconocida.", respuesta.getErrorMessage());
        }
    }

    // --- despacho de operaciones ---

    @Test
    @DisplayName("El nombre del metodo basta para despachar la operacion por reflexion")
    void sePuedeInvocarUnaOperacionPorSuNombre() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            Response respuesta = client.call(new Request("listCategories"));

            assertTrue(respuesta.isSuccessful());
            @SuppressWarnings("unchecked")
            List<ResourceCategory> categorias = (List<ResourceCategory>) respuesta.getPayload();
            assertEquals(1, categorias.size());
            assertEquals("Salas", categorias.get(0).getDescription());
        }
    }

    @Test
    @DisplayName("Los argumentos llegan al servidor en el mismo orden y con su tipo")
    void losArgumentosLleganEnOrden() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            Response respuesta = client.call(new Request("createReservation",
                    Integer.valueOf(1), "Clase de redes", DAY,
                    LocalTime.of(8, 0), LocalTime.of(10, 0),
                    new ArrayList<>(List.of("Salas"))));

            assertTrue(respuesta.isSuccessful(), respuesta.getErrorMessage());

            Object[] recibidos = lastCreateArgs.get();
            assertEquals(6, recibidos.length);
            assertEquals(Integer.valueOf(1), recibidos[0]);
            assertEquals("Clase de redes", recibidos[1]);
            assertEquals(DAY, recibidos[2]);
            assertEquals(LocalTime.of(8, 0), recibidos[3]);
            assertEquals(LocalTime.of(10, 0), recibidos[4]);
            assertEquals(List.of("Salas"), recibidos[5]);

            Reservation devuelta = (Reservation) respuesta.getPayload();
            assertEquals(77, devuelta.getId(), "la reserva vuelve con su id, que es su identidad");
        }
    }

    @Test
    @DisplayName("Una operacion que no existe en IService se rechaza con un mensaje claro")
    void unaOperacionDesconocidaSeRechaza() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            Response respuesta = client.call(new Request("borrarTodaLaBase"));

            assertFalse(respuesta.isSuccessful());
            assertEquals("Operacion desconocida: borrarTodaLaBase", respuesta.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Un error de negocio viaja como texto, sin la excepcion del Backend")
    void elErrorDeNegocioViajaComoTexto() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            Response respuesta = client.call(new Request("login", Integer.valueOf(1), "clave-mala"));

            assertFalse(respuesta.isSuccessful());
            assertEquals("Usuario o clave incorrectos.", respuesta.getErrorMessage());
            assertNull(respuesta.getPayload(),
                    "si viajara la excepcion, el Frontend fallaria al no tener sus clases");
        }
    }

    @Test
    @DisplayName("Un error inesperado no expone el detalle interno al cliente")
    void unErrorInesperadoNoSeFiltra() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            Response respuesta = client.call(new Request("listEmployees"));

            assertFalse(respuesta.isSuccessful());
            assertEquals("Error interno del servidor. Intente de nuevo.", respuesta.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Despues de un error la conexion sigue sirviendo")
    void laConexionSobreviveUnError() throws Exception {
        try (RawClient client = RawClient.requestChannel()) {
            assertFalse(client.call(new Request("operacionInventada")).isSuccessful());

            assertTrue(client.call(new Request("listCategories")).isSuccessful(),
                    "un error de una peticion no debe desincronizar el socket");
        }
    }

    // --- alertas empujadas (canal de notificaciones) ---

    @Test
    @DisplayName("Al crear una reserva, los demas clientes reciben la alerta")
    void losDemasClientesRecibenLaAlerta() throws Exception {
        try (RawClient ana = RawClient.requestChannel();
             RawClient luis = RawClient.requestChannel()) {

            PushClient avisosDeAna = PushClient.forSession(ana.sessionId());
            try {
                Notification aviso = provokeNotification(luis, avisosDeAna);

                assertEquals(Notification.Type.NEW_RESERVATION, aviso.getType());
                assertEquals(77, aviso.getReservation().reservationId());
                assertEquals("Clase de redes", aviso.getReservation().activity());
                assertEquals("Ana", aviso.getReservation().employeeName());
                assertEquals(List.of("Salas"), aviso.getReservation().resourceDescriptions(),
                        "una reserva nueva no tiene recursos todavia: se informan las categorias");
            } finally {
                avisosDeAna.close();
            }
        }
    }

    @Test
    @DisplayName("Quien crea la reserva NO recibe su propia alerta")
    void quienLaCreaNoRecibeSuPropiaAlerta() throws Exception {
        try (RawClient ana = RawClient.requestChannel();
             RawClient luis = RawClient.requestChannel()) {

            PushClient avisosDeLuis = PushClient.forSession(luis.sessionId());
            try {
                // Primero se comprueba que el canal de Luis esta vivo y recibiendo:
                // Ana crea una reserva y el aviso llega. Sin este paso, la prueba
                // pasaria igual si el canal no estuviera registrado, que es
                // exactamente lo que NO se quiere probar.
                provokeNotification(ana, avisosDeLuis);
                avisosDeLuis.drain();

                luis.call(createReservation("Clase propia"));

                // broadcastExcept debe excluir la sesion que origino el cambio.
                avisosDeLuis.assertNothingArrives();
            } finally {
                avisosDeLuis.close();
            }
        }
    }

    @Test
    @DisplayName("Una operacion que no crea reservas no genera alertas")
    void soloLaCreacionDeReservasGeneraAlerta() throws Exception {
        try (RawClient ana = RawClient.requestChannel();
             RawClient luis = RawClient.requestChannel()) {

            PushClient avisosDeAna = PushClient.forSession(ana.sessionId());
            try {
                provokeNotification(luis, avisosDeAna);
                avisosDeAna.drain();

                luis.call(new Request("listCategories"));

                avisosDeAna.assertNothingArrives();
            } finally {
                avisosDeAna.close();
            }
        }
    }

    /**
     * Hace que {@code creador} registre reservas hasta que llegue la primera
     * alerta al canal indicado, y la devuelve.
     * <p>
     * El reintento no es por lentitud. El servidor contesta el saludo del canal
     * de avisos ANTES de registrarlo en el {@code ClientRegistry}, asi que hay un
     * instante en que la sesion ya saludo pero todavia no recibe nada: una alerta
     * emitida en ese instante se pierde. En la aplicacion real la ventana es
     * inofensiva, porque el Frontend abre los dos canales al arrancar, mucho antes
     * de que alguien registre una reserva. En una prueba que abre el canal y
     * dispara el evento de inmediato, en cambio, se cae dentro de la ventana cada
     * tanto. Lo que se verifica aqui es que la alerta llegue, no cuanto tarda.
     */
    private static Notification provokeNotification(RawClient creador, PushClient canal) throws Exception {
        for (int intento = 0; intento < 10; intento++) {
            creador.call(createReservation("Clase de redes"));
            Notification arrived = canal.pollNotification();
            if (arrived != null) {
                return arrived;
            }
        }
        throw new AssertionError("el canal de notificaciones nunca recibio la alerta");
    }

    private static Request createReservation(String activity) {
        return new Request("createReservation", Integer.valueOf(1), activity, DAY,
                LocalTime.of(8, 0), LocalTime.of(10, 0), new ArrayList<>(List.of("Salas")));
    }

    //--- doble de prueba de IService ---

    /**
     * {@code IService} tiene mas de treinta operaciones. En vez de escribirlas
     * todas vacias, se implementa la interfaz con un proxy dinamico que responde
     * solo a las que estas pruebas usan. Ademas, asi la prueba no se rompe cuando
     * se le agregue una operacion nueva a la interfaz.
     */
    private static IService fakeService() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "listCategories" -> new ArrayList<>(List.of(new ResourceCategory("CAT-000001", "Salas")));
            case "findEmployeeById" -> new Employee("Ana", 88880000, 1, "1");
            case "createReservation" -> {
                lastCreateArgs.set(args);
                yield reservationFrom(args);
            }
            case "login" -> throw new ServiceException("Usuario o clave incorrectos.");
            // Falla con algo que NO es ServiceException, para comprobar que el
            // servidor no le filtra el detalle interno al cliente.
            case "listEmployees" -> throw new IllegalStateException("fallo inesperado de prueba");
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (IService) Proxy.newProxyInstance(IService.class.getClassLoader(),
                new Class<?>[]{IService.class}, handler);
    }

    @SuppressWarnings("unchecked")
    private static Reservation reservationFrom(Object[] args) {
        Reservation reservation = new Reservation((String) args[1], (LocalDate) args[2],
                (LocalTime) args[3], (LocalTime) args[4]);
        reservation.setId(77);
        ArrayList<ResourceCategory> required = new ArrayList<>();
        for (String description : (List<String>) args[5]) {
            required.add(new ResourceCategory("CAT-000001", description));
        }
        reservation.setRequiredCategories(required);
        return reservation;
    }

    //--- clientes crudos ---

    /** Canal de peticiones, hablado a mano con el mismo codec que usa el Proxy real. */
    private static final class RawClient implements AutoCloseable {

        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final String sessionId;

        private RawClient() throws IOException, ClassNotFoundException {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), READ_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            MessageCodec.writeFrame(out, MessageCodec.encode(Hello.request()));
            Response saludo = (Response) MessageCodec.decode(MessageCodec.readFrame(in));
            if (!saludo.isSuccessful()) {
                throw new IOException("el servidor rechazo el saludo: " + saludo.getErrorMessage());
            }
            sessionId = (String) saludo.getPayload();
        }

        static RawClient requestChannel() throws IOException, ClassNotFoundException {
            return new RawClient();
        }

        String sessionId() {
            return sessionId;
        }

        Response call(Request request) throws IOException, ClassNotFoundException {
            MessageCodec.writeFrame(out, MessageCodec.encode(request));
            return (Response) MessageCodec.decode(MessageCodec.readFrame(in));
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    /** Canal de notificaciones de una sesion ya abierta. */
    private static final class PushClient implements AutoCloseable {

        /**
         * Las esperas son cortas a proposito: el servidor emite el aviso
         * inmediatamente despues de contestarle a quien origino el cambio, asi que
         * si no llega en medio segundo es que no va a llegar. Quien espera un aviso
         * reintenta (ver {@code provokeNotification}) en vez de esperar mas.
         */
        private static final int POLL_MS = 500;
        private static final int BRIEF_MS = 1_500;

        private final Socket socket;
        private final DataInputStream in;

        private PushClient(String sessionId) throws IOException, ClassNotFoundException {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), READ_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            MessageCodec.writeFrame(out, MessageCodec.encode(Hello.push(sessionId)));
            // El servidor confirma el saludo ANTES de registrar el canal, para que
            // ninguna notificacion pueda adelantarsele.
            Response saludo = (Response) MessageCodec.decode(MessageCodec.readFrame(in));
            if (!saludo.isSuccessful()) {
                throw new IOException("el servidor rechazo el canal de avisos: " + saludo.getErrorMessage());
            }
        }

        static PushClient forSession(String sessionId) throws IOException, ClassNotFoundException {
            return new PushClient(sessionId);
        }

        /** El siguiente aviso, o null si no llega ninguno en un plazo corto. */
        Notification pollNotification() throws IOException, ClassNotFoundException {
            socket.setSoTimeout(POLL_MS);
            try {
                return (Notification) MessageCodec.decode(MessageCodec.readFrame(in));
            } catch (SocketTimeoutException nothingYet) {
                return null;
            }
        }

        /** Comprueba que NO llegue ningun aviso, e informa cual llego si llega. */
        void assertNothingArrives() throws IOException, ClassNotFoundException {
            socket.setSoTimeout(BRIEF_MS);
            try {
                Object unexpected = MessageCodec.decode(MessageCodec.readFrame(in));
                throw new AssertionError("no se esperaba ningun aviso, pero llego: " + unexpected);
            } catch (SocketTimeoutException expected) {
                // Correcto: no llego nada.
            }
        }

        /** Consume los avisos que hubieran quedado pendientes. */
        void drain() throws IOException, ClassNotFoundException {
            socket.setSoTimeout(POLL_MS);
            try {
                while (true) {
                    MessageCodec.decode(MessageCodec.readFrame(in));
                }
            } catch (SocketTimeoutException done) {
                // No queda nada pendiente.
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    //--- arranque del servidor de prueba ---

    /** Un puerto libre, para no chocar con el 5000 que usa el servidor de verdad. */
    private static int freePort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    private static void waitUntilListening() throws Exception {
        IOException last = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            try (Socket probe = new Socket()) {
                probe.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("el servidor de prueba no llego a escuchar en el puerto " + port, last);
    }
}
