package pl.travelapi.shared.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestSpacerTest {

	private static final long SECOND = Duration.ofSeconds(1).toNanos();

	private final AtomicLong nanoClock = new AtomicLong();
	private final List<Long> sleeps = new ArrayList<>();
	private final RequestSpacer spacer =
			new RequestSpacer(Duration.ofSeconds(1), Duration.ofSeconds(2), nanoClock::get, sleeps::add);

	@Test
	void shouldNotWaitForTheFirstRequest() throws Exception {
		assertThat(spacer.acquire()).isTrue();

		assertThat(sleeps).isEmpty();
	}

	@Test
	void shouldSpaceRequestsOneIntervalApart() throws Exception {
		spacer.acquire();
		spacer.acquire();
		spacer.acquire();

		assertThat(sleeps).containsExactly(SECOND, 2 * SECOND);
	}

	@Test
	void shouldRejectRequestWhenItsSlotIsTooFarAway() throws Exception {
		spacer.acquire();
		spacer.acquire();
		spacer.acquire();

		assertThat(spacer.acquire()).isFalse();
	}

	@Test
	void shouldNotWaitAgainAfterTimePassed() throws Exception {
		spacer.acquire();
		spacer.acquire();
		spacer.acquire();
		nanoClock.addAndGet(10 * SECOND);
		sleeps.clear();

		assertThat(spacer.acquire()).isTrue();
		assertThat(sleeps).isEmpty();
	}
}
