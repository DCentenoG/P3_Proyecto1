package Model;

import Model.exceptions.EmployeeNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generacion de ids y busqueda de usuarios.
 * <p>
 * El contador de ids de reserva vive aqui, y no dentro de cada funcionario,
 * porque un id de reserva tiene que ser unico en TODO el sistema: el
 * administrador trabaja con solicitudes de cualquier funcionario y el Frontend
 * remoto identifica una reserva unicamente por ese numero.
 */
@DisplayName("UserContainer: ids unicos y busqueda de usuarios")
class UserContainerTest {

    @Test
    @DisplayName("Los ids de reserva son unicos y crecientes")
    void idsDeReservaUnicosYCrecientes() {
        UserContainer users = new UserContainer();

        assertEquals(1, users.generateReservationId());
        assertEquals(2, users.generateReservationId());
        assertEquals(3, users.generateReservationId());
        assertEquals(4, users.getNextReservationId(), "el contador debe quedar listo para el siguiente");
    }

    @Test
    @DisplayName("Al agregar un funcionario su clave inicial es su propio id")
    void laClaveInicialEsElId() throws Exception {
        UserContainer users = new UserContainer();

        users.addEmployee("Ana", 88880000);

        Employee ana = users.getEmployeeById(1);
        assertEquals("Ana", ana.getName());
        assertEquals(1, ana.getId());
        assertEquals("1", ana.getPassword());
        assertEquals(88880000, ana.getPhoneNumber());
    }

    @Test
    @DisplayName("Administradores y funcionarios comparten el contador de ids")
    void elContadorDeIdsEsCompartido() throws Exception {
        UserContainer users = new UserContainer();

        users.addAdmin("clave-admin");
        users.addEmployee("Ana", 88880000);
        users.addEmployee("Luis", 88880001);

        assertEquals(2, users.getEmployeeById(2).getId());
        assertEquals(3, users.getEmployeeById(3).getId());
        assertEquals(4, users.getNextId());
    }

    @Test
    @DisplayName("getListOfEmployees deja fuera al administrador")
    void getListOfEmployeesExcluyeAdmins() {
        UserContainer users = new UserContainer();
        users.addAdmin("clave-admin");
        users.addEmployee("Ana", 88880000);

        assertEquals(2, users.getUsers().size());
        assertEquals(1, users.getListOfEmployees().size());
        assertEquals("Ana", users.getListOfEmployees().get(0).getName());
    }

    @Test
    @DisplayName("Buscar por id devuelve el mismo objeto del contenedor")
    void getEmployeeByIdDevuelveElMismoObjeto() throws Exception {
        UserContainer users = Fixture.usersNamed("Ana", "Luis");

        assertSame(users.getListOfEmployees().get(0), users.getEmployeeById(1));
    }

    @Test
    @DisplayName("Buscar un funcionario que no existe falla con EmployeeNotFoundException")
    void buscarUnFuncionarioInexistenteFalla() {
        UserContainer users = Fixture.usersNamed("Ana");

        assertThrows(EmployeeNotFoundException.class, () -> users.getEmployeeById(99));
        assertThrows(EmployeeNotFoundException.class, () -> users.getEmployeeByName("Nadie"));
    }

    @Test
    @DisplayName("Buscar por nombre encuentra al funcionario")
    void getEmployeeByName() throws Exception {
        UserContainer users = Fixture.usersNamed("Ana", "Luis");

        assertEquals(2, users.getEmployeeByName("Luis").getId());
    }

    @Test
    @DisplayName("Eliminar exige nombre y telefono correctos")
    void eliminarExigeNombreYTelefono() throws Exception {
        UserContainer users = new UserContainer();
        users.addEmployee("Ana", 88880000);

        assertThrows(EmployeeNotFoundException.class,
                () -> users.removeEmployeeByNameAndPhoneNumber("Ana", 11111111));
        assertEquals(1, users.getListOfEmployees().size());

        users.removeEmployeeByNameAndPhoneNumber("Ana", 88880000);

        assertTrue(users.getListOfEmployees().isEmpty());
    }

    @Test
    @DisplayName("La lista de usuarios que se entrega afuera no se puede modificar")
    void laListaDeUsuariosEsInmodificable() {
        UserContainer users = Fixture.usersNamed("Ana");

        assertThrows(UnsupportedOperationException.class,
                () -> users.getUsers().add(new Admin(99, "clave")));
    }

    @Test
    @DisplayName("La identidad de un usuario es su id, y un Admin nunca es igual a un Employee")
    void laIdentidadDeUnUsuarioEsSuId() {
        Employee empleado = new Employee("Ana", 88880000, 1, "1");
        Employee mismoId = new Employee("Nombre cambiado", 99999999, 1, "otra");
        Admin admin = new Admin(1, "clave");

        assertEquals(empleado, mismoId);
        assertEquals(empleado.hashCode(), mismoId.hashCode());
        assertNotEquals(empleado, admin, "un administrador y un funcionario no se confunden");
    }
}
