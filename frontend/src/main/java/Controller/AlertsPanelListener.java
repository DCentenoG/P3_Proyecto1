package Controller;

import Dto.ReservationSummary;
import View.AlertsPanel;
import View.ReservationDetailsDialog;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        panel.showAlerts(center.getAlerts());
    }

    private void onClick(MouseEvent e) {
        JTable table = panel.getTable();
        int row = table.rowAtPoint(e.getPoint());
        int column = table.columnAtPoint(e.getPoint());
        List<ReservationSummary> alerts = center.getAlerts();
        if (row < 0 || row >= alerts.size()) {
            return;
        }
        ReservationSummary alert = alerts.get(row);

        if (column == AlertsPanel.VIEW_COLUMN) {
            new ReservationDetailsDialog(SwingUtilities.getWindowAncestor(panel), alert).setVisible(true);
        } else if (column == AlertsPanel.DISMISS_COLUMN
                && DialogHelper.confirm(panel, "¿Desea descartar esta alerta?")) {
            // Por id y no por posicion: mientras el dialogo estaba abierto pudieron llegar alertas nuevas.
            center.remove(alert.reservationId());
        }
    }
}