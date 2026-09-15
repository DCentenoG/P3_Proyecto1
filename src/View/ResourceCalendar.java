package View;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla de recursos x hora (mockup pág. 2): filas de 06:00 a 22:00 y
 * una columna por recurso, mostrando "EVENTO - FUNCIONARIO" cuando el
 * recurso está agendado en esa hora, o "LIBRE" cuando no. Solo
 * construye la grilla; la carga real de reservas se conecta desde el
 * manejo de eventos, llamando a {@link #setSchedule}.
 */
public class ResourceCalendar extends JPanel {

    public static final int START_HOUR = 6;
    public static final int END_HOUR = 22;
    public static final String FREE_LABEL = "LIBRE";

    private final JTable table = new JTable();

    public ResourceCalendar() {
        this(List.of());
    }

    public ResourceCalendar(List<String> resourceNames) {
        super(new BorderLayout());
        setOpaque(false);
        UITheme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
        add(scroll, BorderLayout.CENTER);

        setResources(resourceNames);
    }

    /** Reconstruye la grilla para el conjunto de recursos dado; todas las celdas inician en "LIBRE". */
    public final void setResources(List<String> resourceNames) {
        String[] columns = new String[resourceNames.size() + 1];
        columns[0] = "Hora";
        for (int i = 0; i < resourceNames.size(); i++) {
            columns[i + 1] = resourceNames.get(i);
        }

        List<String> hours = hoursOfDay();
        Object[][] data = new Object[hours.size()][columns.length];
        for (int row = 0; row < hours.size(); row++) {
            data[row][0] = hours.get(row);
            for (int col = 1; col < columns.length; col++) {
                data[row][col] = FREE_LABEL;
            }
        }

        table.setModel(new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMaxWidth(70);
        }
    }

    /** Marca el recurso (por índice de columna, 0-based) en la hora dada con "EVENTO - FUNCIONARIO". */
    public void setSchedule(int resourceIndex, String hour, String eventAndEmployeeLabel) {
        int row = hoursOfDay().indexOf(hour);
        if (row < 0 || resourceIndex < 0 || resourceIndex + 1 >= table.getColumnCount()) {
            return;
        }
        table.getModel().setValueAt(eventAndEmployeeLabel, row, resourceIndex + 1);
    }

    /** Horas de 06:00 a 22:00, en formato de 24 horas. */
    public static List<String> hoursOfDay() {
        List<String> hours = new ArrayList<>();
        for (int h = START_HOUR; h <= END_HOUR; h++) {
            hours.add(String.format("%02d:00", h));
        }
        return hours;
    }

    public JTable getTable() {
        return table;
    }
}
