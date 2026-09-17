package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Report.ReportException;
import Report.ReportService;
import Report.ReservationReportRow;
import Service.AIExtractionException;
import Service.AIReservationExtractionService;
import Service.ExtractedReservationData;
import Service.ServiceException;
import View.DatePickerDialog;
import View.ReservationsView;
import View.TimePickerDialog;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.Cursor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja los eventos de {@link ReservationsView}: el botón de ayuda "?"
 * (explica el llenado por IA), el llenado automático por IA a partir de
 * la "Frase" (ver {@link AIReservationExtractionService}), y las
 * acciones de Guardar, Cancelar y Limpiar de la sección "Nueva reserva".
 * <p>
 * Reglas pedidas: no se puede guardar una reserva con campos vacíos
 * (excepto "Frase"); no se puede cancelar una reserva sin haberla
 * seleccionado en la tabla; y tanto Guardar como Cancelar piden
 * confirmación antes de ejecutarse.
 */
public final class ReservationsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String HELP_MESSAGE =
            "Interpreta la \"Frase\" escrita y completa el resto del formulario. "
                    + "Revise los campos antes de guardar.";

    private final ReservationsView view;
    private final SessionContext session;
    private final Employee employee;

    // La IA solo interpreta la frase y llena texto; no valida disponibilidad
    // ni existencia de categorías (eso sigue en ReservationService), así que
    // no necesita compartir la instancia de Service del resto del programa.
    private final AIReservationExtractionService aiExtractionService = new AIReservationExtractionService();

    /**
     * Se invoca tras crear o cancelar una reserva, para que Calendarización
     * y Actividades (armadas una sola vez al iniciar sesión y mantenidas
     * vivas en sus pestañas) refresquen su propia grilla con los datos ya
     * actualizados, en vez de mostrar la foto desactualizada de su última
     * búsqueda hasta que el usuario la repita manualmente.
     */
    private Runnable afterReservationChange = () -> { };

    public ReservationsViewListener(ReservationsView view, SessionContext session) {
        this.view = view;
        this.session = session;
        this.employee = (Employee) session.getCurrentUser();
        wire();
        loadCategoryOptions();
        renderReservations();
    }

    private void wire() {
        view.getHelpButton().addActionListener(e ->
                DialogHelper.info(view, "Generación automática de reservas", HELP_MESSAGE));
        view.getAiButton().addActionListener(e -> onAiFill());
        view.getSaveButton().addActionListener(e -> onSave());
        view.getCancelButton().addActionListener(e -> onCancelReservation());
        view.getClearButton().addActionListener(e -> clearForm());
        view.getPrintButton().addActionListener(e -> onPrint());
        view.getAddCategoryButton().addActionListener(e -> onAddCategory());
        view.getRemoveCategoryButton().addActionListener(e -> onRemoveCategory());
        view.getDateDropdownButton().addActionListener(e -> onPickDate());
        view.getStartTimeButton().addActionListener(e -> onPickStartTime());
        view.getEndTimeButton().addActionListener(e -> onPickEndTime());
        TableInteractionUtil.deselectOnClickOutside(view.getReservationsTable(), view.getCancelButton());
    }

    public void setAfterReservationChange(Runnable callback) {
        this.afterReservationChange = (callback != null) ? callback : () -> { };
    }

    // ------------------------------------------------------------------
    // Selectores de fecha/hora
    // ------------------------------------------------------------------

    private void onPickDate() {
        LocalDate initial = parseDateOrNull(view.getDateField().getText());
        // Una reserva no se puede hacer en una fecha que ya pasó (regla que
        // ahora también aplica Service.ReservationService.createReservation):
        // se previene directamente en el selector, en vez de dejar elegirla y
        // recién avisar después de intentar guardar.
        LocalDate picked = DatePickerDialog.show(SwingUtilities.getWindowAncestor(view), initial, LocalDate.now(), null);
        if (picked != null) {
            view.getDateField().setText(picked.format(DATE_FORMAT));
        }
    }

    private void onPickStartTime() {
        LocalTime initial = parseTimeOrNull(view.getStartTimeField().getText());
        // La hora de inicio no puede quedar después de la hora de fin ya elegida (si existe).
        LocalTime max = parseTimeOrNull(view.getEndTimeField().getText());
        LocalTime picked = TimePickerDialog.show(SwingUtilities.getWindowAncestor(view), initial, null, max);
        if (picked != null) {
            view.getStartTimeField().setText(picked.format(TIME_FORMAT));
        }
    }

    private void onPickEndTime() {
        LocalTime initial = parseTimeOrNull(view.getEndTimeField().getText());
        // La hora de fin no puede quedar antes de la hora de inicio ya elegida (si existe).
        LocalTime min = parseTimeOrNull(view.getStartTimeField().getText());
        LocalTime picked = TimePickerDialog.show(SwingUtilities.getWindowAncestor(view), initial, min, null);
        if (picked != null) {
            view.getEndTimeField().setText(picked.format(TIME_FORMAT));
        }
    }

    private static LocalDate parseDateOrNull(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    private static LocalTime parseTimeOrNull(String text) {
        try {
            return LocalTime.parse(text.trim(), TIME_FORMAT);
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    private void loadCategoryOptions() {
        JComboBox<String> combo = view.getCategoryCombo();
        combo.removeAllItems();
        for (ResourceCategory category : session.getCategories().getCategories()) {
            combo.addItem(category.getDescription());
        }
    }

    private void onAddCategory() {
        Object selected = view.getCategoryCombo().getSelectedItem();
        if (selected == null) {
            return;
        }
        String category = selected.toString();
        DefaultTableModel model = (DefaultTableModel) view.getCategoriesTable().getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (category.equals(model.getValueAt(row, 0))) {
                return; // ya está en la lista, no se duplica
            }
        }
        model.addRow(new Object[]{category});
    }

    private void onRemoveCategory() {
        int row = view.getCategoriesTable().getSelectedRow();
        if (row < 0) {
            DialogHelper.warn(view, "Debe seleccionar una categoría de la lista para quitarla.");
            return;
        }
        ((DefaultTableModel) view.getCategoriesTable().getModel()).removeRow(row);
    }

    // ------------------------------------------------------------------
    // Llenado automático por IA (a partir de la "Frase")
    // ------------------------------------------------------------------

    /**
     * Envía la "Frase" a {@link AIReservationExtractionService} en un hilo
     * aparte (la llamada de red toma 1-3s) y, con lo que devuelva, precarga
     * el resto del formulario. Nunca bloquea el flujo normal: ante
     * cualquier falla (sin red, clave ausente, bloqueo de seguridad, XML
     * mal formado) solo avisa y deja el formulario para llenado manual.
     */
    private void onAiFill() {
        String phrase = view.getPhraseField().getText().trim();
        if (phrase.isEmpty()) {
            DialogHelper.warn(view, "Escriba una frase en el campo \"Frase\" antes de generar la reserva.");
            return;
        }

        view.getAiButton().setEnabled(false);
        Cursor previousCursor = view.getCursor();
        view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<ExtractedReservationData, Void>() {
            @Override
            protected ExtractedReservationData doInBackground() throws AIExtractionException {
                return aiExtractionService.extractFromPhrase(phrase);
            }

            @Override
            protected void done() {
                view.setCursor(previousCursor);
                view.getAiButton().setEnabled(true);
                try {
                    applyExtractedData(get());
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    DialogHelper.warn(view, "No se pudo interpretar la frase (" + cause.getMessage()
                            + "). Complete los campos manualmente.");
                }
            }
        }.execute();
    }

    /** Precarga el formulario con lo que la IA logró extraer; el usuario puede revisar/editar antes de guardar. */
    private void applyExtractedData(ExtractedReservationData data) {
        if (data.getActivity() != null) {
            view.getActivityField().setText(data.getActivity());
        }
        if (data.getDate() != null) {
            view.getDateField().setText(data.getDate().format(DATE_FORMAT));
        }
        if (data.getStartTime() != null) {
            view.getStartTimeField().setText(data.getStartTime().format(TIME_FORMAT));
        }
        if (data.getEndTime() != null) {
            view.getEndTimeField().setText(data.getEndTime().format(TIME_FORMAT));
        }

        DefaultTableModel model = (DefaultTableModel) view.getCategoriesTable().getModel();
        model.setRowCount(0);
        for (String category : data.getCategoryDescriptions()) {
            model.addRow(new Object[]{normalizeCategoryDescription(category)});
        }

        if (data.isEmpty()) {
            DialogHelper.info(view, "Generación automática",
                    "No se pudo identificar ningún dato en esa frase. Complete el formulario manualmente.");
        }
    }

    /**
     * Si lo que extrajo la IA coincide (sin importar mayúsculas/minúsculas)
     * con una categoría real del sistema, se usa el texto exacto de esa
     * categoría, para que quede reconocida al guardar sin que el usuario
     * tenga que corregir un simple cambio de mayúsculas. Si no coincide con
     * ninguna, se deja tal cual la extrajo la IA: el usuario la revisa y,
     * si hace falta, la reemplaza usando el combo de categorías — esta
     * clase no valida existencia ni disponibilidad, eso sigue en
     * ReservationService.
     */
    private String normalizeCategoryDescription(String extracted) {
        for (ResourceCategory category : session.getCategories().getCategories()) {
            if (category.getDescription().equalsIgnoreCase(extracted)) {
                return category.getDescription();
            }
        }
        return extracted;
    }

    // ------------------------------------------------------------------
    // Guardar reserva
    // ------------------------------------------------------------------

    private void onSave() {
        String activity = view.getActivityField().getText().trim();
        String dateText = view.getDateField().getText().trim();
        String startText = view.getStartTimeField().getText().trim();
        String endText = view.getEndTimeField().getText().trim();
        List<String> selectedCategories = view.getSelectedCategories();

        if (activity.isEmpty() || dateText.isEmpty() || startText.isEmpty() || endText.isEmpty()
                || selectedCategories.isEmpty()) {
            DialogHelper.warn(view, "Debe completar todos los campos marcados con * antes de guardar la reserva.");
            return;
        }

        LocalDate date;
        LocalTime startTime;
        LocalTime endTime;
        try {
            date = LocalDate.parse(dateText, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "La fecha debe tener el formato dd/mm/aaaa.");
            return;
        }
        try {
            startTime = LocalTime.parse(startText, TIME_FORMAT);
            endTime = LocalTime.parse(endText, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            DialogHelper.warn(view, "Las horas deben tener el formato hh:mm (24 horas).");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            DialogHelper.warn(view, "La hora de fin debe ser posterior a la hora de inicio.");
            return;
        }

        if (!DialogHelper.confirm(view, "¿Desea guardar esta reserva?")) {
            return;
        }

        // Armar la reserva (asignar, todo-o-nada, el primer recurso libre de
        // cada categoría requerida, y guardar el XML) ya no se hace aquí:
        // vive en Service.ReservationService (ver Model.Employee#tryBook),
        // así el Controller solo junta los datos del formulario y traduce el
        // resultado a diálogos, como corresponde en la arquitectura MVC.
        try {
            session.getReservationService().createReservation(employee.getId(), activity, date, startTime, endTime,
                    selectedCategories);
        } catch (ServiceException ex) {
            DialogHelper.error(view, ex.getMessage());
            return;
        }

        DialogHelper.info(view, "Reservas", "La reserva se guardó correctamente.");
        clearForm();
        renderReservations();
        afterReservationChange.run();
    }

    // ------------------------------------------------------------------
    // Cancelar reserva
    // ------------------------------------------------------------------

    // DESPUÉS
    private void onCancelReservation() {
        int row = view.getReservationsTable().getSelectedRow();
        if (row < 0 || row >= employee.getReservations().size()) {
            DialogHelper.warn(view, "Debe seleccionar una reserva de la tabla para poder cancelarla.");
            return;
        }
        if (!DialogHelper.confirm(view, "¿Desea cancelar la reserva seleccionada?")) {
            return;
        }

        Reservation toCancel = employee.getReservations().get(row);
        try {
            session.getReservationService().cancelReservation(employee.getId(), toCancel);
        } catch (ServiceException ex) {
            DialogHelper.error(view, ex.getMessage());
            return;
        }

        renderReservations();
        afterReservationChange.run();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void clearForm() {
        view.getPhraseField().setText("");
        view.getActivityField().setText("");
        view.getDateField().setText("");
        view.getStartTimeField().setText("");
        view.getEndTimeField().setText("");
        ((DefaultTableModel) view.getCategoriesTable().getModel()).setRowCount(0);
    }

    private void renderReservations() {
        DefaultTableModel model = (DefaultTableModel) view.getReservationsTable().getModel();
        List<Reservation> reservations = employee.getReservations();

        if (reservations.isEmpty()) {
            model.setRowCount(0);
            view.showReservationsEmptyState(true);
            return;
        }
        view.showReservationsEmptyState(false);
        model.setRowCount(Math.max(5, reservations.size()));

        for (int row = 0; row < model.getRowCount(); row++) {
            if (row < reservations.size()) {
                Reservation reservation = reservations.get(row);
                model.setValueAt(row + 1, row, 0);
                model.setValueAt(reservation.getActivity(), row, 1);
                model.setValueAt(reservation.getDate().format(DATE_FORMAT), row, 2);
                model.setValueAt(reservation.getStartTime().format(TIME_FORMAT) + " - "
                        + reservation.getEndTime().format(TIME_FORMAT), row, 3);
                model.setValueAt(describeResources(reservation), row, 4);
                model.setValueAt("Confirmada", row, 5);
            } else {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    model.setValueAt(null, row, col);
                }
            }
        }
    }

    private String describeResources(Reservation reservation) {
        List<String> descriptions = new ArrayList<>();
        for (Resource resource : reservation.getAssignedResources()) {
            descriptions.add(resource.getDescription());
        }
        return String.join(", ", descriptions);
    }

    // ------------------------------------------------------------------
    // Imprimir (generación de reportes PDF con JasperReports)
    // ------------------------------------------------------------------

    /** Genera un PDF con las reservas actualmente mostradas en la tabla (las de {@code employee}) y lo guarda donde el usuario elija. */
    private void onPrint() {
        List<Reservation> reservations = employee.getReservations();
        if (reservations.isEmpty()) {
            DialogHelper.warn(view, "No tiene reservas registradas para imprimir.");
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("employeeName", employee.getName());

        try {
            byte[] pdf = ReportService.generatePdf("/ReportDesign/mis_reservas.jrxml", toReservationRows(reservations), parameters);
            DialogHelper.savePdfAndOpen(view, pdf, "mis_reservas.pdf", "Reservas");
        } catch (ReportException ex) {
            DialogHelper.error(view, "No fue posible generar el reporte: " + ex.getMessage());
        }
    }

    private List<ReservationReportRow> toReservationRows(List<Reservation> reservations) {
        List<ReservationReportRow> rows = new ArrayList<>();
        for (Reservation reservation : reservations) {
            String timeRange = reservation.getStartTime().format(TIME_FORMAT) + " - "
                    + reservation.getEndTime().format(TIME_FORMAT);
            rows.add(new ReservationReportRow(reservation.getActivity(), reservation.getDate().format(DATE_FORMAT),
                    timeRange, describeResources(reservation), "Confirmada"));
        }
        return rows;
    }

}
