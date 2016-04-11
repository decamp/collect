/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;

/**
 * You shouldn't use PublicList. Pretend it's not here.
 * <p>
 * Like ArrayList, but with public access to internal fields. Needless to say, this
 * class is extremely unsafe and the user can easily place the class into an 
 * inconsistent state if the mSize field is altered. However, it is very 
 * convenient as a resizable array. 
 * <p>
 * To make the class even less safe, it contains methods like <code>clearFast()</code> and
 * <code>removeFast()</code>, which may disrupt the list ordering and leave lingering 
 * references that will prevent objects from being finalized.
 * <p>
 * On the bright side, there are only two fields: <code>mArr</code> and <code>mSize</code>.
 * As long as never give <code>mSize</code> a mValue that is out-of-bounds for <code>mArr</code>,
 * you'll probably be fine.
 * 
 * @author Philip DeCamp
 */
public class PublicList<T> extends AbstractCollection<T> implements List<T> {
    
    
    public static <T> PublicList<T> create( Class<T> clazz ) {
        return create( clazz, 10 );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> PublicList<T> create( Class<T> clazz, int capacity ) {
        return new PublicList<T>( (T[])java.lang.reflect.Array.newInstance( clazz, capacity ) );
    }
    
    
    
    public T[] mArr;
    public int mSize;
    
   
    public PublicList( T[] arrayRef ) {
        mArr  = arrayRef;
        mSize = 0;
    }
    
    
    public PublicList( PublicList<T> copy ) {     
        mArr  = copy.mArr.clone();
        mSize = copy.mSize;
    }
    
    
    
    public boolean add( T t ) {
        ensureCapacity( mSize + 1 );
        mArr[mSize++] = t;
        return true;
    }

    
    public void add( int idx, T item ) {
        if( idx > mSize || idx < 0 ) {
            throw new IndexOutOfBoundsException( "Index: " + idx + ", Size: " + mSize );
        }
        ensureCapacity( mSize + 1 );
        
        if( idx == mSize ) {
            mArr[mSize++] = item;
        } else {
            System.arraycopy( mArr, idx, mArr, idx + 1, mSize++ - idx );
            mArr[idx] = item;
        }
    }
    
    
    public boolean addAll( Collection<? extends T> items ) {
        boolean ret = false;
        int len = items.size();
        ensureCapacity( mSize + len );
        for( T item: items ) {
            mArr[mSize++] = item;
            ret = true;
        }
        return ret;
    }

    
    public boolean addAll( int idx, Collection<? extends T> items ) {
        boolean ret = false;
        int len = items.size();
        ensureCapacity( mSize + len );
        for( T item: items ) {
            add( idx++, item );
            ret = true;
        }
        
        return ret;
    }
    
    
    public void addArray( T[] arr, int off, int len ) {
        ensureCapacity( mSize + len );
        System.arraycopy( arr, off, mArr, mSize, len );
        mSize += len;
    }
    

    public void clear() {
        int s = mSize;
        if( s <= 0 ) {
            return;
        }
        
        for( int i = 0; i < mSize; i++ ) {
            mArr[i] = null;
        }
        mSize = 0;
    }

    /**
     * Clears collection WITHOUT nullifying references. May cause memory leaks. 
     */
    public void clearFast() {
        mSize = 0;
    }
    
    
    public boolean contains( Object item ) {
        return indexOf( item ) >= 0;
    }
    
    
    public void ensureCapacity( int minCap ) {
        final int oldCap = mArr.length;
        if( minCap <= oldCap ) {
            return;
        }
        
        int newCap = ( oldCap * 3 ) / 2 + 1;
        if( newCap < minCap ) {
            newCap = minCap;
        }
        
        mArr = Arrays.copyOf( mArr, newCap );
    }

    
    public T get( int idx ) {
        return mArr[idx];
    }
    
    
    public int indexOf( Object o ) {
        if( o == null ) {
            for( int i = 0; i < mSize; i++ ) {
                if( mArr[i] == null ) {
                    return i;
                }
            }
        } else {
            for( int i = 0; i < mSize; i++ ) {
                if( o.equals( mArr[i] ) ) {
                    return i;
                }
            }
        }
        
        return -1;
    }

    
    public boolean isEmpty() {
        return mSize == 0;
    }
    
    
    public Iterator<T> iterator() {
        return new Iter();
    }

    
    
    public int lastIndexOf( Object o ) {
        if( o == null ) {
            for( int i = mSize - 1; i >= 0; i-- ) {
                if( mArr[i] == null ) {
                    return i;
                }
            }
        } else {
           for( int i = mSize - 1; i >= 0; i-- ) {
               if( o.equals( mArr[i] ) ) {
                   return i;
               }
           }
        }
        
        return -1;
    }

    
    public ListIterator<T> listIterator() {
        return new Iter();
    }
    
    
    public ListIterator<T> listIterator( int idx ) {
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
    public List<T> subList( int n0, int n1 ) {
        throw new UnsupportedOperationException();
    }
    
    
    public T remove( int idx ) {
        if( idx >= mSize ) {
            throw new IndexOutOfBoundsException();

        } else if( idx == mSize - 1 ) {
            mSize--;
            T ret = mArr[idx];
            mArr[idx] = null;
            return ret;
        
        } else {
            T ret = mArr[idx];
            System.arraycopy( mArr, idx + 1, mArr, idx, mSize - idx - 1 );
            mArr[--mSize] = null;
            return ret;
        }
    }
    
    
    public boolean remove( Object item ) {
        int idx = indexOf( item );
        if( idx >= 0 ) {
            remove( idx );
            return true;
        }
        return false;
    }
    
    /**
     * Removes item WITHOUT preserving order of list.
     * Does not nullify refferences and may cause memory leaks.
     */
    public void removeFast( int idx ) {
        if( idx >= mSize ) {
            throw new IndexOutOfBoundsException();
        }
        mArr[idx] = mArr[--mSize];
    }

    /**
     * Removes item WITHOUT preserving order of list.
     * Does not nullify references and may cause memory leaks.
     */
    public boolean removeFast( T t ) {
        int idx = indexOf( t );
        if( idx >= 0 ) {
            removeFast( idx );
            return true;
        } else {
            return false;
        }
    }
    
    
    public T set( int index, T item ) {
        if( index >= mSize || index < 0 ) {
            throw new IndexOutOfBoundsException( "Index: " + index + ", Size: " + mSize );
        }
        T ret = mArr[index];
        mArr[index] = item;
        return ret;
    }
    
    
    public int size() {
        return mSize;
    }

    
    public T[] toArray() {
        return Arrays.copyOf( mArr, mSize );
    }

    @SuppressWarnings( "unchecked" )
    public <S> S[] toArray( S[] out ) {
        if( out.length < mSize ) {
            out = (S[])java.lang.reflect.Array.newInstance( out.getClass().getComponentType(), mSize );
        }
        
        System.arraycopy( mArr, 0, out, 0, mSize );
        if( out.length > mSize ) {
            out[mSize] = null;
        }
        
        return out;
    }
    
    
    
    private final class Iter implements ListIterator<T> {
        
        private int mOffset = 0;
        private int mPrev = -1;
        
        
        Iter() {}
        
        
        Iter( int idx ) {
            mOffset = idx;
        }
        
        
        
        public void add( T item ) {
            if( mOffset < 0 || mOffset > mSize ) {
                throw new IllegalStateException();
            }
            ensureCapacity( mSize + 1 );
            PublicList.this.add( mOffset++, item );
        }

        
        public boolean hasNext() {
            return mOffset < mSize;
        }
        
        
        public boolean hasPrevious() {
            return mOffset > 0;
        }
        
        
        public T next() {
            if( mOffset >= mSize ) {
                throw new NoSuchElementException();
            }
            mPrev = mOffset++;
            return mArr[mPrev];
        }
        
        
        public T previous() {
            if( mOffset <= 0 ) {
                throw new NoSuchElementException();
            }
            mPrev = --mOffset;
            return mArr[mPrev];
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
            PublicList.this.remove( mPrev );
            if( mPrev < mOffset ) {
                mOffset--;
            }
            
            mPrev = -1;
        }
        

        public void set( T item ) {
            if( mPrev < 0 ) {
                throw new IllegalStateException( "No prior call to mNext() or previous()." );
            }
            PublicList.this.set( mPrev, item );
        }
        
    }



    @Deprecated public static <T> PublicList<T> newInstance( Class<T> clazz ) {
        return newInstance( clazz, 10 );
    }
    
 
    @Deprecated public static <T> PublicList<T> newInstance( Class<T> clazz, int capacity ) {
        return new PublicList<T>( (T[])java.lang.reflect.Array.newInstance( clazz, capacity ) );
    }

}
