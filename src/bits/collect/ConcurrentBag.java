/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;

import java.util.*;

/**
 * A collection that may be accessed by multiple threads 
 * while being modified. ConcurrentBag SHOULD be used for 
 * collections where elements must only be removed occasionally,
 * as removing one element may result in rebuilding the entire
 * collection. 
 * <p> 
 * ConcurrentBag is a "bag" in that it holds some collection of elements
 * in no defined order and may contain a given elemen an arbitrary number
 * of times. Items may be null. 
 * <p>
 * ConcurrentBag works by holding a single reference, "head", to
 * an immutable linked list. This allows all iterators provided by 
 * ConcurrentBag to be valid indefinitely. Modifications to ConcurrentBag are 
 * synchronized and guarantee transaction isolation. Modifications do not generate 
 * ConcurrentModificationExceptions. 
 * <p>
 * Adding elements to ConcurrentBag occurs in constant time.
 * Removing elements occurs in linear time, but notably may cause large portions of the linked list to be rebuilt. 
 * On average, the removal of a random element of the collection will cause half of all nodes to be reallocated. 
 * Removal of the tail node requires recreating the entire structure, 
 * while removal of the head node requires no object allocations.
 * <p>
 * The head node can be accessed directly by calling the <code>head()</code> method,
 * and each subsequent node wit node.mNext. This provides more efficient
 * iteration than allocating and using an Iterator object, if you can get over 
 * the psychological trauma of directly accessing a public field in a completely
 * immutable structure.
 * <p>
 *  
 * @param <T> Element type.
 * @author Philip DeCamp
 */
public class ConcurrentBag<T> extends AbstractCollection<T> {
    
    protected Node<T> mHead = null;
    
    
    public ConcurrentBag() {}
    
    
    public ConcurrentBag( Collection<? extends T> coll ) {
        addAll( coll );
    }
    
    
    
    /**
     * Returns a direct link to the immutable, internal item list. 
     * This object is immutable, and can be iterated easily by 
     * accessing the <code>Node.mHead</code> object. 
     * 
     * @return link to internal list. May be <code>null</code>.
     */
    public Node<T> head() {
        return mHead;
    }
        
    
    @Override
    public int size() {
        Node<T> n = mHead;
        int ret = 0;
        while( n != null ) {
            ret++;
            n = n.mNext;
        }
        
        return ret;
    }
    
    
    @Override
    public boolean isEmpty() {
        return mHead == null;
    }
    
    /**
     * Adds element to bag. Elements may be added multiple times.
     * <code>null</code> may be added.
     * @param item Item to add
     * @return true as insertation is always succesful.
     */
    @Override
    public synchronized boolean add( T item ) {
        mHead = new Node<T>( item, mHead );
        return true;
    }
    
    
    @Override
    public synchronized void clear() {
        mHead = null;
    }
    
    
    @Override
    public synchronized boolean remove( Object item ) {
        Node<T> node = mHead;
        
        // Loop until out of nodes or node contains item.
        while( node != null ) {
            if( item == node.mItem || item != null && item.equals( node.mItem ) ) {
                removeNode( node );
                return true;
            }
            
            node = node.mNext;
        }
        
        return false;
    }

    
    @Override
    public boolean contains( Object item ) {
        Node<T> node = mHead;
        
        // Loop until out of nodes or node contains item.
        while( node != null ) {
            if( item == node.mItem || item != null && item.equals( node.mItem ) ) {
                return true;
            }
            
            node = node.mNext;
        }
        
        return false;
    }
    
    
    /**
     * Returns an iterator for this collection. This iterator will
     * always be valid and will never throw ConcurrentModificationExceptions.
     * However, the contents of the iterator are not guaranteed to match 
     * what is currently in the bag.
     * <p>
     * The iterator supports the <code>remove()<code> operation. In the case
     * that <code>remove()</code> is called when the previous node has already
     * been removed from the list, this call will not modify the collection in
     * any way. Regardless of success, subsequent calls to <code>remove()</code> 
     * will throw a NoSuchElementException(). Note that removals may be expensive.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iter( mHead );
    }
    
    
    
    public static final class Node<T> {
        public final T mItem;
        public final Node<T> mNext;
        
        public Node( T listener, Node<T> next ) {
            mItem = listener;
            mNext     = next;
        }
    }
    
    
    
    private synchronized void removeNode( Node<T> node ) {
        Node<T> head = mHead;
        Node<T> tail = node.mNext;
        
        while( head != null ) {
            if( head == node ) {
                // Found the node to remove.
                mHead = tail;
                return;
            }
            // Add next element of head to tail list.
            tail = new Node<T>( head.mItem, tail );
            head = head.mNext;
        }
        
        // At this point, head is null, and the condition
        // head == node was never true, meaning that node
        // was not found. No change to mHead is made.
    }
    
        
    private final class Iter implements Iterator<T> {
        
        private Node<T> mNext;
        private Node<T> mPrev; 
        
        
        public Iter( Node<T> node ) {
            mNext = node;
        }
        
        
        @Override
        public boolean hasNext() {
            return mNext != null;
        }
        
        @Override
        public T next() {
            if( mNext == null ) {
                throw new NoSuchElementException();
            }
            mPrev = mNext;
            mNext = mNext.mNext;
            return mPrev.mItem;
        }
        
        @Override
        public void remove() {
            if( mPrev == null ) {
                throw new NoSuchElementException();
            }
            removeNode( mPrev );
            mPrev = null;
        }
        
    }
    
}

