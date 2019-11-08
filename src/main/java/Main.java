import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int threadNum = 6;
        long t1 = System.currentTimeMillis();
        ExecutorService threadPool = Executors.newScheduledThreadPool(threadNum);
        CrawlerDao dao = new MybatisCrawlerDao();
        for (int i = 0; i < threadNum; i++) {
            threadPool.submit(new Crawler(dao));
        }
        threadPool.shutdown();
        while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("线程池没有关闭");
        }
        System.out.println("线程池已经关闭");
        long t2 = System.currentTimeMillis();
        System.out.println("耗时" + (t2 - t1) / 1000 + 's');
    }
}
