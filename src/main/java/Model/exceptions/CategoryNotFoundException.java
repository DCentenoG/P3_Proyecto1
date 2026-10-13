package Model.exceptions;

/** No existe ninguna categoría de recurso con el id o la descripción buscada. */
public class CategoryNotFoundException extends ModelException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
