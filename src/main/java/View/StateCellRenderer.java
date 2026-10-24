package View;

import Model.State;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * Dibuja la celda "Estado" de una tabla de reservas igual que cualquier
 * otra celda de la tabla (misma fuente, sin relleno ni recuadro), pero
 * con el texto en el color de su estado según la paleta del mockup:
 * Aceptada en verde, Rechazada en rojo, En revisión en anaranjado y
 * Cancelada en gris.
 * <p>
 * "Rechazada" se muestra además como un hipervínculo (texto subrayado,
 * con un texto de ayuda): es el único estado que se puede pulsar, para
 * ver el motivo del rechazo en {@link RejectionReasonDialog}. El clic y
 * el cursor de "mano" al pasar por encima se conectan desde el manejo de
 * eventos (ver {@link #isClickable(Object)}).
 * <p>
 * Acepta como valor de la celda un {@link State}; cualquier otro valor
 * (p. ej. una fila de relleno vacía) se muestra como texto normal.
 */
public class StateCellRenderer extends DefaultTableCellRenderer {

    private static final String REJECTED_TOOLTIP = "Haga clic para ver el motivo del rechazo";

    /** Indica si la celda con este valor se puede pulsar (solo "Rechazada", para ver el motivo). */
    public static boolean isClickable(Object value) {
        return value == State.REJECTED;
    }

    /** Color del texto de cada estado, tomado de {@link UITheme}. */
    public static Color colorOf(State state) {
        return switch (state) {
            case ACCEPTED -> UITheme.STATE_ACCEPTED;
            case REJECTED -> UITheme.STATE_REJECTED;
            case IN_REVIEW -> UITheme.STATE_IN_REVIEW;
            case CANCELLED -> UITheme.STATE_CANCELLED;
        };
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        // La base arma la celda exactamente como las demás columnas (fondo, selección, borde).
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setFont(table.getFont());
        setToolTipText(null);

        if (value instanceof State state) {
            setText(state.getLabel());
            setForeground(colorOf(state));
            if (isClickable(state)) {
                setFont(underlined(table.getFont()));
                setToolTipText(REJECTED_TOOLTIP);
            }
        }
        return this;
    }

    /** La misma fuente, subrayada (aspecto de hipervínculo). */
    private static Font underlined(Font font) {
        return font.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    }
}
