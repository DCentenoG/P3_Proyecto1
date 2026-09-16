package Model.exceptions;

/** Ya existe un recurso con ese id dentro de la misma categoría. */
public class DuplicateResourceException extends ModelException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}