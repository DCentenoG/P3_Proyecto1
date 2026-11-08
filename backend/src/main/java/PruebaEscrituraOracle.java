import Dto.PendingRequest;
import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Persistence.ConnectionManager;
import Service.IService;
import Service.LocalService;
import Service.Service;
import Service.ServiceException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Verifica el camino de ESCRITURA: hace un ciclo completo de operaciones a
 * traves de {@link IService} y despues <b>vuelve a cargar la base desde cero</b>
 * con una instancia nueva, para comprobar que todo quedo realmente guardado y
 * no solo en memoria.
 * <p>
 * Esa segunda carga es el punto de la prueba. Sin ella solo se estaria
 * verificando que los objetos en memoria cambiaron, que es lo que ya funcionaba
 * antes de tener base de datos.
 * <p>
 * Clase DESECHABLE. Deja la base limpia al terminar: borra todo lo que creo.
 * <p>
 * Ejecutar con el classpath del modulo sistema-reservas-backend.
 */
public class PruebaEscrituraOracle {

    private static int ok = 0;
    private static int fail = 0;

    private static void check(String what, boolean condition) {
        System.out.printf("  [%s] %s%n", condition ? "OK " : "FALLA", what);
        if (condition) {
            ok++;
        } else {
            fail++;
        }
    }

    public static void main(String[] args) {
        String marca = "PRUEBA-" + System.currentTimeMillis() % 100000;
        try {
            IService s = LocalService.bootstrapFromDatabase();

            System.out.println("\n-- Alta de datos --");
            Employee ana = s.addEmployee("Ana " + marca, "55551111");
            check("funcionario creado con id " + ana.getId(), ana.getId() > 0);
            ResourceCategory cat = s.addCategory("Categoria " + marca);
            check("categoria creada con id " + cat.getId(), cat.getId().startsWith("CAT-"));
            s.addResource(cat.getDescription(), "1", "Recurso A " + marca);
            s.addResource(cat.getDescription(), "2", "Recurso B " + marca);
            check("2 recursos creados", s.listResourcesByCategory(cat.getDescription()).size() == 2);

            System.out.println("\n-- Ciclo completo de una reserva --");
            LocalDate dia = LocalDate.now().plusDays(30);
            Reservation r = s.createReservation(ana.getId(), "Actividad " + marca, dia,
                    LocalTime.of(8, 0), LocalTime.of(9, 0), List.of(cat.getDescription()));
            check("reserva creada con id " + r.getId(), r.getId() > 0);

            PendingRequest pend = buscarSolicitud(s, r.getId());
            check("aparece como solicitud pendiente", pend != null);
            s.markRequestAsSeen(ana.getId(), r.getId());

            Map<ResourceCategory, List<Resource>> libres =
                    s.availableResourcesFor(ana.getId(), r.getId());
            Resource elegido = libres.values().iterator().next().get(0);
            s.approveRequest(ana.getId(), r.getId(), List.of(elegido));
            check("aprobada", s.listReservations(ana.getId()).get(0).getState() == State.ACCEPTED);

            System.out.println("\n-- LA PRUEBA DE VERDAD: recargar la base desde cero --");
            Service recarga = new Service(ConnectionManager.fromProperties());
            recarga.load();

            Employee anaRecargada = buscarEmpleado(recarga, ana.getId());
            check("el funcionario se persistio", anaRecargada != null);
            check("conserva nombre y telefono", anaRecargada != null
                    && anaRecargada.getName().equals("Ana " + marca)
                    && anaRecargada.getPhoneNumber() == 55551111);

            ResourceCategory catRecargada = buscarCategoria(recarga, cat.getId());
            check("la categoria se persistio", catRecargada != null);
            check("con sus 2 recursos", catRecargada != null && catRecargada.getResources().size() == 2);

            Reservation rRecargada = anaRecargada == null ? null
                    : anaRecargada.getReservations().stream()
                        .filter(x -> x.getId() == r.getId()).findFirst().orElse(null);
            check("la reserva se persistio con el MISMO id", rRecargada != null);
            if (rRecargada != null) {
                check("conserva la actividad", rRecargada.getActivity().equals("Actividad " + marca));
                check("conserva fecha y horas", rRecargada.getDate().equals(dia)
                        && rRecargada.getStartTime().equals(LocalTime.of(8, 0)));
                check("conserva el estado Aceptada", rRecargada.getState() == State.ACCEPTED);
                check("conserva su categoria requerida", rRecargada.getRequiredCategories().size() == 1);
                check("conserva el recurso asignado", rRecargada.getAssignedResources().size() == 1);
                check("la notificacion quedo marcada como vista",
                        rRecargada.getNotification().isSeen());
                check("el recurso asignado es la MISMA instancia que vive en su categoria",
                        catRecargada != null && rRecargada.getAssignedResources().size() == 1
                        && catRecargada.getResources().contains(rRecargada.getAssignedResources().get(0))
                        && esMismaInstancia(catRecargada, rRecargada.getAssignedResources().get(0)));
            }
            check("los contadores retoman desde el mayor id",
                    recarga.getUsers().getNextReservationId() > r.getId());

            System.out.println("\n-- La base protege la integridad --");
            try {
                s.removeResource(cat.getDescription(), elegido.getId(), elegido.getDescription());
                check("no deja borrar un recurso asignado a una reserva", false);
            } catch (ServiceException e) {
                check("no deja borrar un recurso asignado: \"" + e.getMessage() + "\"", true);
            }
            try {
                s.removeCategory(cat.getId(), cat.getDescription());
                check("no deja borrar una categoria solicitada por una reserva", false);
            } catch (ServiceException e) {
                check("no deja borrar una categoria solicitada: \"" + e.getMessage() + "\"", true);
            }

            System.out.println("\n-- Rollback: una operacion fallida no deja rastro --");
            int antes = contarRecursos(s, cat.getDescription());
            try {
                s.addResource(cat.getDescription(), "1", "Duplicado");
            } catch (ServiceException ignored) {
                // Se esperaba que falle: el id 1 ya existe en esa categoria.
            }
            check("un alta rechazada no agrego nada", contarRecursos(s, cat.getDescription()) == antes);

            System.out.println("\n-- Limpieza --");
            s.cancelReservation(ana.getId(), r.getId());
            limpiar(s, ana, cat, marca);

            System.out.printf("%n===== %d OK, %d fallas =====%n", ok, fail);
            if (fail == 0) {
                System.out.println("CAMINO DE ESCRITURA OK. La persistencia es la base de datos.");
            }

        } catch (Exception e) {
            System.out.println("\nFALLO INESPERADO: " + e.getMessage());
            System.out.println("Los datos de prueba llevan la marca " + marca
                    + ": se pueden borrar a mano si quedaron.");
            e.printStackTrace();
        }
    }

