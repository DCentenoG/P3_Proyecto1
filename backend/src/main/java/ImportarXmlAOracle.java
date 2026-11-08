import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import Persistence.CategoryDAO;
import Persistence.ConnectionManager;
import Persistence.ReservationDAO;
import Persistence.ResourceDAO;
import Persistence.SystemData;
import Persistence.SystemXmlRepository;
import Persistence.UserDAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Importa a Oracle todo lo que hay en {@code src/Data/system.xml}: usuarios,
 * categorias, recursos y reservas con sus categorias requeridas y sus recursos
 * asignados.
 * <p>
 * Sirve para dos cosas. La primera, migrar los datos de prueba del Proyecto 1 a
 * la base, para no tener que volver a crearlos a mano. La segunda, y mas util a
 * futuro: el XML queda como una <b>copia de respaldo</b> que se puede volver a
 * cargar en cualquier momento si la base se arruina o hay que recrearla.
 *
 * <h2>Uso</h2>
 * <pre>
 *   ImportarXmlAOracle               solo informa que haria; no escribe nada
 *   ImportarXmlAOracle --reemplazar  borra lo que haya en la base e importa el XML
 * </pre>
 * Sin argumentos nunca modifica la base. Hay que pedirlo explicitamente con
 * {@code --reemplazar}, porque la operacion es destructiva.
 * <p>
 * Escribe por los mismos DAOs que usa el resto del sistema, no con sentencias
 * aparte, para que no existan dos caminos de escritura que puedan divergir.
 * <p>
 * Ejecutar con el classpath del modulo sistema-reservas-backend.
 */
public class ImportarXmlAOracle {

    private static final String XML_RELATIVE = "src/Data/system.xml";

    private final UserDAO userDAO = new UserDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public static void main(String[] args) {
        boolean replace = args.length > 0 && "--reemplazar".equals(args[0]);
        try {
            new ImportarXmlAOracle().run(replace);
        } catch (Exception e) {
            System.out.println("\nFALLO: " + e.getMessage());
            System.out.println("La base quedo como estaba: la importacion va en una sola transaccion.");
            e.printStackTrace();
        }
    }

    private void run(boolean replace) throws Exception {
        Path xmlPath = locateXml();
        System.out.println("Archivo de origen: " + xmlPath.toAbsolutePath());

        SystemData data = new SystemXmlRepository(xmlPath.toString()).load();
        resumen("En el XML hay", data);

        ConnectionManager connections = ConnectionManager.fromProperties();
        System.out.println("Base de destino:   " + connections.getUrl());

        try (Connection connection = connections.open()) {
            contarEnBase(connection);

            if (!replace) {
                System.out.println();
                System.out.println("MODO INFORME: no se escribio nada.");
                System.out.println("Para importar de verdad, volver a ejecutar con el argumento:");
                System.out.println("    --reemplazar");
                System.out.println("Eso BORRA lo que haya en la base y la deja igual al XML.");
                return;
            }

            // Todo en una sola transaccion: si algo falla a mitad de camino, la base
            // queda exactamente como estaba, no a medio importar.
            System.out.println("\nBorrando el contenido actual...");
            borrarTodo(connection);

            System.out.println("Importando...");
            int usuarios = importarUsuarios(connection, data);
            int categorias = importarCategorias(connection, data);
            int recursos = importarRecursos(connection, data);
            int reservas = importarReservas(connection, data);

            connection.commit();

            System.out.println("\n------------------------------------");
            System.out.printf("Importados: %d usuarios, %d categorias, %d recursos, %d reservas.%n",
                    usuarios, categorias, recursos, reservas);
            System.out.println("IMPORTACION COMPLETA.");
            System.out.println();
            System.out.println("Ya se puede entrar con las credenciales de siempre.");
            System.out.println("El XML no se modifico: queda como respaldo para volver a importar.");
        }
    }

    //--- Lectura del origen ---

    private static Path locateXml() {
        Path directory = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth <= 3 && directory != null; depth++) {
            Path candidate = directory.resolve(XML_RELATIVE);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("No se encontro " + XML_RELATIVE
                + " subiendo desde " + Paths.get("").toAbsolutePath());
    }

    private static void resumen(String titulo, SystemData data) {
        int reservas = 0;
        for (Employee employee : data.getUsers().getListOfEmployees()) {
            reservas += employee.getReservations().size();
        }
        int recursos = 0;
        for (ResourceCategory category : data.getCategories().getCategories()) {
            recursos += category.getResources().size();
        }
        System.out.printf("%s: %d usuarios, %d categorias, %d recursos, %d reservas.%n",
                titulo, data.getUsers().getUsers().size(),
                data.getCategories().getCategories().size(), recursos, reservas);
    }

    private static void contarEnBase(Connection connection) throws SQLException {
        System.out.print("En la base hay ahora: ");
        String[] tablas = {"USUARIO", "CATEGORIA", "RECURSO", "RESERVA"};
        StringBuilder linea = new StringBuilder();
        for (String tabla : tablas) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tabla);
                 ResultSet rs = statement.executeQuery()) {
                rs.next();
                if (linea.length() > 0) {
                    linea.append(", ");
                }
                linea.append(rs.getInt(1)).append(" en ").append(tabla);
            }
        }
        System.out.println(linea + ".");
    }

    //--- Escritura en el destino ---

    /*
    Orden inverso a las dependencias. Borrar RESERVA arrastra sus filas de
    RESERVA_CATEGORIA y RESERVA_RECURSO por la cascada del esquema.
    */
    private void borrarTodo(Connection connection) throws SQLException {
        for (String sentencia : new String[]{
                "DELETE FROM RESERVA", "DELETE FROM RECURSO",
                "DELETE FROM CATEGORIA", "DELETE FROM USUARIO"}) {
            try (PreparedStatement statement = connection.prepareStatement(sentencia)) {
                int filas = statement.executeUpdate();
                System.out.println("   " + sentencia + "  ->  " + filas + " filas");
            }
        }
    }

    private int importarUsuarios(Connection connection, SystemData data) throws SQLException {
        int total = 0;
        for (User user : data.getUsers().getUsers()) {
            userDAO.insert(connection, user);
            total++;
        }
        System.out.println("   usuarios:   " + total);
        return total;
    }

    private int importarCategorias(Connection connection, SystemData data) throws SQLException {
        int total = 0;
        for (ResourceCategory category : data.getCategories().getCategories()) {
            categoryDAO.insert(connection, category);
            total++;
        }
        System.out.println("   categorias: " + total);
        return total;
    }

    private int importarRecursos(Connection connection, SystemData data) throws SQLException {
        int total = 0;
        for (ResourceCategory category : data.getCategories().getCategories()) {
            for (Resource resource : category.getResources()) {
                resourceDAO.insert(connection, category.getId(), resource.getId(), resource.getDescription());
                total++;
            }
        }
        System.out.println("   recursos:   " + total);
        return total;
    }

    private int importarReservas(Connection connection, SystemData data) throws SQLException {
        int total = 0;
        for (Employee employee : data.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                // El repositorio XML ya les asigno un id a las reservas que no lo
                // tenian, asi que aqui todas llegan con uno valido.
                reservationDAO.insert(connection, employee.getId(), reservation);
                total++;
            }
        }
        System.out.println("   reservas:   " + total);
        return total;
    }
}
