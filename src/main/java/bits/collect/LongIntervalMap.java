/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */
package bits.collect;

import java.util.*;


/**
 * LongIntervalMap is just a version of IntervalMap with convenience methods for
 * placing two long values into an array. Otherwise, LongIntervalMap does not
 * check the input any more than IntervalMap:
 * <p/>
 * 1) It returns the reference to the actual keys (not safe copies) and altering
 * those keys while they are in the map has undefined results.
 * <p/>
 * 2) It does not check that each <b>min</b> parameter is actually
 * less-than-or-equal-to the <b>max</b> parameter. If, someday, I decide this is
 * worth checking for, it would be a check in IntervalComparator, not
 * LongIntervalComparator.
 * <p/>
 * The default IntervalComparator, HALF_OPEN_WITH_ZERO_LENGTH_COMP, uses
 * half-open intervals, but it allows for intervals with equal endpoints to
 * still contain the endpoint, thus permitting the use of zero-length intervals
 * (an interval's <b>min</b> may equals the interval's <b>max</b>).
 *
 * @param <V> The value type to be associated with an interval
 * @author Philip DeCamp
 */
public class LongIntervalMap<V> extends IntervalMap<long[], V> {


    public static final IntervalComparator<long[]> HALF_OPEN_WITH_ZERO_LENGTH_COMP = new IntervalComparator<long[]>() {
        public int compareMins( long[] a, long[] b ) {
            if( a[0] < b[0] ) {
                return -1;
            }
            if( a[0] > b[0] ) {
                return 1;
            }
            return 0;
        }

        public int compareMaxes( long[] a, long[] b ) {
            if( a[1] < b[1] ) {
                return -1;
            }
            if( a[1] > b[1] ) {
                return 1;
            }

            return (a[0] == a[1] ? 1 : 0) - (b[0] == b[1] ? 1 : 0);
        }

        public int compareMinToMax( long[] a, long[] b ) {
            if( a[0] < b[1] ) {
                return -1;
            }
            if( a[0] > b[1] ) {
                return 1;
            }

            return (b[0] == b[1] ? -1 : 0);
        }
    };


    public LongIntervalMap() {
        super( HALF_OPEN_WITH_ZERO_LENGTH_COMP );
    }


    public LongIntervalMap( Map<long[], ? extends V> map ) {
        super( HALF_OPEN_WITH_ZERO_LENGTH_COMP, map );
    }


    public LongIntervalMap( IntervalComparator<long[]> comp ) {
        super( comp );
    }


    public LongIntervalMap( IntervalComparator<long[]> comp, Map<long[], ? extends V> map ) {
        super( comp, map );
    }


