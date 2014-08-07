/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;


import java.util.*;
import java.lang.ref.*;


/**
 * Like {@link java.util.WeakHashMap}, but instead of weakly referencing the
 * keys, the values are weakly referenced. This is useful in cases where values
 * are expected to hold their own keys. It also allows the use of normal mKey
 * comparisons instead of expecting <tt>==</tt> to work.
 * <p>
 * Note that WeakValueHashMap only removes stale entries when modified, which
 * includes the calling of these method types: <tt>put()</tt>, <tt>remove()</tt>, <tt>clear()</tt> and <tt>vacuum()</tt>. (<tt>vacuum()</tt> does nothing but
 * clear stale entries.) Stale entries are never returned by any method,
 * including methods in associated iterators and views.
 * <p>
 * WeakValueHashMap supports the null mValue as a mKey, but cannot contain "null"
 * as a mValue. That is, put(mKey, null) is equivalent to remove(mKey).
 * <p>
 * WeakValueHashMap is not thread-safe.
 * 
 * @see java.util.WeakHashMap
 * @author Philip DeCamp
 */
@SuppressWarnings( "unchecked" )
public class WeakValueHashMap<K, V> extends AbstractMap<K, V> {

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    

    private final transient ReferenceQueue<V> mQueue = new ReferenceQueue<V>();

    private RefEntry<K, V>[] mBuckets;
    private transient int mSize;
    private transient int mModCount;
    private int mThreshold;
    private final float mLoadFactor;

    private transient KeySet mKeySet = null;
    private transient Values mValues = null;
    private transient EntrySet mEntrySet = null;


    public WeakValueHashMap() {
        this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
    }

    public WeakValueHashMap( int initialCapacity ) {
        this( initialCapacity, DEFAULT_LOAD_FACTOR );
    }

    public WeakValueHashMap( int initialCapacity, float loadFactor ) {
        if( initialCapacity < 0 )
            throw new IllegalArgumentException( "Illegal initial capacity: " + initialCapacity );

        if( loadFactor <= 0 || Float.isNaN( loadFactor ) )
            throw new IllegalArgumentException( "Illegal load factor: " + loadFactor );

        initialCapacity = Math.min( initialCapacity, MAXIMUM_CAPACITY );
        mLoadFactor = loadFactor;

        int capacity = 1;
        while( capacity < initialCapacity ) {
            capacity <<= 1;
        }

        mBuckets = new RefEntry[capacity];
        mThreshold = (int)(capacity * mLoadFactor);
    }



    @Override
    public V get( Object key ) {
        RefEntry<K, V> entry;

        if( key != null ) {
            entry = getByKey( key );
        } else {
            entry = getByNullKey();
        }

        return (entry != null ? entry.get() : null);
    }

    @Override
    public boolean containsKey( Object key ) {
        if( key != null ) {
            return getByKey( key ) != null;
        } else {
            return getByNullKey() != null;
        }
    }

    @Override
    public V put( K key, V value ) {
        vacuum();

        RefEntry<K, V> old;

        if( key != null ) {
            if( value != null ) {
                old = putByKey( key, value );
            } else {
                old = removeByKey( key );
            }
        } else {
            if( value != null ) {
                old = putByNullKey( value );
            } else {
                old = removeByNullKey();
            }
        }

        return (old == null ? null : old.get());
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> map ) {
        vacuum();

        int num = map.size();
        if( num == 0 )
            return;

        if( num > mThreshold ) {
            int targetCap = (int)(num / mLoadFactor + 1);
            if( targetCap > MAXIMUM_CAPACITY )
                targetCap = MAXIMUM_CAPACITY;

            int newCap = mBuckets.length;
            while( newCap < targetCap )
                newCap <<= 1;

            if( newCap > mBuckets.length )
                resize( newCap );
        }

        for( Map.Entry<? extends K, ? extends V> entry : map.entrySet() ) {
            K key = entry.getKey();
            V value = entry.getValue();

            if( key != null ) {
                if( value != null ) {
                    putByKey( key, value );
                } else {
                    removeByKey( key );
                }
            } else {
                if( value != null ) {
                    putByNullKey( value );
                } else {
                    removeByNullKey();
                }
            }
        }
    }

    @Override
    public V remove( Object key ) {
        vacuum();

        RefEntry<K, V> old;

        if( key != null ) {
            old = removeByKey( key );
        } else {
            old = removeByNullKey();
        }

        return (old == null ? null : old.get());
    }

