import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {

    private SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public String getNextLinkFromDatabaseThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("MyMapper.selectNextLink");
            if (link != null) {
                session.delete("MyMapper.deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Integer count = (Integer) session.selectOne("selectProcessedLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String date, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("MyMapper.insertNews", new News(title, content, url, date));
        }
    }

    @Override
    public void insertAlreadyLinkIntoDatabase(String link) {
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", "LINKS_ALREADY_PROCESSED");
        map.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("insertLink", map);
        }
    }

    @Override
    public void insertNewLinkIntoDatabase(String link) {
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", "LINKS_TO_BE_PROCESSED");
        map.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("insertLink", map);
        }
    }
}
