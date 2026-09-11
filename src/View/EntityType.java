package View;

/**
 * Identifica la entidad de negocio que un componente CRUD genérico
 * (CRUDView, FilterBuilder, TableBuilder, FormDialog) debe construir.
 * Centraliza aquí los textos y columnas propias de cada entidad evita
 * repetir esa información en cada clase que la necesita.
 */
public enum EntityType {

    FUNCIONARIO("Funcionarios", "funcionario", new String[]{"ID", "Nombre", "Teléfono"}),
    CATEGORIA("Categorías", "categoría", new String[]{"ID", "Descripción"}),
    RECURSO("Recursos", "recurso", new String[]{"ID", "Categoría", "Descripción"});

    private final String pluralTitle;
    private final String singularTitle;
    private final String[] columns;

    EntityType(String pluralTitle, String singularTitle, String[] columns) {
        this.pluralTitle = pluralTitle;
        this.singularTitle = singularTitle;
        this.columns = columns;
    }

    /** Título de la vista/lista, p. ej. "Funcionarios". */
    public String getPluralTitle() {
        return pluralTitle;
    }

    /** Nombre de la entidad en singular, p. ej. "funcionario" (para "Editar funcionario"). */
    public String getSingularTitle() {
        return singularTitle;
    }

    /** Columnas de datos de la tabla de esta entidad (sin contar Editar/Borrar). */
    public String[] getColumns() {
        return columns.clone();
    }
}
