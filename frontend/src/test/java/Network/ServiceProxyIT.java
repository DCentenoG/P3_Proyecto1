package Network;

import Model.Employee;
import Model.ResourceCategory;
import Model.User;
import Protocol.Hello;
import Protocol.MessageCodec;
import Protocol.Request;
import Protocol.Response;
import Service.IService;
import Service.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El Proxy real del Frontend contra un servidor de mentira.
 * <p>
 * Prueba de integracion (Failsafe Plugin): abre un socket de verdad. Lo que
 * verifica es el lado cliente del patron Proxy, que es la indicacion 1 del
 * enunciado: que {@link ServiceProxy} traduzca cada llamada de
 * {@link IService} al mensaje correcto, devuelva el resultado como si la
 * operacion fuera local, y convierta el error que vino por la red en una
 * {@link ServiceException} con el mismo mensaje que vería el usuario si todo
 * corriera en un solo proceso.
 * <p>
 * El servidor de esta prueba no es el {@code ReservationServer}: es un doble
 * minimo armado con el mismo {@link MessageCodec}. A proposito, para que el
 * modulo frontend no dependa del backend ni en alcance de prueba — si lo
 * hiciera, el driver de Oracle entraria en su classpath y se perderia la
 * separacion que exige el enunciado. El servidor de verdad se prueba aparte, en
 * {@code backend}.
 */
