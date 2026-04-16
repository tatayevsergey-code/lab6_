package org.example.server.manager;

import org.example.common.models.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection connection;
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(true);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Создание sequence для генерации ID
            stmt.execute("CREATE SEQUENCE IF NOT EXISTS org_id_seq START WITH 1 INCREMENT BY 1;");
            // Создание таблицы. Поля coord_* и address_* упрощены для примера.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS organizations (
                    id BIGINT PRIMARY KEY DEFAULT nextval('org_id_seq'),
                    name VARCHAR(255) NOT NULL,
                    coord_x DOUBLE PRECISION NOT NULL,
                    coord_y DOUBLE PRECISION NOT NULL,
                    creation_date DATE NOT NULL,
                    annual_turnover REAL NOT NULL CHECK (annual_turnover > 0),
                    type VARCHAR(50) NOT NULL,
                    street VARCHAR(1000),
                    zipcode VARCHAR(10)
                    
                );
            """);
        }
    }

    public List<Organization> loadCollection() throws SQLException {
        List<Organization> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM organizations ORDER BY id")) {
            while (rs.next()) {
                list.add(mapResultSetToOrganization(rs));
            }
        }
        return list;
    }

    private Organization mapResultSetToOrganization(ResultSet rs) throws SQLException {
        // Используем конструктор с валидацией
        return new Organization(
                rs.getLong("id"),
                rs.getObject("creation_date", LocalDate.class),
                rs.getString("name"),
                // ⚠️ Адаптируйте под реальные конструкторы ваших классов Coordinates и Address
                new Coordinates(rs.getDouble("coord_x"), rs.getLong("coord_y")),
                rs.getFloat("annual_turnover"),
                OrganizationType.valueOf(rs.getString("type")),
                new Address(rs.getString("street"),rs.getString("zipcode"))
        );
    }

    /** Вставляет объект в БД и возвращает сгенерированный sequence ID */
    public long insertOrganization(Organization org) throws SQLException {
        String sql = """
            INSERT INTO organizations (name, coord_x, coord_y, creation_date, annual_turnover, type, street, zipcode)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, org.getName());
            ps.setDouble(2, org.getCoordinates().getX());
            ps.setDouble(3, org.getCoordinates().getY());
            ps.setObject(4, org.getCreationDate());
            ps.setFloat(5, org.getAnnualTurnover());
            ps.setString(6, org.getType().name());
            ps.setString(7, org.getOfficialAddress() != null ? org.getOfficialAddress().getStreet() : null);
            ps.setString(8, org.getOfficialAddress() != null ? org.getOfficialAddress().getZipCode() : null);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Не удалось получить ID после вставки");
    }

    public void deleteOrganization(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM organizations WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void updateOrganization(Organization org) throws SQLException {
        String sql = """
            UPDATE organizations SET name=?, coord_x=?, coord_y=?, creation_date=?,
            annual_turnover=?, type=?, street=?, zipcode=? WHERE id=?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, org.getName());
            ps.setDouble(2, org.getCoordinates().getX());
            ps.setDouble(3, org.getCoordinates().getY());
            ps.setObject(4, org.getCreationDate());
            ps.setFloat(5, org.getAnnualTurnover());
            ps.setString(6, org.getType().name());
            ps.setString(7, org.getOfficialAddress() != null ? org.getOfficialAddress().getStreet() : null);
            ps.setString(8, org.getOfficialAddress() != null ? org.getOfficialAddress().getZipCode() : null);
            ps.setLong(9, org.getId());
            ps.executeUpdate();
        }
    }

    public void clearTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE organizations RESTART IDENTITY;");
        }
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }
}