    @Override
    public void clear() {
        mModCount++;
        Entry<K, V>[] buckets = mBuckets;
        for( int i = 0; i < buckets.length; i++ ) {
            buckets[i] = null;
        }

        mSize = 0;
        while( mQueue.poll() != null );
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * Cleans out the map by removing all nodes with values that have
     * been garbage-collected. Vacuum is called automatically on all 
     * modification methods.
     * 
     * @return size of the set immediately after vacuuming has been performed.
     */
    public int vacuum() {
        RefEntry<K, V> entry;
        while( (entry = (RefEntry<K, V>)mQueue.poll()) != null ) {
            removeByEntry( entry );
        }
        return mSize;
    }


    @Override
    public Set<K> keySet() {
        KeySet ret = mKeySet;
        return (ret != null ? ret : (mKeySet = new KeySet()));
    }

    @Override
    public Collection<V> values() {
        Values ret = mValues;
        return (ret != null ? ret : (mValues = new Values()));
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySet ret = mEntrySet;
        return (ret != null ? ret : (mEntrySet = new EntrySet()));
    }



    private void addEntry( K key, V value, int hash, int idx, boolean increment ) {
        RefEntry<K, V> entry = mBuckets[idx];
        entry = new RefEntry<K, V>( key, value, hash, entry, mQueue );
        mBuckets[idx] = entry;

        if( increment && mSize++ >= mThreshold ) {
            resize( 2 * mBuckets.length );
        }
    }

    private RefEntry<K, V> putByKey( K key, V value ) {
        final RefEntry<K, V>[] buckets = mBuckets;
        final int hash = rehash( key.hashCode() );
        final int idx = hash & (buckets.length - 1);

        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = buckets[idx];

        while( child != null ) {
            if( hash == child.mHash && key == child.mKey || key.equals( child.mKey ) ) {
                if( child.get() == value )
                    return child;

                mModCount++;

                if( parent == null ) {
                    buckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                addEntry( key, value, hash, idx, false );
                return child;
            }

            parent = child;
            child = child.mNext;
        }

        mModCount++;
        addEntry( key, value, hash, idx, true );
        return null;
    }

    private RefEntry<K, V> putByNullKey( V value ) {
        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = mBuckets[0];

        while( child != null ) {
            if( child.mKey == null ) {
                if( value == child.get() )
                    return child;

                mModCount++;

                if( parent == null ) {
                    mBuckets[0] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                addEntry( null, value, 0, 0, false );
                return child;
            }

            parent = child;
            child = child.mNext;
        }

        mModCount++;
        addEntry( null, value, 0, 0, true );
        return null;
    }

    private RefEntry<K, V> getByKey( Object key ) {
        final int hash = rehash( key.hashCode() );

        for( RefEntry<K, V> entry = mBuckets[(hash & (mBuckets.length - 1))]; entry != null; entry = entry.mNext ) {
            if( hash == entry.mHash && key == entry.mKey || key.equals( entry.mKey ) )
                return entry;
        }

        return null;
    }

    private RefEntry<K, V> getByNullKey() {
        for( RefEntry<K, V> entry = mBuckets[0]; entry != null; entry = entry.mNext ) {
            if( entry.mKey == null )
                return entry;

        }

        return null;
    }

    private RefEntry<K, V> removeByKey( Object key ) {
        final RefEntry<K, V>[] buckets = mBuckets;
        final int hash = rehash( key.hashCode() );
        final int idx = (hash & (buckets.length - 1));

        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = buckets[idx];

        while( child != null ) {
            if( hash == child.mHash && key == child.mKey || key.equals( child.mKey ) ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    buckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return child;
            }

            parent = child;
            child = child.mNext;
        }

        return null;
    }

    private RefEntry<K, V> removeByNullKey() {
        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = mBuckets[0];

        while( child != null ) {
            if( child.mKey == null ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    mBuckets[0] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return child;
            }

            parent = child;
            child = child.mNext;
        }

        return null;
    }

    private RefEntry<K, V> removeByEntry( RefEntry<K, V> entry ) {
        final RefEntry<K, V>[] buckets = mBuckets;
        final int idx = (entry.mHash & (buckets.length - 1));

        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = buckets[idx];

        while( child != null ) {
            if( entry == child ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    mBuckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return entry;
            }

            parent = child;
            child = child.mNext;
        }

        return null;
    }

    private RefEntry<K, V> removeByEquivalentEntry( Map.Entry<K, V> entry ) {
        final Object key = entry.getKey();
        final RefEntry<K, V>[] buckets = mBuckets;
        final int hash = (key == null) ? 0 : rehash( key.hashCode() );
        final int idx = (hash & (buckets.length - 1));

        RefEntry<K, V> parent = null;
        RefEntry<K, V> child = buckets[idx];

        while( child != null ) {
            if( child.mHash == hash && entry.equals( child ) ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    buckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return child;
            }

            parent = child;
            child = child.mNext;
        }

        return null;
    }



    private void resize( int newCapacity ) {
        RefEntry<K, V>[] oldBuckets = mBuckets;
        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mThreshold = Integer.MAX_VALUE;
            return;
        }

        RefEntry<K, V>[] newBuckets = new RefEntry[newCapacity];

        for( int i = 0; i < oldBuckets.length; i++ ) {
            RefEntry<K, V> entry = oldBuckets[i];

            while( entry != null ) {
                RefEntry<K, V> next = entry.mNext;

                int idx = (entry.mHash & (newCapacity - 1));
                entry.mNext = newBuckets[idx];
                newBuckets[idx] = entry;

                entry = next;
            }
        }

        mBuckets = newBuckets;
        mThreshold = (int)(newCapacity * mLoadFactor);
    }

    private static int rehash( int hash ) {
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }



    private abstract class BaseIterator<E> implements Iterator<E> {

        RefEntry<K, V> mCurrent;
        RefEntry<K, V> mNext;
        V mCurrentValue;
        V mNextValue;
        int mIterModCount;
        int mIndex;

        BaseIterator() {
            mIterModCount = mModCount;
            if( mSize > 0 ) {
                RefEntry<K, V>[] buckets = mBuckets;

                while( mIndex < buckets.length ) {
                    mNext = buckets[mIndex++];

                    while( mNext != null ) {
                        mNextValue = mNext.get();
                        if( mNextValue != null )
                            return;

                        mNext = mNext.mNext;
                    }
                }
            }
        }


        public final boolean hasNext() {
            return mNext != null;
        }

        public void remove() {
            if( mCurrent == null )
                throw new IllegalStateException();

            if( mModCount != mIterModCount )
                throw new ConcurrentModificationException();

            removeByEntry( mCurrent );
            mCurrent = null;
            mCurrentValue = null;
            mIterModCount = mModCount;
        }

        void nextEntry() {
            if( mModCount != mIterModCount )
                throw new ConcurrentModificationException();

            if( mNext == null )
                throw new NoSuchElementException();

            mCurrent = mNext;
            mCurrentValue = mNextValue;

            mNext = mNext.mNext;
            mNextValue = null;
            RefEntry<K, V>[] buckets = mBuckets;

            while( true ) {
                while( mNext != null ) {
                    mNextValue = mNext.get();
                    if( mNextValue != null )
                        return;

                    mNext = mNext.mNext;
                }

                if( mIndex >= buckets.length )
                    return;

                mNext = buckets[mIndex++];
            }
        }

    }



    private final class KeyIterator extends BaseIterator<K> {
        public K next() {
            nextEntry();
            return mCurrent.mKey;
        }
    }



    private final class ValueIterator extends BaseIterator<V> {
        public V next() {
            nextEntry();
            return mCurrentValue;
        }
    }



    private final class EntryIterator extends BaseIterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            nextEntry();
            return mCurrent;
        }
    }



