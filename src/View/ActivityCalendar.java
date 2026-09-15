package View;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Matriz de días (Lunes a Viernes) x hora (mockup pág. 3, "Calendarización
 * de actividades"): cuando una actividad coincide con un horario, la
 * celda se rellena con el color {@code #7DC4F4} y su descripción; si no,
 * queda en blanco. Solo construye la grilla; la carga real de
 * actividades se conecta desde el manejo de eventos, llamando a
 * {@link #setActivity}.
 */
public class ActivityCalendar extends JPanel {

    private static final String[] WEEKDAY_NAMES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    private final JTable table = new JTable();
    private boolean[][] filled; // [fila de hora][índice de día 0=Lunes..4=Viernes]

    public ActivityCalendar() {
        super(new BorderLayout());
        setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
        add(scroll, BorderLayout.CENTER);

        rebuild(WEEKDAY_NAMES.clone());
    }

    /** Ajusta los encabezados de día a la semana (lunes a viernes) que contiene {@code anyDayOfWeek}. */
    public void setWeek(LocalDate anyDayOfWeek) {
        LocalDate monday = anyDayOfWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] headers = new String[WEEKDAY_NAMES.length];
        for (int i = 0; i < WEEKDAY_NAMES.length; i++) {
            headers[i] = WEEKDAY_NAMES[i] + " (" + monday.plusDays(i).format(DATE_FORMAT) + ")";
        }
        rebuild(headers);
    }

    /** Marca la actividad en el día (0=Lunes..4=Viernes) y hora dados, rellenando la celda. */
    public void setActivity(int dayIndex, String hour, String description) {
        List<String> hours = ResourceCalendar.hoursOfDay();
        int row = hours.indexOf(hour);
        if (row < 0 || dayIndex < 0 || dayIndex >= WEEKDAY_NAMES.length) {
            return;
        }
        filled[row][dayIndex] = true;
        table.getModel().setValueAt(description, row, dayIndex + 1);
        table.repaint();
    }

    private void rebuild(String[] dayHeaders) {
        String[] columns = new String[dayHeaders.length + 1];
        columns[0] = "Hora";
        System.arraycopy(dayHeaders, 0, columns, 1, dayHeaders.length);

        List<String> hours = ResourceCalendar.hoursOfDay();
        filled = new boolean[hours.size()][dayHeaders.length];

        Object[][] data = new Object[hours.size()][columns.length];
        for (int row = 0; row < hours.size(); row++) {
            data[row][0] = hours.get(row);
        }

        table.setModel(new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        UITheme.styleTable(table);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        for (int col = 1; col < columns.length; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(new ActivityCellRenderer());
        }
    }

    public JTable getTable() {
        return table;
    }

    private final class ActivityCellRenderer extends DefaultTableCellRenderer {
        ActivityCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int dayIndex = column - 1;
            boolean isFilled = row < filled.length && dayIndex >= 0 && dayIndex < filled[row].length
                    && filled[row][dayIndex];
            setBackground(isFilled ? UITheme.CALENDAR_EVENT : UITheme.WHITE);
            setForeground(UITheme.TEXT_BLUE);
            setFont(UITheme.TABLE_FONT);
            return this;
        }
    }
}
