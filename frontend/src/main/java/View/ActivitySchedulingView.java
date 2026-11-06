package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;

/**
 * Vista de calendarización de actividades (mockup pág. 3): selector de
 * semana de referencia (centrado, con íconos de buscar e imprimir) y,
 * debajo, la grilla {@link ActivityCalendar} (el mockup la rotula
 * "Calendarización de recursos", pero en esta pantalla corresponde a
 * actividades). Solo construye la interfaz; el cambio de semana que
 * recalcula la grilla y la impresión se conectan desde el manejo de
 * eventos.
 */
public class ActivitySchedulingView extends JPanel {

    private JTextField weekField;
    private JButton weekPickerButton;
    private JButton searchButton;
    private JButton printButton;
    private final ActivityCalendar activityCalendar;

    public ActivitySchedulingView() {
        super(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(buildHeader(), BorderLayout.NORTH);

        activityCalendar = new ActivityCalendar();
        activityCalendar.setWeek(LocalDate.now());
        add(activityCalendar, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        header.add(UITheme.leftAligned(UITheme.createSectionTitle("Actividades")));
        header.add(Box.createVerticalStrut(16));
        header.add(UITheme.leftAligned(buildOperationsPanel()));
        header.add(Box.createVerticalStrut(18));
        header.add(UITheme.leftAligned(UITheme.createLabel("Calendarización de actividades")));
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    /**
     * Sub-panel de operaciones del header: el título "Semana de
     * referencia" seguido del selector de semana y los íconos de
     * buscar/imprimir, todos alineados entre sí (a diferencia del título
     * de sección y "Calendarización de actividades", que se alinean con
     * el margen izquierdo de la grilla).
     */
    private JPanel buildOperationsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(UITheme.leftAligned(UITheme.createLabel("Semana de referencia")));
        panel.add(Box.createVerticalStrut(6));
        panel.add(UITheme.leftAligned(buildWeekRow()));

        return panel;
    }

    private JPanel buildWeekRow() {
        weekField = new JTextField(20);
        UITheme.styleField(weekField);
        UITheme.lockAsPickerOnly(weekField); // Semana: solo se selecciona con el botón, no se escribe.
        weekPickerButton = UITheme.createPickerButton("▾");

        JPanel group = new JPanel(new BorderLayout(8, 0));
        group.setOpaque(false);
        group.add(weekField, BorderLayout.CENTER);
        group.add(weekPickerButton, BorderLayout.EAST);

        searchButton = UITheme.createIconOnlyButton(IconLibrary.SEARCH_BLUE, 22);
        searchButton.setToolTipText("Buscar");
        printButton = UITheme.createIconOnlyButton(IconLibrary.PRINTER, 22);
        printButton.setToolTipText("Imprimir");

        return UITheme.row(16, group, searchButton, printButton);
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTextField getWeekField() {
        return weekField;
    }

    public JButton getWeekPickerButton() {
        return weekPickerButton;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JButton getPrintButton() {
        return printButton;
    }

    public ActivityCalendar getActivityCalendar() {
        return activityCalendar;
    }
}
