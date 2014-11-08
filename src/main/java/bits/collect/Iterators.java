/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Provides a few methods for creating simple iterators.
 * 
 * @author Philip DeCamp
 */
public class Iterators {


    @SuppressWarnings( "unchecked" )
    public static <E> Iterator<E> empty() {
        return EMPTY_ITERATOR;
    }


    public static <E> Iterator<E> wrapObject( E obj ) {
        return new SingleIterator<E>( obj );
    }

    @SuppressWarnings( "unchecked" )
    public static <E> Iterator<E> wrapArray( E[] arr ) {
        if( arr == null ) {
            return EMPTY_ITERATOR;
        }
        return new ArrayIterator<E>( arr );
    }

    @SuppressWarnings( "unchecked" )
    public static <E> Iterator<E> wrapArray( E[] arr, int off, int len ) {
        if( arr == null && len == 0 ) {
            return EMPTY_ITERATOR;
        }
        return new ArrayIterator<E>( arr, off, len );
    }


    public static <E> Iterator<E> unmodifiable( final Iterator<E> it ) {
        return new Iterator<E>() {
            public boolean hasNext() {
                return it.hasNext();
            }

            public E next() {
                return it.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    private static class ArrayIterator<E> implements Iterator<E> {
        private final E[] mElements;
        private final int mSize;
        private int mIndex;

        public ArrayIterator( E[] elements ) {
            mElements = elements;
            mIndex = 0;
            mSize = elements.length;
        }

        public ArrayIterator( E[] elements, int off, int len ) {
            mElements = elements;
            mIndex = off;
            mSize = len + off;

            if( off < 0 || len < 0 || off + len > elements.length ) {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
        

        public boolean hasNext() {
            return mIndex < mSize;
        }

        public E next() {
            if( mIndex >= mSize ) {
                throw new NoSuchElementException();
            }
            return mElements[mIndex++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }


    private static class SingleIterator<E> implements Iterator<E> {

        private final E mElement;
        private boolean mHasNext = true;

        SingleIterator( E element ) {
            mElement = element;
        }


        public boolean hasNext() {
            return mHasNext;
        }

        public E next() {
            if( !mHasNext ) {
                throw new NoSuchElementException();
            }

            mHasNext = false;
            return mElement;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    @SuppressWarnings( "rawtypes" )
    private static Iterator EMPTY_ITERATOR = new Iterator() {

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    };


    @Deprecated public static <E> Iterator<E> unmodifiableIterator( Iterable<E> iterable ) {
        final Iterator<E> it = iterable.iterator();
        return new Iterator<E>() {
            public boolean hasNext() {
                return it.hasNext();
            }

            public E next() {
                return it.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
