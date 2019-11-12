import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {
    public static void main(String[] args) throws IOException {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<News> newsFromMySQL = getNewsFromMySQL(sqlSessionFactory);
        // 查询数据库并将数据库数据导入到elasticsearch
        // 并可在浏览器 http://localhost:9200/_search?q=content:北京  将会列出相关信息
        for (int i = 0; i < 10; i++) {
            new Thread(()->writeSingleThread(newsFromMySQL)).start();
        }
    }

    private static void writeSingleThread(List<News> newsFromMySQL) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"), new HttpHost("localhost", 9201, "http")))) {
            // 单线程写入2000 * 1000 = 200_0000条数据，但是每次都发http请求依旧很慢，因此使用批处理操作
            for (int i = 0; i < 1000; i++) {
                for (News news : newsFromMySQL) {
                    IndexRequest request = new IndexRequest("news");

                    Map<String, Object> data = new HashMap<>();
                    data.put("title", news.getTitle());
                    data.put("content", news.getContent());
                    data.put("url", news.getUrl());
                    data.put("date", news.getDate());
                    data.put("createdAt", news.getCreatedAt());
                    data.put("modifiedAt", news.getModifiedAt());

                    request.source(data, XContentType.JSON);

                    IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                    System.out.println(response.status().getStatus());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<News> getNewsFromMySQL(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return sqlSession.selectList("MockMapper.selectAllNews");
        }
    }
}
