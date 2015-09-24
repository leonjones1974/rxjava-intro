package uk.camsw.rxjava.intro;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Example5_ManagingResourcesTest {

    private Observable<Integer> producer;

    @Before
    public void before() {
        // Let's start by creating our own producer (Observable)
        // Imagine that it produces values by polling an external system that hasn't yet
        // discovered event driven architectures
        // It begins polling when someone subscribes, it stops when they unsubscribe

        // Here we introduce Observable.create
        producer = Observable.create(observer -> {
            System.out.println("I am starting a polling scheduler");
            AtomicInteger count = new AtomicInteger(0);
            // We have said we are an observable that produces integers
            // We pass in an action that accepts one param, the observer that is subscribing to us
            // This function is called each time, once for every subscriber

            // Someone subscribes, we begin polling our 'external system' every second, pushing results to our observer
            Subscription schedulerSubscription = Schedulers.computation().createWorker().schedulePeriodically(() -> {
                        observer.onNext(count.getAndIncrement());
                    }, 0, 1, TimeUnit.SECONDS
            );

            // What should we do with the subscription, well, we tell the observer about it via the
            // (hideously misnamed) add method
            observer.add(schedulerSubscription);

            // What does it mean?  That when the subscriber unsubscribes from this stream, we also want it to
            // unsubscribe any other subscriptions we have given it
            // Let's add another Subscription to the observer, just for fun
            observer.add(Subscriptions.create(() ->
                    System.out.println("I can associate whatever tear-down behaviour I need by adding a subscription to the observer")));
        });

    }

    @Test
    public void explicitUnsubscribe() {
        // So, all sounds good right?  Someone subscribes, we begin polling, they unsubscribe, we stop polling
        Subscription subscription = producer.subscribe(n -> {
            System.out.println(n);
        });

        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
        subscription.unsubscribe();

        // After 3 seconds we have explicitly unsubscribed.  We can see our output from the custom subscription,
        // it's fair to assume that the scheduler has also been stopped
    }

    @Test
    public void unsubscribeDueToCompletion() {
        // All good, how about if our stream terminates naturally?
        producer.take(2)
                .subscribe(n -> System.out.println("n = " + n),
                        e -> System.err.println(e),
                        () -> System.out.println("I Completed"));

        // This time we have only taken 2 items, yet we still see our unsubscribe action being invoked
        // This warrants some thinking about.  What's really happened here?
        // Well, I didn't subscribe directly to my producer, rather I subscribed to the observable returned
        // from the take(2) operation
        // It helps (me at least) to think of observables returned from operations as 'wrapping' their source observable
        // So, what's really happening is:
        //   - I subscribe to the take(2)
        //   - The take(2) subscribes to my producer
        //   - The onSubscribe action begins the polling, having tied together all the subscriptions
        //   - The producer emits an event
        //   - The take operator observes the event and, providing it hasn't already had it's quota of 2, passes it on
        //   - ... to me, the subscriber
        // That's all good, but something else happened
        // When the take had taken all it needed, it completed. That completion...
        //   - Propagated down to me 'the subscriber' (You will see the 'I Completed' output)
        //   - Then, my unsubscribe automatically propagated up
        //      - I unsubscribed from take
        //      - take unsubscribed from producer
        //      - producer unsubscribed all it's /subscriptions, killing the polling scheduler
        //
        // DISCLAIMER: I'm sure someone with more knowledge of RX internals will correct me on the exact details
        // of the above, but it illustrates the observed behaviour well enough (to me at least)
        // Please feel free to correct me and I will update this doc.
        //
        // We would also see the equivalent behaviour were the stream to error
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void multipleSubscribers() {
        // In the real world it's quite likely that more than one subscriber is interestedin the values emitted
        // from our legacy producer app
        // Lets subscribe to the observable twice, and see what happens
        Subscription subscription1 = producer.subscribe(n -> {
            System.out.println("Subscriber1: " + n);
        });
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        Subscription subscription2 = producer.subscribe(n -> {
            System.out.println("Subscriber2: " + n);
        });
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        subscription1.unsubscribe();

        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
        subscription2.unsubscribe();

        // Perhaps not the easiest output to read but...
        //  Each sequence is unique
        //  Subsequent subscribers get their own instance of the poller
        //  Subscriptions are totally isolated
    }

    @Test
    public void sharingResources() {
        // The above may be exactly what you want, but the owners of the legacy system may not be impressed when
        // your 10,000 subscribers start polling it simultaneously for the same data
        // In some scenarios we are likely to want to 'share' the stream
        producer = producer.publish().refCount();
        // or the recently added (producer.share()) - I prefer the above

        Subscription subscription1 = producer.subscribe(n -> {
            System.out.println("Susbcriber1: " + n);
        });
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        Subscription subscription2 = producer.subscribe(n -> {
            System.out.println("Susbcriber2: " + n);
        });
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        subscription1.unsubscribe();

        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
        subscription2.unsubscribe();

        // Again, let's take a look at the output - some things should be obvious
        //  - We only started one polling scheduler
        //  - Both subscribers (whilst subscribed) received the same values on the stream
        //  - Subscriber 2 continues to receive values even after subscriber 1 unsubscribes
        //  - When subscriber 2 unsubscribes however, we can see the polling scheduler being taken down
        // We don't see it here (to avoid clutter), but were we to subscribe again,
        // you would see the poller being scheduled again

        // It may not be entirely obvious how this is working, but hopefully you can see the behaviour,
        // which may be summarised:
        //  - publish()   - An operator that multiplexes the stream (effectively this stops the lifecycle of the
        //                  source being controlled by subscribe/ unsubscribe activity)
        //  - refCount() - An operator that manages a multiplexed (published) stream's subscription, based on the
        //                 number of subscribers that have subscribed to it
        //               - Additionally refCount emits every event it receives to all of it's subscribers

        // So, refCount will 'connect' the published observable upon receiving it's first subscriber
        // A subsequent subscribe will increment the 'refcount', the new subscriber will begin receiving events
        // When a subscriber unsubscribes, the refCount is decremented
        // When the refCount is zero, the refCount operator unsubscribes from the 'published' stream,
        // which unsubscribes from the underlying producer and tears down the scheduler

        // That's quite a lot to take in, but in short it's given us a really cool way of creating expensive resources
        // on demand and only keeping them for as long as we need them
    }


}
