package Report;

/*
Fila "plana" para el reporte de Categorias (src/main/resources/reports/categorias.jrxml).
Ver EmployeeReportRow para la explicacion de por que existe este tipo de
clase en vez de pasarle directamente el objeto del Model a JasperReports.
*/
public class CategoryReportRow {

    private final String id;
    private final String description;

    public CategoryReportRow(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
