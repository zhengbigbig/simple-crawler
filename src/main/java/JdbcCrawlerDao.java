import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";
    private final Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        File projectDir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String jdbcUrl = "jdbc:h2:file:" + new File(projectDir, "/news").getAbsolutePath();
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public String getNextLinkFromDatabaseThenDelete() throws SQLException {
        String link = getNextLinkFromDatabase("select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM LINKS_TO_BE_PROCESSED WHERE LINK = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }
        }
        return link;
    }

    public String getNextLinkFromDatabase(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM LINKS_ALREADY_PROCESSED WHERE LINK = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }


    public void insertAlreadyLinkIntoDatabase(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_ALREADY_PROCESSED (LINK) VALUES (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public void insertNewLinkIntoDatabase(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_TO_BE_PROCESSED(LINK) VALUES (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    public void insertNewsIntoDatabase(String link, String title, String time, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (TITLE, CONTENT, DATE, URL, CREATED_AT, MODIFIED_AT) values (?, ?, ?, ?, now(), now())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, time);
            statement.setString(4, link);
            statement.executeUpdate();
        }
    }

}
