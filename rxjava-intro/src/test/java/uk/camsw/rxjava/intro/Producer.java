package uk.camsw.rxjava.intro;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer {

    private final Duration frequency;
    private final int startFrom;
    private final Duration startsAfter;

    public Producer(Builder builder) {
        this.frequency = builder.frequency;
        this.startFrom = builder.startFrom;
        this.startsAfter = builder.startsAfter;
    }

    public Observable<Integer> asObservable() {
        AtomicInteger sequence = new AtomicInteger(startFrom);
        return Observable.create(observer -> {
            System.out.println("Starting producer");
            observer.add(Subscriptions.create(() -> System.out.println("Stopping producer")));
            observer.add(
                    Schedulers.io().createWorker().schedulePeriodically(() -> {
                        observer.onNext(sequence.getAndIncrement());
                    }, startsAfter.toNanos(), frequency.toNanos(), TimeUnit.NANOSECONDS)
            );
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        Duration frequency = Duration.ofSeconds(1);
        int startFrom = 0;
        Duration startsAfter = Duration.ofSeconds(0);

        public Producer build() {
            return new Producer(this);
        }

        public Builder withFrequency(Duration frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder startFrom(int startFrom) {
            this.startFrom = startFrom;
            return this;
        }

        public Builder startsAfter(Duration startsAfter) {
            this.startsAfter = startsAfter;
            return this;
        }
    }

}
