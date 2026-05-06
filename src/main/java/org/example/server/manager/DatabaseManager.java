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
        createUsersTable();
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Создание sequence для генерации ID
            stmt.execute("CREATE SEQUENCE IF NOT EXISTS org_id_seq START WITH 1 INCREMENT BY 1;");
            //stmt.execute("DROP TABLE organizations;");
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
                    zipcode VARCHAR(10),
                    username VARCHAR(255) NOT NULL);
            """);
        }
    }

    public void createUsersTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY, " +
                "login VARCHAR(50) UNIQUE NOT NULL, " +
                "password_hash VARCHAR(64) NOT NULL)";
        try (var stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean registerUser(String login, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, passwordHash);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean authenticateUser(String login, String passwordHash) throws SQLException {
        String sql = "SELECT id FROM users WHERE login = ? AND password_hash = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, passwordHash);
            try (var rs = pstmt.executeQuery()) {
                return rs.next();
            }
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
        Organization org = new Organization(
                rs.getLong("id"),
                rs.getObject("creation_date", LocalDate.class),
                rs.getString("name"),
                // ⚠️ Адаптируйте под реальные конструкторы ваших классов Coordinates и Address
                new Coordinates(rs.getDouble("coord_x"), rs.getLong("coord_y")),
                rs.getFloat("annual_turnover"),
                OrganizationType.valueOf(rs.getString("type")),
                new Address(rs.getString("street"),rs.getString("zipcode")));
        org.setUsername(rs.getString("username"));
        return org;
    }

    /** Вставляет объект в БД и возвращает сгенерированный sequence ID */
    public long insertOrganization(Organization org, String username) throws SQLException {
        String sql = """
            INSERT INTO organizations (name, coord_x, coord_y, creation_date, annual_turnover, type, street, zipcode, username)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(9, username);

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

    public void clearTable(String username) throws SQLException {
            String sql = "DELETE FROM organizations WHERE username = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }
}