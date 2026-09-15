package View.Events;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Utilidades comunes para mostrar confirmaciones, advertencias y mensajes
 * informativos desde los controladores de eventos, con estilo consistente
 * a lo largo y ancho del programa (p. ej. la confirmación previa a
 * "Guardar" o "Borrar"/"Cancelar reserva" pedida en varias pantallas).
 */
public final class DialogHelper {

    private DialogHelper() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Pregunta al usuario si desea proceder con una operación (Sí/No). */
    public static boolean confirm(Component parent, String message) {
        int option = JOptionPane.showConfirmDialog(parent, message, "Confirmar operación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return option == JOptionPane.YES_OPTION;
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
}
