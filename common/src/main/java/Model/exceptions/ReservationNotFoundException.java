package Model.exceptions;

/** La reserva indicada no pertenece al funcionario sobre el que se quiere operar. */
public class ReservationNotFoundException extends ModelException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
