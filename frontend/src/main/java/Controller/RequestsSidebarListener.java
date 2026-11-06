package Controller;

import Service.ReservationService.PendingRequest;
import Service.ServiceException;
import View.RequestsSidebar;
import View.RequestsSidebar.RequestRow;

import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja los eventos del sidebar de solicitudes del administrador
 * ({@link RequestsSidebar}).
 * <p>
 * Muestra las solicitudes "En revisión" de todos los funcionarios en el
 * orden de sus notificaciones (ver
 * {@link Service.ReservationService#listPendingRequests()}): primero las
 * nuevas sin ver (la más reciente arriba) y luego las ya vistas (la marcada
 * más recientemente primero). Ambos botones operan sobre la solicitud
 * seleccionada y avisan si no hay ninguna:
 * <ul>
 *   <li><b>Marcar como vista</b>: solo cambia la notificación (la reserva
 *       sigue "En revisión"); la solicitud baja al grupo de las vistas y deja
 *       de contarse como nueva. Si ya estaba vista, se avisa.</li>
 *   <li><b>Revisar</b> (o doble clic sobre la fila): abre el mismo flujo de
 *       revisión de siempre ({@link RequestReviewFlow}) para aprobarla o
 *       rechazarla. Si la solicitud era nueva, primero se marca como vista
 *       automáticamente.</li>
 * </ul>
 * Después de cualquiera de las dos se avisa al resto de la ventana (ver
 * {@link #setAfterChange}) para que se refresque con los datos ya
 * actualizados; y cualquier otra pantalla que cree o cambie solicitudes
 * puede refrescar este sidebar con {@link #refresh()}.
 */
public final class RequestsSidebarListener {

    private final RequestsSidebar sidebar;
    private final SessionContext session;

    /** Solicitudes que se muestran en la tabla, en el mismo orden que sus filas. */
    private List<PendingRequest> currentRequests = new ArrayList<>();

    /** Se invoca después de marcar como vista, aprobar o rechazar una solicitud. */
    private Runnable afterChange = () -> { };

    public RequestsSidebarListener(RequestsSidebar sidebar, SessionContext session) {
        this.sidebar = sidebar;
        this.session = session;
        wire();
        refresh();
    }

    private void wire() {
        sidebar.getMarkAsSeenButton().addActionListener(e -> onMarkAsSeen());
        sidebar.getReviewButton().addActionListener(e -> onReview());
        sidebar.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    onReview();
                }
            }
        });
        // Los dos botones operan sobre la fila seleccionada: un clic en ellos no debe deseleccionarla.
        TableInteractionUtil.deselectOnClickOutside(sidebar.getTable(),
                sidebar.getMarkAsSeenButton(), sidebar.getReviewButton());
    }

    public void setAfterChange(Runnable callback) {
        this.afterChange = (callback != null) ? callback : () -> { };
    }

    /** Vuelve a cargar las solicitudes pendientes, en el orden de sus notificaciones. */
    public void refresh() {
        currentRequests = session.getReservationService().listPendingRequests();

        List<RequestRow> rows = new ArrayList<>();
        for (PendingRequest request : currentRequests) {
            rows.add(new RequestRow(request.employee().getName(), request.reservation().getActivity(),
                    !request.reservation().getNotification().isSeen()));
        }
        sidebar.showRequests(rows);
    }

    // ------------------------------------------------------------------
    // Marcar como vista
    // ------------------------------------------------------------------

    private void onMarkAsSeen() {
        PendingRequest request = selectedRequest();
        if (request == null) {
            DialogHelper.warn(sidebar, "Debe seleccionar una solicitud de la lista para poder marcarla como vista.");
            return;
        }
        if (request.reservation().getNotification().isSeen()) {
            DialogHelper.warn(sidebar, "La solicitud ya fue marcada como vista.");
            return;
        }

        try {
            session.getReservationService().markRequestAsSeen(request.employee().getId(), request.reservation().getId());
        } catch (ServiceException ex) {
            DialogHelper.error(sidebar, ex.getMessage());
            return;
        }

        afterChangeDone();
    }

    // ------------------------------------------------------------------
    // Revisar (aprobar / rechazar)
    // ------------------------------------------------------------------

    private void onReview() {
        PendingRequest request = selectedRequest();
        if (request == null) {
            DialogHelper.warn(sidebar, "Debe seleccionar una solicitud de la lista para poder revisarla.");
            return;
        }

        // Revisar una solicitud nueva implica haberla visto: se marca como vista
        // automáticamente (sin tener que pulsar "Marcar como vista"), así baja al
        // grupo de las vistas y deja de contarse como nueva, aunque luego se cierre
        // la revisión sin aprobarla ni rechazarla.
        if (!request.reservation().getNotification().isSeen()) {
            try {
                session.getReservationService().markRequestAsSeen(request.employee().getId(), request.reservation().getId());
            } catch (ServiceException ex) {
                DialogHelper.error(sidebar, ex.getMessage());
                return;
            }
            refresh();
        }

        RequestReviewFlow.open(session, sidebar, SwingUtilities.getWindowAncestor(sidebar), request,
                this::afterChangeDone);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** La solicitud seleccionada en la tabla, o {@code null} si no hay ninguna (o es una fila en blanco). */
    private PendingRequest selectedRequest() {
        int index = sidebar.getSelectedRequestIndex();
        return (index >= 0 && index < currentRequests.size()) ? currentRequests.get(index) : null;
    }

    private void afterChangeDone() {
        refresh();
        afterChange.run();
    }
}
