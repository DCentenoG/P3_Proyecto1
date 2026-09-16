package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import View.CalendarView;
import View.DatePickerDialog;
import View.FilterBuilder;
import View.ReservationDetailsDialog;
import View.ResourceCalendar;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja los eventos de {@link CalendarView}: la búsqueda por fecha y/o
 * categoría que recalcula la grilla de {@link ResourceCalendar}. Regla
 * pedida: no se puede buscar si no se aplicó al menos un criterio
 * (fecha o categoría).
 */
public final class CalendarViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CalendarView view;
    private final SessionContext session;

    /** Reserva (y su funcionario) detrás de cada celda ocupada de la grilla actual, indexada por "índice de recurso|hora". */
    private final Map<String, ScheduleEntry> cellIndex = new HashMap<>();

    private record ScheduleEntry(Employee employee, Reservation reservation) {
    }

    public CalendarViewListener(CalendarView view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
        // El combo de categoría se construyó vacío (CalendarView no conoce la
        // sesión al armarse); se sincroniza aquí con las categorías reales.
        refreshCategoryOptions();
        // Se precarga la fecha de hoy (el campo ya no admite escritura manual,
        // solo selección mediante su botón) y se muestra de una vez la
        // calendarización con todos los recursos existentes, en vez de dejar
        // la grilla sin columnas hasta la primera búsqueda manual.
        view.getDateField().setText(LocalDate.now().format(DATE_FORMAT));
        onSearch();
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getPrintButton().addActionListener(e -> DialogHelper.info(view, "Imprimir",
                "La generación de reportes en PDF se implementará en una etapa posterior."));
        view.getDatePickerButton().addActionListener(e -> onPickDate());

        JTable table = view.getResourceCalendar().getTable();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol <= 0) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                int modelCol = table.convertColumnIndexToModel(viewCol);
                List<String> hours = ResourceCalendar.hoursOfDay();
                if (modelRow < 0 || modelRow >= hours.size()) {
                    return;
                }
                String hour = hours.get(modelRow);
                int resourceIndex = modelCol - 1;
                ScheduleEntry entry = cellIndex.get(resourceIndex + "|" + hour);
                if (entry != null) {
                    new ReservationDetailsDialog(SwingUtilities.getWindowAncestor(view),
                            entry.employee(), entry.reservation()).setVisible(true);
                }
            }
        });
    }

    /** Reconstruye el combo de categorías del filtro con las categorías actuales. */
    public void refreshCategoryOptions() {
        JComboBox<String> combo = view.getCategoryCombo();
        Object previousSelection = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(FilterBuilder.NO_FILTER);
        for (ResourceCategory category : session.getCategories().getCategories()) {
            combo.addItem(category.getDescription());
        }
        if (previousSelection != null) {
            combo.setSelectedItem(previousSelection);
        }
    }

    /** Único campo de fecha de esta pantalla: sin pareja inicio/fin, no hay rango que restringir. */
    private void onPickDate() {
        LocalDate initial = parseDateOrNull(view.getDateField().getText());
        LocalDate picked = DatePickerDialog.show(SwingUtilities.getWindowAncestor(view), initial, null, null);
        if (picked != null) {
            view.getDateField().setText(picked.format(DATE_FORMAT));
        }
    }

    private static LocalDate parseDateOrNull(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    private void onSearch() {
        String dateText = view.getDateField().getText().trim();
        Object selectedCategory = view.getCategoryCombo().getSelectedItem();
        boolean categoryApplied = selectedCategory != null && !FilterBuilder.NO_FILTER.equals(selectedCategory);

        // Dejar la categoría en "(Todas)" (y/o la fecha vacía) ya no se trata
        // como "falta un criterio": simplemente significa mostrar todos los
        // recursos existentes (con la fecha de hoy si no se indicó otra).
        LocalDate date = LocalDate.now();
        if (!dateText.isEmpty()) {
            try {
                date = LocalDate.parse(dateText, DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                DialogHelper.warn(view, "La fecha debe tener el formato dd/mm/aaaa.");
                return;
            }
        }

        List<Resource> resources = new ArrayList<>();
        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (categoryApplied && !category.getDescription().equals(selectedCategory.toString())) {
                continue;
            }
            resources.addAll(category.getResources());
        }

        List<String> columnLabels = new ArrayList<>();
        for (Resource resource : resources) {
            columnLabels.add(resource.getDescription() + " (" + resource.getId() + ")");
        }

        ResourceCalendar calendar = view.getResourceCalendar();
        calendar.setResources(columnLabels);
        applySchedule(calendar, resources, date);
    }

    private void applySchedule(ResourceCalendar calendar, List<Resource> resources, LocalDate date) {
        cellIndex.clear();
        for (Employee employee : session.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                if (!reservation.getDate().equals(date)) {
                    continue;
                }
                for (Resource resource : reservation.getAssignedResources()) {
                    int columnIndex = indexOf(resources, resource);
                    if (columnIndex < 0) {
                        continue;
                    }
                    String label = reservation.getActivity() + " - " + employee.getName();
                    for (String hour : hoursBetween(reservation.getStartTime(), reservation.getEndTime())) {
                        calendar.setSchedule(columnIndex, hour, label);
                        cellIndex.put(columnIndex + "|" + hour, new ScheduleEntry(employee, reservation));
                    }
                }
            }
        }
    }

    private static int indexOf(List<Resource> resources, Resource target) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).isSameResource(target)) {
                return i;
            }
        }
        return -1;
    }

    /** Horas (formato "HH:00") de la grilla cubiertas por el intervalo [start, end). Compartido con Actividades. */
    static List<String> hoursBetween(LocalTime start, LocalTime end) {
        List<String> hours = new ArrayList<>();
        int startHour = Math.max(start.getHour(), ResourceCalendar.START_HOUR);
        int endHour = (end.getMinute() == 0) ? end.getHour() - 1 : end.getHour();
        endHour = Math.min(endHour, ResourceCalendar.END_HOUR);
        for (int h = startHour; h <= endHour; h++) {
            hours.add(String.format("%02d:00", h));
        }
        return hours;
    }
}
