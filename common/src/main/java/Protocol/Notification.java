package Protocol;

import Dto.ReservationSummary;

import java.io.Serializable;

/**
 * Un aviso que el Backend empuja al Frontend sin que este lo haya pedido.
 * <p>
 * Es el otro sentido de la comunicacion. Una {@link Request} siempre nace en el
 * Frontend y el Backend contesta con una {@link Response}; una Notification
 * nace en el Backend, cuando algo cambio por accion de OTRO usuario, y llega al
 * cliente en cualquier momento.
 *
 * <h2>Como llega</h2>
 * El Frontend tiene un hilo lector dedicado a escuchar estos avisos. Ese hilo
 * NO es el Event Dispatch Thread de Swing, asi que todo lo que toque la
 * interfaz al recibir una notificacion tiene que ir dentro de
 * {@code SwingUtilities.invokeLater}. Saltarse eso produce repintados corruptos
 * intermitentes que son muy dificiles de reproducir.
 *
 * <h2>Alcance</h2>
 * Hoy existe un solo tipo, {@link Type#NEW_RESERVATION}, que es el que pide el
 * enunciado: cada vez que un usuario registra una reserva, el Backend se lo
 * avisa a todos los demas Frontend activos, que la muestran en su area de
 * alertas. El enum deja lugar para los tipos que haga falta agregar en la rama
 * de alertas.
 */
public final class Notification implements Serializable {

    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    /** Que ocurrio. */
    public enum Type {
        /** Otro usuario registro una reserva nueva. */
        NEW_RESERVATION
    }

    private final Type type;
    private final ReservationSummary reservation;

    public Notification(Type type, ReservationSummary reservation) {
        this.type = type;
        this.reservation = reservation;
    }

    /** Aviso de que otro usuario creo una reserva. */
    public static Notification newReservation(ReservationSummary reservation) {
        return new Notification(Type.NEW_RESERVATION, reservation);
    }

    public Type getType() {
        return type;
    }

    /**
     * La reserva involucrada. Trae actividad, funcionario, fecha, horario y
     * recursos: justo lo que el area de alertas muestra en la lista y en la
     * ventana de detalle.
     */
    public ReservationSummary getReservation() {
        return reservation;
    }

    @Override
    public String toString() {
        return "Notification{" + type + " " + reservation + "}";
    }
}
