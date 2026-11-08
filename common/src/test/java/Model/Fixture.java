package Model;

import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.DuplicateResourceException;
import Model.exceptions.InvalidReservationOperationException;
import Model.exceptions.ResourceNotFoundException;
import Model.exceptions.ResourceUnavailableException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de prueba compartidos por las pruebas del Model y del Protocol.
 * <p>
 * No es una clase de prueba: no la toma Surefire porque su nombre no termina en
 * {@code Test}. Existe para que cada prueba diga en una linea que escenario
 * necesita, en vez de repetir veinte lineas de armado.
 * <p>
 * Todas las fechas son fijas a proposito. Usar {@code LocalDate.now()} en una
 * prueba la vuelve dependiente del dia en que se corre, y tarde o temprano falla
 * sola (por ejemplo, si la logica rechaza fechas pasadas).
 */
public final class Fixture {

    /** Dia en que ocurren las reservas de prueba. */
    public static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    /** Momento en que "entran" las solicitudes de prueba. */
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 8, 9, 0);

    private Fixture() {
    }

    /** La hora en punto indicada, para no repetir {@code LocalTime.of(h, 0)}. */
    public static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    /**
     * Contenedor con un funcionario por cada nombre, en orden: el primero queda
     * con id 1, el segundo con id 2, y asi.
     */
    public static UserContainer usersNamed(String... names) {
        UserContainer users = new UserContainer();
        for (int i = 0; i < names.length; i++) {
            users.addEmployee(names[i], 80000000 + i);
        }
        return users;
    }

    /**
     * Agrega al contenedor una categoria con {@code count} recursos, descritos
     * "<descripcion> 1", "<descripcion> 2"... y la devuelve.
     */
    public static ResourceCategory categoryWith(CategoryContainer categories, String description, int count)
            throws CategoryNotFoundException, DuplicateResourceException {
        categories.addCategory(description);
        ResourceCategory category = categories.getCategorybyDescription(description);
        for (int id = 1; id <= count; id++) {
            category.addResource(id, description + " " + id);
        }
        return category;
    }

    /**
     * Reserva ya Aceptada de {@code owner} que ocupa {@code resource} entre esas
     * dos horas del {@link #DAY}. Se construye por el camino real (solicitar y
     * despues aprobar), no inyectando el estado, para que la prueba se apoye en
     * el mismo flujo que usa la aplicacion.
     */
    public static Reservation accepted(Employee owner, UserContainer users, ResourceCategory category,
                                       Resource resource, int fromHour, int toHour)
            throws ResourceUnavailableException, InvalidReservationOperationException {
        owner.tryBook("Ocupada", DAY, at(fromHour), at(toHour), List.of(category), users, NOW);
        Reservation reservation = owner.getReservations().get(owner.getReservations().size() - 1);
        reservation.approve(List.of(resource), users);
        return reservation;
    }

    /**
     * Escenario completo listo para serializar: una reserva Aceptada con su
     * recurso, su categoria y el funcionario dueno, todo enlazado en las dos
     * direcciones (la reserva apunta al recurso, el recurso a su categoria y la
     * categoria de vuelta al recurso).
     */
    public static UserContainer acceptedScenario() throws CategoryNotFoundException, DuplicateResourceException,
            ResourceNotFoundException, ResourceUnavailableException, InvalidReservationOperationException {
        UserContainer users = usersNamed("Ana", "Luis");
        CategoryContainer categories = new CategoryContainer();
        ResourceCategory salas = categoryWith(categories, "Salas", 2);
        accepted(users.getListOfEmployees().get(0), users, salas, salas.getResourceById(1), 8, 10);
        return users;
    }
}
