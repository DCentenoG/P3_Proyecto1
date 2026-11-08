package Controller;

import Dto.ReservationSummary;
import Protocol.Notification;
import View.AlertsPanel;
import View.RejectionReasonDialog;
import View.ReservationDetailsDialog;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/*
Conecta el AlertsPanel con el AlertCenter: pinta las alertas cuando cambian, abre el
detalle con el icono "Ver" y descarta con el icono "Desc." previa confirmacion.
Descartar NO llama al Backend: solo quita la alerta de la lista local.
*/
public final class AlertsPanelListener {

    private final AlertsPanel panel;
    private final AlertCenter center;

    public AlertsPanelListener(AlertsPanel panel, SessionContext session) {
        this.panel = panel;
        this.center = session.getAlertCenter();

        panel.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick(e);
            }
        });
        center.setOnChange(this::render);
        render();
    }

    private void render() {
        List<String> labels = new ArrayList<>();
        for (AlertCenter.Alert alert : center.getAlerts()) {
            labels.add(alert.label());
        }
        panel.showAlerts(labels);
    }

    private void onClick(MouseEvent e) {
        JTable table = panel.getTable();
        int row = table.rowAtPoint(e.getPoint());
        int column = table.columnAtPoint(e.getPoint());
        List<AlertCenter.Alert> alerts = center.getAlerts();
        if (row < 0 || row >= alerts.size()) {
            return;
        }
        AlertCenter.Alert alert = alerts.get(row);

        if (column == AlertsPanel.VIEW_COLUMN) {
            showDetail(alert);
        } else if (column == AlertsPanel.DISMISS_COLUMN
                && DialogHelper.confirm(panel, "¿Desea descartar esta alerta?")) {
            // Por id de alerta y no por posicion: mientras el dialogo estaba abierto pudieron llegar alertas nuevas.
            center.remove(alert.id());
        }
    }

    /* Una alerta de rechazo muestra el motivo; las demas, el detalle de la reserva. */
    private void showDetail(AlertCenter.Alert alert) {
        Window owner = SwingUtilities.getWindowAncestor(panel);
        ReservationSummary reservation = alert.reservation();
        if (alert.type() == Notification.Type.RESERVATION_REJECTED) {
            String summary = reservation.activity() + " · "
                    + reservation.date().format(ReservationFormSupport.DATE_FORMAT) + " · "
                    + reservation.startTime().format(ReservationFormSupport.TIME_FORMAT) + " - "
                    + reservation.endTime().format(ReservationFormSupport.TIME_FORMAT);
            new RejectionReasonDialog(owner, summary, alert.reason()).setVisible(true);
        } else {
            new ReservationDetailsDialog(owner, reservation).setVisible(true);
        }
    }
}