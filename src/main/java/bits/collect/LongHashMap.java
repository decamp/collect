/*
 * Copyright (c) 2014, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;

/**
 * HashMap that uses primitive longs as keys.
 *
 * @see java.util.HashMap
 */
public class LongHashMap<V> implements LongMap<V> {

    private static final int   DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR      = 0.75f;
    private static final int   MAXIMUM_CAPACITY         = 1 << 30;

    private final float mLoadFactor;

    private Node<V>[] mBuckets;
    private int       mSize;
    private int       mResizeThresh;

    private transient volatile int mModCount = 0;


    private transient volatile EntrySet mEntrySet = null;
    private transient volatile KeySet   mKeySet   = null;
    private transient volatile Values   mValues   = null;


    public LongHashMap() {
        this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
    }


    public LongHashMap( int initialCapacity ) {
        this( initialCapacity, DEFAULT_LOAD_FACTOR );
    }

    @SuppressWarnings( "unchecked" )
    public LongHashMap( int initialCapacity, float loadFactor ) {
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


    public boolean containsKey( long key ) {
        return getNode( key, bucketIndex( key ) ) != null;
    }


    public boolean containsValue( Object value ) {
        final int num = mBuckets.length;
        if( value != null ) {
            for( int i = 0; i < num; i++ ) {
                Node<V> entry = mBuckets[i];
                while( entry != null ) {
                    if( value.equals( entry.mValue ) ) {
                        return true;
                    }
                    entry = entry.mNext;
                }
            }
        } else {
            for( int i = 0; i < num; i++ ) {
                Node<V> entry = mBuckets[i];
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


    public Set<Entry<V>> entrySet() {
        EntrySet ret = mEntrySet;
        return ret != null ? ret : ( mEntrySet = new EntrySet() );
    }


    public V get( long key ) {
        Node<V> m = getNode( key, bucketIndex( key ) );
        return m != null ? m.mValue : null;
    }


    public boolean isEmpty() {
        return mSize == 0;
    }


    public LongSet keySet() {
        KeySet ret = mKeySet;
        return ret != null ? ret : ( mKeySet = new KeySet() );
    }


    public V put( long key, V value ) {
        int index = bucketIndex( key );
        Node<V> node = getNode( key, index );
        mModCount++;

        if( node == null ) {
            if( ++mSize > mResizeThresh ) {
                resize( mBuckets.length * 2 );
                index = bucketIndex( key );
            }
            node = new Node<V>( key, value, mBuckets[index] );
            mBuckets[index] = node;
            return null;

        } else {
            V prev = node.mValue;
            node.mValue = value;
            return prev;
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void putAll( LongMap<? extends V> m ) {
        for( Iterator iter = (Iterator)m.entrySet().iterator(); iter.hasNext(); ) {
            Entry e = (Entry)iter.next();
            put( e.getKey(), (V)e.getValue() );
        }
    }


    public V remove( long key ) {
        Node<V> node = removeNode( key );
        return node == null ? null : node.mValue;
    }


    public int size() {
        return mSize;
    }


    public Collection<V> values() {
        Values ret = mValues;
        return ret != null ? ret : ( mValues = new Values() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( !( o instanceof LongMap ) ) {
            return false;
        }

        LongMap<V> m = (LongMap<V>)o;
        if( m.size() != size() ) {
            return false;
        }

        try {
            for( Entry<V> e: entrySet() ) {
                long key = e.getKey();
                V value  = e.getValue();
                if( value == null ) {
                    if( !( m.get( key ) == null && m.containsKey( key ) ) ) {
                        return false;
                    }
                } else {
                    if( !value.equals( m.get( key ) ) ) {
                        return false;
                    }
                }
            }
        } catch( ClassCastException unused ) {
            return false;
        } catch( NullPointerException unused ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for( Entry<V> e: entrySet() ) {
            h += e.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        Iterator<Entry<V>> i = entrySet().iterator();
        if( !i.hasNext() ) {
            return "{}";
        }

        StringBuilder s = new StringBuilder();
        s.append( '{' );
        while( true ) {
            Entry<V> e = i.next();
            V value = e.getValue();
            s.append( e.getKey() );
            s.append( '=' );
            s.append( value == this ? "(this Map)" : value );
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


    private Node<V> getNode( long key, int index ) {
        Node<V> m = mBuckets[index];
        while( m != null ) {
            if( key == m.mKey ) {
                return m;
            }
            m = m.mNext;
        }
        return null;
    }


    private Node<V> removeNode( long key ) {
        int index      = bucketIndex( key );
        Node<V> entry = mBuckets[index];
        Node<V> last  = null;

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


    private Node<V> removeEntry( Entry<V> entry ) {
        final int num = mBuckets.length;
        for( int i = 0; i < num; i++ ) {
            Node<V> p = null;
            Node<V> e = mBuckets[i];
            while( e != null ) {
                if( entry.equals( e ) ) {
                    if( p == null ) {
                        mBuckets[i] = e.mNext;
                    } else {
                        p.mNext = e.mNext;
                    }
                    mModCount++;
                    mSize--;
                    return e;
                }
                p = e;
                e = e.mNext;
            }
        }

        return null;
    }

    @SuppressWarnings( "unchecked" )
    private void resize( int newCapacity ) {
        Node<V>[] oldBuckets = mBuckets;
        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mResizeThresh = Integer.MAX_VALUE;
            return;
        }

        Node<V>[] newBuckets = mBuckets = new Node[newCapacity];

        for( int i = 0; i < oldBuckets.length; i++ ) {
            Node<V> entry = oldBuckets[i];
            while( entry != null ) {
                Node<V> next = entry.mNext;

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



    private static final class Node<V> implements Entry<V> {

        final long mKey;

        V       mValue;
        Node<V> mNext;


        Node( long key, V value, Node<V> next ) {
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


        public V setValue( V v ) {
            V ret  = mValue;
            mValue = v;
            return ret;
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

            if( ! (object instanceof Entry ) ) {
                return false;
            }

            Entry entry = (Entry)object;
            if( mKey != entry.getKey() ) {
                return false;
            }
            Object val = entry.getValue();
            return mValue == val || mValue != null && mValue.equals( val );
        }

        @Override
        public String toString() {
            return mKey + "=" + mValue;
        }

    }



    private abstract class AbstractIter {

        private int mIterModCount = mModCount;

        private int     mBucketIndex = 0;
        private Node<V> mPrev        = null;
        private Node<V> mNext;


        protected AbstractIter() {
            Node<V> next = null;
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


        final Node<V> nextEntry() {
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



    private final class KeyIter extends AbstractIter implements LongIterator {
        public long next() {
            return nextEntry().mKey;
        }
    }



    private final class ValueIter extends AbstractIter implements Iterator<V> {
        public V next() {
            return nextEntry().mValue;
        }
    }



    private final class EntryIter extends AbstractIter implements Iterator<Entry<V>> {
        public Node<V> next() {
            return nextEntry();
        }
    }



    private final class KeySet extends AbstractLongSet {

        public void clear() {
            LongHashMap.this.clear();
        }

        public boolean contains( long v ) {
            return containsKey( v );
        }

        public LongIterator iterator() {
            return new KeyIter();
        }

        public boolean remove( long v ) {
            return LongHashMap.this.remove( v ) != null;
        }

        public int size() {
            return mSize;
        }
    }



    private final class Values extends AbstractCollection<V> {


        public void clear() {
            LongHashMap.this.clear();
        }


        public boolean contains( Object obj ) {
            return containsValue( obj );
        }


        public Iterator<V> iterator() {
            return new ValueIter();
        }


        public int size() {
            return mSize;
        }

    }



    private final class EntrySet extends AbstractSet<Entry<V>> {

        public void clear() {
            LongHashMap.this.clear();
        }

        @SuppressWarnings( "unchecked" )
        public boolean contains( Object obj ) {
            if( !(obj instanceof Entry) ) {
                return false;
            }

            Entry e = (Entry)obj;
            long key = e.getKey();
            Entry<V> candidate = getNode( e.getKey(), bucketIndex( key ) );
            return candidate != null && candidate.equals( e );
        }

        @SuppressWarnings( "unchecked" )
        public boolean remove( Object o ) {
            return removeEntry( (Entry<V>)o ) != null;
        }


        public Iterator<Entry<V>> iterator() {
            return new EntryIter();
        }


        public int size() {
            return mSize;
        }
    }

}


