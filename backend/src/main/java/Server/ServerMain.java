package Server;

import Service.LocalService;

import java.io.IOException;

/* Punto de entrada del Backend: sin ninguna interfaz grafica. Uso: ServerMain [puerto] */
public final class ServerMain {

    private static final int DEFAULT_PORT = 5000;

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        LocalService service = LocalService.bootstrapFromXml();
        new ReservationServer(port, service).start();
    }

    private static int resolvePort(String[] args) {
        String text = (args.length > 0) ? args[0] : System.getenv("RESERVAS_PORT");
        if (text == null || text.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(text.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Puerto invalido: " + text);
        }
    }
}