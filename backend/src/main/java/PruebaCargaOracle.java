import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.User;
import Persistence.ConnectionManager;
import Persistence.SystemData;
import Persistence.SystemOracleRepository;

/**
 * Verifica el camino de LECTURA completo: configuracion, conexion, los cuatro
 * DAOs y la reconstruccion de los contenedores en memoria.
 * <p>
 * Clase DESECHABLE, igual que PruebaConexionOracle: se borra cuando el servidor
 * ya arranque contra Oracle. A diferencia de aquella, esta NO tiene credenciales
 * dentro -- las lee de db/database.properties -- asi que se puede commitear sin
 * problema.
 * <p>
 * Con la base recien creada y vacia, la salida esperada es todo en cero y los
 * contadores en 1. Eso ya prueba que la configuracion, la conexion y las seis
 * tablas estan bien.
 * <p>
 * Ejecutar con el classpath del modulo sistema-reservas-backend.
 */
public class PruebaCargaOracle {

    public static void main(String[] args) {
        try {
            ConnectionManager connections = ConnectionManager.fromProperties();
            System.out.println("Configuracion leida. URL: " + connections.getUrl());

            SystemOracleRepository repository = new SystemOracleRepository(connections);
            SystemData data = repository.load();
            System.out.println("Carga completada.");
            System.out.println("------------------------------------");

            int admins = 0;
            int employees = 0;
            for (User user : data.getUsers().getUsers()) {
                if (user instanceof Employee) {
                    employees++;
                } else {
                    admins++;
                }
            }
            System.out.println("Administradores: " + admins);
            System.out.println("Funcionarios:    " + employees);

            int resources = 0;
            for (ResourceCategory category : data.getCategories().getCategories()) {
                resources += category.getResources().size();
            }
            System.out.println("Categorias:      " + data.getCategories().getCategories().size());
            System.out.println("Recursos:        " + resources);

            int reservations = 0;
            for (Employee employee : data.getUsers().getListOfEmployees()) {
                reservations += employee.getReservations().size();
            }
            System.out.println("Reservas:        " + reservations);

            System.out.println("------------------------------------");
            System.out.println("Contadores retomados desde el mayor id en uso:");
            System.out.println("  siguiente id de usuario:   " + data.getUsers().getNextId());
            System.out.println("  siguiente id de reserva:   " + data.getUsers().getNextReservationId());
            System.out.println("  siguiente id de categoria: " + data.getCategories().getNextId());

            if (reservations > 0) {
                System.out.println("------------------------------------");
                System.out.println("Detalle de las reservas cargadas:");
                for (Employee employee : data.getUsers().getListOfEmployees()) {
                    for (Reservation reservation : employee.getReservations()) {
                        System.out.printf("  #%d  %s  %s  %s-%s  %s  [%s]%n",
                                reservation.getId(), employee.getName(), reservation.getDate(),
                                reservation.getStartTime(), reservation.getEndTime(),
                                reservation.getActivity(), reservation.getState());
                        for (ResourceCategory required : reservation.getRequiredCategories()) {
                            System.out.println("        requiere: " + required.getDescription());
                        }
                        for (Resource assigned : reservation.getAssignedResources()) {
                            System.out.println("        asignado: " + assigned.getDescription()
                                    + " (" + assigned.getCategoryId() + "/" + assigned.getId() + ")");
                            // Comprobacion clave: el recurso de la reserva tiene que ser la MISMA
                            // instancia que vive en la categoria, no una copia.
                            boolean sameInstance = false;
                            for (ResourceCategory category : data.getCategories().getCategories()) {
                                for (Resource inCategory : category.getResources()) {
                                    if (inCategory == assigned) {
                                        sameInstance = true;
                                    }
                                }
                            }
                            System.out.println("        instancia compartida con su categoria: "
                                    + (sameInstance ? "SI" : "NO (revisar)"));
                        }
                    }
                }
            }

            System.out.println("------------------------------------");
            System.out.println("CAMINO DE LECTURA OK.");

        } catch (Exception e) {
            System.out.println("------------------------------------");
            System.out.println("FALLO: " + e.getMessage());
            System.out.println();
            System.out.println("Que suele significar cada cosa:");
            System.out.println("  No se encontro db/database.properties  -> copiar el .example y completarlo");
            System.out.println("  Falta la propiedad '...'                -> el .properties esta incompleto");
            System.out.println("  ORA-00942 table or view does not exist  -> falta ejecutar db/schema.sql");
            System.out.println("  ORA-01017 invalid username/password     -> revisar db.user y db.password");
            System.out.println("  No suitable driver found                -> se ejecuto con el classpath de");
            System.out.println("                                             otro modulo; usar el del backend");
            System.out.println();
            e.printStackTrace();
        }
    }
}
