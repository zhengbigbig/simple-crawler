import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD") // 强制镇压不检查
    public static void main(String[] args) throws IOException, SQLException {
        File projectDir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String jdbcUrl = "jdbc:h2:file:" + new File(projectDir, "/news").getAbsolutePath();
        Connection connection = DriverManager.getConnection(jdbcUrl, USER_NAME, PASSWORD);
        while (true) {
            // 待处理的链接池
            // 从数据库加载即将处理的链接的代码
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");

            if (linkPool.isEmpty()) {
                break;
            }

            // 从待处理池中捞出一个处理
            // 处理完后从池子（包括数据库）中删除
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED WHERE LINK = ?");

            // 询问数据库，当前链接是不是已经被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                assert doc != null;
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                // 假如这是一个新闻的详细页面，就存入数据库，否则，什么都不做
                storeIntoDatabaseIfNewsPage(doc);
                // 把处理过的放入数据库
                insertLinkIntoDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED(LINK) VALUES (?)");
            }
        }

    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED(LINK) VALUES (?)");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM LINKS_ALREADY_PROCESSED WHERE LINK = ?")) {
            resultSet = statement.executeQuery();
            statement.setString(1, link);
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

    private static void insertLinkIntoDatabase(Connection connection, String link, String s) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(s)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }

    private static void storeIntoDatabaseIfNewsPage(Document doc) {
        Elements articles = doc.select("article");
        if (!articles.isEmpty()) {
            for (Element article : articles) {
                String title = article.children().select("h1").text();
                String time = article.children().select("time").text();
                System.out.println("title = " + title + "time = " + time);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            if (response1.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity1 = response1.getEntity();
                String html = EntityUtils.toString(entity1);
                return Jsoup.parse(html);
            }
        }
        return null;
    }

    private static boolean isInterestingLink(String link) {
        return link.contains("//news.sina.cn") || "https://sina.cn".equals(link);
    }
}
