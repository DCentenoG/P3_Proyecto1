package Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones a la base de datos. Es el unico lugar del proyecto que sabe
 * la URL, el usuario y la clave.
 * <p>
 * Vive en el modulo backend porque es el unico proceso que se conecta a la
 * base, tal como exige el enunciado.
 *
 * <h2>Una conexion por operacion</h2>
 * {@link #open()} devuelve una conexion NUEVA cada vez, con
 * {@code autoCommit} apagado. Quien la pide la usa en un
 * {@code try-with-resources} y hace {@code commit()} al terminar bien.
 * <p>
 * No se comparte una sola conexion entre hilos porque {@link Connection}
 * <b>no es thread-safe</b>, y el servidor atiende a cada cliente en su propio
 * hilo. Una conexion por operacion es la opcion simple y correcta: para la
 * escala de este sistema el costo de abrirla es irrelevante, y evita por
 * completo la clase de errores que aparece cuando dos hilos comparten una.
 *
 * <h2>De donde sale la configuracion</h2>
 * De un archivo {@code database.properties} que NO va al repositorio, porque
 * contiene la clave. Se busca en este orden:
 * <ol>
 *   <li>La ruta que indique la variable de entorno {@code RESERVAS_DB_CONFIG}.</li>
 *   <li>{@code db/database.properties} en la raiz del repositorio.</li>
 * </ol>
 * En {@code db/database.properties.example} esta la plantilla con los valores
 * que hay que llenar.
 */
public final class ConnectionManager {

    private static final String ENV_CONFIG_PATH = "RESERVAS_DB_CONFIG";
    private static final Path DEFAULT_CONFIG = Paths.get("db", "database.properties");

    private final String url;
    private final String user;
    private final String password;

    public ConnectionManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Lee la configuracion del archivo {@code database.properties}.
     *
     * @throws IllegalStateException si no se encuentra el archivo o le falta
     *                               alguna propiedad, con un mensaje que explica
     *                               que hacer. Es un fallo de arranque: sin
     *                               conexion el Backend no puede servir nada.
     */
    public static ConnectionManager fromProperties() {
        Path configPath = locateConfig();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer la configuracion de la base de datos en "
                    + configPath.toAbsolutePath() + ": " + e.getMessage(), e);
        }

        String host = require(properties, "db.host", configPath);
        String port = require(properties, "db.port", configPath);
        String service = require(properties, "db.service", configPath);
        String user = require(properties, "db.user", configPath);
        String password = require(properties, "db.password", configPath);

        // Formato de servicio (no de SID): es el que acepta Oracle XE 11g y
        // tambien las versiones mas nuevas.
        String url = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
        return new ConnectionManager(url, user, password);
    }

    /** Conexion nueva, con la transaccion abierta (autoCommit apagado). */
    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(false);
        return connection;
    }

    /** URL en uso, sin credenciales. Util para el log de arranque del servidor. */
    public String getUrl() {
        return url;
    }

    //--- Localizacion del archivo de configuracion ---

    private static Path locateConfig() {
        String fromEnv = System.getenv(ENV_CONFIG_PATH);
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path path = Paths.get(fromEnv.trim());
            if (Files.isRegularFile(path)) {
                return path;
            }
            throw new IllegalStateException("La variable " + ENV_CONFIG_PATH + " apunta a "
                    + path.toAbsolutePath() + ", que no existe.");
        }

        // Igual que la ruta del XML: el directorio de trabajo puede ser la raiz del
        // repositorio o la carpeta de un modulo, segun como se arranque.
        Path directory = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth <= 3 && directory != null; depth++) {
            Path candidate = directory.resolve(DEFAULT_CONFIG);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }

        throw new IllegalStateException(
                "No se encontro db/database.properties.\n"
                + "Copie db/database.properties.example a db/database.properties y complete los datos\n"
                + "de conexion, o indique la ruta del archivo en la variable de entorno "
                + ENV_CONFIG_PATH + ".");
    }

    private static String require(Properties properties, String key, Path configPath) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad '" + key + "' en "
                    + configPath.toAbsolutePath());
        }
        return value.trim();
    }
}
