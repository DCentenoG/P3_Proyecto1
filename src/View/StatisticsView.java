package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/**
 * Vista de estadísticas (mockup pág. 4): dos paneles independientes,
 * uno de Recursos (Categoría → Cantidad) y otro de Actividades
 * (Semana → Cantidad), cada uno con sus propios filtros de fecha,
 * tabla y {@link BarChart}, sobre fondo {@code #C3CBE3}. Solo
 * construye la interfaz; el cálculo de las estadísticas a partir del
 * rango de fechas se conecta desde el manejo de eventos.
 */
public class StatisticsView extends JPanel {

    private final StatPanel resourcesPanel;
    private final StatPanel activitiesPanel;

    public StatisticsView() {
        super(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(UITheme.leftAligned(UITheme.createSectionTitle("Estadísticas")), BorderLayout.NORTH);

        resourcesPanel = new StatPanel("Recursos", "Categoría", "Cantidad");
        activitiesPanel = new StatPanel("Actividades", "Semana", "Cantidad");

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(resourcesPanel);
        columns.add(activitiesPanel);

        add(columns, BorderLayout.CENTER);
    }

    public StatPanel getResourcesPanel() {
        return resourcesPanel;
    }

    public StatPanel getActivitiesPanel() {
        return activitiesPanel;
    }

    /**
     * Panel estadístico independiente (Recursos o Actividades): filtros
     * de fecha inicio/fin, tabla de conteos y {@link BarChart}. Fondo
     * {@code #C3CBE3} según la paleta del mockup.
     */
    public static final class StatPanel extends JPanel {

        private final JTextField startDateField;
        private final JTextField endDateField;
        private final JButton searchButton;
        private final JTable table;
        private final BarChart barChart;

        private StatPanel(String title, String firstColumn, String secondColumn) {
            super(new BorderLayout(0, 12));
            setBackground(UITheme.STATS_PANEL);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

            startDateField = new JTextField(9);
            UITheme.styleField(startDateField);
            endDateField = new JTextField(9);
            UITheme.styleField(endDateField);
            searchButton = UITheme.createIconOnlyButton(IconLibrary.SEARCH, 20);
            searchButton.setToolTipText("Buscar");

            add(buildHeader(title), BorderLayout.NORTH);

            DefaultTableModel model = new DefaultTableModel(new Object[5][2], new String[]{firstColumn, secondColumn}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);
            UITheme.styleTable(table);
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
            tableScroll.setPreferredSize(new Dimension(10, 150));
            tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

            barChart = new BarChart();
            barChart.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(tableScroll);
            center.add(Box.createVerticalStrut(14));
            center.add(UITheme.leftAligned(UITheme.createLabel("Gráfico")));
            center.add(Box.createVerticalStrut(6));
            center.add(barChart);

            add(center, BorderLayout.CENTER);
        }

        private JPanel buildHeader(String title) {
            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setOpaque(false);

            header.add(UITheme.leftAligned(UITheme.createSectionTitle(title)));
            header.add(Box.createVerticalStrut(12));
            header.add(UITheme.leftAligned(buildDateRow()));
            header.add(Box.createVerticalStrut(14));
            header.add(UITheme.leftAligned(UITheme.createLabel("Estadísticas")));
            header.add(Box.createVerticalStrut(6));

            return header;
        }

        private JPanel buildDateRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            row.setOpaque(false);
            row.add(UITheme.labeledField("Fecha inicio", startDateField));
            row.add(UITheme.labeledField("Fecha fin", endDateField));

            JPanel searchWrapper = new JPanel(new GridBagLayout());
            searchWrapper.setOpaque(false);
            searchWrapper.add(searchButton);
            row.add(searchWrapper);

            return row;
        }

        // ---- Métodos de acceso para el futuro controlador ----

        public JTextField getStartDateField() {
            return startDateField;
        }

        public JTextField getEndDateField() {
            return endDateField;
        }

        public JButton getSearchButton() {
            return searchButton;
        }

        public JTable getTable() {
            return table;
        }

        public BarChart getBarChart() {
            return barChart;
        }
    }
}
