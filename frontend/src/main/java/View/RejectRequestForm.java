package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * Formulario "Rechazar solicitud": se abre cuando el administrador pulsa
 * "Rechazar" en {@link ReviewRequestForm} (que se cierra). Muestra a qué
 * solicitud corresponde y un campo para escribir el motivo del rechazo,
 * que es obligatorio: una solicitud no se puede rechazar sin justificarla.
 * El motivo es el que luego ve el funcionario al pulsar el estado
 * "Rechazada" de su reserva (ver {@link RejectionReasonDialog}).
 * <p>
 * Solo construye la interfaz gráfica: la validación del motivo y el
 * rechazo en sí se conectan desde el manejo de eventos en una etapa
 * posterior.
 */
public class RejectRequestForm extends JDialog {

    /** Ancho del campo del motivo (más ancho que un campo normal: es una frase). */
    private static final int REASON_FIELD_WIDTH = 380;
    private static final int REASON_FIELD_HEIGHT = 40;

    private JLabel requestSummaryLabel;
    private JTextField reasonField;
    private JButton rejectButton;
    private JButton cancelButton;

    public RejectRequestForm(Window owner) {
        super(owner, "Rechazar solicitud", ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    private void initComponents() {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 32, 20, 32));
        setContentPane(content);

        content.add(buildFieldsPanel(), BorderLayout.CENTER);
        content.add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Resumen de la solicitud que se va a rechazar (p. ej. "Juan Perez · Capacitación · 05/11/2026").
        requestSummaryLabel = new JLabel(" ");
        requestSummaryLabel.setFont(UITheme.FIELD_FONT);
        requestSummaryLabel.setForeground(UITheme.TEXT_BLUE);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(requestSummaryLabel, gbc);

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.add(UITheme.createLabel("Motivo del rechazo *"), BorderLayout.WEST);
        labelRow.add(UITheme.createNote("*: Campo obligatorio"), BorderLayout.EAST);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(labelRow, gbc);

        reasonField = new JTextField();
        UITheme.styleField(reasonField);
        reasonField.setPreferredSize(new Dimension(REASON_FIELD_WIDTH, REASON_FIELD_HEIGHT));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(reasonField, gbc);

        return panel;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        panel.setOpaque(false);

        rejectButton = UITheme.createStackedIconButton("Rechazar", IconLibrary.REMOVE_RED, 30);
        rejectButton.setForeground(UITheme.DANGER_RED);
        cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 30);

        panel.add(rejectButton);
        panel.add(cancelButton);
        return panel;
    }

    // ---- Carga de la solicitud (la usa el controlador al abrir el formulario) ----

    /** Texto que identifica la solicitud a rechazar (funcionario, actividad, fecha...). */
    public void setRequestSummary(String summary) {
        requestSummaryLabel.setText(summary);
        pack();
        setMinimumSize(null);
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTextField getReasonField() { return reasonField; }
    /** Confirma el rechazo (solo si se escribió un motivo). */
    public JButton getRejectButton() { return rejectButton; }
    /** Cierra sin rechazar la solicitud. */
    public JButton getCancelButton() { return cancelButton; }
}
