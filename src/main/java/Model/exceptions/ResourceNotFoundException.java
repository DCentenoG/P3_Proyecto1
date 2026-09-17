package Model.exceptions;

/** No existe ningún recurso con el id o la descripción buscada dentro de una categoría. */
public class ResourceNotFoundException extends ModelException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}