package Persistence;

import Model.ResourceCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla {@code CATEGORIA}.
 * <p>
 * Devuelve las categorias sin sus recursos: de llenarlas se encarga
 * {@link ResourceDAO}, para que cada DAO siga siendo responsable de una sola
 * tabla.
 */
public final class CategoryDAO {

    private static final String SELECT_ALL =
            "SELECT id, descripcion FROM CATEGORIA ORDER BY id";

    private static final String INSERT =
            "INSERT INTO CATEGORIA (id, descripcion) VALUES (?, ?)";

    private static final String UPDATE =
            "UPDATE CATEGORIA SET descripcion = ? WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM CATEGORIA WHERE id = ?";

    /** Todas las categorias, en orden de id y todavia sin recursos. */
    public List<ResourceCategory> findAll(Connection connection) throws SQLException {
        List<ResourceCategory> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(new ResourceCategory(rs.getString("id"), rs.getString("descripcion")));
            }
        }
        return categories;
    }

    public void insert(Connection connection, ResourceCategory category) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, category.getId());
            statement.setString(2, category.getDescription());
            statement.executeUpdate();
        }
    }

    public void updateDescription(Connection connection, String categoryId, String description)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, description);
            statement.setString(2, categoryId);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, String categoryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setString(1, categoryId);
            statement.executeUpdate();
        }
    }
}
