package Model;

import java.io.Serializable;
import Model.exceptions.InvalidReservationOperationException;

import java.time.LocalDateTime;

/*
Notificacion de una solicitud de reserva para el administrador (sidebar de
solicitudes). Es un dato APARTE del estado de la reserva (Model.State):
marcar una solicitud como vista solo cambia esta notificacion, la reserva
sigue "En revision" hasta que el administrador la apruebe o la rechace.

Guarda dos momentos, que definen el orden en que se muestran las solicitudes:
- receivedAt: cuando la solicitud entro (al crearse la reserva, o al editarse
  y volver a "En revision"). Entre las no vistas, la mas reciente va arriba.
- seenAt: cuando el administrador la marco como vista (null = no vista).
  Entre las vistas, la marcada mas recientemente va primero, justo debajo de
  las no vistas.

receivedAt puede ser null en reservas de archivos anteriores a este flujo
(no se sabe cuando entraron): se tratan como las mas antiguas.
*/
public class RequestNotification implements Serializable {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;


    private LocalDateTime receivedAt;
    private LocalDateTime seenAt;

    public RequestNotification() {
        this(null, null);
    }

    public RequestNotification(LocalDateTime receivedAt, LocalDateTime seenAt) {
        this.receivedAt = receivedAt;
        this.seenAt = seenAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getSeenAt() {
        return seenAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setSeenAt(LocalDateTime seenAt) {
        this.seenAt = seenAt;
    }

    public boolean isSeen() {
        return seenAt != null;
    }

    /* La solicitud acaba de entrar (nueva o editada): queda como no vista, recibida en "at". */
    public void receive(LocalDateTime at) {
        receivedAt = at;
        seenAt = null;
    }

    /* El administrador la marca como vista en "at". No se puede marcar dos veces. */
    public void markSeen(LocalDateTime at) throws InvalidReservationOperationException {
        if (isSeen()) {
            throw new InvalidReservationOperationException("La solicitud ya fue marcada como vista.");
        }
        seenAt = at;
    }
}
