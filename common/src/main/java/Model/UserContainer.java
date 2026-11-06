package Model;

import Model.exceptions.EmployeeNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserContainer {
    //Attributes
    private ArrayList<User> users;
    int nextId;
    /*
    Contador global de ids de reserva. Vive aqui, y no dentro de cada Employee,
    porque el id de una reserva tiene que ser unico en TODO el sistema: el
    administrador trabaja con solicitudes de cualquier funcionario y, al pasar a
    la arquitectura distribuida, el Frontend identifica una reserva solo por su
    id. En la etapa de base de datos este contador lo reemplaza una secuencia de
    Oracle.
    */
    private int nextReservationId;
    //End of attributes

    //Default Builder
    public UserContainer() {
        users = new ArrayList<>();
        nextId = 1;
        nextReservationId = 1;
    }

    //Parameterized Builder
    public UserContainer(ArrayList<User> users) {
        this.users = users;
        nextId = users.size();
        nextReservationId = 1;
    }

    //getter and setter methods
    public int getNextId() {
        return nextId;
    }
    public void setNextId(int nextId) {
        this.nextId = nextId;
    }

    public int getNextReservationId() {
        return nextReservationId;
    }

    public void setNextReservationId(int nextReservationId) {
        this.nextReservationId = nextReservationId;
    }

    /*
    Reserva el siguiente id de reserva y avanza el contador. Lo usa
    Employee.tryBook al crear una reserva. Un id asignado NUNCA cambia: la reserva
    conserva el mismo id aunque se edite, se cancele o se vuelva a solicitar.
    */
    public int generateReservationId() {
        return nextReservationId++;
    }

    //getter for the full container
    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    //setter for the arrayList of user
    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public void addEmployee(String name, int phoneNumber) {
        int generateId = nextId;
        String generatePassword = Integer.toString(generateId);
        Employee newEmployee = new Employee(name, phoneNumber, generateId, generatePassword);
        users.add(newEmployee);
        nextId++;
    }

    public void addAdmin(String password) {
        int generatedId = nextId;
        Admin newAdmin = new Admin(generatedId, password);
        users.add(newAdmin);
        nextId++;
    }

    public void removeEmployeeByNameAndPhoneNumber(String name, int phoneNumber)
            throws EmployeeNotFoundException {
        for (int i = 0; i < users.size(); i++) {
            if(users.get(i) instanceof Employee) {
                if (((Employee) users.get(i)).getName().equals(name) && ((Employee) users.get(i)).getPhoneNumber() == phoneNumber) {
                    users.remove(i);
                    return;
                }
            }
        }
        throw new EmployeeNotFoundException(
                "No existe ningun funcionario con el nombre y telefono indicados.");
    }

    public Employee getEmployeeById(int id) throws EmployeeNotFoundException {
        for (User user : users) {
            if (user instanceof Employee) {
                if (((Employee) user).getId() == id) {
                    return ((Employee) user);
                }
            }
        }
        throw new EmployeeNotFoundException(
                "No existe ningun funcionario con el id '" + id + "'.");
    }

    public Employee getEmployeeByName(String name) throws EmployeeNotFoundException {
        for (User user : users) {
            if (user instanceof Employee) {
                if (((Employee) user).getName().equals(name)) {
                    return ((Employee) user);
                }
            }
        }
        throw new EmployeeNotFoundException(
                "No existe ningun funcionario con el nombre '" + name + "'.");
    }

    public ArrayList<Employee> getListOfEmployees() {
        ArrayList<Employee> employees = new ArrayList<>();
        for (User user : users) {
            if (user instanceof Employee) {
                employees.add((Employee) user);
            }
        }
        return employees;
    }
}
