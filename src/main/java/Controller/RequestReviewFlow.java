package Controller;

import Model.Employee;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Service.ReservationService.PendingRequest;
import Service.ServiceException;
import View.RejectRequestForm;
import View.ReviewRequestForm;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import java.awt.Component;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flujo de revisión de una solicitud de reserva por el administrador,
 * que se abre desde el sidebar de solicitudes (ver
 * {@link RequestsSidebarListener}):
 * <ul>
 *   <li>Abre {@link ReviewRequestForm} con los datos de la solicitud (solo
 *       lectura) y, por cada categoría requerida, los recursos libres en ese
 *       horario para elegir uno.</li>
 *   <li><b>Aprobar</b>: pide confirmación y deja la reserva "Aceptada" con
 *       los recursos elegidos.</li>
 *   <li><b>Rechazar</b>: cierra la revisión y abre {@link RejectRequestForm};
 *       el motivo es obligatorio y la reserva queda "Rechazada". Cancelar
 *       ahí deja la solicitud pendiente, sin cambios.</li>
 * </ul>
 * Tras aprobar o rechazar se invoca {@code afterReview}, para que quien
 * abrió el flujo refresque su listado y el resto de la ventana.
 */
final class RequestReviewFlow {

    private static final DateTimeFormatter DATE_FORMAT = ReservationFormSupport.DATE_FORMAT;
    private static final DateTimeFormatter TIME_FORMAT = ReservationFormSupport.TIME_FORMAT;
    private static final String NO_RESOURCES_OPTION = "(Sin recursos disponibles)";

    private final SessionContext session;
    private final Window owner;
    private final PendingRequest request;
    private final Runnable afterReview;

    private RequestReviewFlow(SessionContext session, Window owner, PendingRequest request, Runnable afterReview) {
        this.session = session;
        this.owner = owner;
        this.request = request;
        this.afterReview = (afterReview != null) ? afterReview : () -> { };
    }

    /** Abre la revisión de {@code request}; {@code parent} se usa para los avisos si no se puede abrir. */
    static void open(SessionContext session, Component parent, Window owner, PendingRequest request,
                     Runnable afterReview) {
        new RequestReviewFlow(session, owner, request, afterReview).showReviewForm(parent);
    }

    // ------------------------------------------------------------------
    // Revisar solicitud
    // ------------------------------------------------------------------

    private void showReviewForm(Component parent) {
        Map<ResourceCategory, List<Resource>> available;
        try {
            available = session.getReservationService().availableResourcesFor(request.reservation());
        } catch (ServiceException ex) {
            DialogHelper.error(parent, ex.getMessage());
            return;
        }

        ReviewRequestForm form = new ReviewRequestForm(owner);
        form.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loadRequest(form, available);
        form.getApproveButton().addActionListener(e -> onApprove(form, available));
        form.getRejectButton().addActionListener(e -> onReject(form));
        form.setVisible(true);
    }

    /** Carga los datos de la solicitud (solo lectura) y, por categoría, los recursos libres para elegir. */
    private void loadRequest(ReviewRequestForm form, Map<ResourceCategory, List<Resource>> available) {
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

    private void onApprove(ReviewRequestForm form, Map<ResourceCategory, List<Resource>> available) {
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
        afterReview.run();
    }

    // ------------------------------------------------------------------
    // Rechazar solicitud (con motivo obligatorio)
    // ------------------------------------------------------------------

    /** Cierra la revisión y abre el formulario del motivo del rechazo. */
    private void onReject(ReviewRequestForm reviewForm) {
        reviewForm.dispose();

        RejectRequestForm form = new RejectRequestForm(owner);
        form.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        form.setRequestSummary(summaryOf(request));
        form.getRejectButton().addActionListener(e -> onConfirmReject(form));
        // Cancelar: no se rechaza; la solicitud sigue pendiente.
        form.getCancelButton().addActionListener(e -> form.dispose());
        form.setVisible(true);
    }

    private void onConfirmReject(RejectRequestForm form) {
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
        afterReview.run();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String summaryOf(PendingRequest request) {
        Employee employee = request.employee();
        Reservation reservation = request.reservation();
        return employee.getName() + " · " + reservation.getActivity() + " · "
                + reservation.getDate().format(DATE_FORMAT) + " · "
                + reservation.getStartTime().format(TIME_FORMAT) + " - " + reservation.getEndTime().format(TIME_FORMAT);
    }
}
