package uk.camsw.rxjava.intro;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Example2_TemporalOperationsTest {

    private Producer producer;

    @Before
    public void before() {
        producer = Producer.builder()
                .withFrequency(Duration.ofMillis(100))          // Let's speed things up a bit
                .startFrom(0)
                .build();
    }

    @Test
    public void buffer() {
        // Let's take a reasonably simple sounding requirement - buffer items on the stream, emitting them as lists at a given frequency
        producer.asObservable()
                .buffer(1, TimeUnit.SECONDS)
                .map(list -> Lists.reverse(list))           // Still composable
                .subscribe(list -> {
                    System.out.println("list = " + list);
                });

        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void naggle() {
        // How about a naggle implementation?
        producer.asObservable()
                .buffer(1, TimeUnit.SECONDS, 5)
                .filter(list -> !list.isEmpty())
                .subscribe(list -> {
                    System.out.println("list = " + list);
                });

        // Buffers are emitted by frequency, or when they are 'full' (full being 5 in this case)
        // If you look at the outputs,
        //   - no race conditions
        //   - no missing events
        //   - no checking of current state
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void sample() {
        // How about if my source stream was built for high frequency trading and is ticking prices at a rate faster than I want to consume?
        // This example will give me the most up-to-date price every second, dropping any emitted in between
        producer.asObservable()
                .sample(1, TimeUnit.SECONDS)
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // Cool, we've just conflated away prices that are ticking too quickly for us to handle!
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }
}