    private static boolean esMismaInstancia(ResourceCategory category, Resource resource) {
        for (Resource inCategory : category.getResources()) {
            if (inCategory == resource) {
                return true;
            }
        }
        return false;
    }

    private static PendingRequest buscarSolicitud(IService s, int reservationId) throws ServiceException {
        for (PendingRequest request : s.listPendingRequests()) {
            if (request.reservationId() == reservationId) {
                return request;
            }
        }
        return null;
    }

    private static Employee buscarEmpleado(Service service, int id) {
        for (Employee employee : service.getUsers().getListOfEmployees()) {
            if (employee.getId() == id) {
                return employee;
            }
        }
        return null;
    }

    private static ResourceCategory buscarCategoria(Service service, String id) {
        for (ResourceCategory category : service.getCategories().getCategories()) {
            if (category.getId().equals(id)) {
                return category;
            }
        }
        return null;
    }

    private static int contarRecursos(IService s, String categoryDescription) throws ServiceException {
        return s.listResourcesByCategory(categoryDescription).size();
    }

    /* Borra en orden inverso a las dependencias. Si algo no se puede borrar, lo avisa. */
    private static void limpiar(IService s, Employee ana, ResourceCategory cat, String marca) {
        try {
            // Para poder borrar la categoria hay que borrar primero la reserva que la
            // solicito, y eso no es una operacion del sistema: se hace a mano.
            try (java.sql.Connection connection = ConnectionManager.fromProperties().open()) {
                try (java.sql.PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM RESERVA WHERE empleado_id = ?")) {
                    ps.setInt(1, ana.getId());
                    ps.executeUpdate();
                }
                connection.commit();
            }
            // Sobre una COPIA de la lista: listResourcesByCategory devuelve la lista
            // viva de la categoria, y borrar mientras se la recorre hace que el
            // for-each termine antes de tiempo sin lanzar nada. Con 2 recursos
            // borraba el primero, el tamano bajaba a 1, el cursor ya estaba en 1,
            // hasNext() daba false y el segundo quedaba sin borrar.
            for (Resource resource : new java.util.ArrayList<>(
                    s.listResourcesByCategory(cat.getDescription()))) {
                s.removeResource(cat.getDescription(), resource.getId(), resource.getDescription());
            }
            s.removeCategory(cat.getId(), cat.getDescription());
            s.removeEmployee(ana.getName(), ana.getPhoneNumber());
            System.out.println("  base limpia: se borro todo lo creado por la prueba");
        } catch (Exception e) {
            System.out.println("  no se pudo limpiar del todo (" + e.getMessage() + ").");
            System.out.println("  los registros de la prueba llevan la marca " + marca);
        }
    }
}
