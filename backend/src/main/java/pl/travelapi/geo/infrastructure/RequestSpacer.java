package pl.travelapi.geo.infrastructure;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Keeps requests to an external service at least {@code interval} apart, across all threads.
 * Every caller gets the next free time slot; if that slot is further away than {@code maxWait}, the
 * caller is rejected instead of waiting.
 */
public class RequestSpacer {

	@FunctionalInterface
	public interface Sleeper {

		void sleep(long nanos) throws InterruptedException;
	}

	private final long intervalNanos;
	private final long maxWaitNanos;
	private final LongSupplier nanoClock;
	private final Sleeper sleeper;
	private long nextSlot;

	public RequestSpacer(Duration interval, Duration maxWait, LongSupplier nanoClock, Sleeper sleeper) {
		this.intervalNanos = interval.toNanos();
		this.maxWaitNanos = maxWait.toNanos();
		this.nanoClock = nanoClock;
		this.sleeper = sleeper;
		this.nextSlot = nanoClock.getAsLong();
	}

	/**
	 * Waits for the caller's time slot.
	 *
	 * @return {@code false} when the next free slot is too far away (the caller must not send its request)
	 */
	public boolean acquire() throws InterruptedException {
		long waitNanos;
		synchronized (this) {
			long now = nanoClock.getAsLong();
			waitNanos = Math.max(0, nextSlot - now);
			if (waitNanos > maxWaitNanos) {
				return false;
			}
			nextSlot = now + waitNanos + intervalNanos;
		}
		if (waitNanos > 0) {
			sleeper.sleep(waitNanos);
		}
		return true;
	}
}
