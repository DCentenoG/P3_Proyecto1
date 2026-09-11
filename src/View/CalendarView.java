package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
        header.add(UITheme.leftAligned(UITheme.createLabel("Filtros")));
        header.add(Box.createVerticalStrut(6));
        header.add(UITheme.centered(buildFilterRow(categoryOptions)));
        header.add(Box.createVerticalStrut(18));
        header.add(UITheme.leftAligned(UITheme.createLabel("Calendarización de recursos")));
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    private JPanel buildFilterRow(List<String> categoryOptions) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        row.setOpaque(false);

        dateField = new JTextField(12);
        UITheme.styleField(dateField);
        row.add(UITheme.labeledField("Fecha", dateField));

        categoryCombo = new JComboBox<>(categoryOptions.toArray(new String[0]));
        categoryCombo.setFont(UITheme.FIELD_FONT);
        row.add(UITheme.labeledField("Categoría", categoryCombo));

        searchButton = UITheme.createIconOnlyButton(IconLibrary.SEARCH, 22);
        searchButton.setToolTipText("Buscar");
        printButton = UITheme.createIconOnlyButton(IconLibrary.PRINTER, 22);
        printButton.setToolTipText("Imprimir");

        JPanel icons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        icons.setOpaque(false);
        icons.add(searchButton);
        icons.add(printButton);
        row.add(icons);

        return row;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTextField getDateField() {
        return dateField;
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
