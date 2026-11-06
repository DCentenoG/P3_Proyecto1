package View;

import Dto.ReservationSummary;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ventana de solo lectura con el resumen completo de una actividad
 * (reserva) al hacer doble clic sobre una celda ocupada en las grillas
 * de {@link ResourceCalendar} (Calendarización) o {@link ActivityCalendar}
 * (Actividades): funcionario, actividad, fecha, horario y recursos
 * asignados.
 * <p>
 * Recibe un {@link ReservationSummary} y no las entidades del dominio: esta
 * ventana es solo de lectura y el Backend remoto le manda exactamente estos
 * datos, sin que la Vista tenga que conocer Employee ni Reservation.
 */
public class ReservationDetailsDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ReservationDetailsDialog(Window owner, ReservationSummary reservation) {
        super(owner, "Detalle de la actividad", ModalityType.APPLICATION_MODAL);
        setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));
        setContentPane(content);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 12, 0);

        int row = 0;
        gbc.gridy = row++;
        content.add(fieldRow("Actividad", reservation.activity()), gbc);
        gbc.gridy = row++;
        content.add(fieldRow("Funcionario", reservation.employeeName()), gbc);
        gbc.gridy = row++;
        content.add(fieldRow("Fecha", reservation.date().format(DATE_FORMAT)), gbc);
        gbc.gridy = row++;
        content.add(fieldRow("Horario", reservation.startTime().format(TIME_FORMAT) + " - "
                + reservation.endTime().format(TIME_FORMAT)), gbc);
        gbc.gridy = row++;
        content.add(fieldRow("Recursos", describeResources(reservation)), gbc);

        JButton closeButton = UITheme.createStackedIconButton("Cerrar", IconLibrary.REMOVE, 24);
        closeButton.addActionListener(e -> dispose());
        gbc.gridy = row;
        gbc.insets = new Insets(16, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        content.add(closeButton, gbc);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    private JPanel fieldRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(UITheme.createLabel(label + ":"), BorderLayout.WEST);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(UITheme.FIELD_FONT);
        valueLabel.setForeground(UITheme.TEXT_BLUE);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private static String describeResources(ReservationSummary reservation) {
        List<String> descriptions = reservation.resourceDescriptions();
        return (descriptions == null || descriptions.isEmpty()) ? "(ninguno)" : String.join(", ", descriptions);
    }
}
