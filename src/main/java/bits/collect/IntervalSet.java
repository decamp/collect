/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;


/**
 * A balanced binary tree that stores intervals. IntervalSet is backed by an
 * IntervalMap. Unlike conventional Sets, IntervalSet will store multiple
 * elements that are equivalent, and will even store the same instance multiple
 * times.
 *
 * <p>Intervals are ordered first by minimum mValue, then maximum, then the order in
 * which they're added to the map.
 *
 * <p>An interval tree works by keeping intervals sorted in ascending order by
 * start_time, but each node also maintains the maximum end_time of all
 * intervals in the subtree rooted at the node. Therefore, it can quickly be
 * determined whether a search should proceed in a subtree or whether there is
 * no chance of intersection. Mostly based on CLRS (Introduction to Algorithms)
 * and <a href="http://en.wikipedia.org/wiki/Interval_tree#Augmented_tree"> augmented trees</a>
 *
 * <p>Bug: lastByMax() does not resolve ties in a defined way. That is,
 * if there are multiple intervals in the map with the same max mValue, IntervalMap makes
 * no guarantees about which will be returned.  This should eventually fixed to return
 * the LAST interval with the greatest max mValue.  This ambiguity extends to IntervalSet
 * and LongIntervalMap.
 *
 * @param <E> The element type.
 * @author Philip DeCamp
 */
public class IntervalSet<E> implements Set<E> {

    private final IntervalMap<E, E> mMap;
    private final Set<E>            mKeyView;


    public IntervalSet( IntervalComparator<? super E> comp ) {
        this( comp, null );
    }


    public IntervalSet( IntervalComparator<? super E> comp, Collection<? extends E> values ) {
        mMap = new IntervalMap<E, E>( comp );
        mKeyView = mMap.keySet();

        if( values != null ) {
            addAll( values );
        }
    }


    /**
     * &nbsp
     *
     * @param e element
     * @return true if set was modified by this call. Unless element is
     * <tt>null</tt>, the return mValue will be <tt>true</tt>.
     */
    public boolean add( E e ) {
        if( e == null ) {
            return false;
        }
        mMap.put( e, e );
        return true;
    }


    public boolean addAll( Collection<? extends E> elements ) {
        boolean ret = false;
        for( E elem : elements ) {
            if( elem == null ) {
                continue;
            }
            mMap.put( elem, elem );
            ret = true;
        }
        return ret;
    }


    public boolean retainAll( Collection<?> c ) {
        boolean ret = false;
        Iterator<E> iter = iterator();
        while( iter.hasNext() ) {
            if( !c.contains( iter.next() ) ) {
                iter.remove();
                ret = true;
            }
        }
        return ret;
    }


    public boolean isEmpty() {
        return mMap.isEmpty();
    }


    public int size() {
        return mMap.size();
    }


    public void clear() {
        mMap.clear();
    }


    public Iterator<E> iterator() {
        return mKeyView.iterator();
    }


    public Iterator<E> descendingIterator() {
        return mMap.descendingKeySet().iterator();
    }


    /**
     * &nbsp
     *
     * @param e An interval
     * @return true iff this set contains an interval, <code>g</code>, such that
     * <code>g</code> has equivalent min and max values as
     * <code>e</code> AND <code>e.equals( g )</code>.
     */
    public boolean contains( Object e ) {
        return mMap.containsKey( e );
    }

    /**
     * @param e An interval
     * @return true iff this set contains an interval with equivalent min and
     * max values as <i>e</i>. Note that this method may be true even if
     * this IntervalSet does not contain any interval <code>g</code>
     * such that <code>g.equals( e ) == true</code>.
     */
    public boolean containsEquiv( Object e ) {
        return mMap.containsEquivKey( e );
    }

