package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserContainer {
    //Attributes
    private ArrayList<User> users;
    int nextId;
    //End of attributes

    //Default Builder
    public UserContainer() {
        users = new ArrayList<>();
        nextId = 1;
    }

    //Parameterized Builder
    public UserContainer(ArrayList<User> users) {
        this.users = users;
        nextId = users.size();
    }

    //getter and setter methods
    public int getNextId() {
        return nextId;
    }
    public void setNextId(int nextId) {
        this.nextId = nextId;
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

    public void removeEmployeeByNameAndPhoneNumber(String name, int phoneNumber) {
        for (int i = 0; i < users.size(); i++) {
            if(users.get(i) instanceof Employee) {
                if (((Employee) users.get(i)).getName().equals(name) && ((Employee) users.get(i)).getPhoneNumber() == phoneNumber) {
                    users.remove(i);
                    break;
                }
            }
        }
    } //LANZAR EXCEPCION DE EMPLEADO NO EXISTENTE O DE ARGUMENTOS INVALIDOS

    public Employee getEmployeeById(int id) {
        for (User user : users) {
            if (user instanceof Employee) {
                if (((Employee) user).getId() == id) {
                    return ((Employee) user);
                }
            }
        }
        return null; //LANZAR EXCEPCION DE EMPLEADO NO ENCONTRADO
    }

    public Employee getEmployeeByName(String name) {
        for (User user : users) {
            if (user instanceof Employee) {
                if (((Employee) user).getName().equals(name)) {
                    return ((Employee) user);
                }
            }
        }
        return null; //LANZAR EXCEPCION DE EMPLEADO NO ENCONTRADO
    }
}
