package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.State;
import Report.ReportException;
import Report.ReportService;
import Report.ReservationReportRow;
import Service.AIReservationExtractionService;
import Service.ServiceException;
import View.EditReservationForm;
import View.RejectionReasonDialog;
import View.ReservationsView;
import View.StateCellRenderer;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja los eventos de {@link ReservationsView}: el botón de ayuda "?"
 * (explica el llenado por IA), el llenado automático por IA a partir de
 * la "Frase" (ver {@link AIReservationExtractionService}), y las
 * acciones de Guardar, Editar, Cancelar y Limpiar de la sección "Nueva
 * reserva". Los selectores, la minitabla de categorías y la validación
 * del formulario se comparten con "Editar reserva" (ver
 * {@link ReservationFormSupport}).
 * <p>
 * Reglas pedidas: no se puede guardar una reserva con campos vacíos
 * (excepto "Frase"); no se puede editar ni cancelar una reserva sin
 * haberla seleccionado en la tabla; y Guardar y Cancelar piden
 * confirmación antes de ejecutarse.
 */
public final class ReservationsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = ReservationFormSupport.DATE_FORMAT;
    private static final DateTimeFormatter TIME_FORMAT = ReservationFormSupport.TIME_FORMAT;

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
        view.getEditButton().addActionListener(e -> onEditReservation());
        view.getCancelButton().addActionListener(e -> onCancelReservation());
        view.getClearButton().addActionListener(e -> clearForm());
        view.getPrintButton().addActionListener(e -> onPrint());
        view.getAddCategoryButton().addActionListener(e -> onAddCategory());
        view.getRemoveCategoryButton().addActionListener(e -> onRemoveCategory());
        view.getDateDropdownButton().addActionListener(e ->
                ReservationFormSupport.pickDate(view, view.getDateField()));
        view.getStartTimeButton().addActionListener(e ->
                ReservationFormSupport.pickStartTime(view, view.getStartTimeField(), view.getEndTimeField()));
        view.getEndTimeButton().addActionListener(e ->
                ReservationFormSupport.pickEndTime(view, view.getStartTimeField(), view.getEndTimeField()));
        // "Editar reserva" y "Cancelar reserva" operan sobre la fila seleccionada:
        // un clic en ellos no debe deseleccionarla antes de ejecutarse.
        TableInteractionUtil.deselectOnClickOutside(view.getReservationsTable(),
                view.getEditButton(), view.getCancelButton());
        wireRejectedStateLink();
    }

    /**
     * El estado "Rechazada" de "Mis reservas" funciona como un hipervínculo
     * (ver {@link StateCellRenderer}): al pasar el mouse por encima el
     * cursor cambia a una "mano", y al pulsarlo se abre
     * {@link RejectionReasonDialog} con el motivo que indicó el administrador.
     */
    private void wireRejectedStateLink() {
        JTable table = view.getReservationsTable();
        MouseAdapter link = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                table.setCursor(rejectedReservationAt(e) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                table.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Reservation rejected = rejectedReservationAt(e);
                if (rejected != null) {
                    showRejectionReason(rejected);
                }
            }
        };
        table.addMouseListener(link);
        table.addMouseMotionListener(link);
    }

    /** La reserva "Rechazada" cuya celda de Estado está bajo el mouse, o {@code null} si no hay ninguna. */
    private Reservation rejectedReservationAt(MouseEvent e) {
        JTable table = view.getReservationsTable();
        int row = table.rowAtPoint(e.getPoint());
        int column = table.columnAtPoint(e.getPoint());
        if (row < 0 || column != ReservationsView.STATE_COLUMN || row >= employee.getReservations().size()) {
            return null;
        }
        Object value = table.getValueAt(row, column);
        return StateCellRenderer.isClickable(value) ? employee.getReservations().get(row) : null;
    }

    private void showRejectionReason(Reservation reservation) {
        String summary = reservation.getActivity() + " · " + reservation.getDate().format(DATE_FORMAT) + " · "
                + reservation.getStartTime().format(TIME_FORMAT) + " - " + reservation.getEndTime().format(TIME_FORMAT);
        new RejectionReasonDialog(SwingUtilities.getWindowAncestor(view), summary, reservation.getRejectionReason())
                .setVisible(true);
    }

    public void setAfterReservationChange(Runnable callback) {
        this.afterReservationChange = (callback != null) ? callback : () -> { };
    }

    // ------------------------------------------------------------------
    // Categorías requeridas (selectores de fecha/hora: ver wire())
    // ------------------------------------------------------------------

    private void loadCategoryOptions() {
        ReservationFormSupport.loadCategoryOptions(view.getCategoryCombo(), session.getCategories().getCategories());
    }

    private void onAddCategory() {
        ReservationFormSupport.addSelectedCategory(view.getCategoryCombo(), view.getCategoriesTable());
    }

    private void onRemoveCategory() {
        ReservationFormSupport.removeSelectedCategory(view, view.getCategoriesTable());
    }

    // ------------------------------------------------------------------
    // Llenado automático por IA (a partir de la "Frase")
    // ------------------------------------------------------------------

    /** El llenado en sí (hilo aparte, avisos, normalización de categorías) está en {@link ReservationFormSupport}. */
    private void onAiFill() {
        ReservationFormSupport.fillFromPhrase(view, aiExtractionService, view.getPhraseField(), view.getAiButton(),
                new ReservationFormSupport.FormFields(view.getActivityField(), view.getDateField(),
                        view.getStartTimeField(), view.getEndTimeField(), view.getCategoriesTable()),
                session.getCategories().getCategories());
    }

    // ------------------------------------------------------------------
    // Guardar reserva
    // ------------------------------------------------------------------

    private void onSave() {
        ReservationFormSupport.ReservationInput input = ReservationFormSupport.readAndValidate(view,
                view.getActivityField().getText(), view.getDateField().getText(),
                view.getStartTimeField().getText(), view.getEndTimeField().getText(),
                view.getSelectedCategories());
        if (input == null) {
            return;
        }

        if (!DialogHelper.confirm(view, "¿Desea guardar esta reserva?")) {
            return;
        }

        // Crear la solicitud (validar disponibilidad, dejarla "En revisión" y
        // guardar el XML) vive en Service.ReservationService (ver
        // Model.Employee#tryBook): el Controller solo junta los datos del
        // formulario y traduce el resultado a diálogos, como corresponde en MVC.
        try {
            session.getReservationService().createReservation(employee.getId(), input.activity(), input.date(),
                    input.startTime(), input.endTime(), input.categories());
        } catch (ServiceException ex) {
            DialogHelper.error(view, ex.getMessage());
            return;
        }

        DialogHelper.info(view, "Reservas", "La reserva se envió a revisión. Quedará \"En revisión\" "
                + "hasta que un administrador la apruebe y le asigne los recursos.");
        clearForm();
        renderReservations();
        afterReservationChange.run();
    }

    // ------------------------------------------------------------------
    // Editar reserva
    // ------------------------------------------------------------------

    /**
     * Abre {@link EditReservationForm} con los datos de la reserva
     * seleccionada en "Mis reservas". Igual que "Cancelar reserva", no se
     * puede editar sin haber seleccionado antes una reserva de la tabla.
     */
    private void onEditReservation() {
        int row = view.getReservationsTable().getSelectedRow();
        if (row < 0 || row >= employee.getReservations().size()) {
            DialogHelper.warn(view, "Debe seleccionar una reserva de la tabla para poder editarla.");
            return;
        }

        Reservation toEdit = employee.getReservations().get(row);
        EditReservationForm form = new EditReservationForm(SwingUtilities.getWindowAncestor(view));
        new EditReservationFormListener(form, session, toEdit, () -> {
            renderReservations();
            afterReservationChange.run();
        });
        form.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Cancelar reserva
    // ------------------------------------------------------------------

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
                // Se pasa el State (no un texto): la columna lo muestra con el color
                // de su estado y "Rechazada" como hipervínculo (ver StateCellRenderer).
                model.setValueAt(reservation.getState(), row, ReservationsView.STATE_COLUMN);
            } else {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    model.setValueAt(null, row, col);
                }
            }
        }
    }

    /**
     * Recursos de la reserva. Solo una reserva Aceptada los tiene (los asigna
     * el administrador al aprobarla); mientras está En revisión se indica que
     * están por asignar.
     */
    private String describeResources(Reservation reservation) {
        if (reservation.getState() == State.IN_REVIEW) {
            return "Por asignar";
        }
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
                    timeRange, describeResources(reservation), reservation.getState().getLabel()));
        }
        return rows;
    }

}
