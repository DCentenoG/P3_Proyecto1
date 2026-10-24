package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Report.ReportException;
import Report.ReportService;
import Report.RequestReportRow;
import Service.ReservationService;
import Service.ReservationService.PendingRequest;
import Service.ServiceException;
import View.RejectRequestForm;
import View.RequestsView;
import View.ReviewRequestForm;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja los eventos de la pestaña "Solicitudes" del administrador
 * ({@link RequestsView}) y de sus formularios de revisión
 * ({@link ReviewRequestForm}) y de rechazo ({@link RejectRequestForm}).
 * <p>
 * El listado muestra solo las reservas "En revisión" de todos los
 * funcionarios (ver {@link ReservationService#listPendingRequests()}).
 * "Revisar" (o doble clic sobre la fila) abre la solicitud seleccionada:
 * <ul>
 *   <li><b>Aprobar</b>: el administrador elige, por cada categoría
 *       requerida, uno de los recursos libres en ese horario; la reserva
 *       queda "Aceptada" y empieza a mostrarse en todo el sistema.</li>
 *   <li><b>Rechazar</b>: se cierra la revisión y se abre el formulario
 *       del motivo, que es obligatorio; la reserva queda "Rechazada".</li>
 * </ul>
 * En ambos casos la solicitud deja de aparecer en el listado, y se avisa
 * al resto de pestañas (Calendarización, Actividades) para que se
 * refresquen con los datos ya actualizados.
 */
public final class RequestsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = ReservationFormSupport.DATE_FORMAT;
    private static final DateTimeFormatter TIME_FORMAT = ReservationFormSupport.TIME_FORMAT;

    /** Filas mínimas de la tabla (las demás quedan en blanco), igual que los demás listados. */
    private static final int MIN_ROWS = 7;
    private static final String NO_RESOURCES_OPTION = "(Sin recursos disponibles)";

    private final RequestsView view;
    private final SessionContext session;

    /** Solicitudes que se muestran en la tabla, en el mismo orden que sus filas. */
    private List<PendingRequest> currentRequests = new ArrayList<>();

    /** Se invoca después de aprobar o rechazar, para refrescar las demás pestañas. */
    private Runnable afterReview = () -> { };

    public RequestsViewListener(RequestsView view, SessionContext session) {
        this.view = view;
        this.session = session;
        wire();
        refresh();
    }

    private void wire() {
        view.getReviewButton().addActionListener(e -> onReview());
        view.getPrintButton().addActionListener(e -> onPrint());
        view.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    onReview();
                }
            }
        });
        // "Revisar" opera sobre la fila seleccionada: un clic en él no debe deseleccionarla.
        TableInteractionUtil.deselectOnClickOutside(view.getTable(), view.getReviewButton());
    }

    public void setAfterReview(Runnable callback) {
        this.afterReview = (callback != null) ? callback : () -> { };
    }

    /** Vuelve a cargar el listado con las solicitudes "En revisión" actuales. */
    public void refresh() {
        currentRequests = session.getReservationService().listPendingRequests();

        DefaultTableModel model = (DefaultTableModel) view.getTable().getModel();
        if (currentRequests.isEmpty()) {
            model.setRowCount(0);
            view.showEmptyState(true);
            return;
        }
        view.showEmptyState(false);
        model.setRowCount(Math.max(MIN_ROWS, currentRequests.size()));

        for (int row = 0; row < model.getRowCount(); row++) {
            if (row < currentRequests.size()) {
                PendingRequest request = currentRequests.get(row);
                Reservation reservation = request.reservation();
                model.setValueAt(request.employee().getId(), row, 0);
                model.setValueAt(request.employee().getName(), row, 1);
                model.setValueAt(reservation.getActivity(), row, 2);
                model.setValueAt(reservation.getDate().format(DATE_FORMAT), row, 3);
                model.setValueAt(timeRange(reservation), row, 4);
            } else {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    model.setValueAt(null, row, col);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Revisar solicitud
    // ------------------------------------------------------------------

    private void onReview() {
        int row = view.getTable().getSelectedRow();
        if (row < 0 || row >= currentRequests.size()) {
            DialogHelper.warn(view, "Debe seleccionar una solicitud de la tabla para poder revisarla.");
            return;
        }
        PendingRequest request = currentRequests.get(row);

        Map<ResourceCategory, List<Resource>> available;
        try {
            available = session.getReservationService().availableResourcesFor(request.reservation());
        } catch (ServiceException ex) {
            DialogHelper.error(view, ex.getMessage());
            return;
        }

        ReviewRequestForm form = new ReviewRequestForm(windowOf());
        form.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loadRequest(form, request, available);
        form.getApproveButton().addActionListener(e -> onApprove(form, request, available));
        form.getRejectButton().addActionListener(e -> onReject(form, request));
        form.setVisible(true);
    }

    /** Carga los datos de la solicitud (solo lectura) y, por categoría, los recursos libres para elegir. */
    private void loadRequest(ReviewRequestForm form, PendingRequest request,
                             Map<ResourceCategory, List<Resource>> available) {
        Reservation reservation = request.reservation();
        form.setEmployeeId(String.valueOf(request.employee().getId()));
        form.setEmployeeName(request.employee().getName());
        form.setActivity(reservation.getActivity());
        form.setDate(reservation.getDate().format(DATE_FORMAT));
        form.setStartTime(reservation.getStartTime().format(TIME_FORMAT));
        form.setEndTime(reservation.getEndTime().format(TIME_FORMAT));

        List<String> categoryNames = new ArrayList<>();
        for (ResourceCategory category : available.keySet()) {
            categoryNames.add(category.getDescription());
        }
        form.setRequiredCategories(categoryNames);

        for (Map.Entry<ResourceCategory, List<Resource>> entry : available.entrySet()) {
            JComboBox<String> combo = form.getResourceCombo(entry.getKey().getDescription());
            if (entry.getValue().isEmpty()) {
                combo.addItem(NO_RESOURCES_OPTION);
                combo.setEnabled(false);
                continue;
            }
            for (Resource resource : entry.getValue()) {
                combo.addItem(resource.getDescription());
            }
        }
    }

    private void onApprove(ReviewRequestForm form, PendingRequest request,
                           Map<ResourceCategory, List<Resource>> available) {
        List<Resource> chosen = new ArrayList<>();
        List<String> withoutResources = new ArrayList<>();
        for (Map.Entry<ResourceCategory, List<Resource>> entry : available.entrySet()) {
            JComboBox<String> combo = form.getResourceCombo(entry.getKey().getDescription());
            int index = combo.getSelectedIndex();
            if (entry.getValue().isEmpty() || index < 0) {
                withoutResources.add(entry.getKey().getDescription());
            } else {
                // Las opciones del combo están en el mismo orden que la lista de recursos libres.
                chosen.add(entry.getValue().get(index));
            }
        }
        if (!withoutResources.isEmpty()) {
            DialogHelper.warn(form, "No hay recursos disponibles en ese horario para: "
                    + String.join(", ", withoutResources)
                    + ". La solicitud no se puede aprobar; puede rechazarla indicando el motivo.");
            return;
        }
        if (!DialogHelper.confirm(form, "¿Desea aprobar esta solicitud con los recursos elegidos?")) {
            return;
        }

        try {
            session.getReservationService().approveRequest(request.employee().getId(), request.reservation(), chosen);
        } catch (ServiceException ex) {
            DialogHelper.error(form, ex.getMessage());
            return;
        }

        DialogHelper.info(form, "Solicitudes", "La solicitud fue aprobada. La reserva quedó \"Aceptada\".");
        form.dispose();
        afterReviewDone();
    }

    // ------------------------------------------------------------------
    // Rechazar solicitud (con motivo obligatorio)
    // ------------------------------------------------------------------

    /** Cierra la revisión y abre el formulario del motivo del rechazo. */
    private void onReject(ReviewRequestForm reviewForm, PendingRequest request) {
        reviewForm.dispose();

        RejectRequestForm form = new RejectRequestForm(windowOf());
        form.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        form.setRequestSummary(summaryOf(request));
        form.getRejectButton().addActionListener(e -> onConfirmReject(form, request));
        // Cancelar: no se rechaza; la solicitud sigue pendiente en el listado.
        form.getCancelButton().addActionListener(e -> form.dispose());
        form.setVisible(true);
    }

    private void onConfirmReject(RejectRequestForm form, PendingRequest request) {
        String reason = form.getReasonField().getText().trim();
        if (reason.isEmpty()) {
            DialogHelper.warn(form, "Debe escribir el motivo del rechazo para poder rechazar la solicitud.");
            form.getReasonField().requestFocusInWindow();
            return;
        }
        if (!DialogHelper.confirm(form, "¿Desea rechazar esta solicitud?")) {
            return;
        }

        try {
            session.getReservationService().rejectRequest(request.employee().getId(), request.reservation(), reason);
        } catch (ServiceException ex) {
            DialogHelper.error(form, ex.getMessage());
            return;
        }

        DialogHelper.info(form, "Solicitudes", "La solicitud fue rechazada. El funcionario podrá ver el motivo.");
        form.dispose();
        afterReviewDone();
    }

    private void afterReviewDone() {
        refresh();
        afterReview.run();
    }

    // ------------------------------------------------------------------
    // Imprimir (reporte PDF con JasperReports)
    // ------------------------------------------------------------------

    private void onPrint() {
        if (currentRequests.isEmpty()) {
            DialogHelper.warn(view, "No hay solicitudes pendientes para imprimir.");
            return;
        }
        List<RequestReportRow> rows = new ArrayList<>();
        for (PendingRequest request : currentRequests) {
            Reservation reservation = request.reservation();
            rows.add(new RequestReportRow(String.valueOf(request.employee().getId()), request.employee().getName(),
                    reservation.getActivity(), reservation.getDate().format(DATE_FORMAT), timeRange(reservation),
                    String.join(", ", ReservationFormSupport.categoriesOf(reservation))));
        }
        try {
            byte[] pdf = ReportService.generatePdf("/ReportDesign/solicitudes.jrxml", rows, new LinkedHashMap<>());
            DialogHelper.savePdfAndOpen(view, pdf, "solicitudes.pdf", "Solicitudes");
        } catch (ReportException ex) {
            DialogHelper.error(view, "No fue posible generar el reporte: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String timeRange(Reservation reservation) {
        return reservation.getStartTime().format(TIME_FORMAT) + " - " + reservation.getEndTime().format(TIME_FORMAT);
    }

    private static String summaryOf(PendingRequest request) {
        Employee employee = request.employee();
        Reservation reservation = request.reservation();
        return employee.getName() + " · " + reservation.getActivity() + " · "
                + reservation.getDate().format(DATE_FORMAT) + " · " + timeRange(reservation);
    }

    private Window windowOf() {
        return SwingUtilities.getWindowAncestor(view);
    }
}
