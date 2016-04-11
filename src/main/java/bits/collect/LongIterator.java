/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

/**
 * Primitive long version of {@link java.util.Iterator}.
 *
 * @see java.util.Iterator
 */
public interface LongIterator {
    boolean hasNext();
    long next();
    void remove();
}