    /**
     * &nbsp
     *
     * @param elements Collection of elements
     * @return true iff all elements are contained by this set.
     */
    public boolean containsAll( Collection<?> elements ) {
        for( Object e : elements ) {
            if( !mMap.containsKey( e ) ) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param e An interval
     * @return true iff this Set contains an interval that intersects
     * <i>mKey</i>.
     */
    public boolean containsIntersection( Object e ) {
        return mMap.containsIntersectionKey( e );
    }

    /**
     * @param e An interval
     * @return true iff this map contains an interval that is a subset of
     * <i>mKey</i>.
     */
    public boolean containsSubset( Object e ) {
        return mMap.containsSubsetKey( e );
    }

    /**
     * @param e An interval
     * @return true iff this map contains an interval that is a superset of
     * <i>mKey</i>.
     */
    public boolean containsSuperset( Object e ) {
        return mMap.containsSupersetKey( e );
    }

    /**
     * @param e An interval
     * @return true iff the union of intervals in this Set is a superset of
     * <i>e</i>.
     */
    public boolean containsSupersetUnion( Object e ) {
        return mMap.containsSupersetUnion( e );
    }


    /**
     * Removes the the first element with interval equivalent to <tt>e</tt>.
     *
     * @param e interval
     * @return true iff Set was modified by this call.
     */
    public boolean remove( Object e ) {
        return mMap.remove( e ) != null;
    }

    /**
     * Calls remove(element) for each element in elements.
     *
     * @param elements element
     * @return true iff call modified this map.
     */
    public boolean removeAll( Collection<?> elements ) {
        boolean ret = false;
        for( Object obj : elements ) {
            ret |= remove( obj );
        }
        return ret;
    }

    /**
     * Same as remove(e), except that the element that gets removed is returned
     * instead of a boolean mValue.
     *
     * @param e interval
     * @return the mValue that is removed by this call
     */
    public E removeEquiv( Object e ) {
        return mMap.remove( e );
    }

    /**
     * Removes the first element that intersects <b>e</b>.
     *
     * @param e element
     */
    public E removeIntersection( Object e ) {
        return mMap.removeIntersection( e );
    }

    /**
     * Removes the mapping for the first interval that's a superset of <b>e</b>.
     *
     * @param e interval
     * @return the mValue that is removed by this call.
     */
    public E removeSuperset( Object e ) {
        return mMap.removeSuperset( e );
    }

    /**
     * Removes the mapping for the first interval that's a subset of <b>e</b>.
     *
     * @param e An interval
     * @return the mValue that is removed by this call.
     */
    public E removeSubset( Object e ) {
        return mMap.removeSubset( e );
    }


    public E first() {
        return mMap.firstKey();
    }


    public E last() {
        return mMap.lastKey();
    }

    /**
     * Finds the highest element that is lower than <tt>e</tt>. If there are
     * elements <tt>e0...eN</tt> in this IntervalSet such that
     * <tt>e.equals(eM)</tt>, then this method will return the element
     * immediately before the first such elements.
     *
     * @param e Input element
     * @return Lower element
     */
    public E lower( E e ) {
        return mMap.lowerKey( e );
    }

    /**
     * Finds the lowest element that is higher than <tt>e</tt>. If there are
     * elements <tt>e0...eN</tt> in this IntervalSet such that
     * <tt>e.equals(eM)</tt>, then this method will return the element
     * immediately after the last such elements.
     *
     * @param e Input element
     * @return Higher element, or null.
     */
    public E higher( E e ) {
        return mMap.higherKey( e );
    }

    /**
     * Finds the highest element that is lower than or equivalent to <tt>e</tt>.
     * If there are multiple elements with equivalent intervals, the last such
     * element will be returned. If there are elements <tt>e0...eN</tt> in this
     * IntervalSet such that they not only have equivalent intervals, but also
     * <tt>e.equals(eM)</tt>, then the highest such element will be returned.
     *
     * @param e Input element
     * @return Equivalent or lower element.
     */
    public E floor( E e ) {
        return mMap.floorKey( e );
    }

    /**
     * Finds the lowest element that is higher than or equivalent to <tt>e</tt>.
     * If there are multiple equivalent elements, the first such element will be
     * returned. If there are elements <tt>e0...eN</tt> in this IntervalSet such
     * that they not only have equivalent intervals, but also
     * <tt>e.equals(eM)</tt>, then the lowest such element will be returned.
     *
     * @param e Input element
     * @return Equivalent or higher element.
     */
    public E ceiling( E e ) {
        return mMap.ceilingKey( e );
    }

    /**
     * @return the element with the greastest max interval mValue.
     */
    public E lastByMax() {
        return mMap.lastKeyByMax();
    }


    public E firstEquiv( E e ) {
        return mMap.firstEquivKey( e );
    }


    public E lastEquiv( E e ) {
        return mMap.lastEquivKey( e );
    }


    public E firstIntersection( E e ) {
        return mMap.firstIntersectionKey( e );
    }


    public E lastIntersection( E e ) {
        return mMap.lastIntersectionKey( e );
    }


    public E firstSuperset( E e ) {
        return mMap.firstSupersetKey( e );
    }


    public E lastSuperset( E e ) {
        return mMap.lastSupersetKey( e );
    }


    public E firstSubset( E e ) {
        return mMap.firstSubsetKey( e );
    }


    public E lastSubset( E e ) {
        return mMap.lastSubsetKey( e );
    }


    /**
     * @return a Set view of the elements in this Set in descending order.
     */
    public Set<E> descendingSet() {
        return mMap.descendingKeySet();
    }

    /**
     * Returns a Set view of the elements contained in this Set that are
     * equivalent to <tt>e</tt>
     *
     * @param e interval
     * @return a set view of intervals
     */
    public Set<E> equivSet( E e ) {
        return mMap.equivKeySet( e );
    }

    /**
     * Returns a Set view of the elements contained in this Set that are
     * equivalent to <tt>e</tt> in descending order.
     *
     * @param e interval
     * @return a set view of intervals
     */
    public Set<E> descendingEquivSet( E e ) {
        return mMap.descendingEquivKeySet( e );
    }

    /**
     * Returns a Set view of the elements contained in this Set that intersect
     * <tt>e</tt>
     *
     * @param e interval
     * @return a set view of intervals
     */
    public Set<E> intersectionSet( E e ) {
        return mMap.intersectionKeySet( e );
    }

    /**
     * Returns a Set view of the elements contained in this Set that intersect
     * <tt>e</tt> in descending order.
     *
     * @param e element
     * @return a set view of intervals
     */
    public Set<E> descendingIntersectionSet( E e ) {
        return mMap.descendingIntersectionKeySet( e );
    }

    /**
     * Returns a Set view of the elements contained in this Set that are
     * supersets of <tt>e</tt>
     *
     * @param e element
     * @return a set view of intervals
     */
    public Set<E> supersetSet( E e ) {
        return mMap.supersetKeySet( e );
    }

    /**
     * Returns a set view of the elements contained in this Set that are
     * supersets of <tt>e</tt> in descending order.
     *
     * @param e interval
     * @return a set view of intervals
     */
    public Set<E> descendingSupersetSet( E e ) {
        return mMap.descendingSupersetKeySet( e );
    }

    /**
     * Returns a Set view of the elements contained in this Set that are subsets
     * of <tt>e</tt>
     *
     * @param item interval
     * @return a set view of intervals
     */
    public Set<E> subsetSet( E item ) {
        return mMap.subsetKeySet( item );
    }

    /**
     * Returns a Set view of the elements contained in this Set that are subsets
     * of <tt>e</tt> in descending order.
     *
     * @param e interval
     * @return a set view of intervals
     */
    public Set<E> descendingSubsetSet( E e ) {
        return mMap.descendingSubsetKeySet( e );
    }


    public Object[] toArray() {
        return mKeyView.toArray();
    }


    public <T> T[] toArray( T[] a ) {
        return mKeyView.toArray( a );
    }



    /* ************************************************************
     * Package private debugging
     * ***********************************************************
     */

    boolean validateMaxStops() {
        return mMap.validateMaxStops();
    }

}
