package Protocol;

import Dto.ReservationSummary;

import java.io.Serializable;

/**
 * Un aviso que el Backend empuja al Frontend sin que este lo haya pedido.
 * <p>
 * Una {@link Request} siempre nace en el Frontend y el Backend contesta con una
 * {@link Response}; una Notification nace en el Backend, cuando algo cambio por
 * accion de OTRO usuario, y llega al cliente en cualquier momento.
 * <p>
 * El Frontend la lee en un hilo propio que NO es el Event Dispatch Thread de
 * Swing: todo lo que toque la interfaz debe ir dentro de
 * {@code SwingUtilities.invokeLater}.
 *
 * <h2>Tipos</h2>
 * <ul>
 *   <li>{@link Type#NEW_RESERVATION}: otro usuario registro una reserva. Se envia a
 *       todos los demas clientes.</li>
 *   <li>{@link Type#RESERVATION_APPROVED} y {@link Type#RESERVATION_REJECTED}: el
 *       administrador reviso una solicitud. Se envia solo al funcionario duenio de
 *       la reserva.</li>
 * </ul>
 */
public final class Notification implements Serializable {

    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    /** Que ocurrio. */
    public enum Type {
        /** Otro usuario registro una reserva nueva. */
        NEW_RESERVATION,
        /** El administrador aprobo una solicitud del usuario. */
        RESERVATION_APPROVED,
        /** El administrador rechazo una solicitud del usuario. */
        RESERVATION_REJECTED
    }

    private final Type type;
    private final ReservationSummary reservation;
    private final String reason;

    public Notification(Type type, ReservationSummary reservation) {
        this(type, reservation, null);
    }

    public Notification(Type type, ReservationSummary reservation, String reason) {
        this.type = type;
        this.reservation = reservation;
        this.reason = reason;
    }

    /** Aviso de que otro usuario creo una reserva. */
    public static Notification newReservation(ReservationSummary reservation) {
        return new Notification(Type.NEW_RESERVATION, reservation);
    }

    /** Aviso de que el administrador aprobo la reserva; el resumen trae los recursos asignados. */
    public static Notification reservationApproved(ReservationSummary reservation) {
        return new Notification(Type.RESERVATION_APPROVED, reservation);
    }

    /** Aviso de que el administrador rechazo la reserva, con el motivo que indico. */
    public static Notification reservationRejected(ReservationSummary reservation, String reason) {
        return new Notification(Type.RESERVATION_REJECTED, reservation, reason);
    }

    public Type getType() {
        return type;
    }

    public ReservationSummary getReservation() {
        return reservation;
    }

    /** Motivo del rechazo; solo tiene valor en {@link Type#RESERVATION_REJECTED}. */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "Notification{" + type + " " + reservation + "}";
    }
}