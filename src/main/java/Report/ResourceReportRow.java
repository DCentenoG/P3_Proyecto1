package Report;

/*
Fila "plana" para el reporte de Recursos (src/main/resources/reports/recursos.jrxml).

Existe porque Resource.getResourceCategoryReference() devuelve un objeto
ResourceCategory completo, y JasperReports/JRBeanCollectionDataSource solo
sabe leer propiedades simples de un bean (getXxx()), no navegar
"getResourceCategoryReference().getDescription()" como si fuera una sola
expresion de campo. Por eso se aplana aqui, antes de pasarle los datos al
reporte.
*/
public class ResourceReportRow {

    private final String categoryDescription;
    private final int id;
    private final String description;

    public ResourceReportRow(String categoryDescription, int id, String description) {
        this.categoryDescription = categoryDescription;
        this.id = id;
        this.description = description;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
