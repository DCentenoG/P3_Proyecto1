package Controller;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Utilidades comunes para mostrar confirmaciones, advertencias y mensajes
 * informativos desde los controladores de eventos, con estilo consistente
 * a lo largo y ancho del programa (p. ej. la confirmación previa a
 * "Guardar" o "Borrar"/"Cancelar reserva" pedida en varias pantallas), y
 * para guardar los reportes PDF que genera {@link Report.ReportService}
 * (ver {@link #savePdfAndOpen}).
 */
public final class DialogHelper {

    private DialogHelper() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Opciones de los diálogos de confirmación, en español (el Look &amp; Feel por defecto las muestra en inglés). */
    private static final Object[] YES_NO = {"Sí", "No"};

    /** Pregunta al usuario si desea proceder con una operación (Sí/No). */
    public static boolean confirm(Component parent, String message) {
        int choice = JOptionPane.showOptionDialog(parent, message, "Confirmar operación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, YES_NO, YES_NO[0]);
        return choice == 0; // 0 == "Sí"
    }

    /** Notifica al usuario que debe corregir algo antes de continuar (p. ej. seleccionar una fila). */
    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    /** Muestra un mensaje informativo con título propio (p. ej. la explicación del llenado por IA). */
    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Notifica un error de negocio (credenciales inválidas, operación imposible, etc.). */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Deja que el usuario elija dónde guardar un reporte PDF ya generado, usando el diálogo
     * "Guardar como" NATIVO del sistema operativo ({@link FileDialog}, no {@code JFileChooser}):
     * así no hace falta diseñar una ventana propia y el usuario ve el mismo explorador de
     * archivos de siempre (en Windows, el cuadro de diálogo real de Guardar como). Si el usuario
     * cancela, no hace nada; si guarda con éxito, además intenta abrir el PDF con el visor
     * asociado del sistema operativo. Centraliza lo que antes estaba duplicado, casi idéntico, en
     * Reservas/Calendarización/Actividades y en los tres CRUD.
     *
     * @param parent          cualquier componente de la pantalla que pidió el reporte: se usa
     *                        para ubicar la ventana dueña del diálogo (debe colgar de un
     *                        {@link Frame}, como {@code MainFrame}) y para los mensajes de
     *                        error/éxito
     * @param pdf             contenido ya generado del PDF
     * @param defaultFileName nombre sugerido (con extensión .pdf) que aparece precargado
     * @param successTitle    título del diálogo de éxito (p. ej. "Reservas", "Recursos")
     */
    public static void savePdfAndOpen(Component parent, byte[] pdf, String defaultFileName, String successTitle) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(parent);
        FileDialog dialog = new FileDialog(owner, "Guardar reporte", FileDialog.SAVE);
        dialog.setFile(defaultFileName);
        dialog.setVisible(true); // bloquea hasta que el usuario elija un archivo o cancele

        String fileName = dialog.getFile();
        if (fileName == null) {
            return; // el usuario canceló
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        File target = new File(dialog.getDirectory(), fileName);

        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(pdf);
        } catch (IOException ex) {
            error(parent, "No fue posible guardar el archivo PDF: " + ex.getMessage());
            return;
        }

        info(parent, successTitle, "Reporte generado correctamente: " + target.getName());
        openPdfIfPossible(target);
    }

    /** Abre el PDF recién generado con la aplicación asociada del sistema operativo, si el entorno lo permite. */
    private static void openPdfIfPossible(File file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            return;
        }
        try {
            desktop.open(file);
        } catch (IOException ignored) {
            // No hay visor de PDF asociado, o falló al abrirlo: el archivo ya quedó guardado igual.
        }
    }
}
