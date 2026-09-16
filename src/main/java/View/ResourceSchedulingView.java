package View;

import java.util.List;

/**
 * Vista CRUD de recursos (mockup pág. 7): filtros por Categoría (lista
 * desplegable) y Descripción, ícono de buscar/imprimir, y el listado
 * con columnas ID, Categoría, Descripción, Editar y Borrar. Toda la
 * construcción la resuelve {@link CRUDView}; esta clase solo la
 * especializa para la entidad Recurso.
 */
public class ResourceSchedulingView extends CRUDView {

    public ResourceSchedulingView() {
        this(List.of());
    }

    /** Permite inyectar las categorías disponibles para el filtro (se conectará con el Model más adelante). */
    public ResourceSchedulingView(List<String> categoryOptions) {
        super(EntityType.RECURSO, categoryOptions);
    }
}
