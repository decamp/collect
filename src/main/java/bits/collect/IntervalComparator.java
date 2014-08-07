/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;

/**
 * Three comparison functions that impose a <i>total ordering</i> on a set
 * of intervals. IntervalComparators are <i>not necessarily consistent
 * with equal</i>. Two non-equivalent objects MAY share the same interval
 * ordering, and equivalent objects SHOULD share the same interval ordering.
 *
 * @author Philip DeCamp
 * @see java.util.Comparator
 */
public interface IntervalComparator<T> {

    /**
     * Compare the minimum values of two intervals.
     *
     * @param a  First object to compare.
     * @param b  Second object to compare.
     * @return negative mValue iff min of <tt>a</tt> is less than min of <tt>b</tt>;
     *         0 iff equivalent; positive mValue iff min of <tt>a</tt> is greater
     *         than min of <tt>b</tt>
     */
    public int compareMins( T a, T b );

    /**
     * Compare the maximum values of two intervals.
     *
     * @param a  First object to compare.
     * @param b  Second object to compare.
     * @return negative mValue iff max of <tt>a</tt> is less than max of <tt>b</tt>;
     *         0 iff equivalent; positive mValue iff max of <tt>a</tt> is greater
     *         than max of <tt>b</tt>
     */
    public int compareMaxes( T a, T b );

    /**
     * Compare the minimum mValue of one interval to the maximum mValue of
     * another.
     *
     * @param a  First object to compare.
     * @param b  Second object to compare.
     * @return negative mValue iff min of <tt>a</tt> is smaller than max of
     *         <tt>b</tt>; 0 iff equivalent; positive mValue iff min of <tt>a</tt> is
     *         greater than maxof <tt>b</tt>
     */
    public int compareMinToMax( T a, T b );

}