    /**
     * @param min Min endpoint of key interval
     * @param max Max endpiont of key interval
     * @return true iff this map contains an interval with equivalent min and
     * max values as <i>key</i>.
     */
    public boolean containsKey( long min, long max ) {
        return super.containsKey( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return true iff this map contains an interval that is a superset of
     * <i>key</i>.
     */
    public boolean containsSupersetKey( long min, long max ) {
        return super.containsSupersetKey( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return true iff the union of intervals in this map is a superset of
     * <i>key</i>.
     */
    public boolean containsSupersetUnion( long min, long max ) {
        return super.containsSupersetUnion( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return true iff this map contains an interval that intersects
     * <i>key</i>.
     */
    public boolean containsIntersectionKey( long min, long max ) {
        return super.containsIntersectionKey( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return value mapped to first interval equivalent to <tt>key</tt>
     * contained in this map.
     */
    public V get( long min, long max ) {
        return super.get( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return value mapped to first interval intersecting <tt>key</tt>
     * contained in this map.
     */
    public V getIntersection( long min, long max ) {
        return super.getIntersection( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return value mapped to first interval that is a superset of <tt>key</tt>
     * contained in this map.
     */
    public V getSuperset( long min, long max ) {
        return super.getSuperset( new long[]{ min, max } );
    }

    /**
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return value mapped to first interval that is a subset of <tt>key</tt>
     * contained in this map.
     */
    public V getSubset( long min, long max ) {
        return super.getSubset( new long[]{ min, max } );
    }

    /**
     * @param min   Min endpoint of key interval
     * @param max   Max endpoint of key interval
     * @param value Arbitrary value
     * @return null (Existing mappings are never overwritten by calls to
     * <i>put()</i>)
     */
    public V put( long min, long max, V value ) {
        return super.put( new long[]{ min, max }, value );
    }


    public Map.Entry<long[], V> lowerEntry( long min, long max ) {
        return super.lowerEntry( new long[]{ min, max } );
    }


    public long[] lowerKey( long min, long max ) {
        return super.lowerKey( new long[]{ min, max } );
    }


    public Map.Entry<long[], V> higherEntry( long min, long max ) {
        return super.higherEntry( new long[]{ min, max } );
    }


    public long[] higherKey( long min, long max ) {
        return super.higherKey( new long[]{ min, max } );
    }


    public Map.Entry<long[], V> ceilingEntry( long min, long max ) {
        return super.ceilingEntry( new long[]{ min, max } );
    }


    public long[] ceilingKey( long min, long max ) {
        return super.ceilingKey( new long[]{ min, max } );
    }


    public Map.Entry<long[], V> floorEntry( long min, long max ) {
        return super.floorEntry( new long[]{ min, max } );
    }


    public long[] floorKey( long min, long max ) {
        return super.floorKey( new long[]{ min, max } );
    }


    /**
     * Removes the mapping for the first interval that is equivalent to
     * <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return the value that is removed by this call
     */
    public V remove( long min, long max ) {
        return super.remove( new long[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that intersects <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return the value that is removed by this call.
     */
    public V removeIntersection( long min, long max ) {
        return super.removeIntersection( new long[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that's a superset of
     * <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return the value that is removed by this call.
     */
    public V removeSuperset( long min, long max ) {
        return super.removeSuperset( new long[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that's a subset of
     * <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return the value that is removed by this call.
     */
    public V removeSubset( long min, long max ) {
        return super.removeSubset( new long[]{ min, max } );
    }


    /**
     * Returns a set view of the intervals contained in this map that are
     * equivalent to <tt>key</tt>
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     */
    public Set<long[]> equivKeySet( long min, long max ) {
        return super.equivKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * equivalent to <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> descendingEquivKeySet( long min, long max ) {
        return super.descendingEquivKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>key</tt>
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> intersectionKeySet( long min, long max ) {
        return super.intersectionKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> descendingIntersectionKeySet( long min, long max ) {
        return super.descendingIntersectionKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>key</tt>
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> supersetKeySet( long min, long max ) {
        return super.supersetKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> descendingSupersetKeySet( long min, long max ) {
        return super.descendingSupersetKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>key</tt>
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> subsetKeySet( long min, long max ) {
        return super.subsetKeySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<long[]> descendingSubsetKeySet( long min, long max ) {
        return super.descendingSubsetKeySet( new long[]{ min, max } );
    }


    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> equivValues( long min, long max ) {
        return super.equivValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingEquivValues( long min, long max ) {
        return super.descendingEquivValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> intersectionValues( long min, long max ) {
        return super.intersectionValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingIntersectionValues( long min, long max ) {
        return super.descendingIntersectionValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> supersetValues( long min, long max ) {
        return super.supersetValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSupersetValues( long min, long max ) {
        return super.descendingSupersetValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are subsets of <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> subsetValues( long min, long max ) {
        return super.subsetValues( new long[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSubsetValues( long min, long max ) {
        return super.descendingSubsetValues( new long[]{ min, max } );
    }


    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> equivEntrySet( long min, long max ) {
        return super.equivEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> descendingEquivEntrySet( long min, long max ) {
        return super.descendingEquivEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> intersectionEntrySet( long min, long max ) {
        return super.intersectionEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> descendingIntersectionEntrySet( long min, long max ) {
        return super.descendingIntersectionEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> supersetEntrySet( long min, long max ) {
        return super.supersetEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> descendingSupersetEntrySet( long min, long max ) {
        return super.descendingSupersetEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>key</tt>.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> subsetEntrySet( long min, long max ) {
        return super.subsetEntrySet( new long[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>key</tt> in descending order.
     *
     * @param min Min endpoint of key interval
     * @param max Max endpoint of key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<long[], V>> descendingSubsetEntrySet( long min, long max ) {
        return super.descendingSubsetEntrySet( new long[]{ min, max } );
    }


    public Set<long[]> intersectingKeySet( long min, long max ) {
        return super.intersectionKeySet( new long[]{ min, max } );
    }

}
