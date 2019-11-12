import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {

    private static void getAllNewsAndInsertIntoDataBase(SqlSessionFactory sqlSessionFactory, int mockCount) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> newsList = sqlSession.selectList("MockMapper.selectAllNews");
            System.out.println("newsList = " + newsList);
            int count = mockCount - newsList.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(newsList.size());
                    News newsToBeInserted = new News(newsList.get(index));

                    Instant currentTime = newsToBeInserted.getCreatedAt();
                    currentTime = currentTime.minusSeconds(365 * 24 * 3600);
                    newsToBeInserted.setCreatedAt(currentTime);
                    newsToBeInserted.setModifiedAt(currentTime);

                    sqlSession.insert("MockMapper.insertNews", newsToBeInserted);

                    System.out.println("Left: " + count);
                    if (count % 2000 == 0) {
                        sqlSession.flushStatements();
                    }
                }
                sqlSession.commit(); // 事务的原子性  如果都成功则提交
                System.out.println("committed");
            } catch (Exception e) {
                sqlSession.rollback(); // 若发生异常，则回滚
                throw new RuntimeException(e);

            }
        }
    }

    public static void main(String[] args) {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

            getAllNewsAndInsertIntoDataBase(sqlSessionFactory, 100_0000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
