/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;

import java.util.*;
import java.lang.ref.*;


/**
 * Set that stores all objects using weak or strong references.
 * <p>
 * Note that SemiWeakHashSet only removes stale entries when modified, which
 * includes the calling of these method types: <tt>add()</tt>,
 * <tt>addWeakly()</tt>, <tt>remove()</tt>, <tt>clear()</tt> and
 * <tt>vacuum()</tt>. (<tt>vacuum()</tt> does nothing but clear stale entries.)
 * Stale entries are never returned by any method, including methods in
 * associated iterators and views.
 * <p>
 * SemiWeakHashSet will not store the null mValue, but won't throw an exception
 * if you try.
 * <p>
 * SemiWeakHashSet is not thread-safe.
 * 
 * @see java.lang.ref.WeakReference
 * @author Philip DeCamp
 */
@SuppressWarnings( "unchecked" )
public class SemiWeakHashSet<E> extends AbstractSet<E> {

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int MAXIMUM_CAPACITY = 1 << 30;


    private final transient ReferenceQueue<E> mQueue = new ReferenceQueue<E>();

    private Node<E>[] mBuckets;
    private transient int mSize;
    private transient int mModCount;
    private int mThreshold;
    private final float mLoadFactor;

    
    public SemiWeakHashSet() {
        this( DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR );
    }


    public SemiWeakHashSet( int initialCapacity ) {
        this( initialCapacity, DEFAULT_LOAD_FACTOR );
    }


    public SemiWeakHashSet( int initialCapacity, float loadFactor ) {
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

        mBuckets = new Node[capacity];
        mThreshold = (int)(capacity * mLoadFactor);
    }



    @Override
    public boolean add( E o ) {
        if( o == null )
            return false;

        vacuum();
        return doAdd( o, true );
    }


    public boolean addWeakly( E o ) {
        if( o == null )
            return false;

        vacuum();
        return doAdd( o, false );
    }


    @Override
    public boolean addAll( Collection<? extends E> c ) {
        return doAddAll( c, true );
    }


    public boolean addAllWeakly( Collection<? extends E> c ) {
        return doAddAll( c, false );
    }


    @Override
    public void clear() {
        mModCount++;
        Node<E>[] buckets = mBuckets;
        for( int i = 0; i < buckets.length; i++ ) {
            buckets[i] = null;
        }

        mSize = 0;
        while( mQueue.poll() != null )
            ;
    }


    /**
     * @return true iff set contains object, regardless of whether a weak or
     *         strong reference is used.
     */
    @Override
    public boolean contains( Object o ) {
        return findNode( o ) != null;
    }

    /**
     * @return true iff set contains object with a weak reference.
     */
    public boolean containsWeakly( Object o ) {
        Node<E> n = findNode( o );
        return n != null && n.mStrong == null;
    }

    /**
     * @return true iff set contains object with a strong reference.
     */
    public boolean containsStrongly( Object o ) {
        Node<E> n = findNode( o );
        return n != null && n.mStrong != null;
    }


    @Override
    public boolean containsAll( Collection<?> c ) {
        for( Object o : c ) {
            if( !contains( o ) )
                return false;
        }

        return true;
    }


    @Override
    public boolean remove( Object o ) {
        if( o == null )
            return false;

        vacuum();
        Node<E>[] buckets = mBuckets;
        final int hash = rehash( o.hashCode() );
        final int idx = hash & (buckets.length - 1);

        Node<E> parent = null;
        Node<E> child = buckets[idx];

        while( child != null ) {
            if( hash == child.mHash && o.equals( child.get() ) ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    buckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return true;
            }

            parent = child;
            child = child.mNext;
        }

        return false;
    }

    /**
     * Size may not return the exact number of objects actually in the set. If
     * garbage collection has run since the last time the set was modified,
     * WeakHashSet may contain empty nodes that would not normally count towards
     * the collection size.
     * 
     * @return the estimated size of the set, not correcting for
     *         garbage-collected nodes.
     * @see #vacuum()
     */
    @Override
    public int size() {
        return mSize;
    }

    /**
     * Equivalent to:
     * 
     * <pre>
     * size() == 0
     * </pre>
     * 
     * Like size(), this method is only an estimate, and does not correct for
     * garbage-collected nodes.
     * 
     * @return true iff the estimated size of the set is 0.
     * @see #size()
     * @see #vacuum()
     */
    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * Cleans the set by removing all elements that have been garbage-collected.
     * Vacuum is called automatically on all modification methods.
     * 
     * @return size of the set immediately after vacuuming has been performed.
     */
    public int vacuum() {
        Node<E> node;

        while( (node = (Node<E>)mQueue.poll()) != null ) {
            removeNode( node );
        }

        return mSize;
    }



    @Override
    public Iterator<E> iterator() {
        return new SetIterator();
    }



