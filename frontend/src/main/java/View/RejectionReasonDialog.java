package View;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

/**
 * Mini-ventana de solo lectura con el motivo por el que el administrador
 * rechazó una solicitud de reserva. Se abre cuando el funcionario pulsa
 * el estado "Rechazada" de una de sus reservas en "Mis reservas" (ver
 * {@link StateCellRenderer#isClickable(Object)}).
 * <p>
 * Muestra a qué reserva corresponde y el motivo completo (con salto de
 * línea automático si es largo). Se cierra con el botón "X" de la
 * ventana.
 */
public class RejectionReasonDialog extends JDialog {

    private static final int REASON_WIDTH = 340;
    private static final int REASON_MIN_HEIGHT = 60;
    private static final int REASON_MAX_HEIGHT = 160;

    /**
     * @param reservationSummary texto que identifica la reserva (p. ej. "Capacitación de Excel · 05/11/2026 · 14:00 - 16:00")
     * @param reason             motivo escrito por el administrador al rechazarla
     */
    public RejectionReasonDialog(Window owner, String reservationSummary, String reason) {
        super(owner, "Motivo del rechazo", ModalityType.APPLICATION_MODAL);
        setResizable(false);
        // Se cierra con el botón "X" de la propia ventana (no tiene botón "Cerrar").
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));
        setContentPane(content);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel summaryLabel = new JLabel(reservationSummary);
        summaryLabel.setFont(UITheme.FIELD_FONT);
        summaryLabel.setForeground(UITheme.TEXT_BLUE);
        header.add(summaryLabel, BorderLayout.NORTH);
        header.add(UITheme.createLabel("Su solicitud fue rechazada por el siguiente motivo:"), BorderLayout.SOUTH);
        content.add(header, BorderLayout.NORTH);

        JTextArea reasonArea = new JTextArea(reason == null || reason.isBlank() ? "(No se indicó un motivo.)" : reason);
        reasonArea.setEditable(false);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setFont(UITheme.FIELD_FONT);
        reasonArea.setForeground(UITheme.TEXT_BLUE);
        reasonArea.setBackground(UITheme.WHITE);
        reasonArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        // Se fija el ancho para que el texto se ajuste a esas líneas y así medir el alto que necesita.
        reasonArea.setSize(new Dimension(REASON_WIDTH, Short.MAX_VALUE));
        int neededHeight = reasonArea.getPreferredSize().height + 2;

        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        UITheme.styleScrollPane(reasonScroll);
        reasonScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        reasonScroll.setPreferredSize(new Dimension(REASON_WIDTH,
                Math.max(REASON_MIN_HEIGHT, Math.min(neededHeight, REASON_MAX_HEIGHT))));
        content.add(reasonScroll, BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }
}