@DisplayName("Integracion: el Proxy del Frontend por socket")
class ServiceProxyIT {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);
    private static final String SESSION_ID = "sesion-de-prueba";

    private StubServer server;
    private ServiceProxy proxy;

    @BeforeEach
    void setUp() throws Exception {
        server = StubServer.started();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (proxy != null) {
            proxy.close();
        }
        server.close();
    }

    /** Conecta el Proxy real al servidor de mentira. */
    private ServiceProxy connect() throws IOException {
        proxy = new ServiceProxy("127.0.0.1", server.port());
        return proxy;
    }

    // --- saludo ---

    @Test
    @DisplayName("Al construirse, el Proxy saluda y guarda el id de sesion")
    void elProxySaludaYGuardaLaSesion() throws Exception {
        ServiceProxy proxy = connect();

        assertEquals(SESSION_ID, proxy.getSessionId(),
                "el id de sesion es lo que despues identifica el canal de notificaciones");
        assertEquals(Hello.Channel.REQUEST, server.awaitHello().getChannel());
    }

    @Test
    @DisplayName("Si el servidor rechaza el saludo, el Proxy no se construye")
    void siElSaludoEsRechazadoElProxyNoSeConstruye() {
        server.rejectHandshake();

        assertThrows(IOException.class, this::connect);
    }

    // --- traduccion de llamadas ---

    @Test
    @DisplayName("Una llamada se traduce al nombre del metodo con sus argumentos en orden")
    void laLlamadaSeTraduceAlMensajeCorrecto() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.success(new Employee("Ana", 88880000, 7, "7")));

        proxy.findEmployeeById(7);

        Request enviada = server.awaitRequest();
        assertEquals("findEmployeeById", enviada.getMethod());
        assertEquals(1, enviada.getArgs().length);
        assertEquals(Integer.valueOf(7), enviada.getArgs()[0]);
    }

    @Test
    @DisplayName("El resultado del servidor se devuelve como si la operacion fuera local")
    void elResultadoSeDevuelveTalCual() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.success(new Employee("Ana", 88880000, 7, "7")));

        Employee recibido = proxy.findEmployeeById(7);

        assertNotNull(recibido);
        assertEquals("Ana", recibido.getName());
        assertEquals(7, recibido.getId());
    }

    @Test
    @DisplayName("Una lista viaja completa")
    void unaListaViajaCompleta() throws Exception {
        ServiceProxy proxy = connect();
        ArrayList<ResourceCategory> catalogo = new ArrayList<>(List.of(
                new ResourceCategory("CAT-000001", "Salas"),
                new ResourceCategory("CAT-000002", "Proyectores")));
        server.willRespond(Response.success(catalogo));

        List<ResourceCategory> recibidas = proxy.listCategories();

        assertEquals(2, recibidas.size());
        assertEquals("Salas", recibidas.get(0).getDescription());
        assertEquals("Proyectores", recibidas.get(1).getDescription());
    }

    @Test
    @DisplayName("Una operacion sin resultado no devuelve nada y no falla")
    void unaOperacionSinResultado() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.success(null));

        proxy.changePassword(7, "vieja", "nueva");

        Request enviada = server.awaitRequest();
        assertEquals("changePassword", enviada.getMethod());
        assertEquals(3, enviada.getArgs().length);
    }

    @Test
    @DisplayName("Cada llamada manda el estado actual de sus argumentos, no el de la anterior")
    void cadaLlamadaMandaElEstadoActual() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.success(null));
        server.willRespond(Response.success(null));

        List<String> categorias = new ArrayList<>(List.of("Salas"));
        proxy.createReservation(1, "Primera", DAY, LocalTime.of(8, 0), LocalTime.of(10, 0), categorias);
        categorias.add("Proyectores");
        proxy.createReservation(1, "Segunda", DAY, LocalTime.of(14, 0), LocalTime.of(16, 0), categorias);

        Request primera = server.awaitRequest();
        Request segunda = server.awaitRequest();

        assertEquals(List.of("Salas"), primera.getArgs()[5]);
        assertEquals(List.of("Salas", "Proyectores"), segunda.getArgs()[5],
                "si el ObjectOutputStream se reusara sin reset(), aqui llegaria la lista vieja");
    }

    // --- errores ---

    @Test
    @DisplayName("Un error del servidor llega como ServiceException con el mismo mensaje")
    void elErrorLlegaComoServiceException() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.failure("No hay disponibilidad para: Salas"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> proxy.createReservation(1, "Clase", DAY, LocalTime.of(8, 0), LocalTime.of(10, 0),
                        new ArrayList<>(List.of("Salas"))));

        assertEquals("No hay disponibilidad para: Salas", error.getMessage(),
                "el usuario debe ver el mismo mensaje que veria sin red de por medio");
    }

    @Test
    @DisplayName("Un error de negocio NO rompe la conexion: se puede seguir trabajando")
    void unErrorDeNegocioNoRompeLaConexion() throws Exception {
        ServiceProxy proxy = connect();
        server.willRespond(Response.failure("No hay disponibilidad."));
        server.willRespond(Response.success(new ArrayList<ResourceCategory>()));

        assertThrows(ServiceException.class, () -> proxy.findEmployeeById(7));

        assertNotNull(proxy.listCategories(), "la conexion debe seguir viva");
    }

    @Test
    @DisplayName("Una respuesta no reconocida cierra la conexion en vez de seguir desincronizada")
    void unaRespuestaNoReconocidaCierraLaConexion() throws Exception {
        ServiceProxy proxy = connect();
        // El servidor manda un Request donde deberia ir un Response.
        server.willRespondRaw(new Request("esto-no-es-una-respuesta"));

        assertThrows(ServiceException.class, () -> proxy.findEmployeeById(7));

        ServiceException despues = assertThrows(ServiceException.class, () -> proxy.listCategories());
        assertTrue(despues.getMessage().contains("cerrada"),
                "tras cerrarse, el Proxy debe decirlo claro: " + despues.getMessage());
    }

    @Test
    @DisplayName("Si el servidor se cae, la llamada falla con un mensaje entendible")
    void siElServidorSeCaeLaLlamadaFalla() throws Exception {
        ServiceProxy proxy = connect();
        server.close();

        ServiceException error = assertThrows(ServiceException.class, () -> proxy.findEmployeeById(7));

        assertTrue(error.getMessage().contains("comunicacion") || error.getMessage().contains("cerrada"),
                "mensaje poco claro: " + error.getMessage());
    }

    @Test
    @DisplayName("Despues de close() no se puede seguir llamando")
    void despuesDeCerrarNoSePuedeLlamar() throws Exception {
        ServiceProxy proxy = connect();

        proxy.close();
        proxy.close(); // idempotente

        ServiceException error = assertThrows(ServiceException.class, () -> proxy.listCategories());
        assertTrue(error.getMessage().contains("cerrada"), error.getMessage());
    }

    @Test
    @DisplayName("Usado solo como IService, el Proxy responde como si el servicio fuera local")
    void elProxySeUsaComoIService() throws Exception {
        // El resto del Frontend conoce unicamente este tipo: no sabe que hay una
        // red de por medio. Eso es lo que aporta el patron Proxy.
        IService servicio = connect();
        server.willRespond(Response.success(new Employee("Ana", 88880000, 7, "7")));

        User usuario = servicio.findEmployeeById(7);

        assertEquals(7, usuario.getId());
        assertEquals("findEmployeeById", server.awaitRequest().getMethod());
    }

    //--- servidor de mentira ---

    /**
     * Atiende UNA conexion: responde el saludo y despues contesta cada peticion
     * con lo que se le haya dejado en la cola, guardando lo que recibio para que
     * la prueba lo revise.
     */
    private static final class StubServer implements AutoCloseable {

        private static final int WAIT_SECONDS = 5;

        private final ServerSocket serverSocket;
        private final BlockingQueue<Request> received = new ArrayBlockingQueue<>(16);
        private final BlockingQueue<Hello> hellos = new ArrayBlockingQueue<>(4);
        private final BlockingQueue<Object> scripted = new ArrayBlockingQueue<>(16);
        private volatile boolean rejectHandshake;
        private volatile Socket client;

        private StubServer() throws IOException {
            serverSocket = new ServerSocket(0);
        }

        static StubServer started() throws IOException {
            StubServer stub = new StubServer();
            Thread thread = new Thread(stub::serve, "servidor-de-mentira");
            thread.setDaemon(true);
            thread.start();
            return stub;
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        /** Hace que el servidor conteste el saludo con un error. */
        void rejectHandshake() {
            rejectHandshake = true;
        }

        /** Deja en la cola la respuesta de la siguiente peticion. */
        void willRespond(Response response) {
            scripted.add(response);
        }

        /** Deja en la cola un objeto cualquiera, para probar una respuesta invalida. */
        void willRespondRaw(Object message) {
            scripted.add(message);
        }

        Hello awaitHello() throws InterruptedException {
            Hello hello = hellos.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (hello == null) {
                throw new IllegalStateException("el Proxy no envio el saludo");
            }
            return hello;
        }

        Request awaitRequest() throws InterruptedException {
            Request request = received.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (request == null) {
                throw new IllegalStateException("no llego ninguna peticion al servidor de prueba");
            }
            return request;
        }

        private void serve() {
            try (Socket socket = serverSocket.accept()) {
                client = socket;
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                Object first = MessageCodec.decode(MessageCodec.readFrame(in));
                if (first instanceof Hello hello) {
                    hellos.add(hello);
                }
                if (rejectHandshake) {
                    MessageCodec.writeFrame(out, MessageCodec.encode(Response.failure("No se admiten clientes.")));
                    return;
                }
                MessageCodec.writeFrame(out, MessageCodec.encode(Response.success(SESSION_ID)));

                while (true) {
                    Object message = MessageCodec.decode(MessageCodec.readFrame(in));
                    if (message instanceof Request request) {
                        received.add(request);
                    }
                    Object reply = scripted.poll();
                    if (reply == null) {
                        reply = Response.success(null);
                    }
                    MessageCodec.writeFrame(out, MessageCodec.encode(reply));
                }
            } catch (IOException | ClassNotFoundException closed) {
                // El cliente se fue o la prueba cerro el servidor: es lo esperado.
            }
        }

        @Override
        public void close() throws IOException {
            Socket open = client;
            if (open != null) {
                open.close();
            }
            serverSocket.close();
        }
    }
}
