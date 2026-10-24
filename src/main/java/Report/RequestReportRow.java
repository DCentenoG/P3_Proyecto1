package Report;

/*
Fila "plana" para el reporte de Solicitudes pendientes de revision
(src/main/resources/ReportDesign/solicitudes.jrxml): una solicitud de reserva
"En revision" con el funcionario que la hizo, igual que se ve en la tabla de
la pestaña "Solicitudes" del administrador, mas las categorias que pidio.

Como en las demas filas de reporte, JasperReports enlaza cada $F{...} del
.jrxml con el getter del mismo nombre.
*/
public class RequestReportRow {

    private final String employeeId;
    private final String employeeName;
    private final String activity;
    private final String date;
    private final String timeRange;
    private final String categories;

    public RequestReportRow(String employeeId, String employeeName, String activity, String date, String timeRange,
                            String categories) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.activity = activity;
        this.date = date;
        this.timeRange = timeRange;
        this.categories = categories;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getActivity() {
        return activity;
    }

    public String getDate() {
        return date;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getCategories() {
        return categories;
    }
}
