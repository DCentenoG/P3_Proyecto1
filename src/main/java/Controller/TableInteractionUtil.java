package Controller;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;

/**
 * Utilidades compartidas para el comportamiento de selección de las
 * tablas de listado (los CRUD de Funcionarios/Categorías/Recursos y
 * "Mis reservas"): deseleccionar la fila activa al hacer clic fuera de
 * la tabla y de sus botones de acción asociados.
 */
final class TableInteractionUtil {

    private TableInteractionUtil() {
    }

    /**
     * Instala un listener global de mouse que deselecciona la fila activa
     * de {@code table} en cuanto el usuario hace clic en cualquier parte
     * de la misma ventana que no sea la propia tabla ni uno de los
     * {@code exemptAreas} (normalmente los botones que operan sobre la
     * fila seleccionada, p. ej. "Editar"/"Borrar" o "Cancelar reserva").
     * Un clic en otra ventana (p. ej. un diálogo modal) se ignora.
     */
    static void deselectOnClickOutside(JTable table, JComponent... exemptAreas) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent mouseEvent) || mouseEvent.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            if (!table.isShowing() || table.getSelectedRow() < 0) {
                return;
            }
            Component source = mouseEvent.getComponent();
            if (source == null) {
                return;
            }
            Component clicked = SwingUtilities.getDeepestComponentAt(source, mouseEvent.getX(), mouseEvent.getY());
            if (clicked == null) {
                return;
            }
            Window tableWindow = SwingUtilities.getWindowAncestor(table);
            Window clickedWindow = (clicked instanceof Window w) ? w : SwingUtilities.getWindowAncestor(clicked);
            if (tableWindow != clickedWindow) {
                return; // clic en otra ventana (p. ej. un diálogo modal): no afecta esta tabla
            }
            if (SwingUtilities.isDescendingFrom(clicked, table)) {
                return;
            }
            for (JComponent area : exemptAreas) {
                if (area != null && SwingUtilities.isDescendingFrom(clicked, area)) {
                    return;
                }
            }
            table.clearSelection();
        }, AWTEvent.MOUSE_EVENT_MASK);
    }
}
