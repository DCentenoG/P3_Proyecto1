package Report;

/*
Fila "plana" para los reportes de Calendarización (recursos) y Actividades
(src/main/resources/reports/calendarizacion.jrxml y actividades.jrxml).

Se usa la misma clase para ambos reportes -- lo único que cambia entre
ellos es qué representa "column" (la descripción de un recurso, o el
nombre de un día de la semana) y el rótulo de esa columna en el .jrxml
("Recurso" vs. "Día") -- porque en ambos casos es, ni más ni menos, una
franja ocupada de la grilla que se ve en pantalla (ResourceCalendar /
ActivityCalendar), aplanada a una fila de listado.
*/
public class ScheduleReportRow {

    private final String column;
    private final String hour;
    private final String activity;
    private final String employeeName;

    public ScheduleReportRow(String column, String hour, String activity, String employeeName) {
        this.column = column;
        this.hour = hour;
        this.activity = activity;
        this.employeeName = employeeName;
    }

    public String getColumn() {
        return column;
    }

    public String getHour() {
        return hour;
    }

    public String getActivity() {
        return activity;
    }

    public String getEmployeeName() {
        return employeeName;
    }
}
