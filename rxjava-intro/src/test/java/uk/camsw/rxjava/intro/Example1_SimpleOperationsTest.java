package uk.camsw.rxjava.intro;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Example1_SimpleOperationsTest {

    private Producer producer;

    @Before
    public void before() {
        // This is a test producer that you will see throughout these examples
        // Don't worry about how it works, just note that its behaviour can be tweaked
        // Here, for example, we produce a new item every 1 second, starting at zero
        producer = Producer.builder()
                .withFrequency(Duration.ofSeconds(1))
                .startFrom(0)
                .build();
    }

    @Test
    public void subscribe() {
        // In order to receive any values from a stream we need to subscribe to it
        // For this example we will subscribe using only a handler for onNext

        // Once we have coerced our 'real-life' callbacks into an observable sequence, we can begin to play with it
        Observable<Integer> source = producer.asObservable();

        // We subscribe here, our lambda function will be executed, asynchronously for each emission of the integer 'n'
        source.subscribe(n -> {
            System.out.println("n = " + n);
        });

        // We wait so we can see some output before terminating:
        //   n = 0
        //   n = 1
        //   n = 2
        //   n = 3
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void unsubscribe() {
        // In the above example we simply rely on the test terminating to stop the stream
        // Fortunately RX gives us a better way, every subscribe returns a subscription, that can be used to unsubscribe

        Subscription subscription = producer.asObservable()
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // This time we'll wait again, but then unsubscribe explicitly
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
        subscription.unsubscribe();
    }

    @Test
    public void onErrorAndOnCompleted() {
        // Before we proceed to other examples, we need to take a look at the other event handler functions
        // that can be provided
        // Here's a subscription, providing handlers for all 3 RX events
        producer.asObservable()
                .subscribe(
                        n -> {
                            System.out.println("n = " + n);             // onNext(T)
                        },
                        e -> {
                            System.err.println("An error: " + e);       // onError(Throwable)
                        },
                        () -> {
                            System.out.println("STREAM COMPLETED");     // onCompleted(Unit)
                        }

                );

        // Why didn't I see an onCompleted?  Because the stream didn't complete, it's producing items infinitely
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void reducing_byNumber() {
        // What about if we only want to take two items from the stream?
        // We can use the take(n) operator
        producer.asObservable()
                .take(2)
                .doOnCompleted(() -> System.out.println("Stream completed"))
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // We can see here that we have taken an infinite stream and made it finite.
        // After two events have been consumed, the stream completes (the producer stops, but more about that much later)
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void map() {
        // We can map values in the stream.  Note, a clever use of generics gives us a type-safe sequence when
        // we map from one type to another, i.e. Integer -> String
        producer.asObservable()
                .map(n -> "value: " + n)
                .map(s -> s.toUpperCase())
                .subscribe(s -> {
                    System.out.println("s = " + s);
                });

        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void filter() {
        // We can filter items in the stream, here we only keep odds
        producer.asObservable()
                .filter(n -> n % 2 != 0)
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void reducing_leftFold() {
        // One of the things we will inevitably be doing in an event sourced architecture is folding over
        // history to reconstitute current state
        // In this example we provide a function that simply sums the first 3 items from a stream
        producer.asObservable()
                .take(3)
                .reduce((prev, current) -> prev + current)
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // Q: What happens here if we remove the take(3)?
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void scan_RunningTotal() {
        // Boring example I know, but what if we want the running total at each emission?
        producer.asObservable()
                .scan((prev, current) -> prev + current)
                .subscribe(n -> {
                    System.out.println("n = " + n);
                });

        // No need to create a finite stream here, scan emits the running total on every event
        // It feeds each emission back in as the 'previous' when a new item arrives
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

}