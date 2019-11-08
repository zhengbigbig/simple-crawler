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

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class Crawler extends Thread {

    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    @Override
    public void run() {
        try {
            String link;
            // 先从数据库里拿出来一个链接(拿出来并从数据库删除)，准备处理之
            while ((link = dao.getNextLinkFromDatabaseThenDelete()) != null) {
                // 询问数据库，当前链接是不是已经被处理过了
                if (dao.isLinkProcessed(link)) {
                    continue;
                }

                if (isInterestingLink(link)) {
                    Document doc = httpGetAndParseHtml(link);
                    assert doc != null;
                    parseUrlsFromPageAndStoreIntoDatabase(doc);
                    // 假如这是一个新闻的详细页面，就存入数据库，否则，什么都不做
                    storeIntoDatabaseIfNewsPage(doc, link);
                    // 把处理过的放入数据库
                    dao.insertAlreadyLinkIntoDatabase(link);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInterestingLink(String link) {
        return link.contains("news.sina.cn") && link.contains("//") || "https://sina.cn".equals(link);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
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

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.insertNewLinkIntoDatabase(href);
            }
        }
    }


    private void storeIntoDatabaseIfNewsPage(Document doc, String link) throws SQLException {
        Elements articles = doc.select("article");
        if (!articles.isEmpty()) {
            for (Element article : articles) {
                String title = article.select("h1").text();
                System.out.println("title = " + title);
                String time = article.select("time").text();
                System.out.println("time = " + time);
                String content = article.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                System.out.println("content = " + content);
                System.out.println(link);
                dao.insertNewsIntoDatabase(link, title, time, content);
            }
        }
    }


}
