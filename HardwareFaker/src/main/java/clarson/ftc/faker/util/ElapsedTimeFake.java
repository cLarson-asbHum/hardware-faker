package clarson.ftc.faker.util;

import clarson.ftc.faker.updater.Updateable;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * A polymorphic ElapsedTime that relies on a setter method to get the current 
 * time, rather than relying on `System.now()`. This allows the time of an opmode
 * to be simulated rather than reliant on the system clock (and so actual time).
 * 
 * By implementing Updateable, this is able to be added to an updater and be 
 * synced with any global time updater.
 */
public class ElapsedTimeFake extends ElapsedTime implements Updateable {
    private long accumulatedNanos = 0;
    private boolean isUpdatingEnabled = true;

    public ElapsedTimeFake() {
        super();
    }
    
    public ElapsedTimeFake(long startTime) {
        super(startTime);
    }
    
    public ElapsedTimeFake(Resolution resolution) {
        super(resolution);
    }

    @Override
    public long nsNow() {
        return accumulatedNanos;
    }

    /**
     * @return Change in the number of nano seconds.
     */
    @Override
    public double update(double deltaSeconds) {
        accumulatedNanos += (long) (deltaSeconds * 1_000_000_000);
        return (long) (deltaSeconds * 1_000_000_000);
    }

    @Override
    public boolean isUpdatingEnabled() {
        return isUpdatingEnabled;
    }

    @Override
    public boolean setUpdatingEnabled(boolean shouldBeEnabled) {
        final boolean oldValue = isUpdatingEnabled;
        isUpdatingEnabled = shouldBeEnabled;
        return shouldBeEnabled != oldValue;
    }
}