    private final class KeySet extends AbstractSet<K> {

        public int size() {
            return mSize;
        }

        public boolean contains( Object key ) {
            return containsKey( key );
        }

        public boolean remove( Object key ) {
            if( key != null ) {
                return removeByKey( key ) != null;
            } else {
                return removeByNullKey() != null;
            }
        }

        public void clear() {
            WeakValueHashMap.this.clear();
        }

        public Iterator<K> iterator() {
            return new KeyIterator();
        }

    }



    private final class Values extends AbstractCollection<V> {

        public int size() {
            return mSize;
        }

        public boolean contains( Object obj ) {
            return containsValue( obj );
        }

        public void clear() {
            WeakValueHashMap.this.clear();
        }

        public Iterator<V> iterator() {
            return new ValueIterator();
        }

    }



    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        public int size() {
            return mSize;
        }

        public boolean contains( Object obj ) {
            if( !(obj instanceof Map.Entry) )
                return false;

            Object key = ((Map.Entry<K, V>)obj).getKey();
            RefEntry<K, V> entry;

            if( key != null ) {
                entry = getByKey( key );
            } else {
                entry = getByNullKey();
            }

            return obj.equals( entry );
        }

        public boolean remove( Object obj ) {
            if( !(obj instanceof Map.Entry) )
                return false;

            return (removeByEquivalentEntry( (Map.Entry<K, V>)obj ) != null);
        }

        public void clear() {
            WeakValueHashMap.this.clear();
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

    }



    private static final class RefEntry<K, V> extends WeakReference<V> implements Map.Entry<K, V> {

        final K mKey;
        final int mHash;
        RefEntry<K, V> mNext;

        RefEntry( K key, V value, int hash, RefEntry<K, V> next, ReferenceQueue<V> queue ) {
            super( value, queue );

            mKey = key;
            mHash = hash;
            mNext = next;
        }


        public final K getKey() {
            return mKey;
        }

        public final V getValue() {
            return get();
        }

        public final V setValue( V value ) {
            throw new UnsupportedOperationException();
        }



        public final int hashCode() {
            V value = get();
            return (mKey == null ? 0 : mKey.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        public final boolean equals( Object obj ) {
            if( !(obj instanceof Map.Entry) )
                return false;

            Object key = ((Map.Entry<K, V>)obj).getKey();
            if( mKey == key || mKey != null && mKey.equals( key ) ) {
                Object v1 = get();
                Object v2 = ((Map.Entry<K, V>)obj).getValue();

                return (v1 == v2 || v1 != null && v1.equals( v2 ));
            }

            return false;
        }

        public final String toString() {
            return mKey + "=" + get();
        }

    }


}
