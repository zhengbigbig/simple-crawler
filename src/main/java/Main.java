import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        // 待处理的链接池
        List<String> linkPool = new ArrayList<>();
        // 已处理的链接池
        Set<String> processedLinks = new HashSet<>();
        linkPool.add("https://sina.cn");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            // ArrayList 从尾部删除更有效率,remove 方法会返回要删除的元素
            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLinks.contains(link)) {
                continue;
            }

            if (link.contains("//news.sina.cn") || "https://sina.cn".equals(link)) {
                // 这是我们感兴趣的，我们只处理新浪站
                CloseableHttpClient httpclient = HttpClients.createDefault();
//                System.out.println(link);
                if(link.startsWith("//")){
                    link = "https:" + link;
                }

                HttpGet httpGet = new HttpGet(link);
                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36");
                CloseableHttpResponse response1 = httpclient.execute(httpGet);
                try {
//                    System.out.println(response1.getStatusLine());

                    if(response1.getStatusLine().getStatusCode() == 200){
                        HttpEntity entity1 = response1.getEntity();
                        String html = EntityUtils.toString(entity1);

                        Document doc = Jsoup.parse(html);

                        Elements elements = doc.select("a");
                        for (Element element : elements) {
                            linkPool.add(element.attr("href"));
                        }

                        // 假如这是一个新闻的详细页面，就存入数据库，否则，什么都不做
                        Elements articles = doc.select("article");
                        if (!articles.isEmpty()) {
                            for (Element article : articles) {
                                String title = article.children().select("h1").text();
                                String time = article.children().select("time").text();
                                System.out.println("title = " + title + "time = " + time);
                            }
                        }
                    }
                    processedLinks.add(link);

                } finally {
                    response1.close();
                }
            }
        }

    }
}
