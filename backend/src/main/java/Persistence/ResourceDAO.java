package Persistence;

import Model.ResourceCategory;
import Model.exceptions.DuplicateResourceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a la tabla {@code RECURSO}.
 * <p>
 * Un recurso se identifica por la pareja {@code (categoria_id, id)}, que es su
 * clave primaria compuesta y la misma identidad que define
 * {@code Model.Resource.equals}: el id de un recurso solo es unico dentro de su
 * categoria.
 */
public final class ResourceDAO {

    private static final String SELECT_ALL =
            "SELECT categoria_id, id, descripcion FROM RECURSO ORDER BY categoria_id, id";

    private static final String INSERT =
            "INSERT INTO RECURSO (categoria_id, id, descripcion) VALUES (?, ?, ?)";

    private static final String UPDATE =
            "UPDATE RECURSO SET descripcion = ? WHERE categoria_id = ? AND id = ?";

    private static final String DELETE =
            "DELETE FROM RECURSO WHERE categoria_id = ? AND id = ?";

    private static final String COUNT_IN_USE =
            "SELECT COUNT(*) FROM RESERVA_RECURSO WHERE categoria_id = ? AND recurso_id = ?";

    /**
     * Carga todos los recursos y los mete dentro de la categoria que les
     * corresponde.
     * <p>
     * Se hace en UNA sola consulta y no una por categoria: con una consulta por
     * categoria el costo crece con la cantidad de categorias (el problema
     * clasico de "N+1 consultas"), y al arrancar el servidor se nota.
     */
    public void attachAll(Connection connection, List<ResourceCategory> categories) throws SQLException {
        Map<String, ResourceCategory> byId = new LinkedHashMap<>();
        for (ResourceCategory category : categories) {
            byId.put(category.getId(), category);
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String categoryId = rs.getString("categoria_id");
                ResourceCategory category = byId.get(categoryId);
                if (category == null) {
                    // No deberia pasar: hay una clave foranea que lo impide.
                    throw new SQLException("El recurso " + rs.getInt("id")
                            + " referencia una categoria inexistente: '" + categoryId + "'.");
                }
                try {
                    // addResource crea el Resource y le pone la referencia a su categoria,
                    // de modo que la relacion queda en los dos sentidos igual que en memoria.
                    category.addResource(rs.getInt("id"), rs.getString("descripcion"));
                } catch (DuplicateResourceException e) {
                    throw new SQLException("La base contiene un recurso duplicado en la categoria '"
                            + categoryId + "'.", e);
                }
            }
        }
    }

    public void insert(Connection connection, String categoryId, int resourceId, String description)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, categoryId);
            statement.setInt(2, resourceId);
            statement.setString(3, description);
            statement.executeUpdate();
        }
    }

    public void updateDescription(Connection connection, String categoryId, int resourceId, String description)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, description);
            statement.setString(2, categoryId);
            statement.setInt(3, resourceId);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, String categoryId, int resourceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setString(1, categoryId);
            statement.setInt(2, resourceId);
            statement.executeUpdate();
        }
    }

    /**
     * Cuantas reservas tienen este recurso asignado.
     * <p>
     * La capa de servicio no verificaba esto antes de eliminar un recurso. La
     * clave foranea de {@code RESERVA_RECURSO} lo impide de todos modos, pero
     * preguntando primero se le puede dar al usuario un mensaje entendible en
     * vez de un error de Oracle.
     */
    public int countReservationsUsing(Connection connection, String categoryId, int resourceId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(COUNT_IN_USE)) {
            statement.setString(1, categoryId);
            statement.setInt(2, resourceId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
