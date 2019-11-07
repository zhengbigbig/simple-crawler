import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkFromDatabaseThenDelete() throws SQLException;

    void updateDatabase(String link, String s) throws SQLException;

    String getNextLinkFromDatabase(String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String time, String content) throws SQLException;

    void insertAlreadyLinkIntoDatabase(String link) throws SQLException;

    void insertNewLinkIntoDatabase(String href) throws SQLException;
}