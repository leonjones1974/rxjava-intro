package uk.camsw.rxjava.intro;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Example3_CombiningStreamsTest {

    private Producer producer1;
    private Producer producer2;

    @Before
    public void before() {
        producer1 = Producer.builder()
                .withFrequency(Duration.ofSeconds(1))
                .startFrom(0)
                .build();

        producer2 = Producer.builder()
                .withFrequency(Duration.ofSeconds(1))
                .startFrom(100)
                .build();
    }

    @Test
    public void merge() {
        // We can merge two async streams (of the same type) into a single stream, ordered as they are produced
        producer1.asObservable().mergeWith(producer2.asObservable())
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // When I subscribe, I am subscribed to both streams and end up with something like this:
        // n = 0
        // n = 100
        // n = 1
        // n = 101

        // Perhaps a real world use of this would be producer ID pattern matching in the ordo container?
        // If my callbacks become observables then I can begin to deal with them in different ways,
        // without really usage specific writing code
        // to demultiplex callbacks
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void concat() {
        // How about if I want to consume 1 stream until such time as it completes, then seamlessly
        // switch over to another
        producer1.asObservable().first().concatWith(producer2.asObservable())
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // Cool, took the first one from the first stream, then switched to the second stream
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void zip() {
        // Given 2 streams, (producing asynchronously), we can combine each pair of values emitted
        producer1.asObservable()
                .zipWith(producer2.asObservable(), (n1, n2) -> String.format("%d + %d = %d", n1, n2, n1 + n2))
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // As each new pair is formed, they are passed through my mapping function, resulting in a new stream
        // In this case we map them to a string representing the sum of the pair
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void zipWithDifferentFrequencyProducers() {
        // What happens if one of the streams is producing quicker than the other?
        producer2 = Producer.builder()
                .withFrequency(Duration.ofMillis(100))  // Stream 2 produces an order of magnitude quicker than stream 1
                .startFrom(100)
                .build();

        producer1.asObservable()
                .zipWith(producer2.asObservable()
                        .doOnNext(System.out::println), (n1, n2) -> String.format("%d + %d = %d", n1, n2, n1 + n2))
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // I've added a side effect on 'stream 2',(doOnNext) logging the emissions
        // You can see that even though one side is producing much quicker than the other, it is still giving us
        // our correctly 'paired' items
        // RX is taking care of buffering for us!
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void zipWithBackpressureHandling() {
        // In the previous example we demonstrated that RX would buffer the faster stream, absorbing backpressure
        // However, we clearly don't want infinite memory consumption so if you speed up stream 2 enough, RX will
        // pretty quickly give up and request you provide a 'backpressure' strategy
        producer2 = Producer.builder()
                .withFrequency(Duration.ofMillis(1))      // Now we are well mismatched
                .startFrom(100)
                .build();

        // Try the below, with onBackpressure commented out
        producer1.asObservable()
                .zipWith(producer2.asObservable()
                        .doOnNext(System.out::println)
//                        .onBackpressureLatest()
                        , (n1, n2) -> "" + n1 + " + " + n2 + " = " + (n1 + n2))
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // You should see a backpressure failure after around 230 events on stream 2

        // Now uncomment the backpressure behaviour, you will have to wait a long while to see the impact
        // Because we chose 'onBackpressureLatest', once we have hit the limit of our buffer, we conflate away
        // subsequent events, until such time as we are 'caught up' and available to receive again
        // Other backpressure strategies exist
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.MINUTES);
    }

    @Test
    public void combineLatest() {
        // A little like zip, except that it will combine the latest pair, whenever either stream emits
        producer2 = Producer.builder()
                .withFrequency(Duration.ofMillis(100))      // Stream 2 produces more quickly
                .startFrom(100)
                .build();

        // each item is combined with the last item on stream 1
        Observable.combineLatest(
                producer1.asObservable(), producer2.asObservable()
                , (n1, n2) -> String.format("%d + %d = %d", n1, n2, n1 + n2))
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // You should see a value from stream 1 being combined multiple times with different values from the
        // faster producing stream 2

        // A one sided version of this operator exists.  Imagine a stream of 'leaders' from ordo, you could use
        // a combine to ensure you always produced to the last known leader
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void aMoreComplexScenario() {
        // Slightly contrived, but let's say we are planning to switch over from one source of data to another
        // The other stream comes from an external system.  We can hook up to it, but we don't know when it becomes live

        //Producer 2 is configured to wait a couple of seconds before emitting it's first item
        producer2 = Producer.builder()
                .withFrequency(Duration.ofMillis(100))
                .startsAfter(Duration.ofSeconds(2))
                .startFrom(100)
                .build();

        // Here are our old and new streams
        Observable<Integer> deprecatedStream = producer1.asObservable();
        Observable<Integer> newStream = producer2
                .asObservable()
//                .cache(1)
                ;

        deprecatedStream
                .takeUntil(newStream)
                .concatWith(newStream)
                .take(5)
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // What's going on?
        // We initially receive items from the old stream
        // The new stream emits its first item
        // The takeUntil causes the deprecated stream to complete
        //      (it's contract being 'take until this given stream emits')
        // The completion of the deprecatedStream causes the concat operation to subscribe to the new stream
        //      (it's contract being 'switch to this stream following my normal termination')
        // We are now subscribed to the new, live stream and are no longer consuming from the deprecated stream
        // NOTE: We take(5) just to prove our composability, we want 5 items and we don't care where they came from

        // Is it perfect?  Nope, look carefully and you will see that we missed the first emission (100) from the
        // new stream, it triggered the concat but never emitted the item
        // How can we solve it?
        //    - Uncomment the cache(1) line on the new stream.  Cache is an operator that will cache the last
        //     'n' emissions, thereby replaying them to late subscribers
        Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
    }

}
