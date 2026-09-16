package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Report.ReportException;
import Report.ReportService;
import Report.ScheduleReportRow;
import View.CalendarView;
import View.DatePickerDialog;
import View.FilterBuilder;
import View.ReservationDetailsDialog;
import View.ResourceCalendar;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * Última búsqueda realmente aplicada (con "Buscar", o la carga
     * inicial): es contra ESTO, y no contra lo que haya en ese momento
     * seleccionado en el campo de fecha o el combo de categoría, que se
     * recarga la grilla cuando hay un refresco automático por un cambio de
     * datos hecho en otra pantalla (una reserva creada/cancelada, o un
     * recurso/categoría editado — ver {@link #refresh()}). Así, cambiar el
     * filtro nunca actualiza la grilla por sí solo — solo lo hace
     * "Buscar" — y eso queda totalmente separado del refresco en tiempo
     * real.
     */
    private LocalDate activeDate = LocalDate.now();

    /** {@code null} = "(Todas)"; si no, la descripción de la categoría activa. */
    private String activeCategory;

    /** Recursos (mismo orden que las columnas de la grilla) de la última búsqueda aplicada; usado al imprimir. */
    private List<Resource> currentResources = new ArrayList<>();

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
        runSearch();
    }

    private void wire() {
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getPrintButton().addActionListener(e -> onPrint());
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

        // Este es el ÚNICO lugar donde lo que hay seleccionado en el campo
        // de fecha y el combo de categoría pasa a ser la búsqueda activa.
        activeDate = date;
        activeCategory = categoryApplied ? selectedCategory.toString() : null;
        runSearch();
    }

    /**
     * Vuelve a ejecutar la última búsqueda aplicada ({@link #activeDate}/
     * {@link #activeCategory}) contra los datos actuales, para reflejar una
     * reserva creada/cancelada, o un recurso/categoría editado, hecho en
     * otra pantalla. A propósito no relee los campos de fecha/categoría en
     * pantalla: si el usuario tiene una fecha u categoría nueva elegida ahí
     * pero todavía no presionó "Buscar", ese cambio no debe colarse en la
     * grilla.
     */
    public void refresh() {
        runSearch();
    }

    private void runSearch() {
        List<Resource> resources = new ArrayList<>();
        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (activeCategory != null && !category.getDescription().equals(activeCategory)) {
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
        applySchedule(calendar, resources, activeDate);
        currentResources = resources;
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

    // ------------------------------------------------------------------
    // Imprimir (generación de reportes PDF con JasperReports)
    // ------------------------------------------------------------------

    /**
     * Genera un PDF con el listado de ocupación (recurso, hora, actividad,
     * funcionario) de la grilla actualmente mostrada -- es decir, a partir
     * de {@link #cellIndex}, ya calculado por la última búsqueda aplicada,
     * en vez de recalcularlo -- y lo guarda donde el usuario elija.
     */
    private void onPrint() {
        List<ScheduleReportRow> rows = buildScheduleRows();
        if (rows.isEmpty()) {
            DialogHelper.warn(view, "No hay recursos agendados para los criterios seleccionados.");
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateText", activeDate.format(DATE_FORMAT));
        parameters.put("categoryText", (activeCategory != null) ? activeCategory : "(Todas)");

        try {
            byte[] pdf = ReportService.generatePdf("/ReportDesign/calendarizacion.jrxml", rows, parameters);
            saveAndOpenPdf(pdf);
        } catch (ReportException ex) {
            DialogHelper.error(view, "No fue posible generar el reporte: " + ex.getMessage());
        }
    }

    /** Aplana {@link #cellIndex} a una fila por franja ocupada, ordenada por recurso y luego por hora. */
    private List<ScheduleReportRow> buildScheduleRows() {
        List<ScheduleReportRow> rows = new ArrayList<>();
        List<String> hours = ResourceCalendar.hoursOfDay();
        for (int resourceIndex = 0; resourceIndex < currentResources.size(); resourceIndex++) {
            Resource resource = currentResources.get(resourceIndex);
            for (String hour : hours) {
                ScheduleEntry entry = cellIndex.get(resourceIndex + "|" + hour);
                if (entry != null) {
                    rows.add(new ScheduleReportRow(resource.getDescription(), hour,
                            entry.reservation().getActivity(), entry.employee().getName()));
                }
            }
        }
        return rows;
    }

    /** Deja que el usuario elija dónde guardar el PDF y, si es posible, lo abre con el visor por defecto. */
    private void saveAndOpenPdf(byte[] pdf) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("calendarizacion.pdf"));
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

        DialogHelper.info(view, "Calendarización", "Reporte generado correctamente: " + target.getName());
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
