package Model.exceptions;

/** La reserva indicada no pertenece al funcionario sobre el que se quiere operar. */
public class ReservationNotFoundException extends ModelException {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
