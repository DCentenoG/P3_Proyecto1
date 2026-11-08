package Persistence;

import Model.Employee;
import Model.RequestNotification;
import Model.Reservation;
import Model.Resource;
import Model.ResourceCategory;
import Model.State;
import Model.User;
import Model.exceptions.ResourceNotFoundException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a la tabla {@code RESERVA} y a sus dos tablas puente.
 * <p>
 * Una reserva se reparte en tres tablas:
 * <ul>
 *   <li>{@code RESERVA} con sus datos propios,</li>
 *   <li>{@code RESERVA_CATEGORIA} con las categorias que el funcionario
 *       <b>solicito</b> al crearla,</li>
 *   <li>{@code RESERVA_RECURSO} con los recursos que el administrador le
 *       <b>asigno</b> al aprobarla.</li>
 * </ul>
 * Por eso escribir una reserva toca varias tablas, y esas escrituras tienen que
 * ir en una sola transaccion.
 */
public final class ReservationDAO {

    private static final String SELECT_ALL =
            "SELECT id, empleado_id, actividad, fecha, hora_inicio, hora_fin, estado, "
            + "motivo_rechazo, recibida_en, vista_en FROM RESERVA ORDER BY empleado_id, id";

    private static final String SELECT_ALL_REQUIRED_CATEGORIES =
            "SELECT reserva_id, categoria_id FROM RESERVA_CATEGORIA ORDER BY reserva_id";

    private static final String SELECT_ALL_ASSIGNED_RESOURCES =
            "SELECT reserva_id, categoria_id, recurso_id FROM RESERVA_RECURSO ORDER BY reserva_id";

    private static final String INSERT =
            "INSERT INTO RESERVA (id, empleado_id, actividad, fecha, hora_inicio, hora_fin, estado, "
            + "motivo_rechazo, recibida_en, vista_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE RESERVA SET actividad = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, estado = ?, "
            + "motivo_rechazo = ?, recibida_en = ?, vista_en = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM RESERVA WHERE id = ?";

    private static final String INSERT_REQUIRED_CATEGORY =
            "INSERT INTO RESERVA_CATEGORIA (reserva_id, categoria_id) VALUES (?, ?)";

    private static final String DELETE_REQUIRED_CATEGORIES =
            "DELETE FROM RESERVA_CATEGORIA WHERE reserva_id = ?";

    private static final String INSERT_ASSIGNED_RESOURCE =
            "INSERT INTO RESERVA_RECURSO (reserva_id, categoria_id, recurso_id) VALUES (?, ?, ?)";

    private static final String DELETE_ASSIGNED_RESOURCES =
            "DELETE FROM RESERVA_RECURSO WHERE reserva_id = ?";

    /* Par (categoria, recurso) tal como viene de RESERVA_RECURSO. */
    private record ResourceKey(String categoryId, int resourceId) {
    }

