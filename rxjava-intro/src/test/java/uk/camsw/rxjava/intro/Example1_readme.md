## Simple Operations

Example one shows some simple operations that can be performed on a stream (in this case a stream of Integers)

Many of these operations are similar to those found in Java 8 streams/ Guava Collections.  

### The main difference?  

Events are pushed asynchronously (Observable), rather than pulled on demand (Iterable)


### The Producer

Ignore the implementation of the producer for the time being, it's enough to know that it generates an integer sequence on a background thread

### The 3 RX Events

RX is comprised of just 3 events

* onNext(T) - An item emitted on the stream
* onError(Throwable) - The stream has terminated with an error
* onCompleted() - The stream completed naturally

### Style

In these examples I've used lambda notation rather than Java 8 method references in an attempt to avoid what may be less familiar syntax
