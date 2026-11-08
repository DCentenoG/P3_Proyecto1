package Persistence;

import Model.Admin;
import Model.Employee;
import Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla {@code USUARIO}.
 * <p>
 * Administradores y funcionarios comparten tabla porque comparten un unico
 * contador de ids: {@code Model.UserContainer} genera ids unicos entre los dos
 * tipos, y {@code Model.User} define la identidad por ese id. La columna
 * {@code tipo} distingue cual es cual, y {@code nombre} y {@code telefono}
 * quedan nulos para un administrador.
 * <p>
 * Devuelve los funcionarios sin sus reservas: de eso se encarga
 * {@link ReservationDAO}.
 */
public final class UserDAO {

    private static final String ADMIN = "ADMIN";
    private static final String EMPLOYEE = "EMPLOYEE";

    private static final String SELECT_ALL =
            "SELECT id, tipo, password, nombre, telefono FROM USUARIO ORDER BY id";

    private static final String INSERT =
            "INSERT INTO USUARIO (id, tipo, password, nombre, telefono) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_EMPLOYEE =
            "UPDATE USUARIO SET nombre = ?, telefono = ? WHERE id = ?";

    private static final String UPDATE_PASSWORD =
            "UPDATE USUARIO SET password = ? WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM USUARIO WHERE id = ?";

    /** Todos los usuarios, en orden de id y todavia sin reservas. */
    public List<User> findAll(Connection connection) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String password = rs.getString("password");
                if (ADMIN.equals(rs.getString("tipo"))) {
                    users.add(new Admin(id, password));
                } else {
                    users.add(new Employee(rs.getString("nombre"), rs.getInt("telefono"), id, password));
                }
            }
        }
        return users;
    }

    public void insert(Connection connection, User user) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setInt(1, user.getId());
            if (user instanceof Employee employee) {
                statement.setString(2, EMPLOYEE);
                statement.setString(3, employee.getPassword());
                statement.setString(4, employee.getName());
                statement.setInt(5, employee.getPhoneNumber());
            } else {
                statement.setString(2, ADMIN);
                statement.setString(3, user.getPassword());
                statement.setNull(4, Types.VARCHAR);
                statement.setNull(5, Types.NUMERIC);
            }
            statement.executeUpdate();
        }
    }

    public void updateEmployee(Connection connection, int userId, String name, int phoneNumber)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_EMPLOYEE)) {
            statement.setString(1, name);
            statement.setInt(2, phoneNumber);
            statement.setInt(3, userId);
            statement.executeUpdate();
        }
    }

    public void updatePassword(Connection connection, int userId, String password) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD)) {
            statement.setString(1, password);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }
}
