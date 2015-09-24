## Concurrency

So far we haven't done anything around concurrency.  What does doing nothing mean?  Well, nothing actually... contrary to common belief, RX is not doing anything around concurrency.
Each item is being emitted serially, on the same thread on which it was produced

That may be a good enough model, but what if we want to do some stuff in parallel

### What now?  

Example 4 demonstrates how we can schedule on different threads