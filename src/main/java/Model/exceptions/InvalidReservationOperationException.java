package Model.exceptions;

/**
 * La operación pedida no se puede aplicar a la reserva en su estado
 * actual (p. ej. cancelar una reserva rechazada, aprobar una solicitud
 * que ya fue revisada) o con los datos indicados (p. ej. aprobar sin
 * elegir un recurso por cada categoría requerida, o rechazar sin motivo).
 */
public class InvalidReservationOperationException extends ModelException {
    public InvalidReservationOperationException(String message) {
        super(message);
    }
}
