/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

/**
 * Primitive long version of {@link java.util.AbstractSet }.
 *
 * @see java.util.AbstractSet
 */
public abstract class AbstractLongSet extends AbstractLongCollection implements LongSet {

    @Override
    public boolean equals( Object obj ) {
        if( obj == this ) {
            return true;
        }

        if( !(obj instanceof LongSet) ) {
            return false;
        }

        LongSet c = (LongSet)obj;
        if( c.size() != size() ) {
            return false;
        }

        return containsAll( c );
    }

    @Override
    public boolean removeAll( LongCollection coll ) {
        boolean modified = false;
        if( size() > coll.size() ) {
            for( LongIterator i = coll.iterator(); i.hasNext(); ) {
                modified |= remove( i.next() );
            }
        } else {
            for( LongIterator i = iterator(); i.hasNext(); ) {
                if( coll.contains( i.next() ) ) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

}
