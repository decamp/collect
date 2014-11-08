/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;


/**
 * DoubleIntervalMap is just a version of IntervalMap with convenience methods for
 * placing two double values into an array. Otherwise, DoubleIntervalMap does not
 * validate input beyond what IntervalMap performs.
 * <p>
 * 1) It returns the reference to the actual keys (not safe copies) and altering
 * those keys while they are in the map has undefined results.
 * <p>
 * 2) It does not check that each <b>min</b> parameter is actually
 * less-than-or-equal-to the <b>max</b> parameter. If I someday decide this is
 * worth checking for, it would be a check in IntervalComparator, not
 * doubleIntervalComparator.
 * <p>
 * With the default IntervalComparator (doubleIntervalMap.DOUBLE_PAIR_COMP),
 * doubleIntervalMap will store and retrieve zero-length (degenerate) intervals.
 * It merely treats them as if the max mValue was actually inifinitesimally greater
 * than the min. EG: <br>
 * <tt>[3,3) intersects [2,4), [3,3) and [3,4) </tt> <br>
 * <tt>[3,3) does not intersect [2,3) <tt>
  *
 * @param <V> The mValue type to be associated with an interval
 * @author Philip DeCamp
 */
public class DoubleIntervalMap<V> extends IntervalMap<double[], V> {

    /**
     * Comparator for intervals defined by length-2 double arrays: <tt>double[] v = {v0, v1}</tt> <br>
     * Intervals are half-open, <tt>[ v0, v1 )</tt>, containing v0 but not v1. <br>
     * Intervals in which <tt>v0 > v1</tt> are not valid and produce undefined results. <br>
     * In the case of a degenerate interval in which <tt>v[0] == v[1]</tt>,
     * the interval WILL contain the mValue: <tt>v0 in [v0, v1)</tt>. <br>
     * EG: <br>
     * <tt>[3,3) intersects [2,4), [3,3) and [3,4) </tt> <br>
     * <tt>[3,3) does not intersect [2,3) <tt>
     */
    public static final IntervalComparator<double[]> DOUBLE_PAIR_COMP = new IntervalComparator<double[]>() {
        public int compareMins( double[] a, double[] b ) {
            if( a[0] < b[0] ) {
                return -1;
            }
            if( a[0] > b[0] ) {
                return 1;
            }
            return 0;
        }

        public int compareMaxes( double[] a, double[] b ) {
            if( a[1] < b[1] ) {
                return -1;
            }
            if( a[1] > b[1] ) {
                return 1;
            }

            return (a[0] == a[1] ? 1 : 0) - (b[0] == b[1] ? 1 : 0);
        }

        public int compareMinToMax( double[] a, double[] b ) {
            if( a[0] < b[1] ) {
                return -1;
            }
            if( a[0] > b[1] ) {
                return 1;
            }

            return (b[0] == b[1] ? -1 : 0);
        }
    };


    public DoubleIntervalMap() {
        super( DOUBLE_PAIR_COMP );
    }


    public DoubleIntervalMap( Map<double[], ? extends V> map ) {
        super( DOUBLE_PAIR_COMP, map );
    }


    public DoubleIntervalMap( IntervalComparator<double[]> comp ) {
        super( comp );
    }


