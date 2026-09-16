package Service;

import Model.Admin;
import Model.Employee;
import Model.User;
import Model.exceptions.EmployeeNotFoundException;

import java.util.List;

/*
Servicio de dominio de usuarios: login, cambio de clave y CRUD de
funcionarios (funcionalidades 1 y 3 del enunciado). Delega toda la logica de
coleccion en Model.UserContainer y solo agrega las validaciones minimas y el
guardado en el XML despues de cada cambio.

Los Controllers actuales (CrudViewListener, PasswordChangeFormListener, etc.)
siguen operando directamente sobre SessionContext.getUsers() por ahora; este
servicio queda listo para que se conecten cuando se sumen las validaciones
completas, sin tener que rediseñar la persistencia.
*/
public class UserService {

    private final Service service;

    public UserService(Service service) {
        this.service = service;
    }

    //--- Ingreso y clave ---

    public User login(int id, String password) throws ServiceException {
        for (User user : service.getUsers().getUsers()) {
            if (user.getId() == id) {
                if (!user.getPassword().equals(password)) {
                    throw new ServiceException("La clave ingresada es incorrecta.");
                }
                return user;
            }
        }
        throw new ServiceException("No existe ningun usuario registrado con el id indicado.");
    }

    public void changePassword(User user, String currentPassword, String newPassword) throws ServiceException {
        if (user == null) {
            throw new ServiceException("Debe indicar el usuario al que se le cambiara la clave.");
        }
        if (!user.getPassword().equals(currentPassword)) {
            throw new ServiceException("La clave actual ingresada no es correcta.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ServiceException("La nueva clave no puede estar vacia.");
        }

        user.setPassword(newPassword);
        service.save();
    }

    //--- Funcionarios ---

    public List<Employee> listEmployees() {
        return service.getUsers().getListOfEmployees();
    }

    public Employee findEmployeeById(int id) throws ServiceException {
        try {
            return service.getUsers().getEmployeeById(id);
        } catch (EmployeeNotFoundException e) {
            throw new ServiceException(
                    "No existe ningun funcionario registrado con el id '" + id + "'.", e);
        }
    }

    public Employee findEmployeeByName(String name) throws ServiceException {
        try {
            return service.getUsers().getEmployeeByName(name);
        } catch (EmployeeNotFoundException e) {
            throw new ServiceException(
                    "No existe ningun funcionario registrado con el nombre '" + name + "'.", e);
        }
    }

    public Employee addEmployee(String name, String phoneText) throws ServiceException {
        int phoneNumber = parsePhoneNumber(phoneText);
        if (name == null || name.isBlank()) {
            throw new ServiceException("Debe indicar el nombre del funcionario.");
        }
        try {
            findEmployeeByName(name.trim());
            throw new ServiceException("Ya existe un funcionario registrado con ese nombre.");
        } catch (ServiceException e) {
            if (e.getCause() instanceof EmployeeNotFoundException) {
                // El nombre esta disponible.
            } else {
                throw e;
            }
        }

        //addEmployee genera el id y usa el mismo id como clave inicial (regla del enunciado)
        service.getUsers().addEmployee(name.trim(), phoneNumber);
        service.save();
        return findEmployeeByName(name.trim());
    }

    public Employee addEmployee(String name, int phoneNumber) throws ServiceException {
        return addEmployee(name, String.valueOf(phoneNumber));
    }

    public void updateEmployee(int id, String newName, String phoneText) throws ServiceException {
        int newPhoneNumber = parsePhoneNumber(phoneText);
        Employee employee = findEmployeeById(id);
        if (newName == null || newName.isBlank()) {
            throw new ServiceException("Debe indicar el nombre del funcionario.");
        }

        try {
            Employee other = findEmployeeByName(newName.trim());
            if (other != employee) {
                throw new ServiceException("Ya existe otro funcionario registrado con ese nombre.");
            }
        } catch (ServiceException e) {
            if (!(e.getCause() instanceof EmployeeNotFoundException)) {
                throw e;
            }
        }

        employee.setName(newName.trim());
        employee.setPhoneNumber(newPhoneNumber);
        service.save();
    }

    public void updateEmployee(int id, String newName, int newPhoneNumber) throws ServiceException {
        updateEmployee(id, newName, String.valueOf(newPhoneNumber));
    }

    public void removeEmployee(String name, int phoneNumber) throws ServiceException {
        Employee employee;
        try {
            employee = service.getUsers().getEmployeeByName(name);
        } catch (EmployeeNotFoundException e) {
            throw new ServiceException(
                    "No existe ningun funcionario registrado con ese nombre y telefono.", e);
        }
        if (employee.getPhoneNumber() != phoneNumber) {
            throw new ServiceException("No existe ningun funcionario registrado con ese nombre y telefono.");
        }
        if (!employee.getReservations().isEmpty()) {
            throw new ServiceException(
                    "No se puede eliminar al funcionario '" + name + "' porque tiene reservas registradas.");
        }

        try {
            service.getUsers().removeEmployeeByNameAndPhoneNumber(name, phoneNumber);
        } catch (EmployeeNotFoundException e) {
            throw new ServiceException("No fue posible eliminar el funcionario indicado.", e);
        }
        service.save();
    }

    private int parsePhoneNumber(String phoneText) throws ServiceException {
        if (phoneText == null || phoneText.isBlank()) {
            throw new ServiceException("Debe indicar un numero de telefono.");
        }
        try {
            int phone = Integer.parseInt(phoneText.trim());
            if (phone < 0) {
                throw new NumberFormatException();
            }
            return phone;
        } catch (NumberFormatException e) {
            throw new ServiceException("El telefono debe ser un numero entero valido.", e);
        }
    }

    //--- Administradores (soporte para poder crear el primer usuario del sistema) ---

    public Admin addAdmin(String password) throws ServiceException {
        if (password == null || password.isBlank()) {
            throw new ServiceException("Debe indicar una clave para el nuevo administrador.");
        }

        service.getUsers().addAdmin(password);
        service.save();

        List<User> users = service.getUsers().getUsers();
        return (Admin) users.get(users.size() - 1);
    }
}
