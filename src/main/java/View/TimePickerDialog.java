package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.LocalTime;

/**
 * Selector de hora (HH:mm) con dos listas desplegables -horas de 06:00 a
 * 22:00 en pasos de 1, minutos en pasos de 5- con la misma estética que
 * el resto de la aplicación. Se abre junto al botón "…" de un campo de
 * hora bloqueado con {@link UITheme#lockAsPickerOnly}.
 * <p>
 * Admite un rango [{@code min}, {@code max}] opcional (cualquiera de los
 * dos puede ser {@code null}): las horas/minutos fuera de ese rango
 * directamente no aparecen en las listas, para que no se pueda elegir,
 * por ejemplo, una hora de fin anterior a la hora de inicio ya escogida
 * (o viceversa).
 */
public final class TimePickerDialog extends JDialog {

    public static final int MIN_HOUR = 6;
    public static final int MAX_HOUR = 22;
    private static final int MINUTE_STEP = 5;

    private final LocalTime min;
    private final LocalTime max;
    private LocalTime selected;

    private JComboBox<String> hourCombo;
    private JComboBox<String> minuteCombo;

    private TimePickerDialog(Window owner, LocalTime initial, LocalTime min, LocalTime max) {
        super(owner, "Seleccionar hora", ModalityType.APPLICATION_MODAL);
        this.min = min;
        this.max = max;
        initComponents(initial);
    }

    /**
     * Muestra el selector y devuelve la hora elegida, o {@code null} si el
     * usuario canceló. {@code min}/{@code max} (cualquiera puede ser
     * {@code null}) acotan las horas seleccionables.
     */
    public static LocalTime show(Window owner, LocalTime initial, LocalTime min, LocalTime max) {
        TimePickerDialog dialog = new TimePickerDialog(owner, initial, min, max);
        dialog.setVisible(true);
        return dialog.selected;
    }

    private void initComponents(LocalTime initial) {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
        setContentPane(content);

        content.add(buildSelectors(initial), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private int minHourBound() {
        return Math.max(MIN_HOUR, min != null ? min.getHour() : MIN_HOUR);
    }

    private int maxHourBound() {
        return Math.min(MAX_HOUR, max != null ? max.getHour() : MAX_HOUR);
    }

    private JPanel buildSelectors(LocalTime initial) {
        hourCombo = new JComboBox<>();
        UITheme.styleCombo(hourCombo);
        minuteCombo = new JComboBox<>();
        UITheme.styleCombo(minuteCombo);

        populateHours();
        hourCombo.addActionListener(e -> populateMinutes(null));

        LocalTime reference = initial != null ? initial : LocalTime.of(minHourBound(), 0);
        int refHour = clampInt(reference.getHour(), minHourBound(), maxHourBound());
        hourCombo.setSelectedItem(String.format("%02d", refHour));
        populateMinutes(String.format("%02d", roundDownToStep(reference.getMinute())));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        row.add(UITheme.labeledField("Hora", hourCombo));
        JLabel separator = new JLabel(":");
        separator.setFont(UITheme.SECTION_FONT);
        separator.setForeground(UITheme.TEXT_BLUE);
        row.add(separator);
        row.add(UITheme.labeledField("Min.", minuteCombo));
        return row;
    }

    private JPanel buildFooter() {
        JButton acceptButton = UITheme.createStackedIconButton("Aceptar", IconLibrary.CHECKED, 22);
        acceptButton.addActionListener(e -> onAccept());

        JButton cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 22);
        cancelButton.addActionListener(e -> {
            selected = null;
            dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        footer.setOpaque(false);
        footer.add(acceptButton);
        footer.add(cancelButton);
        return footer;
    }

    private void populateHours() {
        hourCombo.removeAllItems();
        for (int h = minHourBound(); h <= maxHourBound(); h++) {
            hourCombo.addItem(String.format("%02d", h));
        }
    }

    /** Repuebla los minutos disponibles para la hora actualmente elegida, respetando min/max. */
    private void populateMinutes(String preferredSelection) {
        if (hourCombo.getSelectedItem() == null) {
            return;
        }
        int hour = Integer.parseInt((String) hourCombo.getSelectedItem());

        int lowMinute = 0;
        int highMinute = 55;
        if (min != null && hour == min.getHour()) {
            lowMinute = Math.max(lowMinute, roundUpToStep(min.getMinute()));
        }
        if (max != null && hour == max.getHour()) {
            highMinute = Math.min(highMinute, roundDownToStep(max.getMinute()));
        }
        if (lowMinute > highMinute) {
            lowMinute = highMinute; // rango degenerado defensivo: al menos un valor seleccionable
        }

        String currentSelection = (preferredSelection != null)
                ? preferredSelection : (String) minuteCombo.getSelectedItem();

        minuteCombo.removeAllItems();
        for (int m = lowMinute; m <= highMinute; m += MINUTE_STEP) {
            minuteCombo.addItem(String.format("%02d", m));
        }
        if (currentSelection != null && containsItem(minuteCombo, currentSelection)) {
            minuteCombo.setSelectedItem(currentSelection);
        }
    }

    private static boolean containsItem(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void onAccept() {
        int hour = Integer.parseInt((String) hourCombo.getSelectedItem());
        int minute = Integer.parseInt((String) minuteCombo.getSelectedItem());
        selected = LocalTime.of(hour, minute);
        dispose();
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int roundUpToStep(int minute) {
        return Math.min(60 - MINUTE_STEP, ((minute + MINUTE_STEP - 1) / MINUTE_STEP) * MINUTE_STEP);
    }

    private static int roundDownToStep(int minute) {
        return (minute / MINUTE_STEP) * MINUTE_STEP;
    }
}
