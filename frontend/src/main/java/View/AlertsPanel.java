package View;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/*
Area de Alertas (enunciado del Proyecto 2): panel fijo a la derecha, visible para
todos los usuarios y en todas las pestanas, con las reservas que registraron otros
usuarios durante la sesion. Es solo informativa: cada fila tiene un icono para ver
el detalle y otro para descartarla. Solo construye la interfaz; el comportamiento
lo conecta AlertsPanelListener.
*/
public class AlertsPanel extends JPanel {

    public static final int PANEL_WIDTH = RequestsSidebar.SIDEBAR_WIDTH;
    public static final String[] COLUMNS = {"Actividad", "Ver", "Desc."};
    public static final int VIEW_COLUMN = 1;
    public static final int DISMISS_COLUMN = 2;

    private static final String TITLE = "Alertas";
    private static final String NO_ALERTS_MESSAGE = "No hay alertas de reservas nuevas.";
    private static final int BLANK_ROWS = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_COLUMN_WIDTH = 44;

    private final JLabel titleLabel;
    private final JTable table;
    private final JPanel tableContainer;

    public AlertsPanel() {
        super(new BorderLayout(0, 10));
        setBackground(UITheme.SIDEBAR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 12, 14, 12));

        titleLabel = UITheme.createSectionTitle(TITLE);
        add(titleLabel, BorderLayout.NORTH);

        table = buildTable();
        JScrollPane tableScroll = new JScrollPane(table);
        UITheme.styleScrollPane(tableScroll);

        JLabel emptyLabel = new JLabel("<html><body style='width: 200px; text-align: center'>"
                + NO_ALERTS_MESSAGE + "</body></html>", SwingConstants.CENTER);
        emptyLabel.setFont(UITheme.TABLE_FONT);
        emptyLabel.setForeground(UITheme.TEXT_BLUE);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        emptyPanel.add(emptyLabel, BorderLayout.NORTH);

        tableContainer = new JPanel(new CardLayout());
        tableContainer.setOpaque(false);
        tableContainer.add(tableScroll, "table");
        tableContainer.add(emptyPanel, "empty");
        add(tableContainer, BorderLayout.CENTER);

        setPreferredSize(new Dimension(PANEL_WIDTH, 10));
    }

    private JTable buildTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[BLANK_ROWS][COLUMNS.length], COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable alertsTable = new JTable(model);
        UITheme.styleTable(alertsTable, UITheme.SIDEBAR_TABLE_HEADER);
        alertsTable.setRowSelectionAllowed(false);
        alertsTable.setFocusable(false);

        alertsTable.getColumnModel().getColumn(0).setCellRenderer(new TooltipCellRenderer());
        for (int column : new int[]{VIEW_COLUMN, DISMISS_COLUMN}) {
            alertsTable.getColumnModel().getColumn(column).setMinWidth(ICON_COLUMN_WIDTH);
            alertsTable.getColumnModel().getColumn(column).setMaxWidth(ICON_COLUMN_WIDTH);
        }
        alertsTable.getColumnModel().getColumn(VIEW_COLUMN).setCellRenderer(
                new IconCellRenderer(IconLibrary.SEARCH_BLUE, "Ver el detalle de la alerta"));
        alertsTable.getColumnModel().getColumn(DISMISS_COLUMN).setCellRenderer(
                new IconCellRenderer(IconLibrary.REMOVE_RED, "Descartar la alerta"));
        return alertsTable;
    }

    /** Muestra las alertas (una etiqueta por fila) en el orden recibido, con el contador del titulo y el estado vacio. */
    public void showAlerts(List<String> labels) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int count = labels.size();
        model.setRowCount(Math.max(BLANK_ROWS, count));
        for (int row = 0; row < model.getRowCount(); row++) {
            boolean filled = row < count;
            model.setValueAt(filled ? labels.get(row) : null, row, 0);
            model.setValueAt(filled ? Boolean.TRUE : null, row, VIEW_COLUMN);
            model.setValueAt(filled ? Boolean.TRUE : null, row, DISMISS_COLUMN);
        }
        titleLabel.setText(count == 0 ? TITLE : TITLE + " - " + count);
        ((CardLayout) tableContainer.getLayout()).show(tableContainer, count == 0 ? "empty" : "table");
    }

    public JTable getTable() {
        return table;
    }

    /** Celda de texto con el contenido completo como tooltip (la columna es angosta). */
    private static final class TooltipCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setToolTipText(value == null ? null : value.toString());
            return this;
        }
    }

    /** Celda que dibuja un icono centrado (solo en las filas que tienen una alerta). */
    private static final class IconCellRenderer extends DefaultTableCellRenderer {
        private final ImageIcon icon;
        private final String tooltip;

        IconCellRenderer(String iconFile, String tooltip) {
            this.icon = IconLibrary.get(iconFile, ICON_SIZE);
            this.tooltip = tooltip;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, null, false, false, row, column);
            setIcon(value == null ? null : icon);
            setToolTipText(value == null ? null : tooltip);
            return this;
        }
    }
}