    private boolean doAdd( E element, boolean strong ) {
        Node<E>[] buckets = mBuckets;
        final int hash = rehash( element.hashCode() );
        final int idx = hash & (buckets.length - 1);

        Node<E> old = buckets[idx];
        while( old != null ) {
            if( hash == old.mHash ) {
                E oldEl = old.get();

                if( element.equals( oldEl ) ) {
                    // If the element is only weakly referenced and is being
                    // added strongly,
                    // add strong reference to node.
                    if( old.mStrong == null && strong ) {
                        old.mStrong = oldEl;
                    }

                    return false;
                }
            }

            old = old.mNext;
        }

        addNode( element, strong, hash, idx );
        return true;
    }


    private void addNode( E element, boolean strong, int hash, int idx ) {
        mModCount++;
        Node<E>[] buckets = mBuckets;
        Node<E> node = buckets[idx];
        node = new Node<E>( element, strong, hash, node, mQueue );
        buckets[idx] = node;

        if( mSize++ >= mThreshold ) {
            resize( 2 * buckets.length );
        }
    }


    private boolean doAddAll( Collection<? extends E> c, boolean strong ) {
        vacuum();

        int num = c.size();
        if( num == 0 )
            return false;

        if( num > mThreshold ) {
            int targetCap = (int)(num / mLoadFactor + 1);
            if( targetCap > MAXIMUM_CAPACITY )
                targetCap = MAXIMUM_CAPACITY;

            int newCap = mBuckets.length;
            while( newCap < targetCap ) {
                newCap <<= 1;
            }
            if( newCap > mBuckets.length ) {
                resize( newCap );
            }
        }

        boolean modified = false;

        for( E element : c ) {
            if( element != null ) {
                modified |= doAdd( element, strong );
            }
        }

        return modified;
    }


    private void removeNode( Node<E> node ) {
        Node<E>[] buckets = mBuckets;
        int idx = node.mHash & (buckets.length - 1);
        Node<E> parent = null;
        Node<E> child = buckets[idx];

        while( child != null ) {
            if( child == node ) {
                mModCount++;
                mSize--;

                if( parent == null ) {
                    buckets[idx] = child.mNext;
                } else {
                    parent.mNext = child.mNext;
                }

                return;
            }

            parent = child;
            child = child.mNext;
        }
    }


    private int rehash( int hash ) {
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }


    private void resize( int newCapacity ) {
        Node<E>[] oldBuckets = mBuckets;

        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mThreshold = Integer.MAX_VALUE;
            return;
        }

        Node<E>[] newBuckets = new Node[newCapacity];

        for( int i = 0; i < oldBuckets.length; i++ ) {
            Node<E> node = oldBuckets[i];

            while( node != null ) {
                Node<E> next = node.mNext;

                int idx = (node.mHash & (newCapacity - 1));
                node.mNext = newBuckets[idx];
                newBuckets[idx] = node;

                node = next;
            }
        }

        mBuckets = newBuckets;
        mThreshold = (int)(newCapacity * mLoadFactor);
    }


    private Node<E> findNode( Object o ) {
        if( o == null ) {
            return null;
        }

        final Node<E>[] buckets = mBuckets;
        final int hash = rehash( o.hashCode() );
        final int idx = (hash & (buckets.length - 1));

        Node<E> node = buckets[idx];
        while( node != null ) {
            if( hash == node.mHash && o.equals( node.get() ) ) {
                return node;
            }
            node = node.mNext;
        }

        return null;
    }



    private class SetIterator implements Iterator<E> {
        Node<E> mCurrent;
        Node<E> mNext;
        E mCurrentElement;
        E mNextElement;
        int mIterModCount;
        int mIndex;
        
        SetIterator() {
            mIterModCount = mModCount;
            if( mSize > 0 ) {
                Node<E>[] buckets = mBuckets;

                while( mIndex < buckets.length ) {
                    mNext = buckets[mIndex++];

                    while( mNext != null ) {
                        mNextElement = mNext.get();
                        if( mNextElement != null )
                            return;

                        mNext = mNext.mNext;
                    }
                }
            }
        }


        public boolean hasNext() {
            return mNext != null;
        }
        
        public void remove() {
            if( mCurrent == null ) {
                throw new IllegalStateException();
            }
            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }
            
            removeNode( mCurrent );
            mCurrent = null;
            mCurrentElement = null;
            mIterModCount = mModCount;
        }
        
        public E next() {
            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }
            if( mNext == null ) {
                throw new NoSuchElementException();
            }
            
            mCurrent = mNext;
            mCurrentElement = mNextElement;

            mNext = mNext.mNext;
            mNextElement = null;
            Node<E>[] buckets = mBuckets;

            while( true ) {
                while( mNext != null ) {
                    mNextElement = mNext.get();
                    if( mNextElement != null )
                        return mCurrentElement;

                    mNext = mNext.mNext;
                }

                if( mIndex >= buckets.length )
                    return mCurrentElement;

                mNext = buckets[mIndex++];
            }
        }
    }



    private static final class Node<E> extends WeakReference<E> {
        final int mHash;
        Node<E> mNext;
        E mStrong;

        Node( E value, boolean strong, int hash, Node<E> next, ReferenceQueue<E> queue ) {
            super( value, queue );

            mHash = hash;
            mNext = next;
            mStrong = (strong ? value : null);
        }
    }

}
