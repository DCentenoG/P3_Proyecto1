package Model.exceptions;

/** Ya existe un recurso con ese id dentro de la misma categoría. */
public class DuplicateResourceException extends ModelException {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(message);
    }
}