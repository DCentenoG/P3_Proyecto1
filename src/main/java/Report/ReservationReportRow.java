package Report;

/*
Fila "plana" para el reporte de Mis reservas (src/main/resources/reports/mis_reservas.jrxml).
Los campos ya vienen formateados como texto (fecha, rango de horas, lista de
recursos separada por comas) para no tener que repetir esa lógica de
formato dentro del .jrxml: ReservationsViewListener ya la aplica igual para
mostrar la tabla en pantalla, así que aquí solo se reutiliza.
*/
public class ReservationReportRow {

    private final String activity;
    private final String date;
    private final String timeRange;
    private final String resources;
    private final String status;

    public ReservationReportRow(String activity, String date, String timeRange, String resources, String status) {
        this.activity = activity;
        this.date = date;
        this.timeRange = timeRange;
        this.resources = resources;
        this.status = status;
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

    public String getResources() {
        return resources;
    }

    public String getStatus() {
        return status;
    }
}
