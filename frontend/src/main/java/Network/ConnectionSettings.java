package Network;

/* Host y puerto del Backend. Prioridad: argumentos > variables de entorno > valores por defecto. */
public record ConnectionSettings(String host, int port) {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    /** Uso: Main [host] [puerto]. Lanza IllegalArgumentException si el puerto no es valido. */
    public static ConnectionSettings resolve(String[] args) {
        String host = firstNonBlank(args.length > 0 ? args[0] : null,
                System.getenv("RESERVAS_HOST"), DEFAULT_HOST);
        String portText = firstNonBlank(args.length > 1 ? args[1] : null,
                System.getenv("RESERVAS_PORT"), String.valueOf(DEFAULT_PORT));
        try {
            int port = Integer.parseInt(portText.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return new ConnectionSettings(host.trim(), port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Puerto invalido: " + portText);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("Se esperaba al menos un valor por defecto.");
    }
}