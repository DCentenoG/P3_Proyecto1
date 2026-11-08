package Model;

import Model.exceptions.CategoryNotFoundException;
import Model.exceptions.DuplicateResourceException;
import Model.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Categorias de recursos: generacion del id, recursos duplicados y borrado.
 */
@DisplayName("CategoryContainer y ResourceCategory: catalogo de recursos")
class CategoryContainerTest {

    private CategoryContainer categories;
    private ResourceCategory salas;

    @BeforeEach
    void setUp() throws Exception {
        categories = new CategoryContainer();
        salas = Fixture.categoryWith(categories, "Salas", 2);
    }

    @Test
    @DisplayName("El id de la categoria se genera con el formato CAT-000000")
    void elIdSeGeneraConElFormatoEsperado() throws Exception {
        categories.addCategory("Proyectores");

        assertEquals("CAT-000001", categories.getCategorybyDescription("Salas").getId());
        assertEquals("CAT-000002", categories.getCategorybyDescription("Proyectores").getId());
        assertEquals(3, categories.getNextId());
    }

    @Test
    @DisplayName("Buscar una categoria que no existe falla con CategoryNotFoundException")
    void buscarUnaCategoriaInexistenteFalla() {
        assertThrows(CategoryNotFoundException.class,
                () -> categories.getCategorybyDescription("No existe"));
    }

    @Test
    @DisplayName("No se puede repetir el id de un recurso dentro de la misma categoria")
    void noSeRepiteElIdEnLaMismaCategoria() {
        assertThrows(DuplicateResourceException.class, () -> salas.addResource(1, "Sala duplicada"));
        assertEquals(2, salas.getSize(), "la categoria no debe crecer con el intento fallido");
    }

    @Test
    @DisplayName("El mismo id si se puede usar en otra categoria")
    void elMismoIdSirveEnOtraCategoria() throws Exception {
        ResourceCategory proyectores = Fixture.categoryWith(categories, "Proyectores", 1);

        assertEquals(1, proyectores.getResourceById(1).getId());
        assertEquals(1, salas.getResourceById(1).getId());
    }

    @Test
    @DisplayName("Buscar un recurso por id o por descripcion")
    void buscarRecursos() throws Exception {
        assertEquals("Salas 1", salas.getResourceById(1).getDescription());
        assertEquals(2, salas.getResourceByDescription("Salas 2").getId());
        assertSame(salas.getResourceById(1), salas.getResourceByIndex(0));
    }

    @Test
    @DisplayName("Buscar un recurso que no existe falla con ResourceNotFoundException")
    void buscarUnRecursoInexistenteFalla() {
        assertThrows(ResourceNotFoundException.class, () -> salas.getResourceById(99));
        assertThrows(ResourceNotFoundException.class, () -> salas.getResourceByDescription("No existe"));
    }

    @Test
    @DisplayName("Borrar un recurso exige el id Y la descripcion")
    void borrarUnRecursoExigeIdYDescripcion() throws Exception {
        assertThrows(ResourceNotFoundException.class,
                () -> salas.deleteResourceByIdAndDescription(1, "Nombre equivocado"));
        assertEquals(2, salas.getSize());

        salas.deleteResourceByIdAndDescription(1, "Salas 1");

        assertEquals(1, salas.getSize());
        assertThrows(ResourceNotFoundException.class, () -> salas.getResourceById(1));
    }

    @Test
    @DisplayName("La lista de recursos que se entrega afuera no se puede modificar")
    void laListaDeRecursosEsInmodificable() {
        assertThrows(UnsupportedOperationException.class,
                () -> salas.getResources().add(new Resource(9, salas, "Colada")));
    }

    @Test
    @DisplayName("La lista de categorias que se entrega afuera no se puede modificar")
    void laListaDeCategoriasEsInmodificable() {
        assertThrows(UnsupportedOperationException.class,
                () -> categories.getCategories().add(new ResourceCategory("CAT-999999", "Colada")));
    }

    @Test
    @DisplayName("Borrar una categoria exige id y descripcion, y vacia sus recursos")
    void borrarLaCategoriaVaciaSusRecursos() throws Exception {
        assertThrows(CategoryNotFoundException.class,
                () -> categories.deleteCategoryByIdAndDescription("CAT-000001", "Nombre equivocado"));
        assertEquals(1, categories.getCategories().size());

        categories.deleteCategoryByIdAndDescription("CAT-000001", "Salas");

        assertTrue(categories.getCategories().isEmpty());
        assertEquals(0, salas.getSize(), "la categoria borrada queda sin recursos");
    }

    @Test
    @DisplayName("deleteAllResources vacia la categoria sin borrarla")
    void deleteAllResources() {
        salas.deleteAllResources();

        assertEquals(0, salas.getSize());
        assertEquals(1, categories.getCategories().size());
    }

    @Test
    @DisplayName("La identidad de una categoria es su id: la descripcion es editable")
    void laIdentidadDeUnaCategoriaEsSuId() {
        ResourceCategory original = new ResourceCategory("CAT-000001", "Salas");
        ResourceCategory renombrada = new ResourceCategory("CAT-000001", "Salas de reunion");
        ResourceCategory otra = new ResourceCategory("CAT-000002", "Salas");

        assertEquals(original, renombrada);
        assertEquals(original.hashCode(), renombrada.hashCode());
        assertNotEquals(original, otra);
        assertNotEquals(original, "CAT-000001");
    }
}
