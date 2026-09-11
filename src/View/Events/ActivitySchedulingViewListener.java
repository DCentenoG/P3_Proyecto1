package View.Events;

import Model.Employee;
import Model.Reservation;
import View.ActivityCalendar;
import View.ActivitySchedulingView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/**
 * Maneja los eventos de {@link ActivitySchedulingView}: la búsqueda por
 * semana de referencia que recalcula la grilla de {@link ActivityCalendar}.
 * Regla pedida: no se puede buscar sin haber indicado la semana de
 * referencia.
 */
public final class ActivitySchedulingViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ActivitySchedulingView view;
    private final SessionContext session;

    public ActivitySchedulingViewListener(ActivitySchedulingView view, SessionContext session) {
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
        String weekText = view.getWeekField().getText().trim();
        if (weekText.isEmpty()) {
            DialogHelper.warn(view, "Debe indicar una fecha de referencia de la semana antes de continuar.");
            return;
        }

        LocalDate referenceDate;
        try {
            referenceDate = LocalDate.parse(weekText, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "La fecha debe tener el formato dd/mm/aaaa.");
            return;
        }

        ActivityCalendar calendar = view.getActivityCalendar();
        calendar.setWeek(referenceDate);

        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        for (Employee employee : session.getUsers().getListOfEmployees()) {
            for (Reservation reservation : employee.getReservations()) {
                LocalDate date = reservation.getDate();
                if (date.isBefore(monday) || date.isAfter(friday)) {
                    continue;
                }
                int dayIndex = date.getDayOfWeek().getValue() - 1; // 0=Lunes..4=Viernes
                String label = reservation.getActivity() + " - " + employee.getName();
                for (String hour : CalendarViewListener.hoursBetween(reservation.getStartTime(), reservation.getEndTime())) {
                    calendar.setActivity(dayIndex, hour, label);
                }
            }
        }
    }
}
