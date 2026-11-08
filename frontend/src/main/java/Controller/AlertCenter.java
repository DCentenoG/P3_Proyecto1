package Controller;

import Dto.ReservationSummary;
import Protocol.Notification;

import java.util.ArrayList;
import java.util.List;

/*
Alertas de la sesion actual, solo en memoria (nada se persiste ni se envia al
Backend). Se usa unicamente desde el EDT: quien reciba una Notification debe
entregarla con SwingUtilities.invokeLater, por eso no necesita sincronizacion.
*/
public final class AlertCenter {

    private final List<ReservationSummary> alerts = new ArrayList<>();
    private Runnable onChange = () -> { };

    public void add(Notification notification) {
        ReservationSummary reservation = notification.getReservation();
        if (reservation == null) {
            return;
        }
        alerts.add(0, reservation);   // la mas reciente arriba
        onChange.run();
    }

    /* Descartar es una accion puramente local: se identifica por el id de la reserva. */
    public void remove(int reservationId) {
        if (alerts.removeIf(alert -> alert.reservationId() == reservationId)) {
            onChange.run();
        }
    }

    /** Copia de las alertas actuales, de la mas reciente a la mas antigua. */
    public List<ReservationSummary> getAlerts() {
        return List.copyOf(alerts);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = (onChange != null) ? onChange : () -> { };
    }

    /** Al iniciar o cerrar sesion: sin alertas y sin pantalla escuchando. */
    public void reset() {
        alerts.clear();
        onChange = () -> { };
    }
}