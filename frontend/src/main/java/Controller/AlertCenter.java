package Controller;

import Dto.ReservationSummary;
import Protocol.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*
Alertas de la sesion actual, solo en memoria (nada se persiste ni se envia al
Backend). Se usa unicamente desde el EDT: quien reciba una Notification debe
entregarla con SwingUtilities.invokeLater, por eso no necesita sincronizacion.
*/
public final class AlertCenter {

    /* Una alerta mostrada en el panel. Tiene su propio id porque una reserva puede generar varias. */
    public record Alert(long id, Notification.Type type, ReservationSummary reservation, String reason) {

        /** Texto de la fila en el panel de alertas. */
        public String label() {
            return switch (type) {
                case NEW_RESERVATION -> reservation.label();
                case RESERVATION_APPROVED -> "Aprobada: " + reservation.activity();
                case RESERVATION_REJECTED -> "Rechazada: " + reservation.activity();
            };
        }
    }

    private final List<Alert> alerts = new ArrayList<>();
    private long nextId = 1;
    private Runnable onChange = () -> { };
    private Consumer<Notification> onNotification = notification -> { };

    public void add(Notification notification) {
        ReservationSummary reservation = notification.getReservation();
        if (reservation == null) {
            return;
        }
        alerts.add(0, new Alert(nextId++, notification.getType(), reservation, notification.getReason()));
        onChange.run();
        onNotification.accept(notification);
    }

    /* Descartar es una accion puramente local. */
    public void remove(long alertId) {
        if (alerts.removeIf(alert -> alert.id() == alertId)) {
            onChange.run();
        }
    }

    /** Copia de las alertas actuales, de la mas reciente a la mas antigua. */
    public List<Alert> getAlerts() {
        return List.copyOf(alerts);
    }

    /** Se invoca cada vez que cambia la lista (llega o se descarta una alerta): repinta el panel. */
    public void setOnChange(Runnable onChange) {
        this.onChange = (onChange != null) ? onChange : () -> { };
    }

    /** Se invoca al llegar un aviso, para que las demas pantallas se refresquen con los datos nuevos. */
    public void setOnNotification(Consumer<Notification> onNotification) {
        this.onNotification = (onNotification != null) ? onNotification : notification -> { };
    }

    /** Al iniciar o cerrar sesion: sin alertas y sin pantallas escuchando. */
    public void reset() {
        alerts.clear();
        onChange = () -> { };
        onNotification = notification -> { };
    }
}