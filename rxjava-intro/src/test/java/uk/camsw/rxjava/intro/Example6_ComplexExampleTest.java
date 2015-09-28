package uk.camsw.rxjava.intro;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Example6_ComplexExampleTest {

    private Observable<Integer> producer;
    private Producer producer1, producer2;

    @Before
    public void before() {
        // We'll go back to using our pre-canned test producer
        producer1 = Producer.builder()
                .withFrequency(Duration.ofSeconds(1))
                .startFrom(0)
                .build();

        producer2 = Producer.builder()
                .withFrequency(Duration.ofMillis(500))
                .startFrom(100)
                .build();

    }

    @Test
    public void oddsAndEvens() {
        // We'll take two streams
        // Stream 1 we will 'group' into odds and evens
        // We push evens and odds onto 2 separate event loops
        // We will combine with the latest from stream 2
        //    - Summing for evens
        //    - Multiplying for odds
        // We will batch into groups of 5
        // And take a total of 5 batches

        Scheduler evensEventLoop = Schedulers.from(Executors.newSingleThreadExecutor());
        Scheduler oddsEventLoop = Schedulers.from(Executors.newSingleThreadExecutor());

        Observable<Integer> stream1 = producer1.asObservable().publish().refCount();
        Observable<Integer> stream2 = producer2.asObservable().publish().refCount();
        stream1
                .groupBy(this::isEven)
                .flatMap(group -> isEven(group)
                        ? group.observeOn(evensEventLoop).withLatestFrom(stream2, (n1, n2) -> "n1+n2=" + (n1 + n2))
                        : group.observeOn(oddsEventLoop).withLatestFrom(stream2, (n1, n2) -> "n1*n2=" + (n1 * n2)))
                .buffer(5)
                .take(5)
                .subscribe(s -> {
                    System.out.println("Thread: [" + Thread.currentThread().getId() + "] " + s);
                });

        Uninterruptibles.sleepUninterruptibly(60, TimeUnit.SECONDS);
    }

    private Boolean isEven(GroupedObservable<Boolean, Integer> group) {
        return group.getKey();
    }

    private boolean isEven(Integer n) {
        return n % 2 == 0;
    }


}
