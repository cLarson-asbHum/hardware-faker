package clarson.ftc.faker.util;

public interface ThreadSleepMethod {
    void accept(long millis, int nanos) throws InterruptedException;
}