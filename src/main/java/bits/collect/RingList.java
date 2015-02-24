/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;


/**
 * Like an ArrayList, but also implements Queue and provides 
 * fast insertion/removal at both beginning and end of List,
 * making it ideal for FIFO or FILO queueing.
 * <p>
 * Compared to ArrayList: much faster when when inserting/deleting 
 * at front of list; otherwise slightly slower.
 * <p>
 * Compared to LinkedList: much slower when inserting/deleting in
 * middle of list often; otherwise much faster.
 * <p>
 * Memory allocation is nearly identical to ArrayList.
 * <p>
 * Not thread-safe.
 * 
 * @author Philip DeCamp
 * @deprecated in favor of {@link java.util.ArrayDeque}
 */
@SuppressWarnings("unchecked")
public class RingList<E> extends AbstractCollection<E> implements Queue<E>, List<E> {
    
    static final int DEFAULT_INITIAL_CAPACITY = 10;
    static final int MAXIMUM_CAPACITY         = 1 << 29;
    
    private final int mMaxCap;
    
    private Object[] mBuffer; 
    private int mPos  = 0;
    private int mSize = 0;
    
    private transient int mModCount = 0;
    
    
    public RingList() {
        this( DEFAULT_INITIAL_CAPACITY, MAXIMUM_CAPACITY );
    }

    
    public RingList( int initialCapacity ) {
        this( initialCapacity, MAXIMUM_CAPACITY );
    }
    
    
    public RingList( int initialCapacity, int maximumCapacity ) {
        mMaxCap = Math.max( 0, Math.min( maximumCapacity, MAXIMUM_CAPACITY ) );
        if( initialCapacity <= 0 ) {
            initialCapacity = 10;
        }
        
        initialCapacity = Math.min( initialCapacity, mMaxCap );
        mBuffer = new Object[initialCapacity];
    }
    
    
    
