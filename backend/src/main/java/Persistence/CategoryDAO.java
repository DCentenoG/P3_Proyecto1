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

    private static final String COUNT_REQUIRED_BY =
            "SELECT COUNT(*) FROM RESERVA_CATEGORIA WHERE categoria_id = ?";

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

    /**
     * Cuantas reservas solicitaron esta categoria.
     * <p>
     * La clave foranea de RESERVA_CATEGORIA impide eliminar una categoria que
     * alguna reserva haya solicitado, aunque esa reserva ya este cancelada o
     * rechazada. Preguntando primero se le puede dar al usuario un mensaje
     * entendible en vez de un error de Oracle.
     */
    public int countReservationsRequiring(Connection connection, String categoryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(COUNT_REQUIRED_BY)) {
            statement.setString(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
