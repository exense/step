package step.expressions;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExpressionHandlerTest {

    @Test
    public void testContentionOnGroovyPool() throws InterruptedException {
        for (int l=0; l < 10; l++) {
            ExecutorService executorService = Executors.newFixedThreadPool(100);

            long start = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                executorService.submit(() -> {
                    try (ExpressionHandler expressionHandler = new ExpressionHandler()) {
                    }
                });
            }
            executorService.shutdown();
            boolean b = executorService.awaitTermination(20, TimeUnit.SECONDS);
            System.out.println("Success: " + b);
            System.out.println(System.currentTimeMillis() - start);
        }
    }
}
