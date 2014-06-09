### Another Collections Library
 
_IntervalMap_, _IntervalSet_ and _LongIntervalMap_ are collections that provide interval-based indexing. For example, if you have a class that represents a block of time, you can store instances in one of these containers and efficiently retrieve all elements that intersect a given point in time, or are completely contained by another block, or that completely contain another time block themselves, etc., all in roughly O(log) time..

RingList, ConcurrentBag, and PublicList provide better performance than java.util alternatives, but aren't as pretty (RingList is pretty enough).

SemiWeakHashSet, WeakHashSet, and WeakValueHashMap are collections that use weak references and may allow elements to be garbage collected.

GroupMap and GroupSet provide multiple indices on a set in a way that most people will find too confusing to bother with.


### Build:
$ ant


### Runtime:
After build, add all jars in **lib** and **target** directories to your project.


### Dependencies:
None.

---
Author: Philip DeCamp
