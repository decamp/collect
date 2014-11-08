/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

/**
 * Primitive long version of {@link java.util.Collection}.
 *
 * @see java.util.Collection
 */
public interface LongCollection {
    boolean	     add( long e );
    boolean	     addAll( LongCollection coll );
    void         clear();
    boolean	     contains( long e );
    boolean	     containsAll( LongCollection coll );
    boolean	     isEmpty();
    LongIterator iterator();
    boolean	     remove( long v );
    boolean	     removeAll( LongCollection coll );
    boolean	     retainAll( LongCollection coll );
    int	         size();
    long[]       toArray();
    long[]       toArray( long[] arr );
}
