package Controller;

import Model.Reservation;
import Report.ReportException;
import Report.ReportService;
import Report.RequestReportRow;
import Service.ReservationService;
import Service.ReservationService.PendingRequest;
import View.RequestsView;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Maneja los eventos de la pestaña "Solicitudes" del administrador
 * ({@link RequestsView}).
 * <p>
 * TEMPORAL: esta pestaña se reemplaza por el sidebar de solicitudes
 * ({@link RequestsSidebarListener}) y se elimina al terminar esa migración.
 * Mientras tanto, "Revisar" (o doble clic sobre la fila) abre exactamente
 * el mismo flujo de revisión que el sidebar ({@link RequestReviewFlow}).
 * <p>
 * El listado muestra solo las reservas "En revisión" de todos los
 * funcionarios (ver {@link ReservationService#listPendingRequests()}).
 * Tras aprobar o rechazar, la solicitud deja de aparecer en el listado, y
 * se avisa al resto de pestañas y al sidebar para que se refresquen.
 */
public final class RequestsViewListener {

    private static final DateTimeFormatter DATE_FORMAT = ReservationFormSupport.DATE_FORMAT;
    private static final DateTimeFormatter TIME_FORMAT = ReservationFormSupport.TIME_FORMAT;

    /** Filas mínimas de la tabla (las demás quedan en blanco), igual que los demás listados. */
    private static final int MIN_ROWS = 7;

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
        // Mismo flujo de revisión (aprobar / rechazar con motivo) que el sidebar de solicitudes.
        RequestReviewFlow.open(session, view, windowOf(), currentRequests.get(row), this::afterReviewDone);
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

    private Window windowOf() {
        return SwingUtilities.getWindowAncestor(view);
    }
}
