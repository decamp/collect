/*
 * Copyright (c) 2014, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;

/**
 * HashMap that uses primitive longs as keys.
 * <p>
 * @author Philip DeCamp
 */
public class LongHashMap<V> {

    private static final int   DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR      = 0.75f;
    private static final int   MAXIMUM_CAPACITY         = 1 << 30;

    private final float mLoadFactor;

    private Entry<V>[] mBuckets;
    private int        mSize;
    private int        mResizeThresh;

    private transient int mModCount = 0;


    /**
     * Constructs a new empty {@code HashMap} instance.
     *
     * @since Android 1.0
     */
    public LongHashMap() {
        this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity.
     *
     * @param initialCapacity the initial capacity of this hash map.
     * @throws IllegalArgumentException when the capacity is less than zero.
     * @since Android 1.0
     */
    public LongHashMap( int initialCapacity ) {
        this( initialCapacity, DEFAULT_LOAD_FACTOR );
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity and
     * load factor.
     *
     * @param initialCapacity the initial capacity of this hash map.
     * @param loadFactor      the initial load factor.
     * @throws IllegalArgumentException when the capacity is less than zero or the load factor is
     *                                  less or equal to zero.
     */
    @SuppressWarnings( "unchecked" )
    public LongHashMap( int initialCapacity, float loadFactor ) {
        if( initialCapacity < 0 ) {
            throw new IllegalArgumentException( "Illegal initial capacity: " + initialCapacity );
        }

        if( loadFactor <= 0 || Float.isNaN( loadFactor ) ) {
            throw new IllegalArgumentException( "Illegal load factor: " + loadFactor );
        }

        mLoadFactor = loadFactor;
        mBuckets    = new Entry[ceilPot( Math.min( initialCapacity, MAXIMUM_CAPACITY ) )];
        mSize       = 0;
        computeResizeThresh();
    }


    /**
     * Returns the mValue of the mapping with the specified mKey.
     *
     * @param key the key.
     * @return the value of the mapping with the specified key, or {@code null}
     * if no mapping for the specified key is found.
     */
    public V get( long key ) {
        Entry<V> m = findEntry( key, bucketIndex( key ) );
        return m != null ? m.mValue : null;
    }

    /**
     * Maps the specified mKey to the specified mValue.
     *
     * @param key   the key.
     * @param value the value.
     * @return the  value of any previous mapping with the specified key or
     * {@code null} if there was no such mapping.
     */
    public V put( long key, V value ) {
        int index = bucketIndex( key );
        Entry<V> entry = findEntry( key, index );
        mModCount++;

        if( entry == null ) {
            if( ++mSize > mResizeThresh ) {
                resize( mBuckets.length * 2 );
                index = bucketIndex( key );
            }
            entry = new Entry<V>( key, value, mBuckets[index] );
            mBuckets[index] = entry;
            return null;

        } else {
            V prev = entry.mValue;
            entry.mValue = value;
            return prev;
        }
    }


    public <T extends V> void putAll( LongHashMap<T> map ) {
        Iterator<Entry<T>> iter = map.entryIterator();
        while( iter.hasNext() ) {
            Entry<T> e = iter.next();
            put( e.getKey(), e.getValue() );
        }
    }

    /**
     * Removes the mapping with the specified mKey from this map.
     *
     * @param key the mKey of the mapping to remove.
     * @return the mValue of the removed mapping or {@code null} if no mapping
     * for the specified mKey was found.
     * @since Android 1.0
     */
    public V remove( long key ) {
        Entry<V> entry = removeEntry( key );
        return entry == null ? null : entry.mValue;
    }

    /**
     * Removes all mappings from this hash map, leaving it empty.
     *
     * @see #isEmpty
     * @see #size
     * @since Android 1.0
     */
    public void clear() {
        if( mSize > 0 ) {
            mSize = 0;
            Arrays.fill( mBuckets, null );
            mModCount++;
        }
    }

    /**
     * Returns the number of elements in this map.
     *
     * @return the number of elements in this map.
     * @since Android 1.0
     */
    public int size() {
        return mSize;
    }

    /**
     * Returns whether this map is empty.
     *
     * @return {@code true} if this map has no elements, {@code false}
     * otherwise.
     * @see #size()
     * @since Android 1.0
     */
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * Returns whether this map contains the specified mKey.
     *
     * @param key the mKey to search for.
     * @return {@code true} if this map contains the specified mKey,
     * {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean containsKey( long key ) {
        return findEntry( key, bucketIndex( key ) ) != null;
    }

    /**
     * Returns whether this map contains the specified mValue.
     *
     * @param value the mValue to search for.
     * @return {@code true} if this map contains the specified mValue,
     * {@code false} otherwise.
     * @since Android 1.0
     */
    public boolean containsValue( Object value ) {
        final int num = mBuckets.length;
        if( value != null ) {
            for( int i = 0; i < num; i++ ) {
                Entry<V> entry = mBuckets[i];
                while( entry != null ) {
                    if( value == entry.mValue || value.equals( entry.mValue ) ) {
                        return true;
                    }
                    entry = entry.mNext;
                }
            }
        } else {
            for( int i = 0; i < num; i++ ) {
                Entry<V> entry = mBuckets[i];
                while( entry != null ) {
                    if( entry.mValue == null ) {
                        return true;
                    }
                    entry = entry.mNext;
                }
            }
        }
        return false;
    }

    /**
     * @return Iterator over keys in map
     */
    public LongIterator keyIterator() {
        return new KeyIter();
    }

    /**
     * @return Iterator over values in map
     */
    public Iterator<V> valueIterator() {
        return new ValueIter();
    }

    /**
     * @return Iterator over entries in map
     */
    public Iterator<Entry<V>> entryIterator() {
        return new EntryIter();
    }



    private int bucketIndex( long hash ) {
        return (int)hash & ( mBuckets.length - 1 );
    }


    private Entry<V> findEntry( long key, int index ) {
        Entry<V> m = mBuckets[index];
        while( m != null ) {
            if( key == m.mKey ) {
                return m;
            }
            m = m.mNext;
        }
        return null;
    }

    @SuppressWarnings( "unchecked" )
    private void resize( int newCapacity ) {
        Entry<V>[] oldBuckets = mBuckets;
        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mResizeThresh = Integer.MAX_VALUE;
            return;
        }

        Entry<V>[] newBuckets = mBuckets = new Entry[newCapacity];

        for( int i = 0; i < oldBuckets.length; i++ ) {
            Entry<V> entry = oldBuckets[i];
            while( entry != null ) {
                Entry<V> next = entry.mNext;

                int idx = bucketIndex( entry.mKey );
                entry.mNext = newBuckets[idx];
                newBuckets[idx] = entry;

                entry = next;
            }
        }

        computeResizeThresh();
    }


    Entry<V> removeEntry( long key ) {
        int index      = bucketIndex( key );
        Entry<V> entry = mBuckets[index];
        Entry<V> last  = null;

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

//
//    void removeEntry( Entry<V> entry ) {
//        final int num = mBuckets.length;
//        for( int i = 0; i < num; i++ ) {
//            Entry<V> p = null;
//            Entry<V> e = mBuckets[i];
//            while( e != null ) {
//                if( e == entry ) {
//                    if( p == null ) {
//                        mBuckets[i] = e.mNext;
//                    } else {
//                        p.mNext = e.mNext;
//                    }
//                    mModCount++;
//                    mSize--;
//                    return;
//                }
//            }
//        }
//    }
//

    private void computeResizeThresh() {
        mResizeThresh = (int)( mBuckets.length * mLoadFactor );
    }


    private static int ceilPot( int val ) {
        if( --val <= 0 ) {
            return 1;
        }
        val = (val >> 1) | val;
        val = (val >> 2) | val;
        val = (val >> 4) | val;
        val = (val >> 8) | val;
        val = (val >> 16) | val;
        return val + 1;
    }



    public static final class Entry<V> {

        long     mKey;
        V        mValue;
        Entry<V> mNext;

//        Entry( long theKey ) {
//            this.mKey = theKey;
//            this.mValue = null;
//        }

        Entry( long key, V value, Entry<V> next ) {
            mKey   = key;
            mValue = value;
            mNext  = next;
        }


        public long getKey() {
            return mKey;
        }

        public V getValue() {
            return mValue;
        }


        @Override
        public int hashCode() {
            return (int)(mKey) ^ (mValue == null ? 0 : mValue.hashCode());
        }

        @Override
        public boolean equals( Object object ) {
            if( this == object ) {
                return true;
            }
            if( object instanceof Entry ) {
                Entry<?> entry = (Entry)object;
                return (mKey == entry.mKey) &&
                       (mValue == entry.mValue || mValue != null && mValue.equals( entry.mValue ));
            }
            return false;
        }

        @Override
        public String toString() {
            return mKey + "=" + mValue;
        }

    }


    private abstract class AbstractIter {

        private int mIterModCount = mModCount;

        private int      mBucketIndex = 0;
        private Entry<V> mPrev        = null;
        private Entry<V> mNext;


        protected AbstractIter() {
            Entry<V> next = null;
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

            removeEntry( mPrev.mKey );
            mPrev = null;
            mIterModCount = mModCount;
        }


        final Entry<V> nextEntry() {
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


    private class KeyIter extends AbstractIter implements LongIterator {
        public long next() {
            return nextEntry().mKey;
        }
    }


    private class ValueIter extends AbstractIter implements Iterator<V> {
        public V next() {
            return nextEntry().mValue;
        }
    }


    private class EntryIter extends AbstractIter implements Iterator<LongHashMap.Entry<V>> {
        public Entry<V> next() {
            return nextEntry();
        }
    }


}