    public DoubleIntervalMap( IntervalComparator<double[]> comp, Map<double[], ? extends V> map ) {
        super( comp, map );
    }


    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpiont of mKey interval
     * @return true iff this map contains an interval [min,max).
     */
    public boolean containsKey( double min, double max ) {
        return super.containsKey( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return true iff this map contains an interval that is a superset of [min, max).
     */
    public boolean containsSupersetKey( double min, double max ) {
        return super.containsSupersetKey( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return true iff the union of intervals in this map is a superset of [min,max).
     */
    public boolean containsSupersetUnion( double min, double max ) {
        return super.containsSupersetUnion( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return true iff this map contains an interval that intersects [min,max)
     */
    public boolean containsIntersectionKey( double min, double max ) {
        return super.containsIntersectionKey( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return mValue mapped to first interval equivalent to [min,max).
     */
    public V get( double min, double max ) {
        return super.get( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return mValue mapped to first interval intersecting [min,max).
     */
    public V getIntersection( double min, double max ) {
        return super.getIntersection( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return mValue mapped to first interval that is a superset of [min,max).
     */
    public V getSuperset( double min, double max ) {
        return super.getSuperset( new double[]{ min, max } );
    }

    /**
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return mValue mapped to first interval that is a subset of [min,max).
     */
    public V getSubset( double min, double max ) {
        return super.getSubset( new double[]{ min, max } );
    }

    /**
     * @param min   Min endpoint of mKey interval
     * @param max   Max endpoint of mKey interval
     * @param value Arbitrary mValue
     * @return null (Existing mappings are never overwritten by calls to <tt>put()</tt>)
     */
    public V put( double min, double max, V value ) {
        return super.put( new double[]{ min, max }, value );
    }


    public Entry<double[], V> lowerEntry( double min, double max ) {
        return super.lowerEntry( new double[]{ min, max } );
    }


    public double[] lowerKey( double min, double max ) {
        return super.lowerKey( new double[]{ min, max } );
    }


    public Entry<double[], V> higherEntry( double min, double max ) {
        return super.higherEntry( new double[]{ min, max } );
    }


    public double[] higherKey( double min, double max ) {
        return super.higherKey( new double[]{ min, max } );
    }


    public Entry<double[], V> ceilingEntry( double min, double max ) {
        return super.ceilingEntry( new double[]{ min, max } );
    }


    public double[] ceilingKey( double min, double max ) {
        return super.ceilingKey( new double[]{ min, max } );
    }


    public Entry<double[], V> floorEntry( double min, double max ) {
        return super.floorEntry( new double[]{ min, max } );
    }


    public double[] floorKey( double min, double max ) {
        return super.floorKey( new double[]{ min, max } );
    }


    /**
     * Removes the mapping for the first interval that is equivalent to
     * <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return the mValue that is removed by this call
     */
    public V remove( double min, double max ) {
        return super.remove( new double[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that intersects <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return the mValue that is removed by this call.
     */
    public V removeIntersection( double min, double max ) {
        return super.removeIntersection( new double[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that's a superset of
     * <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return the mValue that is removed by this call.
     */
    public V removeSuperset( double min, double max ) {
        return super.removeSuperset( new double[]{ min, max } );
    }

    /**
     * Removes the mapping for the first interval that's a subset of
     * <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return the mValue that is removed by this call.
     */
    public V removeSubset( double min, double max ) {
        return super.removeSubset( new double[]{ min, max } );
    }


    /**
     * Returns a set view of the intervals contained in this map that are
     * equivalent to <tt>mKey</tt>
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     */
    public Set<double[]> equivKeySet( double min, double max ) {
        return super.equivKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * equivalent to <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> descendingEquivKeySet( double min, double max ) {
        return super.descendingEquivKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>mKey</tt>
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> intersectionKeySet( double min, double max ) {
        return super.intersectionKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> descendingIntersectionKeySet( double min, double max ) {
        return super.descendingIntersectionKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>mKey</tt>
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> supersetKeySet( double min, double max ) {
        return super.supersetKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> descendingSupersetKeySet( double min, double max ) {
        return super.descendingSupersetKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>mKey</tt>
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> subsetKeySet( double min, double max ) {
        return super.subsetKeySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<double[]> descendingSubsetKeySet( double min, double max ) {
        return super.descendingSubsetKeySet( new double[]{ min, max } );
    }


    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> equivValues( double min, double max ) {
        return super.equivValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingEquivValues( double min, double max ) {
        return super.descendingEquivValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> intersectionValues( double min, double max ) {
        return super.intersectionValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingIntersectionValues( double min, double max ) {
        return super.descendingIntersectionValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> supersetValues( double min, double max ) {
        return super.supersetValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSupersetValues( double min, double max ) {
        return super.descendingSupersetValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are subsets of <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> subsetValues( double min, double max ) {
        return super.subsetValues( new double[]{ min, max } );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSubsetValues( double min, double max ) {
        return super.descendingSubsetValues( new double[]{ min, max } );
    }


    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> equivEntrySet( double min, double max ) {
        return super.equivEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> descendingEquivEntrySet( double min, double max ) {
        return super.descendingEquivEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> intersectionEntrySet( double min, double max ) {
        return super.intersectionEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> descendingIntersectionEntrySet( double min, double max ) {
        return super.descendingIntersectionEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> supersetEntrySet( double min, double max ) {
        return super.supersetEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> descendingSupersetEntrySet( double min, double max ) {
        return super.descendingSupersetEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>mKey</tt>.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> subsetEntrySet( double min, double max ) {
        return super.subsetEntrySet( new double[]{ min, max } );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>mKey</tt> in descending order.
     *
     * @param min Min endpoint of mKey interval
     * @param max Max endpoint of mKey interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Entry<double[], V>> descendingSubsetEntrySet( double min, double max ) {
        return super.descendingSubsetEntrySet( new double[]{ min, max } );
    }


    public Set<double[]> intersectingKeySet( double min, double max ) {
        return super.intersectionKeySet( new double[]{ min, max } );
    }

}
