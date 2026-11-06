package Model.exceptions;

/** No existe ningún funcionario con el id o el nombre buscado. */
public class EmployeeNotFoundException extends ModelException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}