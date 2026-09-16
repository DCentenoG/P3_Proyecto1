package Service;

import Model.Admin;
import Model.Employee;
import Model.User;

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

    public Employee findEmployeeById(int id) {
        return service.getUsers().getEmployeeById(id);
    }

    public Employee findEmployeeByName(String name) {
        return service.getUsers().getEmployeeByName(name);
    }

    public Employee addEmployee(String name, int phoneNumber) throws ServiceException {
        if (name == null || name.isBlank()) {
            throw new ServiceException("Debe indicar el nombre del funcionario.");
        }
        if (findEmployeeByName(name.trim()) != null) {
            throw new ServiceException("Ya existe un funcionario registrado con ese nombre.");
        }

        //addEmployee genera el id y usa el mismo id como clave inicial (regla del enunciado)
        service.getUsers().addEmployee(name.trim(), phoneNumber);
        service.save();
        return findEmployeeByName(name.trim());
    }

    public void updateEmployee(int id, String newName, int newPhoneNumber) throws ServiceException {
        Employee employee = findEmployeeById(id);
        if (employee == null) {
            throw new ServiceException("No existe ningun funcionario registrado con el id '" + id + "'.");
        }
        if (newName == null || newName.isBlank()) {
            throw new ServiceException("Debe indicar el nombre del funcionario.");
        }

        Employee other = findEmployeeByName(newName.trim());
        if (other != null && other != employee) {
            throw new ServiceException("Ya existe otro funcionario registrado con ese nombre.");
        }

        employee.setName(newName.trim());
        employee.setPhoneNumber(newPhoneNumber);
        service.save();
    }

    public void removeEmployee(String name, int phoneNumber) throws ServiceException {
        Employee employee = service.getUsers().getEmployeeByName(name);
        if (employee == null || employee.getPhoneNumber() != phoneNumber) {
            throw new ServiceException("No existe ningun funcionario registrado con ese nombre y telefono.");
        }
        if (!employee.getReservations().isEmpty()) {
            throw new ServiceException(
                    "No se puede eliminar al funcionario '" + name + "' porque tiene reservas registradas.");
        }

        service.getUsers().removeEmployeeByNameAndPhoneNumber(name, phoneNumber);
        service.save();
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
