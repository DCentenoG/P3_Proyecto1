package View;

/**
 * Identifica la entidad de negocio que un componente CRUD genérico
 * (CRUDView, FilterBuilder, TableBuilder, FormDialog) debe construir.
 * Centraliza aquí los textos y columnas propias de cada entidad evita
 * repetir esa información en cada clase que la necesita.
 */
public enum EntityType {

    FUNCIONARIO("Funcionarios", "funcionario",
            new String[]{"ID", "Nombre", "Teléfono"},
            new String[]{"ID", "Nombre", "Teléfono"}),
    CATEGORIA("Categorías", "categoría",
            new String[]{"ID", "Descripción"},
            new String[]{"ID", "Descripción"}),
    RECURSO("Recursos", "recurso",
            new String[]{"ID", "Categoría", "Descripción"},
            new String[]{"Categoría", "ID", "Descripción"});

    private final String pluralTitle;
    private final String singularTitle;
    private final String[] columns;
    private final String[] tableColumns;

    EntityType(String pluralTitle, String singularTitle, String[] columns, String[] tableColumns) {
        this.pluralTitle = pluralTitle;
        this.singularTitle = singularTitle;
        this.columns = columns;
        this.tableColumns = tableColumns;
    }

    /** Título de la vista/lista, p. ej. "Funcionarios". */
    public String getPluralTitle() {
        return pluralTitle;
    }

    /** Nombre de la entidad en singular, p. ej. "funcionario" (para "Editar funcionario"). */
    public String getSingularTitle() {
        return singularTitle;
    }

    /**
     * Campos del formulario de Agregar/Editar, en el orden en que se
     * arman (p. ej. Recurso: ID, Categoría, Descripción).
     */
    public String[] getColumns() {
        return columns.clone();
    }

    /**
     * Columnas de la tabla de "Listado", en el orden en que se muestran
     * (puede diferir del orden del formulario; p. ej. Recurso muestra
     * primero la Categoría y luego el ID).
     */
    public String[] getTableColumns() {
        return tableColumns.clone();
    }
}
