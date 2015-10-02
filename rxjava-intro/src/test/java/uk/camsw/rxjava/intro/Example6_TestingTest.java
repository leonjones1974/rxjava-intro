package uk.camsw.rxjava.intro;

import org.junit.Test;
import rx.schedulers.Schedulers;
import uk.camsw.rxjava.test.dsl.TestScenario;
import uk.camsw.rxjava.test.dsl.scenario.SingleSourceScenario;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class Example6_TestingTest {

    @Test
    public void simple() {
        SingleSourceScenario<String, Integer> testScenario = TestScenario.singleSource();

        // Here we create a simple sequence under test (n -> n + 1), pump some events into it, and assert on the events
        // received by the subscriber
        testScenario
                .given()
                .theStreamUnderTest(source -> source.map(s -> Integer.parseInt(s) + 1))

                .when()
                .theSubscriber().subscribes()
                .theSource().emits("1")
                .theSource().emits("2")
                .theSource().completes()

                .then()
                .theSubscriber()
                .eventCount().isEqualTo(2)
                .event(0).isEqualTo(2)
                .event(1).isEqualTo(3);
    }


    @Test
    public void rendered() {
        // If we want our tests to be a little more 'visual', we can assert on a customizable stream rendering
        SingleSourceScenario<Integer, String> testScenario = TestScenario.singleSource();

        testScenario
                .given()
                .theStreamUnderTest(source -> source.map(n -> n == 0 ? "a" : "B"))
                .theRenderer(event -> String.format("'%s'", event))

                .when()
                .theSubscriber().subscribes()
                .theSource().emits(0)
                .theSource().emits(1)
                .theSource().completes()

                .then()
                .theSubscriber()
                .eventCount().isEqualTo(2)
                .renderedStream().isEqualTo("['a']-['B']-|")
                .completedCount().isEqualTo(1);
    }

    @Test
    public void temporal() {
        SingleSourceScenario<String, List<String>> testScenario = TestScenario.singleSource();

        // Here we are testing a stream that emits buffered events, every 10 seconds
        // We can use the test libraries underlying 'test scheduler' to play with time
        testScenario
                .given()
                .theStreamUnderTest((source, scheduler) -> source.buffer(10, TimeUnit.SECONDS, scheduler))

                .when()
                .theSubscriber().subscribes()
                .theSource().emits("1a")
                .theSource().emits("1b")
                .theSource().emits("1c")
                .time().advancesBy(Duration.ofSeconds(11))
                .theSource().emits("2a")
                .theSource().emits("2b")
                .theSource().completes()

                .then()
                .theSubscriber()
                .eventCount().isEqualTo(2)
                .event(0).isEqualTo(asList("1a", "1b", "1c"))
                .event(1).isEqualTo(asList("2a", "2b"));
    }

    @Test
    public void async() {
        SingleSourceScenario<String, String> testScenario = TestScenario.singleSource();

        // Integration tests wont be tampering with underlying schedulers, but can use the same test library
        // In this instance we block until we receive the expected number of events from something happening
        // on a different thread
        testScenario
                .given()
                .theStreamUnderTest(source -> source.observeOn(Schedulers.computation()).delay(1, TimeUnit.SECONDS))
                .asyncTimeoutOf(Duration.ofSeconds(2))

                .when()
                .theSubscriber().subscribes()
                .theSource().emits("a")
                .theSource().emits("b")
                .theSubscriber().waitsForEvents(2)

                .then().theSubscriber()
                .renderedStream().isEqualTo("[a]-[b]");
    }

}
