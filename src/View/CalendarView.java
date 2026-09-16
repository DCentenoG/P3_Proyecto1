package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Vista de calendarización de recursos (mockup pág. 2): filtros por
 * fecha y categoría centrados, con íconos de buscar e imprimir, y la
 * grilla {@link ResourceCalendar}. Solo construye la interfaz; la
 * búsqueda que recalcula la grilla según la fecha/categoría elegidas y
 * la impresión se conectan desde el manejo de eventos.
 */
public class CalendarView extends JPanel {

    private JTextField dateField;
    private JButton datePickerButton;
    private JComboBox<String> categoryCombo;
    private JButton searchButton;
    private JButton printButton;
    private final ResourceCalendar resourceCalendar;

    public CalendarView() {
        this(List.of());
    }

    public CalendarView(List<String> categoryOptions) {
        super(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(buildHeader(categoryOptions), BorderLayout.NORTH);

        resourceCalendar = new ResourceCalendar();
        add(resourceCalendar, BorderLayout.CENTER);
    }

    private JPanel buildHeader(List<String> categoryOptions) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        header.add(UITheme.leftAligned(UITheme.createSectionTitle("Calendarización")));
        header.add(Box.createVerticalStrut(16));
        header.add(UITheme.leftAligned(buildOperationsPanel(categoryOptions)));
        header.add(Box.createVerticalStrut(18));
        header.add(UITheme.leftAligned(UITheme.createLabel("Calendarización de recursos")));
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    /**
     * Sub-panel de operaciones del header: el título "Filtros" seguido de
     * los campos de fecha/categoría y los íconos de buscar/imprimir, todos
     * alineados entre sí (a diferencia del título de sección y
     * "Calendarización de recursos", que se alinean con el margen
     * izquierdo de la grilla).
     */
    private JPanel buildOperationsPanel(List<String> categoryOptions) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(UITheme.leftAligned(UITheme.createLabel("Filtros")));
        panel.add(Box.createVerticalStrut(6));
        panel.add(UITheme.leftAligned(buildFilterRow(categoryOptions)));

        return panel;
    }

    private JPanel buildFilterRow(List<String> categoryOptions) {
        dateField = new JTextField(12);
        UITheme.styleField(dateField);
        UITheme.lockAsPickerOnly(dateField); // Fecha: solo se selecciona con el botón, no se escribe.
        datePickerButton = UITheme.createPickerButton("▾");

        JPanel dateGroup = new JPanel(new BorderLayout(8, 0));
        dateGroup.setOpaque(false);
        dateGroup.add(dateField, BorderLayout.CENTER);
        dateGroup.add(datePickerButton, BorderLayout.EAST);

        categoryCombo = new JComboBox<>();
        categoryCombo.addItem(FilterBuilder.NO_FILTER);
        for (String option : categoryOptions) {
            categoryCombo.addItem(option);
        }
        UITheme.styleCombo(categoryCombo);

        searchButton = UITheme.createIconOnlyButton(IconLibrary.SEARCH_BLUE, 22);
        searchButton.setToolTipText("Buscar");
        printButton = UITheme.createIconOnlyButton(IconLibrary.PRINTER, 22);
        printButton.setToolTipText("Imprimir");

        return UITheme.row(24,
                UITheme.labeledField("Fecha", dateGroup),
                UITheme.labeledField("Categoría", categoryCombo),
                UITheme.row(12, searchButton, printButton));
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTextField getDateField() {
        return dateField;
    }

    /** Botón selector de fecha junto al campo "Fecha" (solo diseño; sin lógica de calendario todavía). */
    public JButton getDatePickerButton() {
        return datePickerButton;
    }

    public JComboBox<String> getCategoryCombo() {
        return categoryCombo;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JButton getPrintButton() {
        return printButton;
    }

    public ResourceCalendar getResourceCalendar() {
        return resourceCalendar;
    }
}
