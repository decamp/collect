/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;


/**
 * HashSet for primitive longs.
 * <p>
 * TODO: No reason to create a new object to hold each value. Memory here should be a flat array.
 *
 * @see java.util.HashMap
 */
public class LongHashSet extends AbstractLongSet implements LongSet {

    private static final int   DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR      = 0.75f;
    private static final int   MAXIMUM_CAPACITY         = 1 << 30;

    private final float mLoadFactor;

    private Node[] mBuckets;
    private int    mSize;
    private int    mResizeThresh;

    private transient volatile int mModCount = 0;


    public LongHashSet() {
        this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
    }


    public LongHashSet( int initialCapacity ) {
        this( initialCapacity, DEFAULT_LOAD_FACTOR );
    }

    @SuppressWarnings( "unchecked" )
    public LongHashSet( int initialCapacity, float loadFactor ) {
        if( initialCapacity < 0 ) {
            throw new IllegalArgumentException( "Illegal initial capacity: " + initialCapacity );
        }

        if( loadFactor <= 0 || Float.isNaN( loadFactor ) ) {
            throw new IllegalArgumentException( "Illegal load factor: " + loadFactor );
        }

        mLoadFactor = loadFactor;
        mBuckets    = new Node[ ceilPot( Math.min( initialCapacity, MAXIMUM_CAPACITY ) ) ];
        mSize       = 0;

        computeResizeThresh();
    }



    public void clear() {
        if( mSize == 0 ) {
            return;
        }

        mModCount++;
        mSize = 0;
        Arrays.fill( mBuckets, null );
    }


    public boolean contains( long key ) {
        return getNode( key, bucketIndex( key ) ) != null;
    }


    public boolean isEmpty() {
        return mSize == 0;
    }


    public boolean add( long key ) {
        int index = bucketIndex( key );
        Node node = getNode( key, index );
        if( node != null ) {
            return false;
        }

        mModCount++;
        if( ++mSize > mResizeThresh ) {
            resize( mBuckets.length * 2 );
            index = bucketIndex( key );
        }
        node = new Node( key, mBuckets[index] );
        mBuckets[index] = node;
        return true;
    }


    public boolean remove( long key ) {
        Node node = removeNode( key );
        return node != null;
    }


    public int size() {
        return mSize;
    }


    public LongIterator iterator() {
        return new Iter();
    }

    @Override
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( !( o instanceof LongSet ) ) {
            return false;
        }

        LongSet m = (LongSet)o;
        if( m.size() != size() ) {
            return false;
        }

        try {
            LongIterator iter = iterator();
            while( iter.hasNext() ) {
                if( !contains( iter.next() ) ) {
                    return false;
                }
            }
        } catch( ClassCastException | NullPointerException unused ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        long v = 0;
        LongIterator iter = iterator();
        while( iter.hasNext() ) {
            v ^= iter.next();
        }
        return (int)( ( v >> 32 ) ^ v );
    }

    @Override
    public String toString() {
        LongIterator i = iterator();
        if( !i.hasNext() ) {
            return "{}";
        }

        StringBuilder s = new StringBuilder();
        s.append( '{' );
        while( true ) {
            long value = i.next();
            s.append( value );
            if( !i.hasNext() ) {
                s.append( '}' );
                return s.toString();
            }
            s.append( ", " );
        }
    }





    private int bucketIndex( long hash ) {
        return (int)hash & ( mBuckets.length - 1 );
    }


    private Node getNode( long key, int index ) {
        Node m = mBuckets[index];
        while( m != null ) {
            if( key == m.mKey ) {
                return m;
            }
            m = m.mNext;
        }
        return null;
    }


    private Node removeNode( long key ) {
        int index      = bucketIndex( key );
        Node entry = mBuckets[index];
        Node last  = null;

        while( entry != null && key != entry.mKey ) {
            last  = entry;
            entry = entry.mNext;
        }

        if( entry == null ) {
            return null;
        }

        if( last == null ) {
            mBuckets[index] = entry.mNext;
        } else {
            last.mNext = entry.mNext;
        }
        mModCount++;
        mSize--;
        return entry;
    }

    @SuppressWarnings( "unchecked" )
    private void resize( int newCapacity ) {
        Node[] oldBuckets = mBuckets;
        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mResizeThresh = Integer.MAX_VALUE;
            return;
        }

        Node[] newBuckets = mBuckets = new Node[newCapacity];

        for( int i = 0; i < oldBuckets.length; i++ ) {
            Node entry = oldBuckets[i];
            while( entry != null ) {
                Node next = entry.mNext;

                int idx = bucketIndex( entry.mKey );
                entry.mNext = newBuckets[idx];
                newBuckets[idx] = entry;

                entry = next;
            }
        }

        computeResizeThresh();
    }


    private void computeResizeThresh() {
        mResizeThresh = (int)( mBuckets.length * mLoadFactor );
    }


    private static int ceilPot( int val ) {
        if( --val <= 0 ) {
            return 1;
        }
        val = (val >>  1) | val;
        val = (val >>  2) | val;
        val = (val >>  4) | val;
        val = (val >>  8) | val;
        val = (val >> 16) | val;
        return val + 1;
    }


    private static final class Node {
        final long mKey;
        Node mNext;

        Node( long key, Node next ) {
            mKey   = key;
            mNext  = next;
        }
    }


    private class Iter implements LongIterator {

        private int mIterModCount = mModCount;

        private int  mBucketIndex = 0;
        private Node mPrev        = null;
        private Node mNext;


        protected Iter() {
            Node next = null;
            while( mBucketIndex < mBuckets.length ) {
                next = mBuckets[mBucketIndex++];
                if( next != null ) {
                    break;
                }
            }
            mNext = next;
        }


        public boolean hasNext() {
            return mNext != null;
        }


        public void remove() {
            if( mPrev == null ) {
                throw new IllegalStateException();
            }
            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }

            removeNode( mPrev.mKey );
            mPrev = null;
            mIterModCount = mModCount;
        }


        public long next() {
            return nextEntry().mKey;
        }


        final Node nextEntry() {
            if( mNext == null ) {
                throw new NoSuchElementException();
            }

            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }

            mPrev = mNext;
            mNext = mNext.mNext;

            while( mNext == null && mBucketIndex < mBuckets.length ) {
                mNext = mBuckets[mBucketIndex++];
            }

            return mPrev;
        }

    }

}


