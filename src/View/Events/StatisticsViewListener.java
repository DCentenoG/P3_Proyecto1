package View.Events;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import View.StatisticsView;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maneja los eventos de {@link StatisticsView}: la búsqueda por rango de
 * fechas de cada panel (Recursos y Actividades), que recalcula la tabla
 * de conteos y el {@link View.BarChart}. Regla pedida: no se puede
 * buscar si no se aplicó al menos un criterio (fecha de inicio o fin).
 */
public final class StatisticsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StatisticsView view;
    private final SessionContext session;

    public StatisticsViewListener(StatisticsView view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
    }

    private void wire() {
        view.getResourcesPanel().getSearchButton().addActionListener(e -> onSearch(view.getResourcesPanel(), true));
        view.getActivitiesPanel().getSearchButton().addActionListener(e -> onSearch(view.getActivitiesPanel(), false));
    }

    private void onSearch(StatisticsView.StatPanel panel, boolean byCategory) {
        String startText = panel.getStartDateField().getText().trim();
        String endText = panel.getEndDateField().getText().trim();

        if (startText.isEmpty() && endText.isEmpty()) {
            DialogHelper.warn(view, "Debe indicar al menos una fecha (inicio o fin) antes de continuar.");
            return;
        }

        LocalDate start = LocalDate.MIN;
        LocalDate end = LocalDate.MAX;
        try {
            if (!startText.isEmpty()) {
                start = LocalDate.parse(startText, DATE_FORMAT);
            }
            if (!endText.isEmpty()) {
                end = LocalDate.parse(endText, DATE_FORMAT);
            }
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "Las fechas deben tener el formato dd/mm/aaaa.");
            return;
        }

        Map<String, Integer> counts = new TreeMap<>();
        for (Employee employee : session.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                LocalDate date = reservation.getDate();
                if (date.isBefore(start) || date.isAfter(end)) {
                    continue;
                }
                if (byCategory) {
                    for (Resource resource : reservation.getAssignedResources()) {
                        String key = resource.getResourceCategoryReference().getDescription();
                        counts.merge(key, 1, Integer::sum);
                    }
                } else {
                    String key = "Semana " + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + "-" + date.getYear();
                    counts.merge(key, 1, Integer::sum);
                }
            }
        }

        renderTable(panel.getTable(), counts);
        panel.getBarChart().setData(new LinkedHashMap<>(counts));
    }

    private void renderTable(JTable table, Map<String, Integer> counts) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(Math.max(5, counts.size()));

        int row = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            model.setValueAt(entry.getKey(), row, 0);
            model.setValueAt(entry.getValue(), row, 1);
            row++;
        }
        for (; row < model.getRowCount(); row++) {
            model.setValueAt(null, row, 0);
            model.setValueAt(null, row, 1);
        }
    }
}