    public boolean add( E obj ) {
        if( !ensureCapacity( mSize + 1 ) ) {
            throw new IllegalStateException( "Maximum capacity exceeded." );
        }
        doAdd( mSize, obj );
        return true;
    }
    
    
    public void add( int index, E o ) {
        if( !ensureCapacity( mSize + 1 ) ) {
            throw new IllegalStateException( "Maximum capacity exceeded." );
        }
        doAdd( index, o );
    }
    
    
    public boolean addAll( int index, Collection<? extends E> c ) {
        boolean ret = false;
        for( E o: c ) {
            add( index++, o );
            ret = true;
        }
        return ret;
    }
    
    
    public boolean offer( E obj ) {
        if( mSize == mBuffer.length && !ensureCapacity( mSize + 1 ) ) {
            return false;
        }
        doAdd( mSize, obj );
        return true;
    }
    
    
    public E get( int idx ) {
        if( idx < 0 || idx >= mSize ) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (E)mBuffer[ (mPos + idx) % mBuffer.length ];
    }
    
    
    public E element() {
        if( mSize == 0 ) { 
            throw new NoSuchElementException();
        }
        return (E)mBuffer[mPos];
    }

    
    public E peek() {
        if( mSize == 0 ) {
            return null;
        }
        return (E)mBuffer[mPos];
    }
    
    
    public E remove() {
        if( mSize == 0 ) {
            throw new NoSuchElementException();
        }
        E ret = (E)mBuffer[mPos];
        doRemove( 0 );
        return ret;
    }
    
    
    public E poll() {
        if( mSize == 0 ) {
            return null;
        }
        E ret = (E)mBuffer[mPos];
        doRemove( 0 );
        return ret;
    }

    
    @Override
    public void clear() {
        mModCount++;
        mPos  = 0;
        mSize = 0;
    }

    
    @Override
    public boolean contains( Object o ) {
        return indexOf( o ) >= 0;
    }
    
    
    public E remove( int idx ) {
        if( idx < 0 || idx >= mSize ) {
            throw new NoSuchElementException();
        }
        E ret = (E)mBuffer[ (mPos + idx) % mBuffer.length ];
        doRemove( idx );
        return ret;
    }
    
    
    @Override
    public boolean remove( Object o ) {
        int idx = indexOf( o );
        if( idx < 0 ) {
            return false;
        }
        doRemove( idx );
        return true;
    }

    
    public int indexOf( Object o ) {
        final int cap = mBuffer.length;
        if( o == null ) {
            for( int i = 0; i < mSize; i++ ) {
                if( mBuffer[ ( mPos + i ) % cap ] == null ) {
                    return i;
                }
            }
        } else {
            for( int i = 0; i < mSize; i++ ) {
                if( o.equals( mBuffer[ ( mPos + i ) % cap ] ) ) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    
    public int lastIndexOf( Object o ) {
        final int cap = mBuffer.length;
        if( o == null ) {
            for( int i = mSize - 1; i >= 0; i-- ) {
                if( mBuffer[ ( mPos + i ) % cap ] == null ) {
                    return i;
                }
            }
        } else {
           for( int i = mSize - 1; i >= 0; i-- ) {
               if( o.equals( mBuffer[ ( mPos + i ) % cap ] ) ) {
                   return i;
               }
           }
        }
        
        return -1;
    }
    
    
    public E set( int index, E o ) {
        if( index < 0 || index >= mSize ) {
            throw new IndexOutOfBoundsException();
        }
        
        mModCount++;
        index = (index + mPos) % mBuffer.length;
        Object ret = mBuffer[index];
        mBuffer[index] = o;
        return (E)ret;
    }
    
    
    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }
    
    
    public int size() {
        return mSize;
    }
    
    
    public Iterator<E> iterator() {
        return new Iter();
    }

    
    public ListIterator<E> listIterator() {
        return new Iter();
    }
    
    
    public ListIterator<E> listIterator( int idx ) {
        if( idx < 0 || idx >= mSize ) { 
            throw new ArrayIndexOutOfBoundsException();
        }
        return new Iter( idx );
    }

    /**
     * Currently unsupported. 
     * 
     * @throws UnsupportedOperationException
     */
    public List<E> subList( int n0, int n1 ) {
        throw new UnsupportedOperationException();
    }
    
    
    public boolean ensureCapacity( int minCap ) {
        final int oldCap = mBuffer.length;
        if( minCap <= oldCap ) {
            return true;
        } else if( minCap > mMaxCap ) {
            return false;
        }
        
        int newCap = ( oldCap * 3 ) / 2 + 1;
        if( newCap > mMaxCap ) {
            newCap = mMaxCap;
        } else if( newCap < minCap ) {
            newCap = minCap;
        }
        
        mModCount++;
        
        Object[] old = mBuffer;
        Object[] arr = new Object[ newCap ];
        int oldFirst = mPos;
        int oldLast  = mPos + mSize;
        
        if( oldLast <= oldCap ) {
            // Contents in previous buffer do not wrap around.
            System.arraycopy( old, oldFirst, arr, 0, oldLast - oldFirst );
        } else {
            // Contents in previous buffer do wrap around.
            System.arraycopy( old, oldFirst, arr, 0, oldCap - oldFirst );
            System.arraycopy( old, 0, arr, oldCap - oldFirst, oldLast - oldCap );
        }
        
        mBuffer = arr;
        mPos    = 0;
        return true;
    }

    
    public String toString() {
        Iterator<E> it = iterator();
        if( !it.hasNext() ) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while( true ) {
            E e = it.next();
            sb.append( e == this ? "(this Collection)" : e );
            if ( !it.hasNext() ) {
                return sb.append( ']' ).toString();
            }
            sb.append( ", " );
        }
    }
    
    
    
    private void doAdd( int offset, Object o ) {
        mModCount++;
        
        if( offset == mSize ) {
            //Add new element to end.
            mBuffer[ (mPos + mSize++) % mBuffer.length ] = o;
            return;

        } else if( offset == 0 ) {
            //Add new element to front.
            mPos = (mPos + mBuffer.length - 1) % mBuffer.length;
            mBuffer[mPos] = o;
            mSize++;
            return;
        }
        
        int p0 = mPos;
        int p1 = ( p0 + offset ) % mBuffer.length;
        int p2 = ( p0 + mSize++ ) % mBuffer.length;
        
        if( p1 > p0 ) {
            if( p2 > p0 ) {
                //Shift right side of buffer to right.
                shiftRight( p1, p2 );
            } else {
                //Shift left side of buffer to right...
                shiftRight( 0, p2 );
                //Move last element to first position...
                mBuffer[0] = mBuffer[mBuffer.length-1];
                //Shift right side of buffer to right.
                shiftRight( p1, mBuffer.length - 1 );
            }
        } else {
            //Shift left side of buffer right.
            shiftRight( p1, p2 );
        }
        
        mBuffer[p1] = o;
    }
    

    private void doRemove( int offset ) {
        mModCount++;
        if( offset == mSize ) {
            //Remove last element.
            mBuffer[ (mPos + --mSize) % mBuffer.length ] = null;
            return;
            
        } else if( offset == 0 ) {
            //Remove first element.
            mBuffer[mPos] = null;
            mPos = (mPos + 1) % mBuffer.length;
            mSize--;
            return;
        }
        
        int p0 = mPos;
        int p1 = (p0 + offset) % mBuffer.length;
        int p2 = (p0 + mSize--) % mBuffer.length;
        
        if( p1 > p0 ) {
            if( p2 > p0 ) {
                //Subset from removal point to end of buffer is continuous.
                shiftLeft( p1 + 1, p2 );
            } else {
                //Shift to end of buffer.
                shiftLeft( p1 + 1, mBuffer.length );
                if( p2 > 0 ) {
                    //Move left-most element in buffer to end, then shift left segment of buffer.
                    mBuffer[mBuffer.length-1] = mBuffer[0];
                    shiftLeft( 1, p2 );
                }
            }
        } else {
            //Subset from removal point to end of buffer must be continuous.
            shiftLeft( p1 + 1, p2 );
        }
    }
    
    
    private void shiftLeft( int n0, int n1 ) {
        for( int i = n0; i < n1; i++ ) {
            mBuffer[i-1] = mBuffer[i];
        }
        mBuffer[n1-1] = null;
    }
    
    
    private void shiftRight( int n0, int n1 ) {
        for( int i = n1 - 1; i >= n0; i-- ) {
            mBuffer[i+1] = mBuffer[i];
        }
        mBuffer[n0] = null;
    }
    
    
    
    private final class Iter implements ListIterator<E> {
        
        private int mIterMod = mModCount;
        private int mOffset = 0;
        private int mPrev = -1;
        
        
        Iter() {}
        
        
        Iter( int idx ) {
            mOffset = idx;
        }
        
        
        
        public void add( E o ) {
            assertUnmodified();
            if( mOffset < 0 || mOffset > mSize ) {
                throw new IllegalStateException();
            }
            if( mSize == mBuffer.length && !ensureCapacity( mSize + 1 ) ) {
                throw new IllegalStateException( "Maximum capacity exceeded." );
            }
            doAdd( mOffset++, o );
            mIterMod = mModCount;
        }

        
        public boolean hasNext() {
            return mOffset < mSize;
        }
        
        
        public boolean hasPrevious() {
            return mOffset > 0;
        }
        
        
        public E next() {
            assertUnmodified();
            if( mOffset >= mSize ) {
                throw new NoSuchElementException();
            }
            mPrev = mOffset++;
            return (E)mBuffer[ (mPos + mPrev) % mBuffer.length ];
        }
        
        
        public E previous() {
            assertUnmodified();
            if( mOffset <= 0 ) {
                throw new NoSuchElementException();
            }
            mPrev = --mOffset;
            return (E)mBuffer[ (mPos + mPrev) % mBuffer.length ];
        }
            
        
        public int nextIndex() {
            return mOffset;
        }
        
        
        public int previousIndex() {
            return mOffset - 1;
        }

        
        public void remove() {
            if( mPrev < 0 ) {
                throw new IllegalStateException( "No call to mNext() or present()." );
            }
            assertUnmodified();
            doRemove( mPrev );
            if( mPrev < mOffset ) {
                mOffset--;
            }
            
            mPrev = -1;
            mIterMod = mModCount;
        }
        

        public void set( E o ) {
            if( mPrev < 0 ) {
                throw new IllegalStateException( "No prior call to mNext() or previous()." );
            }
            assertUnmodified();
            RingList.this.set( mPrev, o );
            mIterMod = mModCount;
        }
        
        
        private void assertUnmodified() {
            if( mIterMod != mModCount ) {
                throw new ConcurrentModificationException();
            }
        }
        
    }
    
}
