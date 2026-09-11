package View.Events;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import View.CalendarView;
import View.FilterBuilder;
import View.ResourceCalendar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

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

    public CalendarViewListener(CalendarView view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getPrintButton().addActionListener(e -> DialogHelper.info(view, "Imprimir",
                "La generación de reportes en PDF se implementará en una etapa posterior."));
    }

    private void onSearch() {
        String dateText = view.getDateField().getText().trim();
        Object selectedCategory = view.getCategoryCombo().getSelectedItem();
        boolean categoryApplied = selectedCategory != null && !FilterBuilder.NO_FILTER.equals(selectedCategory);

        if (dateText.isEmpty() && !categoryApplied) {
            DialogHelper.warn(view, "Debe indicar al menos un criterio de búsqueda (fecha o categoría) antes de continuar.");
            return;
        }

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
