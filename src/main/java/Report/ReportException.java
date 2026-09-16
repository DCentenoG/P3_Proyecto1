package Report;

/*
Excepcion de negocio para fallos al generar un reporte en PDF (plantilla
.jrxml no encontrada, error de JasperReports al compilar o llenar el
reporte, error al exportar a PDF, etc.). Sigue el mismo patron que
ServiceException: mensaje en espanol listo para mostrarle al usuario con
DialogHelper.
*/
public class ReportException extends Exception {

    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
