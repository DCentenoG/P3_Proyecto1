package View;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Construye dinámicamente la tabla de {@link CRUDView} según la entidad
 * a mostrar (Funcionario, Categoría o Recurso), con únicamente las
 * columnas de datos propias de la entidad. Las acciones de Editar y
 * Borrar no viven en la tabla: son botones independientes que operan
 * sobre la fila seleccionada (ver {@link CRUDView#getEditButton()} y
 * {@link CRUDView#getDeleteButton()}). La tabla se entrega vacía (o con
 * filas en blanco de relleno); la carga real de datos se conecta desde
 * el manejo de eventos en una etapa posterior.
 */
public final class TableBuilder {

    private TableBuilder() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Construye la tabla de {@code entityType} con {@code blankRows} filas vacías de relleno. */
    public static JTable build(EntityType entityType, int blankRows) {
        String[] columns = entityType.getColumns();

        DefaultTableModel model = new DefaultTableModel(new Object[Math.max(blankRows, 0)][columns.length], columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        UITheme.styleTable(table);
        return table;
    }
}
