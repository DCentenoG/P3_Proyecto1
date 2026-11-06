package Model.exceptions;

/** No existe ningún recurso con el id o la descripción buscada dentro de una categoría. */
public class ResourceNotFoundException extends ModelException {
    /* Version del formato serializado: ver Protocol.Request. */
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}