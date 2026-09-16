package View;

import java.util.List;

/**
 * Vista CRUD de funcionarios (mockup pág. 5): filtros por ID y Nombre,
 * ícono de buscar/imprimir, y el listado con columnas ID, Nombre,
 * Teléfono, Editar y Borrar. Toda la construcción la resuelve
 * {@link CRUDView}; esta clase solo la especializa para la entidad
 * Funcionario.
 */
public class EmployeesView extends CRUDView {

    public EmployeesView() {
        super(EntityType.FUNCIONARIO, List.of());
    }
}
