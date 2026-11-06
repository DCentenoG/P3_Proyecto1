package View;

import java.util.List;

/**
 * Vista CRUD de categorías (mockup pág. 6): filtro por Descripción,
 * ícono de buscar/imprimir, y el listado con columnas ID, Descripción,
 * Editar y Borrar. Toda la construcción la resuelve {@link CRUDView};
 * esta clase solo la especializa para la entidad Categoría.
 */
public class CategoriesView extends CRUDView {

    public CategoriesView() {
        super(EntityType.CATEGORIA, List.of());
    }
}
