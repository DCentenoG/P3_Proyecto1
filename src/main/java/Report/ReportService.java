package Report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
Compila y llena las plantillas .jrxml del proyecto (ubicadas en
src/main/resources/ReportDesign, y por lo tanto disponibles en el classpath
como /ReportDesign/nombre.jrxml, igual que IconLibrary carga los iconos desde
/Resources/Icons), y devuelve el PDF resultante como arreglo de bytes
listo para guardarse en disco.

La plantilla se compila en memoria en cada llamada (JasperCompileManager):
para el tamano de este proyecto es mas simple que agregar un paso de
precompilacion a .jasper en el build de Maven. Si el catalogo de reportes
llegara a crecer mucho y la recompilacion se sintiera lenta, se puede
cachear el JasperReport ya compilado por ruta de plantilla.

Se usa un DataSource de JavaBeans (JRBeanCollectionDataSource) porque los
datos del sistema viven en objetos Java en memoria (cargados del XML),
no en una base de datos: cada fila del reporte es un bean cuyos getters
deben calzar con los $F{...} definidos en el .jrxml correspondiente.
*/
public final class ReportService {

    private ReportService() {
    }

    /**
     * Genera un PDF a partir de la plantilla en {@code reportResourcePath}
     * (ej. {@code "/ReportDesign/categorias.jrxml"}) y los datos en {@code rows}.
     *
     * @param reportResourcePath ruta de la plantilla dentro del classpath, empezando con "/"
     * @param rows                los beans que va a recorrer el reporte (uno por fila del detalle)
     * @param parameters          parametros adicionales para la plantilla (titulo, fecha, etc.); puede ser null
     */
    public static byte[] generatePdf(String reportResourcePath, Collection<?> rows, Map<String, Object> parameters)
            throws ReportException {
        try (InputStream reportStream = ReportService.class.getResourceAsStream(reportResourcePath)) {
            if (reportStream == null) {
                throw new ReportException("No se encontro la plantilla de reporte: " + reportResourcePath);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);
            Map<String, Object> effectiveParameters = (parameters != null) ? parameters : new HashMap<>();

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, effectiveParameters, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new ReportException("No fue posible generar el reporte PDF: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ReportException("No fue posible leer la plantilla del reporte: " + e.getMessage(), e);
        }
    }
}
