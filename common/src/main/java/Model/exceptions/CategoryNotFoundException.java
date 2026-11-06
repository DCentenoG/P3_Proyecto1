package Model.exceptions;

/** No existe ninguna categoría de recurso con el id o la descripción buscada. */
public class CategoryNotFoundException extends ModelException {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
