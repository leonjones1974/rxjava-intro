## Managing Underlying Resources

Until now we've largely ignored the test producer, we haven't really questioned it too much.  
All it seems to do is produce a sequence of numbers right?  Kind of, if we dig a bit deeper:

* It didn't start producing until we subscribed (cold observable)
* It stopped producing when we unsubscribed


### What now?  

Example 5 looks at how you can begin to manage underlying resources effectively, using RX