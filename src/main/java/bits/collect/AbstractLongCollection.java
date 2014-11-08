/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

import java.util.Arrays;


/**
 * Primitive long version of {@link java.util.AbstractCollection}.
 *
 * @see java.util.AbstractCollection
 */
public abstract class AbstractLongCollection implements LongCollection {


    protected AbstractLongCollection() {}


    public abstract LongIterator iterator();


    public abstract int size();


    public boolean isEmpty() {
        return size() == 0;
    }


    public boolean contains( long v ) {
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            if( v == iter.next() ) {
                return true;
            }
        }
        return false;
    }


    public long[] toArray() {
        int size          = size();
        long[] ret        = new long[ size];
        LongIterator iter = iterator();

        for( int i = 0; i < size; i++ ) {
            if( !iter.hasNext() ) {
                return Arrays.copyOf( ret, i );
            }
            ret[i] = iter.next();
        }

        return iter.hasNext() ? finishToArray( ret, iter ) : ret;
    }


    public long[] toArray( long[] arr ) {
        int          size = size();
        long[]       ret  = arr.length >= size ? arr : new long[ size ];
        LongIterator iter = iterator();

        for( int i = 0; i < ret.length; i++ ) {
            if( iter.hasNext() ) {
                ret[i] = iter.next();
            } else if( arr != ret ) {
                return Arrays.copyOf( ret, i );
            } else {
                return ret;
            }
        }

        return iter.hasNext() ? finishToArray( ret, iter ) : ret;
    }


    public boolean add( long v ) {
        throw new UnsupportedOperationException();
    }


    public boolean remove( long v ) {
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            if (v == iter.next() ) {
                iter.remove();
                return true;
            }
        }
        return false;
    }


    public boolean containsAll( LongCollection coll ) {
        LongIterator iter = coll.iterator();
        while( iter.hasNext() ) {
            if( !contains( iter.next() ) ) {
                return false;
            }
        }
        return true;
    }


    public boolean addAll( LongCollection coll ) {
        boolean modified = false;
        LongIterator iter = coll.iterator();
        while( iter.hasNext() ) {
            modified |= add( iter.next() );
        }
        return modified;
    }


    public boolean removeAll( LongCollection coll ) {
        boolean modified = false;
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            if( coll.contains( iter.next() ) ) {
                iter.remove();
                modified = true;
            }
        }
        return modified;
    }


    public boolean retainAll( LongCollection coll ) {
        boolean modified = false;
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            if( !coll.contains( iter.next() ) ) {
                iter.remove();
                modified = true;
            }
        }
        return modified;
    }


    public void clear() {
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            iter.next();
            iter.remove();
        }
    }


    public int hashCode() {
        long h = 0;
        LongIterator i = iterator();
        while( i.hasNext() ) {
            long obj = i.next();
            h ^= obj;
        }
        return (int)( h >>> 32 ^ h );
    }


    public String toString() {
        LongIterator i = iterator();
        if( !i.hasNext() ) {
            return "[]";
        }

        StringBuilder s = new StringBuilder();
        s.append( '[' );

        while( true ) {
            long e = i.next();
            s.append( e );
            if( !i.hasNext() ) {
                s.append( ']' );
                return s.toString();
            }
            s.append( ", " );
        }
    }


    private static long[] finishToArray( long[] r, LongIterator it ) {
        int i = r.length;
        while( it.hasNext() ) {
            int cap = r.length;
            if( i == cap ) {
                int newCap = ( ( cap / 2 ) + 1 ) * 3;
                if( newCap <= cap ) { // integer overflow
                    if( cap == Integer.MAX_VALUE ) {
                        throw new OutOfMemoryError( "Required array size too large" );
                    }
                    newCap = Integer.MAX_VALUE;
                }
                r = Arrays.copyOf( r, newCap );
            }
            r[i++] = it.next();
        }
        // trim if overallocated
        return ( i == r.length ) ? r : Arrays.copyOf( r, i );
    }

}
