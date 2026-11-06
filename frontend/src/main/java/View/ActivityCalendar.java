package View;

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
 * Matriz de días (Domingo a Sábado) x hora (mockup pág. 3, "Calendarización
 * de actividades"): cuando una actividad coincide con un horario, la
 * celda se rellena con el color {@code #7DC4F4} y su descripción; si no,
 * queda en blanco. Solo construye la grilla; la carga real de
 * actividades se conecta desde el manejo de eventos, llamando a
 * {@link #setActivity}.
 */
public class ActivityCalendar extends JPanel {

    /**
     * Días de la semana, en el orden de las columnas: la semana empieza el
     * domingo (índice 0 = Domingo). Incluye sábado y domingo porque el
     * sistema permite reservar cualquier día, así que la semana completa
     * debe poder verse.
     */
    public static final List<String> DAY_NAMES =
            List.of("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado");

    /** Primer día (domingo) de la semana que contiene {@code anyDayOfWeek}. */
    public static LocalDate weekStartOf(LocalDate anyDayOfWeek) {
        return anyDayOfWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    /** Índice de columna (0=Domingo..6=Sábado) del día de {@code date}. */
    public static int dayIndexOf(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7; // DayOfWeek: 1=Lunes..7=Domingo
    }
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    private final JTable table = new JTable();
    private boolean[][] filled; // [fila de hora][índice de día 0=Domingo..6=Sábado]

    public ActivityCalendar() {
        super(new BorderLayout());
        setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        UITheme.styleScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

        rebuild(DAY_NAMES.toArray(new String[0]));
    }

    /** Ajusta los encabezados de día a la semana (domingo a sábado) que contiene {@code anyDayOfWeek}. */
    public void setWeek(LocalDate anyDayOfWeek) {
        LocalDate weekStart = weekStartOf(anyDayOfWeek);
        String[] headers = new String[DAY_NAMES.size()];
        for (int i = 0; i < DAY_NAMES.size(); i++) {
            headers[i] = DAY_NAMES.get(i) + " (" + weekStart.plusDays(i).format(DATE_FORMAT) + ")";
        }
        rebuild(headers);
    }

    /** Marca la actividad en el día (0=Domingo..6=Sábado) y hora dados, rellenando la celda. */
    public void setActivity(int dayIndex, String hour, String description) {
        List<String> hours = ResourceCalendar.hoursOfDay();
        int row = hours.indexOf(hour);
        if (row < 0 || dayIndex < 0 || dayIndex >= DAY_NAMES.size()) {
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
            // Con siete días las columnas son más angostas: el texto completo va en el tooltip.
            setToolTipText(value == null ? null : value.toString());
            setForeground(UITheme.TEXT_BLUE);
            setFont(UITheme.TABLE_FONT);
            return this;
        }
    }
}
