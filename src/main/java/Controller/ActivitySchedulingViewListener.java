package Controller;

import Model.Employee;
import Model.Reservation;
import Report.ReportException;
import Report.ReportService;
import Report.ScheduleReportRow;
import View.ActivityCalendar;
import View.ActivitySchedulingView;
import View.DatePickerDialog;
import View.ReservationDetailsDialog;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maneja los eventos de {@link ActivitySchedulingView}: la búsqueda por
 * semana de referencia que recalcula la grilla de {@link ActivityCalendar}.
 * Regla pedida: no se puede buscar sin haber indicado la semana de
 * referencia.
 */
public final class ActivitySchedulingViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Mismos nombres y orden (Lunes..Viernes) que las columnas de {@link ActivityCalendar}. */
    private static final String[] WEEKDAY_NAMES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};

    private final ActivitySchedulingView view;
    private final SessionContext session;

    /** Reserva (y su funcionario) detrás de cada celda ocupada de la grilla actual, indexada por "día (0=Lunes)|hora". */
    private final Map<String, ScheduleEntry> cellIndex = new HashMap<>();

    private record ScheduleEntry(Employee employee, Reservation reservation) {
    }

    /**
     * Última semana de referencia realmente buscada (con "Buscar", o la
     * carga inicial): contra ESTA, y no contra lo que haya en ese momento
     * seleccionado en el campo de semana, se recarga la grilla ante un
     * refresco automático por una reserva creada/cancelada en otra
     * pantalla (ver {@link #refresh()}).
     */
    private LocalDate activeWeek = LocalDate.now();

    public ActivitySchedulingViewListener(ActivitySchedulingView view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
        // Se precarga la semana actual (el campo ya no admite escritura
        // manual, solo selección mediante su botón) y se muestra de una vez
        // la calendarización con las reservas existentes, en vez de dejar la
        // grilla vacía hasta la primera búsqueda manual.
        view.getWeekField().setText(LocalDate.now().format(DATE_FORMAT));
        runSearch();
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getPrintButton().addActionListener(e -> onPrint());
        view.getWeekPickerButton().addActionListener(e -> onPickWeek());

        JTable table = view.getActivityCalendar().getTable();
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
                List<String> hours = View.ResourceCalendar.hoursOfDay();
                if (modelRow < 0 || modelRow >= hours.size()) {
                    return;
                }
                String hour = hours.get(modelRow);
                int dayIndex = modelCol - 1;
                ScheduleEntry entry = cellIndex.get(dayIndex + "|" + hour);
                if (entry != null) {
                    new ReservationDetailsDialog(SwingUtilities.getWindowAncestor(view),
                            entry.employee(), entry.reservation()).setVisible(true);
                }
            }
        });
    }

    /** Único campo de fecha de esta pantalla: sin pareja inicio/fin, no hay rango que restringir. */
    private void onPickWeek() {
        LocalDate initial = parseDateOrNull(view.getWeekField().getText());
        LocalDate picked = DatePickerDialog.show(SwingUtilities.getWindowAncestor(view), initial, null, null);
        if (picked != null) {
            view.getWeekField().setText(picked.format(DATE_FORMAT));
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
        String weekText = view.getWeekField().getText().trim();
        LocalDate referenceDate;
        if (weekText.isEmpty()) {
            referenceDate = LocalDate.now();
        } else {
            try {
                referenceDate = LocalDate.parse(weekText, DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                DialogHelper.warn(view, "La fecha debe tener el formato dd/mm/aaaa.");
                return;
            }
        }

        // Este es el ÚNICO lugar donde la semana elegida en el campo pasa a
        // ser la búsqueda activa.
        activeWeek = referenceDate;
        runSearch();
    }

    /**
     * Vuelve a ejecutar la última búsqueda de semana aplicada
     * ({@link #activeWeek}) contra los datos actuales, para reflejar una
     * reserva creada/cancelada en otra pantalla. A propósito no relee el
     * campo de semana en pantalla: si el usuario tiene una semana nueva
     * elegida ahí pero todavía no presionó "Buscar", ese cambio no debe
     * colarse en la grilla.
     */
    public void refresh() {
        runSearch();
    }

    private void runSearch() {
        ActivityCalendar calendar = view.getActivityCalendar();
        calendar.setWeek(activeWeek);

        LocalDate monday = activeWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        cellIndex.clear();
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
                    cellIndex.put(dayIndex + "|" + hour, new ScheduleEntry(employee, reservation));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Imprimir (generación de reportes PDF con JasperReports)
    // ------------------------------------------------------------------

    /**
     * Genera un PDF con el listado de ocupación (día, hora, actividad,
     * funcionario) de la grilla actualmente mostrada -- a partir de
     * {@link #cellIndex}, ya calculado por la última búsqueda aplicada -- y
     * lo guarda donde el usuario elija.
     */
    private void onPrint() {
        List<ScheduleReportRow> rows = buildScheduleRows();
        if (rows.isEmpty()) {
            DialogHelper.warn(view, "No hay actividades agendadas para la semana seleccionada.");
            return;
        }

        LocalDate monday = activeWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("weekText", monday.format(DATE_FORMAT) + " al " + friday.format(DATE_FORMAT));

        try {
            byte[] pdf = ReportService.generatePdf("/ReportDesign/actividades.jrxml", rows, parameters);
            saveAndOpenPdf(pdf);
        } catch (ReportException ex) {
            DialogHelper.error(view, "No fue posible generar el reporte: " + ex.getMessage());
        }
    }

    /** Aplana {@link #cellIndex} a una fila por franja ocupada, ordenada por día (Lunes..Viernes) y luego por hora. */
    private List<ScheduleReportRow> buildScheduleRows() {
        List<ScheduleReportRow> rows = new ArrayList<>();
        List<String> hours = View.ResourceCalendar.hoursOfDay();
        for (int dayIndex = 0; dayIndex < WEEKDAY_NAMES.length; dayIndex++) {
            for (String hour : hours) {
                ScheduleEntry entry = cellIndex.get(dayIndex + "|" + hour);
                if (entry != null) {
                    rows.add(new ScheduleReportRow(WEEKDAY_NAMES[dayIndex], hour,
                            entry.reservation().getActivity(), entry.employee().getName()));
                }
            }
        }
        return rows;
    }

    /** Deja que el usuario elija dónde guardar el PDF y, si es posible, lo abre con el visor por defecto. */
    private void saveAndOpenPdf(byte[] pdf) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("actividades.pdf"));
        int result = chooser.showSaveDialog(view);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = new File(target.getParentFile(), target.getName() + ".pdf");
        }

        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(pdf);
        } catch (IOException ex) {
            DialogHelper.error(view, "No fue posible guardar el archivo PDF: " + ex.getMessage());
            return;
        }

        DialogHelper.info(view, "Actividades", "Reporte generado correctamente: " + target.getName());
        openIfPossible(target);
    }

    /** Abre el PDF recién generado con la aplicación asociada del sistema operativo, si el entorno lo permite. */
    private void openIfPossible(File file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            return;
        }
        try {
            desktop.open(file);
        } catch (IOException ignored) {
            // No hay visor de PDF asociado, o falló al abrirlo: el archivo ya quedó guardado igual.
        }
    }
}
