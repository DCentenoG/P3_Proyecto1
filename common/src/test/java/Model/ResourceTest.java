package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Identidad de un recurso.
 * <p>
 * Esta clase existe por un error real que tuvo el proyecto: el recurso se
 * comparaba por (id, descripcion) sin tomar en cuenta la categoria, y como el id
 * solo es unico DENTRO de su categoria, el recurso 1 de "Salas" y el 1 de
 * "Proyectores" se consideraban el mismo. El efecto visible era que reservar una
 * sala bloqueaba un proyector. Estas pruebas son la regresion de ese error.
 */
@DisplayName("Resource: la identidad es la pareja (categoria, id)")
class ResourceTest {

    private ResourceCategory salas;
    private ResourceCategory proyectores;
    private Resource sala1;
    private Resource proyector1;

    @BeforeEach
    void setUp() throws Exception {
        CategoryContainer categories = new CategoryContainer();
        salas = Fixture.categoryWith(categories, "Salas", 1);
        proyectores = Fixture.categoryWith(categories, "Proyectores", 1);
        sala1 = salas.getResourceById(1);
        proyector1 = proyectores.getResourceById(1);
    }

    @Test
    @DisplayName("El recurso 1 de una categoria NO es el recurso 1 de otra")
    void elIdSoloEsUnicoDentroDeLaCategoria() {
        assertEquals(1, sala1.getId());
        assertEquals(1, proyector1.getId());

        assertNotEquals(sala1, proyector1);
        assertFalse(sala1.isSameResource(proyector1));
        assertFalse(proyector1.isSameResource(sala1));
    }

    @Test
    @DisplayName("La descripcion no forma parte de la identidad: se puede editar")
    void laDescripcionNoEsIdentidad() {
        Resource renombrado = new Resource(1, salas, "Sala principal");

        assertTrue(sala1.isSameResource(renombrado));
        assertEquals(sala1, renombrado);
        assertEquals(sala1.hashCode(), renombrado.hashCode());
    }

    @Test
    @DisplayName("Un recurso sin categoria no es igual a uno que si la tiene")
    void sinCategoriaNoEsIgualAConCategoria() {
        Resource suelto = new Resource(1, null, "Suelto");

        assertNotEquals(sala1, suelto);
        assertNotEquals(suelto, sala1);
        assertNull(suelto.getCategoryId());
    }

    @Test
    @DisplayName("getCategoryId devuelve el id de la categoria dueña")
    void getCategoryId() {
        assertEquals("CAT-000001", sala1.getCategoryId());
        assertEquals("CAT-000002", proyector1.getCategoryId());
    }

    @Test
    @DisplayName("Un HashSet distingue recursos de categorias distintas con el mismo id")
    void hashSetLosDistingue() {
        Set<Resource> conjunto = new HashSet<>();
        conjunto.add(sala1);
        conjunto.add(proyector1);

        assertEquals(2, conjunto.size());
        assertTrue(conjunto.contains(new Resource(1, salas, "otra descripcion")));
    }

    @Test
    @DisplayName("contains encuentra el recurso por identidad compuesta, no por descripcion")
    void containsUsaLaIdentidadCompuesta() {
        List<Resource> asignados = List.of(sala1);

        assertTrue(asignados.contains(new Resource(1, salas, "descripcion distinta")));
        assertFalse(asignados.contains(proyector1), "el proyector 1 no es la sala 1");
    }

    @Test
    @DisplayName("Un recurso no es igual a un objeto de otro tipo")
    void noEsIgualAOtroTipo() {
        assertNotEquals(sala1, "Salas 1");
    }

    @Test
    @DisplayName("Dos recursos de la misma categoria con id distinto no son iguales")
    void idsDistintosEnLaMismaCategoria() {
        Resource sala2 = new Resource(2, salas, "Salas 2");

        assertNotEquals(sala1, sala2);
    }
}
