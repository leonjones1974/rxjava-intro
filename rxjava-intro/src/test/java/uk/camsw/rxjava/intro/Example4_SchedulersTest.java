package uk.camsw.rxjava.intro;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Example4_SchedulersTest {

    private Producer producer1, producer2;

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
    public void noSpecifiedConcurrency() {
        // Let's take a deeper look at what happens when we merge 2 async streams into a single sequence
        producer1.asObservable()
                .mergeWith(producer2.asObservable())
                .subscribe(n -> {
                    System.out.println("n[" + Thread.currentThread().getId() + "] = " + n);
                });

        // As expected, RX has done nothing for us.  We produced on 2 separate threads, we observed on the same 2
        // threads
        //            n[14] = 0
        //            n[15] = 100
        //            n[14] = 1
        //            n[15] = 101
        //            n[14] = 2
        //            n[15] = 102
        // Stream 1 on thread 14, Stream 2 on Thread 15
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void observeOn() {
        // Perhaps that wasn't what we wanted?  Even if we have merged two streams, perhaps we want to
        // process all emitted items in an orderly fashion on a single thread event loop
        producer1.asObservable()
                .mergeWith(producer2.asObservable())
                .observeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe(n -> {
                    System.out.println("n[" + Thread.currentThread().getId() + "] = " + n);
                });

        // Done! The ordering across the 2 source streams is still non deterministic, but we have forced the observation
        // of those streams onto a single event loop thread so we at least know we are processing only one at a time
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);

        // There are several 'out of the box' schedulers available.  Alternatively you can, as we have above,
        // wrap one around any java Executor
        Schedulers.computation();
        Schedulers.io();
        Schedulers.newThread();
        Schedulers.trampoline();            // etc.
    }

    @Test
    public void usingASchedulerDirectly() {
        // Until now the only scheduler we have really used is hidden within the test producer
        // Let's look a at one now (the computation scheduler actually, a pool scaled according to CPU
        // count I believe)
        Schedulers.computation().createWorker().schedulePeriodically(() -> {
                    System.out.println("Hello World");
                }, 0, 1, TimeUnit.SECONDS
        );

        // Great, so we've output Hello World a few times before exiting the JVM - not that exciting right?
        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
    }

    @Test
    public void unsubscribingFromAScheduler() {
        // More interestingly, as with subscribing to an observable, scheduling work
        // on a scheduler also returns a subscription
        Subscription subscription = Schedulers.computation().createWorker().schedulePeriodically(() -> {
                    System.out.println("Hello World");
                }, 0, 1, TimeUnit.SECONDS
        );

        Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
        subscription.unsubscribe();
        // Doesn't matter how long I wait, I'm not getting anything else because I unsubscribed
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);

        // So, observables and schedulers both return subscriptions,
        // ... might be quite good for managing resources if they could somehow be combined?
    }
}