    /**
     * Carga todas las reservas y las reparte entre los funcionarios que las
     * hicieron, resolviendo sus categorias requeridas y recursos asignados
     * contra las categorias ya cargadas.
     * <p>
     * Son tres consultas en total, no tres por reserva: traer todo de una vez y
     * armar los indices en memoria evita el problema de "N+1 consultas", que
     * con unas pocas decenas de reservas ya se nota al arrancar.
     * <p>
     * Los recursos que se le ponen a la reserva son las <b>mismas instancias</b>
     * que viven dentro de su categoria, no copias. Eso mantiene la relacion
     * compartida que el resto del dominio espera.
     */
    public void attachAll(Connection connection, List<User> users, List<ResourceCategory> categories)
            throws SQLException {

        Map<Integer, Employee> employeesById = new LinkedHashMap<>();
        for (User user : users) {
            if (user instanceof Employee employee) {
                employeesById.put(employee.getId(), employee);
                employee.setReservations(new ArrayList<>());
            }
        }
        Map<String, ResourceCategory> categoriesById = new LinkedHashMap<>();
        for (ResourceCategory category : categories) {
            categoriesById.put(category.getId(), category);
        }

        Map<Integer, List<String>> requiredByReservation = loadRequiredCategoryIds(connection);
        Map<Integer, List<ResourceKey>> assignedByReservation = loadAssignedResourceKeys(connection);

        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int reservationId = rs.getInt("id");
                int employeeId = rs.getInt("empleado_id");
                Employee employee = employeesById.get(employeeId);
                if (employee == null) {
                    // La clave foranea lo impide; si pasa, los datos estan corruptos.
                    throw new SQLException("La reserva " + reservationId
                            + " referencia al usuario " + employeeId + ", que no es un funcionario.");
                }

                Reservation reservation = new Reservation(
                        rs.getString("actividad"),
                        rs.getDate("fecha").toLocalDate(),
                        LocalTime.parse(rs.getString("hora_inicio")),
                        LocalTime.parse(rs.getString("hora_fin")));
                reservation.setId(reservationId);
                reservation.setState(State.valueOf(rs.getString("estado")));
                reservation.setRejectionReason(rs.getString("motivo_rechazo"));
                reservation.setNotification(new RequestNotification(
                        toLocalDateTime(rs.getTimestamp("recibida_en")),
                        toLocalDateTime(rs.getTimestamp("vista_en"))));

                reservation.setRequiredCategories(resolveCategories(
                        requiredByReservation.get(reservationId), categoriesById, reservationId));
                reservation.setAssignedResources(resolveResources(
                        assignedByReservation.get(reservationId), categoriesById, reservationId));

                employee.getReservations().add(reservation);
            }
        }
    }

    private Map<Integer, List<String>> loadRequiredCategoryIds(Connection connection) throws SQLException {
        Map<Integer, List<String>> index = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_REQUIRED_CATEGORIES);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                index.computeIfAbsent(rs.getInt("reserva_id"), key -> new ArrayList<>())
                        .add(rs.getString("categoria_id"));
            }
        }
        return index;
    }

    private Map<Integer, List<ResourceKey>> loadAssignedResourceKeys(Connection connection) throws SQLException {
        Map<Integer, List<ResourceKey>> index = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_ASSIGNED_RESOURCES);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                index.computeIfAbsent(rs.getInt("reserva_id"), key -> new ArrayList<>())
                        .add(new ResourceKey(rs.getString("categoria_id"), rs.getInt("recurso_id")));
            }
        }
        return index;
    }

    private ArrayList<ResourceCategory> resolveCategories(List<String> categoryIds,
                                                          Map<String, ResourceCategory> categoriesById,
                                                          int reservationId) throws SQLException {
        ArrayList<ResourceCategory> resolved = new ArrayList<>();
        if (categoryIds == null) {
            return resolved;
        }
        for (String categoryId : categoryIds) {
            ResourceCategory category = categoriesById.get(categoryId);
            if (category == null) {
                throw new SQLException("La reserva " + reservationId
                        + " requiere la categoria '" + categoryId + "', que no existe.");
            }
            resolved.add(category);
        }
        return resolved;
    }

    private ArrayList<Resource> resolveResources(List<ResourceKey> keys,
                                                 Map<String, ResourceCategory> categoriesById,
                                                 int reservationId) throws SQLException {
        ArrayList<Resource> resolved = new ArrayList<>();
        if (keys == null) {
            return resolved;
        }
        for (ResourceKey key : keys) {
            ResourceCategory category = categoriesById.get(key.categoryId());
            if (category == null) {
                throw new SQLException("La reserva " + reservationId + " tiene asignado un recurso de la "
                        + "categoria '" + key.categoryId() + "', que no existe.");
            }
            try {
                // La MISMA instancia que vive en la categoria, no una copia.
                resolved.add(category.getResourceById(key.resourceId()));
            } catch (ResourceNotFoundException e) {
                throw new SQLException("La reserva " + reservationId + " tiene asignado el recurso "
                        + key.resourceId() + " de la categoria '" + key.categoryId()
                        + "', que no existe.", e);
            }
        }
        return resolved;
    }

    //--- Escritura ---

    /** Inserta la reserva y sus dos tablas puente. El llamador controla la transaccion. */
    public void insert(Connection connection, int employeeId, Reservation reservation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setInt(1, reservation.getId());
            statement.setInt(2, employeeId);
            bindCommonColumns(statement, reservation, 3);
            statement.executeUpdate();
        }
        replaceRequiredCategories(connection, reservation);
        replaceAssignedResources(connection, reservation);
    }

    /** Actualiza la reserva y vuelve a escribir sus dos tablas puente. */
    public void update(Connection connection, Reservation reservation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            int index = bindCommonColumns(statement, reservation, 1);
            statement.setInt(index, reservation.getId());
            statement.executeUpdate();
        }
        replaceRequiredCategories(connection, reservation);
        replaceAssignedResources(connection, reservation);
    }

    /** Las filas de las tablas puente se borran en cascada (ver db/schema.sql). */
    public void delete(Connection connection, int reservationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setInt(1, reservationId);
            statement.executeUpdate();
        }
    }

    /*
    Las listas de categorias requeridas y recursos asignados se reescriben enteras
    en lugar de calcular que cambio. Son listas de dos o tres elementos, y asi la
    base queda siempre igual a lo que hay en memoria, sin estados intermedios
    raros: es la opcion mas simple y la mas facil de verificar.
    */
    private void replaceRequiredCategories(Connection connection, Reservation reservation) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_REQUIRED_CATEGORIES)) {
            delete.setInt(1, reservation.getId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_REQUIRED_CATEGORY)) {
            for (ResourceCategory category : reservation.getRequiredCategories()) {
                insert.setInt(1, reservation.getId());
                insert.setString(2, category.getId());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void replaceAssignedResources(Connection connection, Reservation reservation) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_ASSIGNED_RESOURCES)) {
            delete.setInt(1, reservation.getId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_ASSIGNED_RESOURCE)) {
            for (Resource resource : reservation.getAssignedResources()) {
                insert.setInt(1, reservation.getId());
                insert.setString(2, resource.getCategoryId());
                insert.setInt(3, resource.getId());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /* Columnas comunes al INSERT y al UPDATE. Devuelve el siguiente indice libre. */
    private int bindCommonColumns(PreparedStatement statement, Reservation reservation, int from)
            throws SQLException {
        int index = from;
        statement.setString(index++, reservation.getActivity());
        statement.setDate(index++, Date.valueOf(reservation.getDate()));
        statement.setString(index++, reservation.getStartTime().toString());
        statement.setString(index++, reservation.getEndTime().toString());
        statement.setString(index++, reservation.getState().name());
        setNullableString(statement, index++, reservation.getRejectionReason());
        RequestNotification notification = reservation.getNotification();
        setNullableTimestamp(statement, index++, notification == null ? null : notification.getReceivedAt());
        setNullableTimestamp(statement, index++, notification == null ? null : notification.getSeenAt());
        return index;
    }

    //--- Conversiones entre java.time y los tipos de Oracle ---

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return (timestamp == null) ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void setNullableTimestamp(PreparedStatement statement, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
