/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */
package bits.collect;

import java.util.*;


/**
 * A balanced binary tree that stores intervals and associated values.
 * <p/>
 * Multiple overlapping or equivalent intervals are allowed. Null keys are not
 * supported.
 * <p/>
 * Intervals are ordered first by minimum value, then maximum, then the order in
 * which they're added to the map.
 * <p/>
 * An interval tree works by keeping intervals sorted in ascending order by
 * start_time, but each node also maintains the maximum end_time of all
 * intervals in the subtree rooted at the node. Therefore, it can quickly be
 * determined whether a search should proceed in a subtree or whether there is
 * no chance of intersection. Mostly based on CLRS (Introduction to Algorithms)
 * and <a href=http://en.wikipedia.org/wiki/Interval_tree#Augmented_tree>
 * http://en.wikipedia.org/wiki/Interval_tree#Augmented_tree</a>
 * <p/>
 * Bug: The IntervalMap lastEntryByMax() does not resolve ties in a defined way.
 * That is, if there are multiple intervals in the map with the same max value,
 * IntervalMap makes no guarantees about which will be returned. This should
 * eventually fixed to return the LAST interval with the greatest max value.
 * This ambiguity extends to IntervalSet and LongIntervalMap.
 *
 * @param <K> The key type that defines the intervals.
 * @param <V> The value type to be associated with an interval.
 * @author Philip DeCamp
 */
@SuppressWarnings( "unchecked" )
public class IntervalMap<K, V> implements Map<K, V> {

    private final IntervalComparator<? super K> mComp;

    private Node mRoot = null;
    private int  mSize = 0;

    private transient int mModCount = 0;


    public IntervalMap( IntervalComparator<? super K> comp ) {
        mComp = comp;
    }


    public IntervalMap( IntervalComparator<? super K> comp, Map<? extends K, ? extends V> map ) {
        mComp = comp;
        putAll( map );
    }


    /**
     * &nbsp
     *
     * @param key An interval
     * @return true iff this map contains an interval, <code>g</code>, such that
     * <code>g</code> has equivalent min and max values as
     * <code>key</code> AND <code>key.equals( g )</code>.
     */
    @Override
    public boolean containsKey( Object key ) {
        K k = (K)key;
        Node n = firstEquivNode( k );

        while( n != null ) {
            if( key.equals( n.mKey ) ) {
                return true;
            }

            n = nextEquivNode( n, k );
        }

        return false;
    }

    /**
     * @param key An interval
     * @return true iff this map contains an interval with equivalent min and
     * max values as <i>key</i>. Note that this method may be true even
     * if this IntervalMap does not contain any interval <code>g</code>
     * such that <code>g.equals( key ) == true</code>.
     */
    public boolean containsEquivKey( Object key ) {
        return firstEquivNode( (K)key ) != null;
    }

    /**
     * @param key An interval
     * @return true iff this map contains an interval that intersects
     * <i>key</i>.
     */
    public boolean containsIntersectionKey( Object key ) {
        return firstIntersectionNode( mRoot, (K)key ) != null;
    }

    /**
     * @param key An interval
     * @return true iff this map contains an interval that is a subset of
     * <i>key</i>.
     */
    public boolean containsSubsetKey( Object key ) {
        return firstIntersectionNode( mRoot, (K)key ) != null;
    }

    /**
     * @param key An interval
     * @return true iff this map contains an interval that is a superset of
     * <i>key</i>.
     */
    public boolean containsSupersetKey( Object key ) {
        return firstSupersetNode( (K)key ) != null;
    }

    /**
     * @param key An interval
     * @return true iff the union of intervals in this map is a superset of
     * <i>key</i>.
     */
    public boolean containsSupersetUnion( Object key ) {
        final K k = (K)key;

        Node node = mRoot;
        if( node == null ) {
            return false;
        }

        // Find left edge.
        while( true ) {
            // Check if node overlaps min edge.
            if( mComp.compareMinToMax( k, node.mKey ) < 0 &&
                mComp.compareMins( k, node.mKey ) >= 0 )
            {
                // Check if this node completely contains key.
                if( mComp.compareMaxes( k, node.mKey ) <= 0 ) {
                    return true;
                }

                break;
            }

            // Descend left subtree if intersection is possible.
            if( node.mLeft != null && mComp.compareMinToMax( k, node.mLeft.mMaxStop.mKey ) < 0 ) {
                node = node.mLeft;
                continue;
            }

            // Descend right subtree if the current node doesn't overlap and
            // intersection is possible.
            if( node.mRight != null && mComp.compareMinToMax( k, node.mRight.mMaxStop.mKey ) < 0 ) {
                node = node.mRight;
                continue;
            }

            return false;
        }

        Node left = node;


        // Now starting working to the right, find a set of connecting nodes to
        // right edge.
        while( true ) {

            // If the current node is it's own max stop, no point in descending.
            if( node.mMaxStop != node ) {
                // Check left node for greater max stop.
                if( node.mLeft != null && mComp.compareMaxes( node.mLeft.mMaxStop.mKey, left.mKey ) > 0 ) {
                    left = node.mLeft.mMaxStop;

                    // Check if we've reached end.
                    if( mComp.compareMaxes( k, left.mKey ) <= 0 ) {
                        return true;
                    }
                }

                // Check right node for greater max stop.
                if( node.mRight != null && (node.mMaxStop == node.mRight.mMaxStop ||
                                            mComp.compareMaxes( node.mMaxStop.mKey, node.mRight.mMaxStop.mKey ) <= 0) )
                {
                    // Well, hell. Now we have to search through right subtree
                    // to make sure there's an overlap.
                    Node temp = node.mRight;

                    // Keep going left until first overlap is found.
                    while( mComp.compareMinToMax( temp.mKey, left.mKey ) > 0 ) {
                        temp = temp.mLeft;
                        if( temp == null ) {
                            break;
                        }
                    }

                    if( temp != null ) {
                        // Found overlap. Check if we've extended left boundary.
                        node = temp;

                        if( mComp.compareMaxes( temp.mKey, left.mKey ) > 0 ) {
                            left = node;

                            // Check if we've reach end.
                            if( mComp.compareMaxes( k, left.mKey ) <= 0 ) {
                                return true;
                            }
                        }

                        continue;
                    }
                }
            }

            // Time to ascend.
            while( true ) {
                if( node.mParent == null ) {
                    return false;
                }

                if( node.mParent.mLeft == node ) {
                    node = node.mParent;

                    // Check if no overlap.
                    if( mComp.compareMinToMax( node.mKey, left.mKey ) > 0 ) {
                        return false;
                    }

                    // Check if extends left.
                    if( mComp.compareMaxes( left.mKey, node.mKey ) < 0 ) {
                        left = node;

                        // Check if we've reached end.
                        if( mComp.compareMaxes( k, left.mKey ) <= 0 ) {
                            return true;
                        }
                    }

                    // Check children.
                    break;
                }

                node = node.mParent;
            }
        }
    }

    /**
     * @param value A value
     * @return true iff this map contains an equivalent value.
     */
    @Override
    public boolean containsValue( Object value ) {
        Node node = mRoot;

        if( value == null ) {
            while( node != null ) {
                if( node.mValue == null ) {
                    return true;
                }

                node = nextNode( node );
            }
        } else {
            while( node != null ) {
                if( value.equals( node.mValue ) ) {
                    return true;
                }

                node = nextNode( node );
            }
        }

        return false;
    }


    /**
     * &nbsp
     *
     * @param key interval
     * @return value mapped to first interval equivalent to <b>key</b> contained
     * in this map.
     */
    @Override
    public V get( Object key ) {
        Node node = firstEquivNode( (K)key );
        return (node == null ? null : node.mValue);
    }

    /**
     * @param key interval
     * @return value mapped to first interval intersecting <b>key</b> contained
     * in this map.
     */
    public V getIntersection( Object key ) {
        Node node = firstIntersectionNode( mRoot, (K)key );
        return (node == null ? null : node.mValue);
    }

    /**
     * @param key interval
     * @return value mapped to first interval in this map that is a superset of
     * <b>key</b>.
     */
    public V getSuperset( Object key ) {
        Node node = firstSupersetNode( (K)key );
        return (node == null ? null : node.mValue);
    }

    /**
     * @param key interval
     * @return value mapped to first interval in this map that is a subset of
     * <b>key</b>.
     */
    public V getSubset( Object key ) {
        Node node = firstSubsetNode( (K)key );
        return (node == null ? null : node.mValue);
    }


    /**
     * &nbsp
     *
     * @param key   interval
     * @param value Arbitrary value
     * @return null (Existing mappings are never overwritten by calls to
     * <i>put()</i>)
     */
    @Override
    public V put( K key, V value ) {
        Node node = mRoot;
        Node newNode = new Node( key, value );

        if( node == null ) {
            insertNode( newNode, node, false );
            return null;
        }

        while( true ) {
            if( mComp.compareMaxes( key, node.mMaxStop.mKey ) > 0 ) {
                node.mMaxStop = newNode;
            }

            int c = mComp.compareMins( key, node.mKey );
            if( c == 0 ) {
                c = mComp.compareMaxes( key, node.mKey );
            }

            if( c < 0 ) {
                if( node.mLeft == null ) {
                    insertNode( newNode, node, true );
                    break;
                }

                node = node.mLeft;

            } else {
                if( node.mRight == null ) {
                    insertNode( newNode, node, false );
                    break;
                }

                node = node.mRight;
            }
        }

        return null;
    }


    @Override
    public void putAll( Map<? extends K, ? extends V> map ) {
        if( map == null ) {
            return;
        }

        for( Map.Entry<? extends K, ? extends V> e : map.entrySet() ) {
            put( e.getKey(), e.getValue() );
        }
    }


    /**
     * Removes the mapping for the first interval that is equivalent to
     * <b>key</b>.
     *
     * @param key interval
     * @return the value that is removed by this call
     */
    @Override
    public V remove( Object key ) {
        Node node = firstEquivNode( (K)key );
        if( node == null ) {
            return null;
        }

        removeNode( node );
        return node.mValue;
    }

    /**
     * Removes the mapping for the first interval that intersects <b>key</b>.
     *
     * @param key interval
     * @return the value that is removed by this call.
     */
    public V removeIntersection( Object key ) {
        Node node = firstIntersectionNode( mRoot, (K)key );
        if( node == null ) {
            return null;
        }

        removeNode( node );
        return node.mValue;
    }

    /**
     * Removes the mapping for the first interval that's a superset of
     * <b>key</b>.
     *
     * @param key interval
     * @return the value that is removed by this call.
     */
    public V removeSuperset( Object key ) {
        Node node = firstSupersetNode( (K)key );
        if( node == null ) {
            return null;
        }

        removeNode( node );
        return node.mValue;
    }

    /**
     * Removes the mapping for the first interval that's a subset of <b>key</b>.
     *
     * @param key interval
     * @return the value that is removed by this call.
     */
    public V removeSubset( Object key ) {
        Node node = firstSubsetNode( (K)key );
        if( node == null ) {
            return null;
        }

        removeNode( node );
        return node.mValue;
    }


    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }


    @Override
    public int size() {
        return mSize;
    }


    @Override
    public void clear() {
        mModCount++;
        mRoot = null;
        mSize = 0;
    }


    public Map.Entry<K, V> firstEntry() {
        return firstNode();
    }


    public K firstKey() {
        Node node = firstNode();
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lastEntry() {
        return lastNode();
    }


    public K lastKey() {
        Node node = lastNode();
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lowerEntry( K key ) {
        Node node = floorNode( key );
        Node afterEqNode = null;

        // Find equals() key, if there is one.
        while( node != null ) {
            if( mComp.compareMins( key, node.mKey ) != 0 ||
                mComp.compareMaxes( key, node.mKey ) != 0 )
            {
                // Non equivalent ordering. Return.
                return afterEqNode == null ? node : afterEqNode;
            }

            if( key.equals( node.mKey ) ) {
                // Found equals() equiv node. Return previous node.
                node = afterEqNode = prevNode( node );
            } else {
                node = prevNode( node );
            }
        }

        return afterEqNode;
    }


    public K lowerKey( K key ) {
        Map.Entry<K, V> e = lowerEntry( key );
        return e == null ? null : e.getKey();
    }


    public Map.Entry<K, V> higherEntry( K key ) {
        Node node = ceilingNode( key );
        Node afterEqNode = null;

        // Find equals() equivalent key, if there is one.
        while( node != null ) {
            if( mComp.compareMins( key, node.mKey ) != 0 ||
                mComp.compareMaxes( key, node.mKey ) != 0 )
            {
                return afterEqNode == null ? node : afterEqNode;
            }

            if( key.equals( node.mKey ) ) {
                // Found equals() equiv node. Return next node.
                node = afterEqNode = nextNode( node );
            } else {
                node = nextNode( node );
            }
        }

        return afterEqNode;
    }


    public K higherKey( K key ) {
        Map.Entry<K, V> e = higherEntry( key );
        return e == null ? null : e.getKey();
    }


    public Map.Entry<K, V> floorEntry( K key ) {
        Node ret = floorNode( key );
        Node node = ret;

        while( node != null ) {
            if( mComp.compareMins( key, node.mKey ) != 0 ||
                mComp.compareMaxes( key, node.mKey ) != 0 )
            {
                return ret;
            }

            if( key.equals( node.mKey ) ) {
                return node;
            }

            node = prevNode( node );
        }

        return ret;
    }


    public K floorKey( K key ) {
        Map.Entry<K, V> e = floorEntry( key );
        return e == null ? null : e.getKey();
    }


    public Map.Entry<K, V> ceilingEntry( K key ) {
        Node ret = ceilingNode( key );
        Node node = ret;

        while( node != null ) {
            if( mComp.compareMins( key, node.mKey ) != 0 ||
                mComp.compareMaxes( key, node.mKey ) != 0 )
            {
                return ret;
            }

            if( key.equals( node.mKey ) ) {
                return node;
            }

            node = nextNode( node );
        }

        return ret;
    }


    public K ceilingKey( K key ) {
        Map.Entry<K, V> e = ceilingEntry( key );
        return e == null ? null : e.getKey();
    }

    /**
     * @return the entry with the greatest max interval value.
     */
    public Map.Entry<K, V> lastEntryByMax() {
        Node root = mRoot;
        return root == null ? null : root.mMaxStop;
    }

    /**
     * @return the key with the greastest max interval value.
     */
    public K lastKeyByMax() {
        Node root = mRoot;
        return root == null ? null : root.mMaxStop.mKey;
    }


    public Map.Entry<K, V> firstEquivEntry( K key ) {
        return firstEquivNode( key );
    }


    public K firstEquivKey( K key ) {
        Node node = firstEquivNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lastEquivEntry( K key ) {
        return lastEquivNode( key );
    }


    public K lastEquivKey( K key ) {
        Node node = lastEquivNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> firstIntersectionEntry( K key ) {
        return firstIntersectionNode( mRoot, key );
    }


    public K firstIntersectionKey( K key ) {
        Node node = firstIntersectionNode( mRoot, key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lastIntersectionEntry( K key ) {
        return lastIntersectionNode( key );
    }


    public K lastIntersectionKey( K key ) {
        Node node = lastIntersectionNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> firstSupersetEntry( K key ) {
        return firstSupersetNode( key );
    }


    public K firstSupersetKey( K key ) {
        Node node = firstSupersetNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lastSupersetEntry( K key ) {
        return lastSupersetNode( key );
    }


    public K lastSupersetKey( K key ) {
        Node node = lastSupersetNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> firstSubsetEntry( K key ) {
        return firstSubsetNode( key );
    }


    public K firstSubsetKey( K key ) {
        Node node = firstSubsetNode( key );
        return node == null ? null : node.mKey;
    }


    public Map.Entry<K, V> lastSubsetEntry( K key ) {
        return lastSubsetNode( key );
    }


    public K lastSubsetKey( K key ) {
        Node node = lastSubsetNode( key );
        return node == null ? null : node.mKey;
    }


    @Override
    public Set<K> keySet() {
        return new KeySet( false );
    }

    /**
     * @return a Set view of the keys in this map in descending order.
     */
    public Set<K> descendingKeySet() {
        return new KeySet( true );
    }

    /**
     * Returns a set view of the key contained in this map that are equivalent
     * to <tt>key</tt>
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> equivKeySet( K key ) {
        return new EquivKeySet( key, false );
    }

    /**
     * Returns a set view of the keys contained in this map that are equivalent
     * to <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> descendingEquivKeySet( K key ) {
        return new EquivKeySet( key, true );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>key</tt>
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> intersectionKeySet( K key ) {
        return new IntersectionKeySet( key, false );
    }

    /**
     * Returns a set view of the intervals contained in this map that intersect
     * <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> descendingIntersectionKeySet( K key ) {
        return new IntersectionKeySet( key, true );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>key</tt>
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> supersetKeySet( K key ) {
        return new SupersetKeySet( key, false );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * supersets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> descendingSupersetKeySet( K key ) {
        return new SupersetKeySet( key, true );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>key</tt>
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> subsetKeySet( K key ) {
        return new SubsetKeySet( key, false );
    }

    /**
     * Returns a set view of the intervals contained in this map that are
     * subsets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of intervals
     * @see #keySet()
     */
    public Set<K> descendingSubsetKeySet( K key ) {
        return new SubsetKeySet( key, true );
    }


    @Override
    public Collection<V> values() {
        return new Values( false );
    }

    /**
     * @return a Collection view of the values in this map in descending order.
     */
    public Collection<V> descendingValues() {
        return new Values( true );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>key</tt>.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> equivValues( K key ) {
        return new EquivValues( key, false );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals
     * equivalent to <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingEquivValues( K key ) {
        return new EquivValues( key, true );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>key</tt>.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> intersectionValues( K key ) {
        return new IntersectionValues( key, false );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * intersect <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingIntersectionValues( K key ) {
        return new IntersectionValues( key, true );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>key</tt>.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> supersetValues( K key ) {
        return new SupersetValues( key, false );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are supersets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSupersetValues( K key ) {
        return new SupersetValues( key, true );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are subsets of <tt>key</tt>.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> subsetValues( K key ) {
        return new SubsetValues( key, false );
    }

    /**
     * Returns a collection view of the values that are mapped to intervals that
     * are subsets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a collection view of values
     * @see #values()
     */
    public Collection<V> descendingSubsetValues( K key ) {
        return new SubsetValues( key, true );
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet( false );
    }

    /**
     * @return a Set view of the entries in this map in descending order.
     */
    public Set<Map.Entry<K, V>> descendingEntrySet() {
        return new EntrySet( true );
    }

    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>key</tt>.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> equivEntrySet( K key ) {
        return new EquivEntrySet( key, false );
    }

    /**
     * Returns a set view of the mappings that contain intervals equivalent to
     * <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> descendingEquivEntrySet( K key ) {
        return new EquivEntrySet( key, true );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>key</tt>.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> intersectionEntrySet( K key ) {
        return new IntersectionEntrySet( key, false );
    }

    /**
     * Returns a set view of the mappings that contain intervals that intersect
     * <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> descendingIntersectionEntrySet( K key ) {
        return new IntersectionEntrySet( key, true );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>key</tt>.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> supersetEntrySet( K key ) {
        return new SupersetEntrySet( key, false );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * supersets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> descendingSupersetEntrySet( K key ) {
        return new SupersetEntrySet( key, true );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>key</tt>.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> subsetEntrySet( K key ) {
        return new SubsetEntrySet( key, false );
    }

    /**
     * Returns a set view of the mappings that contain intervals that are
     * subsets of <tt>key</tt> in descending order.
     *
     * @param key interval
     * @return a set view of values
     * @see #entrySet()
     */
    public Set<Map.Entry<K, V>> descendingSubsetEntrySet( K key ) {
        return new SubsetEntrySet( key, true );
    }


    /* ************************************************************
     * Package private debugging
     * ***********************************************************
     */

    boolean validateMaxStops() {
        if( mRoot == null ) {
            return true;
        }

        return validateMaxStops( mRoot ) != null;
    }


    private Node validateMaxStops( Node node ) {
        Node stop = node;

        if( node.mLeft != null ) {
            Node n = validateMaxStops( node.mLeft );
            if( n == null ) {
                return null;
            }

            stop = maxStopNode( stop, n );
        }

        if( node.mRight != null ) {
            Node n = validateMaxStops( node.mRight );
            if( n == null ) {
                return null;
            }

            stop = maxStopNode( stop, n );
        }

        if( mComp.compareMaxes( stop.mKey, node.mMaxStop.mKey ) != 0 ) {
            return null;
        }

        return stop;
    }



    /* ************************************************************
     * Internal nodal operations.
     * ***********************************************************
     */

    private Node maxStopNode( Node a, Node b ) {
        return mComp.compareMaxes( a.mKey, b.mKey ) >= 0 ? a : b;
    }


    private Node firstNode() {
        Node node = mRoot;
        if( node == null ) {
            return null;
        }
        while( node.mLeft != null ) {
            node = node.mLeft;
        }

        return node;
    }


    private Node lastNode() {
        Node node = mRoot;
        if( node == null ) {
            return null;
        }
        while( node.mRight != null ) {
            node = node.mRight;
        }

        return node;
    }


    private Node nextNode( Node node ) {
        if( node.mRight != null ) {
            node = node.mRight;
            while( node.mLeft != null ) {
                node = node.mLeft;
            }

        } else {
            while( node.mParent != null && node.mParent.mRight == node ) {
                node = node.mParent;
            }
            node = node.mParent;
        }

        return node;
    }


    private Node prevNode( Node node ) {
        if( node.mLeft != null ) {
            node = node.mLeft;
            while( node.mRight != null ) {
                node = node.mRight;
            }
        } else {
            while( node.mParent != null && node.mParent.mLeft == node ) {
                node = node.mParent;
            }
            node = node.mParent;
        }

        return node;
    }

    /**
     * Returns the node with an equiv key or lower. In the case of multiple
     * equivalent keys, returns the highest one.
     *
     * @param key key
     */
    private Node floorNode( K key ) {
        if( key == null ) {
            return null;
        }

        Node node = mRoot;
        Node eqRet = null;
        Node ltRet = null;

        IntervalComparator<? super K> comp = mComp;

        while( node != null ) {
            int c = comp.compareMins( key, node.mKey );
            if( c == 0 ) {
                c = comp.compareMaxes( key, node.mKey );
            }

            if( c < 0 ) {
                node = node.mLeft;
            } else if( c == 0 ) {
                eqRet = node;
                node = node.mRight;
            } else {
                ltRet = node;
                node = node.mRight;
            }
        }

        return eqRet == null ? ltRet : eqRet;
    }

    /**
     * Returns the node with an equiv key or higher. In the case of multiple
     * equivalent keys, returns the lowest one.
     *
     * @param key key
     */
    private Node ceilingNode( K key ) {
        if( key == null ) {
            return null;
        }

        Node node = mRoot;
        Node eqRet = null;
        Node gtRet = null;

        IntervalComparator<? super K> comp = mComp;

        while( node != null ) {
            int c = comp.compareMins( key, node.mKey );
            if( c == 0 ) {
                c = comp.compareMaxes( key, node.mKey );
            }

            if( c < 0 ) {
                gtRet = node;
                node = node.mLeft;
            } else if( c == 0 ) {
                eqRet = node;
                node = node.mLeft;
            } else {
                node = node.mRight;
            }
        }

        return eqRet == null ? gtRet : eqRet;
    }


    private Node firstEquivNode( K key ) {
        Node node = mRoot;
        Node ret = null;

        // Find lowest node with same start.
        while( node != null ) {
            int c = mComp.compareMins( key, node.mKey );

            if( c < 0 ) {
                node = node.mLeft;
            } else if( c > 0 ) {
                node = node.mRight;
            } else {
                c = mComp.compareMaxes( key, node.mKey );

                if( c < 0 ) {
                    node = node.mLeft;
                } else if( c > 0 ) {
                    node = node.mRight;
                } else {
                    ret = node;
                    node = node.mLeft;
                }
            }
        }

        return ret;
    }


    private Node nextEquivNode( Node node, K key ) {
        node = nextNode( node );
        if( node == null ) {
            return null;
        }
        if( mComp.compareMins( key, node.mKey ) == 0 && mComp.compareMaxes( key, node.mKey ) == 0 ) {
            return node;
        }
        return null;
    }


    private Node lastEquivNode( K key ) {
        Node node = mRoot;
        Node ret = null;

        // Find highest node with same start.
        while( node != null ) {
            int c = mComp.compareMins( key, node.mKey );

            if( c < 0 ) {
                node = node.mLeft;
            } else if( c > 0 ) {
                node = node.mRight;
            } else {
                c = mComp.compareMaxes( key, node.mKey );

                if( c < 0 ) {
                    node = node.mLeft;
                } else if( c > 0 ) {
                    node = node.mRight;
                } else {
                    ret = node;
                    node = node.mRight;
                }
            }
        }

        return ret;
    }


    private Node prevEquivNode( Node node, K key ) {
        node = prevNode( node );
        if( node == null ) {
            return null;
        }
        if( mComp.compareMins( key, node.mKey ) == 0 && mComp.compareMaxes( key, node.mKey ) == 0 ) {
            return node;
        }
        return null;
    }


    private Node firstIntersectionNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        Node ret = null;
        while( true ) {
            if( mComp.compareMinToMax( key, node.mKey ) < 0 && mComp.compareMinToMax( node.mKey, key ) < 0 ) {
                ret = node;
            }

            // Descend left subtree if it might intersect.
            if( node.mLeft != null && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                node = node.mLeft;
                continue;
            }

            // Descend right subtree if possible, and if the current node
            // doesn't already intersect.
            if( node.mRight != null && ret != node && mComp.compareMinToMax( key, node.mRight.mMaxStop.mKey ) < 0 ) {
                node = node.mRight;
                continue;
            }

            return ret;
        }
    }


    private Node nextIntersectionNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        while( true ) {
            // Check right tree.
            Node ret = firstIntersectionNode( node.mRight, key );
            if( ret != null ) {
                return ret;
            }

            // Go up until you find a parent to the right.
            while( true ) {
                if( node.mParent == null ) {
                    return null;
                }

                if( node.mParent.mRight == node ) {
                    node = node.mParent;
                } else {
                    node = node.mParent;

                    // Found parent to the right. Check node for intersections.
                    if( mComp.compareMinToMax( key, node.mKey ) < 0 && mComp.compareMinToMax( node.mKey, key ) < 0 ) {
                        return node;
                    }

                    // If key is entirely before node, return null.
                    if( mComp.compareMinToMax( node.mKey, key ) >= 0 ) {
                        return null;
                    }

                    break;
                }
            }
        }
    }


    private Node lastIntersectionNode( K key ) {
        if( mRoot == null ) {
            return null;
        }
        Object[] ret = { mRoot };

        while( true ) {
            if( searchDownForPrevIntersectionNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchUpForPrevIntersectionNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node prevIntersectionNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }
        Object[] ret = { node };

        while( true ) {
            if( searchUpForPrevIntersectionNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchDownForPrevIntersectionNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private boolean searchDownForPrevIntersectionNode( Node node, K key, Object[] ret ) {
        while( true ) {
            int c = mComp.compareMinToMax( node.mKey, key );
            if( c < 0 ) {
                // Node does not occur completely after key.

                // Check if right subtree might intersect key.
                if( node.mRight != null && mComp.compareMinToMax( key, node.mRight.mMaxStop.mKey ) < 0 ) {
                    node = node.mRight;
                    continue;
                }

                // Check if current node intersects.
                if( mComp.compareMinToMax( key, node.mKey ) < 0 ) {
                    ret[0] = node;
                    return true;
                }
            }

            // Check if left subtree might intersect.
            if( node.mLeft != null && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                node = node.mLeft;
                continue;
            }

            // Request upward search from current node.
            ret[0] = node;
            return false;
        }
    }


    private boolean searchUpForPrevIntersectionNode( Node node, K key, Object[] ret ) {
        if( node.mLeft != null && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
            // Request download search on left node.
            ret[0] = node.mLeft;
            return false;
        }

        while( node.mParent != null ) {
            if( node == node.mParent.mLeft ) {
                node = node.mParent;

            } else {
                node = node.mParent;

                // If we're coming from the right, then we've not checked for an
                // intersection
                // with the current node.
                if( mComp.compareMinToMax( key, node.mKey ) < 0 &&
                    mComp.compareMinToMax( node.mKey, key ) < 0 )
                {
                    ret[0] = node;
                    return true;
                }

                // Check if left subtree might intersect.
                if( node.mLeft != null && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                    ret[0] = node.mLeft;
                    return false;
                }
            }
        }

        // Search is complete and unsuccesful.
        ret[0] = null;
        return true;
    }


    private Node firstSupersetNode( K key ) {
        if( mRoot == null ) {
            return null;
        }

        Object[] ret = { mRoot };

        while( true ) {
            if( searchDownForNextSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchUpForNextSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node lastSupersetNode( K key ) {
        if( mRoot == null ) {
            return null;
        }

        Object[] ret = { mRoot };
        while( true ) {
            if( searchDownForPrevSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchUpForPrevSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node nextSupersetNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        Object[] ret = { node };
        while( true ) {
            if( searchUpForNextSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchDownForNextSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node prevSupersetNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        Object[] ret = { node };
        while( true ) {
            if( searchUpForPrevSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchDownForPrevSupersetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }

    /**
     * Finds a node that contains a key, where the search will only traverse
     * children nodes. Searches all subtrees from left to right, depth first.
     * Assumes node parameter HAS NOT already been checked, and may be returned.
     *
     * @return true iff search is concluded, otherwise false.
     */
    private boolean searchDownForNextSupersetNode( Node node, K key, Object[] ret ) {
        while( true ) {
            // Descend left subtree if it might have superset node.
            if( node.mLeft != null && mComp.compareMaxes( key, node.mLeft.mMaxStop.mKey ) <= 0 ) {
                node = node.mLeft;
                continue;
            }

            // Check if current node contains key.
            int c = mComp.compareMins( key, node.mKey );

            if( c >= 0 && mComp.compareMaxes( key, node.mKey ) <= 0 ) {
                ret[0] = node;
                return true;
            }

            // Descend right subtree if it might have a superset node.
            if( node.mRight != null && c >= 0 && mComp.compareMaxes( key, node.mRight.mMaxStop.mKey ) <= 0 ) {
                node = node.mRight;
                continue;
            }

            // Request upward search from current node.
            ret[0] = node;
            return false;
        }
    }

    /**
     * Finds a node that contains a key, where the search will only traverse
     * parent nodes. Searches parent nodes and right subtrees. The node
     * parameter will not be returned.
     *
     * @return true iff search is concluded, false otherwise.
     */
    private boolean searchUpForNextSupersetNode( Node node, K key, Object[] ret ) {
        if( node.mRight != null && mComp.compareMaxes( key, node.mRight.mMaxStop.mKey ) <= 0 ) {
            // Request downward search on right subtree.
            ret[0] = node.mRight;
            return false;
        }

        while( node.mParent != null ) {
            if( node == node.mParent.mRight ) {
                node = node.mParent;
            } else {
                node = node.mParent;

                // If we're coming from the left path, it means we haven't
                // checked the current node or right subtree.
                if( mComp.compareMins( key, node.mKey ) >= 0 ) {
                    if( mComp.compareMaxes( key, node.mKey ) <= 0 ) {
                        ret[0] = node;
                        return true;
                    }

                    // Check if right might contain key.
                    if( node.mRight != null ) {
                        if( mComp.compareMaxes( key, node.mRight.mMaxStop.mKey ) <= 0 ) {
                            // Request downward search on right subtree.
                            ret[0] = node.mRight;
                            return false;
                        }
                    }
                }
            }
        }

        // Search completed.
        ret[0] = null;
        return true;
    }

    /**
     * Finds a node that contains a key, where the search will only traverse
     * children nodes. Searches all subtrees from right to left, depth first.
     * Assumes node parameter HAS NOT already been checked, and may be returned.
     *
     * @return true iff search is concluded, otherwise false.
     */
    private boolean searchDownForPrevSupersetNode( Node node, K key, Object[] ret ) {
        while( true ) {
            // Check if current node is beyond key.
            int c = mComp.compareMins( key, node.mKey );

            if( c >= 0 ) {
                // Descend right subtree if it might have a superset node.
                if( node.mRight != null && mComp.compareMaxes( key, node.mRight.mMaxStop.mKey ) <= 0 ) {
                    node = node.mRight;
                    continue;
                }

                // Check if current node contains key.
                if( mComp.compareMaxes( key, node.mKey ) <= 0 ) {
                    ret[0] = node;
                    return true;
                }
            }

            // Check left subtree if it might have superset node.
            if( node.mLeft != null && mComp.compareMaxes( key, node.mLeft.mMaxStop.mKey ) <= 0 ) {
                node = node.mLeft;
                continue;
            }

            // Request upward search from current node.
            ret[0] = node;
            return false;
        }
    }

    /**
     * Finds a node that contains a key, where the search will only traverse
     * parent nodes. Searches parent nodes and left subtrees. The node parameter
     * will not be returned.
     *
     * @return true iff search is concluded, false otherwise.
     */
    private boolean searchUpForPrevSupersetNode( Node node, K key, Object[] ret ) {
        if( node.mLeft != null && mComp.compareMaxes( key, node.mLeft.mMaxStop.mKey ) <= 0 ) {
            ret[0] = node.mLeft;
            return false;
        }

        while( node.mParent != null ) {
            if( node == node.mParent.mLeft ) {
                node = node.mParent;
            } else {
                node = node.mParent;

                // If we're coming from the right path, it means we haven't
                // checked the current node or left subtree.
                if( mComp.compareMins( key, node.mKey ) >= 0 ) {
                    if( mComp.compareMaxes( key, node.mKey ) <= 0 ) {
                        ret[0] = node;
                        return true;
                    }
                }

                // Check if left subtree might contain key.
                if( node.mLeft != null && mComp.compareMaxes( key, node.mLeft.mMaxStop.mKey ) <= 0 ) {
                    // Request downward search on left subtree.
                    ret[0] = node.mLeft;
                    return false;
                }
            }
        }

        // Search completed.
        ret[0] = null;
        return true;
    }


    private Node firstSubsetNode( K key ) {
        if( mRoot == null ) {
            return null;
        }

        Object[] ret = { mRoot };
        while( true ) {
            if( searchDownForNextSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchUpForNextSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node lastSubsetNode( K key ) {
        if( mRoot == null ) {
            return null;
        }

        Object[] ret = { mRoot };
        while( true ) {
            if( searchDownForPrevSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchUpForPrevSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node nextSubsetNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        Object[] ret = { node };
        while( true ) {
            if( searchUpForNextSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchDownForNextSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private Node prevSubsetNode( Node node, K key ) {
        if( node == null ) {
            return null;
        }

        Object[] ret = { node };
        while( true ) {
            if( searchUpForPrevSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }

            if( searchDownForPrevSubsetNode( (Node)ret[0], key, ret ) ) {
                return (Node)ret[0];
            }
        }
    }


    private boolean searchDownForNextSubsetNode( Node node, K key, Object[] ret ) {
        while( true ) {
            // Check if current node occurs before or after key.
            int c = mComp.compareMins( key, node.mKey );

            // Descend left subtree if it might have subset node.
            if( node.mLeft != null && c <= 0 && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                node = node.mLeft;
                continue;
            }

            if( c <= 0 ) {
                // Key starts before node. Check end point.
                if( mComp.compareMaxes( key, node.mKey ) >= 0 ) {
                    // Key contains node completely.
                    ret[0] = node;
                    return true;
                }

                // Descend right node if key intersects current node.
                if( node.mRight != null && mComp.compareMinToMax( node.mKey, key ) < 0 ) {
                    node = node.mRight;
                    continue;
                }

            } else {
                // Key starts after node. Descend right node if maxstop
                // intersects key.
                if( node.mRight != null && mComp.compareMinToMax( key, node.mRight.mMaxStop.mKey ) < 0 ) {
                    node = node.mRight;
                    continue;
                }
            }

            // Request upward search from current node.
            ret[0] = node;
            return false;
        }
    }


    private boolean searchUpForNextSubsetNode( Node node, K key, Object[] ret ) {

        if( node.mRight != null && mComp.compareMinToMax( node.mKey, key ) < 0 &&
            mComp.compareMinToMax( key, node.mRight.mMaxStop.mKey ) < 0 )
        {
            // Request downard search on right subtree.
            ret[0] = node.mRight;
            return false;
        }

        while( node.mParent != null ) {
            if( node == node.mParent.mRight ) {
                node = node.mParent;
            } else {
                node = node.mParent;

                // If we're coming from the left path, it means we haven't
                // checked the
                // current node or right subtree.

                // We'd would never search to the left of a node that started
                // before the key,
                // so don't bother checking mins.
                if( mComp.compareMaxes( key, node.mKey ) >= 0 ) {
                    ret[0] = node;
                    return true;
                }

                // Check if the right node might be contained.
                if( node.mRight != null ) {
                    ret[0] = node.mRight;
                    return false;
                }
            }
        }

        // Search is complete.
        ret[0] = null;
        return true;

    }


    private boolean searchDownForPrevSubsetNode( Node node, K key, Object[] ret ) {
        while( true ) {

            // Check if current node occurs before or after key.
            int c = mComp.compareMins( key, node.mKey );

            if( c <= 0 ) { // Key starts before node.

                // Descend right subtree if it might contain subset node.
                if( node.mRight != null && mComp.compareMinToMax( node.mKey, key ) < 0 ) {
                    node = node.mRight;
                    continue;
                }

                // Check end point of current node.
                if( mComp.compareMaxes( key, node.mKey ) >= 0 ) {
                    // Key contains node completely.
                    ret[0] = node;
                    return true;
                }

            } else {
                // Key starts after node. Descend right node if maxStop
                // intersects key.
                if( node.mRight != null && mComp.compareMinToMax( key, node.mRight.mMaxStop.mKey ) < 0 ) {
                    node = node.mRight;
                    continue;
                }
            }

            // Descend left subtree if it might have subset node.
            if( node.mLeft != null && c <= 0 && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                node = node.mLeft;
                continue;
            }

            // Request upward search from current node.
            ret[0] = node;
            return false;
        }

    }


    private boolean searchUpForPrevSubsetNode( Node node, K key, Object[] ret ) {

        if( node.mLeft != null && mComp.compareMins( node.mKey, key ) >= 0 &&
            mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 )
        {
            // Request downward searh on left subtree.
            ret[0] = node.mLeft;
            return false;
        }

        while( node.mParent != null ) {
            if( node == node.mParent.mLeft ) {
                node = node.mParent;

            } else {
                node = node.mParent;

                // If we're coming from the right subtree, it means we haven't
                // checked the
                // current node or left subtree.
                int c = mComp.compareMins( key, node.mKey );

                if( c <= 0 ) {
                    if( mComp.compareMaxes( key, node.mKey ) >= 0 ) {
                        ret[0] = node;
                        return true;
                    }

                    // Check if the left node might contain a subset.
                    if( node.mLeft != null && mComp.compareMinToMax( key, node.mLeft.mMaxStop.mKey ) < 0 ) {
                        ret[0] = node.mLeft;
                        return false;
                    }
                }
            }
        }

        // Search is complete.
        ret[0] = null;
        return true;
    }



    //************************************************************
    // Fundamental Red-Black Tree Operations
    //************************************************************

    private static final boolean BLACK = false;
    private static final boolean RED   = true;

    /**
     * Insert the provided node, specifying the parent and whether it is a left
     * child of the given parent.
     * <p/>
     * It is assumed that the maxStop stuff has been calculated for all nodes
     * already.
     *
     * @param node   Node to insert
     * @param parent Parent
     * @param left   Try left.
     */
    private void insertNode( Node node, Node parent, boolean left ) {
        mSize++;
        mModCount++;
        if( parent == null ) {
            mRoot = node;
            node.mColor = BLACK;
            return;
        }

        node.mParent = parent;

        if( left ) {
            assert (parent.mLeft == null);
            parent.mLeft = node;
        } else {
            assert (parent.mRight == null);
            parent.mRight = node;
        }

        while( true ) {
            if( parent == null ) {
                node.mColor = BLACK;
                return;
            }

            node.mColor = RED;
            if( parent.mColor == BLACK ) {
                // in this case, things are well because node does not have black parent.
                return;
            }

            Node grandParent = parent.mParent; // Don't worry about null; parent is red so it can't be root.
            Node uncle = (grandParent.mLeft == parent ? grandParent.mRight : grandParent.mLeft);

            if( uncle != null && uncle.mColor == RED ) {
                parent.mColor = BLACK;
                uncle.mColor = BLACK;
                grandParent.mColor = RED;
                node = grandParent;
                parent = grandParent.mParent;
                left = (parent == null || parent.mLeft == node);
                continue;
            }

            if( !left && parent == grandParent.mLeft ) {
                rotateLeft( parent );
                parent = node;
                node = parent.mLeft;
                left = true;
            } else if( left && parent == grandParent.mRight ) {
                rotateRight( parent );
                parent = node;
                node = parent.mRight;
                left = false;
            }

            parent.mColor = BLACK;
            grandParent.mColor = RED;
            if( left ) {
                rotateRight( grandParent );
            } else {
                rotateLeft( grandParent );
            }
            break;
        }
    }

    /**
     * Remove the given node. Does update mMaxStop stuff.
     */
    private void removeNode( Node node ) {
        mSize--;
        mModCount++;

        // Find maxstop of node subtree for after node has been removed.
        Node subtreeStop = node.mMaxStop;

        if( subtreeStop == node ) {
            if( node.mLeft != null ) {
                if( node.mRight != null ) {
                    subtreeStop = maxStopNode( node.mLeft.mMaxStop, node.mRight.mMaxStop );
                } else {
                    subtreeStop = node.mLeft.mMaxStop;
                }
            } else if( node.mRight != null ) {
                subtreeStop = node.mRight.mMaxStop;
            } else {
                subtreeStop = null;
            }
        }

        // Fix maxstops on all parents.
        {
            Node child = node;
            Node parent = node.mParent;
            Node newStop = subtreeStop == null ? parent : subtreeStop;

            while( parent != null && parent.mMaxStop == node ) {
                newStop = maxStopNode( newStop, parent );

                Node other = parent.mLeft == child ? parent.mRight : parent.mLeft;
                if( other != null ) {
                    newStop = maxStopNode( newStop, other.mMaxStop );
                }

                parent.mMaxStop = newStop;
                child = parent;
                parent = parent.mParent;
            }
        }

        // If we are deleting a node with two children, swap
        // it with a node that has at most one child.
        Node swapNode = null;

        if( node.mLeft != null && node.mRight != null ) {
            swapNode = node.mLeft;
            while( swapNode.mRight != null ) {
                swapNode = swapNode.mRight;
            }

            swapNodes( node, swapNode );
            swapNode.mMaxStop = subtreeStop == null ? swapNode : subtreeStop;
        }

        // We are now guaranteed that node has no more than one non-null child.
        // We now relabel the node to be deleted "oldParent", the parent of that
        // deletion node "newParent", and it's child "node".
        Node oldParent = node;
        Node newParent = node.mParent;

        node = (node.mLeft == null ? node.mRight : node.mLeft);

        // Set parent of child node to be newParent.
        if( node != null ) {
            node.mParent = newParent;
        }

        // Set child of newParent to node.
        if( newParent == null ) {
            mRoot = node;
        } else {
            // left = newParent.mLeft == oldParent;
            if( newParent.mLeft == oldParent ) {
                newParent.mLeft = node;
            } else {
                newParent.mRight = node;
            }

            // Travel up newparent thread and update mMaxStop.
            Node n = newParent;
            while( n != null ) {
                if( n.mMaxStop != swapNode ) {
                    break;
                }

                n.mMaxStop = n;

                if( n.mRight != null ) {
                    n.mMaxStop = maxStopNode( n.mMaxStop, n.mRight.mMaxStop );
                }

                if( n.mLeft != null ) {
                    n.mMaxStop = maxStopNode( n.mMaxStop, n.mLeft.mMaxStop );
                }

                n = n.mParent;
            }
        }

        // If oldParent was RED, the constraints will be maintained.
        if( oldParent.mColor == RED ) {
            return;
        }

        // If the oldParent is BLACK and the node is RED, we swap colors.
        if( node != null && node.mColor == RED ) {
            node.mColor = BLACK;
            return;
        }

        // If both oldParent and child are black, we're in a world of pain and
        // must rebalance the tree.
        while( true ) {

            // Case 1: node is new root. We're done.
            if( newParent == null ) {
                return;
            }

            // Case 2: Sibling is RED. Reverse newParent and sibling colors and
            // rotate at newParent. (If tree was balanced before,
            // sibling is guaranteed to be non-null.)
            boolean left = node == newParent.mLeft;
            Node sibling = left ? newParent.mRight : newParent.mLeft;

            if( sibling.mColor == RED ) {
                newParent.mColor = RED;
                sibling.mColor = BLACK;

                if( left ) {
                    rotateLeft( newParent );
                    sibling = newParent.mRight;
                } else {
                    rotateRight( newParent );
                    sibling = newParent.mLeft;
                }
            }

            if( ( sibling.mLeft  == null || sibling.mLeft.mColor  == BLACK ) &&
                ( sibling.mRight == null || sibling.mRight.mColor == BLACK ) )
            {
                if( newParent.mColor == BLACK ) {
                    // Case 3: newParent, sibling, and sibling's children are
                    // black.
                    // Repaint sibling red and reiterate through loop.
                    sibling.mColor = RED;
                    node = newParent;
                    newParent = node.mParent;
                    continue;
                } else {
                    // Case 4: sibling and sibling's children are black, but
                    // newParent is red. In this case, swap colors between
                    // newParent and sibling.
                    sibling.mColor = RED;
                    newParent.mColor = BLACK;
                    return;
                }
            }

            // Case 5: sibling is black but has at least one red child.
            // Here we perform a series of rotations to balance out the tree.
            if( left ) {
                if( sibling.mRight == null || sibling.mRight.mColor == BLACK ) {
                    rotateRight( sibling );
                    sibling = sibling.mParent;
                }

                sibling.mColor = newParent.mColor;
                sibling.mRight.mColor = BLACK;
                rotateLeft( newParent );

            } else {
                if( sibling.mLeft == null || sibling.mLeft.mColor == BLACK ) {
                    rotateLeft( sibling );
                    sibling = sibling.mParent;
                }

                sibling.mColor = newParent.mColor;
                sibling.mLeft.mColor = BLACK;
                rotateRight( newParent );

            }

            newParent.mColor = BLACK;
            break;
        }
    }

    /**
     * Does not alter colors, but rotates left, setting up all the parent/child
     * relationships
     * <p/>
     * Here's roughly what a rotation does: If "node" has left child A, right
     * child B, then after the rotation B will be where "node" was, "node" will
     * be the left child of B, and "node's" right child will be B's old left
     * child.
     */
    private void rotateLeft( final Node node ) {
        final Node right = node.mRight;
        if( right == null ) {
            return;
        }

        node.mRight = right.mLeft;
        if( node.mRight != null ) {
            node.mRight.mParent = node;
        }

        right.mLeft = node;

        if( node == mRoot ) {
            mRoot = right;
            right.mParent = null;
            node.mParent = right;

        } else {
            right.mParent = node.mParent;
            node.mParent = right;

            if( node == right.mParent.mLeft ) {
                right.mParent.mLeft = right;
            } else {
                right.mParent.mRight = right;
            }
        }

        // Interval tree: Must update mMaxStop values.
        // "right" is now the top of subtree previously rooted under "node" and
        // thus inherits maxStop.
        right.mMaxStop = node.mMaxStop;

        // "node" needs to check the mMaxStop values of it's children.
        if( node.mMaxStop != node ) {
            node.mMaxStop = node;

            if( node.mRight != null ) {
                node.mMaxStop = maxStopNode( node.mMaxStop, node.mRight.mMaxStop );
            }

            if( node.mLeft != null ) {
                node.mMaxStop = maxStopNode( node.mMaxStop, node.mLeft.mMaxStop );
            }
        }
    }

    /**
     * Does not alter colors, but rotates right, setting up all the parent/child
     * relationships. Does the symmetric thing to rotateLeft.
     */
    private void rotateRight( final Node node ) {
        final Node left = node.mLeft;
        if( left == null ) {
            return;
        }

        node.mLeft = left.mRight;
        left.mRight = node;

        if( node.mLeft != null ) {
            node.mLeft.mParent = node;
        }

        if( node == mRoot ) {
            mRoot = left;
            left.mParent = null;
            node.mParent = left;

        } else {
            left.mParent = node.mParent;
            node.mParent = left;

            if( node == left.mParent.mRight ) {
                left.mParent.mRight = left;
            } else {
                left.mParent.mLeft = left;
            }
        }

        // Interval tree: Must update mMaxStop valuse.
        // "left" is now the top of subtree previously rooted under "node" and
        // thus inherits maxStop.
        left.mMaxStop = node.mMaxStop;

        // "node" needs to check the mMaxStop values of it's children.
        if( node.mMaxStop != node ) {
            node.mMaxStop = node;

            if( node.mRight != null ) {
                node.mMaxStop = maxStopNode( node.mMaxStop, node.mRight.mMaxStop );
            }

            if( node.mLeft != null ) {
                node.mMaxStop = maxStopNode( node.mMaxStop, node.mLeft.mMaxStop );
            }
        }
    }

    /**
     * Will not update mMaxStop.
     */
    private void swapNodes( Node a, Node b ) {
        if( a.mParent == b ) {
            swapNodes( b, a );
            return;
        }

        {
            boolean tempColor = a.mColor;
            a.mColor = b.mColor;
            b.mColor = tempColor;
        }

        Node tempNode;
        if( a.mLeft == b ) {
            a.mLeft = b.mLeft;
            b.mLeft = a;
            if( a.mLeft != null ) {
                a.mLeft.mParent = a;
            }

            tempNode = a.mRight;
            a.mRight = b.mRight;
            b.mRight = tempNode;

            if( a.mRight != null ) {
                a.mRight.mParent = a;
            }
            if( b.mRight != null ) {
                b.mRight.mParent = b;
            }

            b.mParent = a.mParent;
            a.mParent = b;
            if( b.mParent == null ) {
                mRoot = b;
            } else if( b.mParent.mLeft == a ) {
                b.mParent.mLeft = b;
            } else {
                b.mParent.mRight = b;
            }

        } else if( a.mRight == b ) {
            a.mRight = b.mRight;
            b.mRight = a;
            if( a.mRight != null ) {
                a.mRight.mParent = a;
            }

            tempNode = a.mLeft;
            a.mLeft = b.mLeft;
            b.mLeft = tempNode;

            if( a.mLeft != null ) {
                a.mLeft.mParent = a;
            }
            if( b.mLeft != null ) {
                b.mLeft.mParent = b;
            }

            b.mParent = a.mParent;
            a.mParent = b;

            if( b.mParent == null ) {
                mRoot = b;
            } else if( b.mParent.mLeft == a ) {
                b.mParent.mLeft = b;
            } else {
                b.mParent.mRight = b;
            }

        } else {
            tempNode = a.mLeft;
            a.mLeft = b.mLeft;
            b.mLeft = tempNode;

            if( a.mLeft != null ) {
                a.mLeft.mParent = a;
            }
            if( b.mLeft != null ) {
                b.mLeft.mParent = b;
            }

            tempNode = a.mRight;
            a.mRight = b.mRight;
            b.mRight = tempNode;

            if( a.mRight != null ) {
                a.mRight.mParent = a;
            }
            if( b.mRight != null ) {
                b.mRight.mParent = b;
            }

            tempNode = a.mParent;
            a.mParent = b.mParent;
            b.mParent = tempNode;

            if( a.mParent == null ) {
                mRoot = a;
            } else if( a.mParent.mLeft == b ) {
                a.mParent.mLeft = a;
            } else {
                a.mParent.mRight = a;
            }

            if( b.mParent == null ) {
                mRoot = b;
            } else if( b.mParent.mLeft == a ) {
                b.mParent.mLeft = b;
            } else {
                b.mParent.mRight = b;
            }
        }
    }



    //************************************************************
    // Node classes
    //************************************************************

    private final class Node implements Map.Entry<K, V> {
        // Node data
        public final K mKey;
        public       V mValue;

        // RB tree stuff
        public boolean mColor  = RED;
        public Node    mParent = null;
        public Node    mLeft   = null;
        public Node    mRight  = null;

        // Interval tree stuff: The descendent (or this) node with the greatest
        // stop value
        public Node mMaxStop;

        public Node( K key, V value ) {
            mKey = key;
            mValue = value;
            mMaxStop = this;
        }


        @Override
        public K getKey() {
            return mKey;
        }

        @Override
        public V getValue() {
            return mValue;
        }

        @Override
        public V setValue( Object value ) {
            V ret = mValue;
            mValue = (V)value;
            return ret;
        }


        @Override
        public int hashCode() {
            int code = mKey.hashCode();

            if( mValue != null ) {
                code ^= mValue.hashCode();
            }

            return code;
        }

        @Override
        public boolean equals( Object obj ) {
            if( !(obj instanceof Map.Entry) ) {
                return false;
            }

            Object key = ((Map.Entry<K, V>)obj).getKey();
            if( mKey == key || mKey.equals( key ) ) {
                Object value = ((Map.Entry<K, V>)obj).getValue();

                return mValue == value || mValue != null && mValue.equals( value );
            }

            return false;
        }

        @Override
        public String toString() {
            return String.format( "%s = %s", mKey, mValue );
        }

    }



    /* ************************************************************
     * Iterators.
     * ************************************************************/


    private static int iterSize( Iterator<?> iter ) {
        int size = 0;
        while( iter.hasNext() ) {
            iter.next();
            size++;
        }
        return size;
    }


    private abstract class AbstractIterator<E> implements Iterator<E> {
        int  mIterModCount = mModCount;
        Node mPrev         = null;
        Node mNext         = null;

        AbstractIterator( Node first ) {
            mNext = first;
        }

        @Override
        public boolean hasNext() {
            return mNext != null;
        }

        @Override
        public void remove() {
            if( mPrev == null ) {
                throw new IllegalStateException();
            }

            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }

            removeNode( mPrev );
            mPrev = null;
            mIterModCount = mModCount;
        }

        @Override
        public abstract E next();

        Node nextIterNode() {
            if( mNext == null ) {
                throw new NoSuchElementException();
            }

            if( mModCount != mIterModCount ) {
                throw new ConcurrentModificationException();
            }

            mPrev = mNext;
            mNext = doNextIterNode( mNext );

            return mPrev;
        }

        abstract Node doNextIterNode( Node node );
    }


    private abstract class AllIterator<E> extends AbstractIterator<E> {
        private final boolean mDescending;

        AllIterator( boolean descending ) {
            super( descending ? lastNode() : firstNode() );
            mDescending = descending;
        }

        @Override
        Node doNextIterNode( Node node ) {
            return mDescending ? prevNode( node ) : nextNode( node );
        }
    }


    private abstract class EquivIterator<E> extends AbstractIterator<E> {
        private final K       mKey;
        private final boolean mDescending;

        EquivIterator( K key, boolean descending ) {
            super( descending ? lastEquivNode( key ) : firstEquivNode( key ) );
            mKey = key;
            mDescending = descending;
        }

        @Override
        Node doNextIterNode( Node node ) {
            return mDescending ? prevEquivNode( node, mKey ) : nextEquivNode( node, mKey );
        }
    }


    private abstract class IntersectingIterator<E> extends AbstractIterator<E> {
        final K       mKey;
        final boolean mDescending;

        IntersectingIterator( K key, boolean descending ) {
            super( descending ? lastIntersectionNode( key ) : firstIntersectionNode( mRoot, key ) );
            mKey = key;
            mDescending = descending;
        }

        @Override
        Node doNextIterNode( Node node ) {
            return mDescending ? prevIntersectionNode( node, mKey ) : nextIntersectionNode( node, mKey );
        }
    }


    private abstract class SupersetIterator<E> extends AbstractIterator<E> {
        final K       mKey;
        final boolean mDescending;

        SupersetIterator( K key, boolean descending ) {
            super( descending ? lastSupersetNode( key ) : firstSupersetNode( key ) );
            mKey = key;
            mDescending = descending;
        }

        @Override
        Node doNextIterNode( Node node ) {
            return mDescending ? prevSupersetNode( node, mKey ) : nextSupersetNode( node, mKey );
        }
    }


    private abstract class SubsetIterator<E> extends AbstractIterator<E> {
        final K       mKey;
        final boolean mDescending;

        SubsetIterator( K key, boolean descending ) {
            super( descending ? lastSubsetNode( key ) : firstSubsetNode( key ) );
            mKey = key;
            mDescending = descending;
        }

        @Override
        Node doNextIterNode( Node node ) {
            return mDescending ? prevSubsetNode( node, mKey ) : nextSubsetNode( node, mKey );
        }
    }



    /* ************************************************************
     * Views
     * ************************************************************/


    private abstract class AbstractKeySet extends AbstractCollection<K> implements Set<K> {
        final boolean mDescending;

        AbstractKeySet( boolean descending ) {
            mDescending = descending;
        }


        @Override
        public boolean contains( Object obj ) {
            Node node = firstEquivNode( (K)obj );
            return node != null && keyInSubset( node.mKey );
        }

        @Override
        public boolean remove( Object obj ) {
            if( obj == null ) {
                return false;
            }

            Node node = mDescending ? lastEquivNode( (K)obj ) : firstEquivNode( (K)obj );
            if( node == null || !keyInSubset( node.mKey ) ) {
                return false;
            }

            removeNode( node );
            return true;
        }

        @Override
        public boolean isEmpty() {
            return firstNodeInSubset() == null;
        }

        @Override
        public int size() {
            return iterSize( iterator() );
        }


        abstract Node firstNodeInSubset();

        abstract boolean keyInSubset( K key );
    }


    private final class KeySet extends AbstractSet<K> {
        private final boolean mDescending;

        KeySet( boolean descending ) {
            mDescending = descending;
        }


        @Override
        public boolean contains( Object key ) {
            return containsKey( key );
        }

        @Override
        public boolean remove( Object key ) {
            if( key == null ) {
                return false;
            }

            Node node = (mDescending ? lastEquivNode( (K)key ) : firstEquivNode( (K)key ));
            if( node == null ) {
                return false;
            }

            removeNode( node );
            return true;
        }

        @Override
        public boolean isEmpty() {
            return mSize > 0;
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public void clear() {
            IntervalMap.this.clear();
        }

        @Override
        public Iterator<K> iterator() {
            return new AllIterator<K>( mDescending ) {
                @Override
                public K next() {
                    return nextIterNode().mKey;
                }
            };
        }
    }


    private final class EquivKeySet extends AbstractKeySet {
        private final K mKey;

        EquivKeySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<K> iterator() {
            return new EquivIterator<K>( mKey, mDescending ) {
                @Override
                public K next() {
                    return nextIterNode().mKey;
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstEquivNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) == 0 &&
                   mComp.compareMaxes( key, mKey ) == 0;
        }
    }


    private final class IntersectionKeySet extends AbstractKeySet {
        private final K mKey;

        IntersectionKeySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<K> iterator() {
            return new IntersectingIterator<K>( mKey, mDescending ) {
                @Override
                public K next() {
                    return nextIterNode().mKey;
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstIntersectionNode( mRoot, mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMinToMax( key, mKey ) < 0 &&
                   mComp.compareMinToMax( mKey, key ) < 0;
        }
    }


    private final class SupersetKeySet extends AbstractKeySet {
        private final K mKey;

        SupersetKeySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<K> iterator() {
            return new SupersetIterator<K>( mKey, mDescending ) {
                @Override
                public K next() {
                    return nextIterNode().mKey;
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstSupersetNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) >= 0 &&
                   mComp.compareMaxes( key, mKey ) <= 0;
        }
    }


    private final class SubsetKeySet extends AbstractKeySet {
        private final K mKey;

        SubsetKeySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<K> iterator() {
            return new SubsetIterator<K>( mKey, mDescending ) {
                @Override
                public K next() {
                    return nextIterNode().mKey;
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstSubsetNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) >= 0 &&
                   mComp.compareMaxes( key, mKey ) <= 0;
        }
    }


    private final class Values extends AbstractCollection<V> {
        private final boolean mDescending;

        Values( boolean descending ) {
            mDescending = descending;
        }


        @Override
        public boolean contains( Object obj ) {
            return containsValue( obj );
        }

        @Override
        public void clear() {
            IntervalMap.this.clear();
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public boolean isEmpty() {
            return mSize <= 0;
        }


        @Override
        public Iterator<V> iterator() {
            return new AllIterator<V>( mDescending ) {
                @Override
                public V next() {
                    return nextIterNode().mValue;
                }
            };
        }
    }


    private final class EquivValues extends AbstractCollection<V> {
        private final K       mKey;
        private final boolean mDescending;

        EquivValues( K key, boolean descending ) {
            mKey = key;
            mDescending = descending;
        }


        @Override
        public int size() {
            return iterSize( iterator() );
        }

        @Override
        public boolean isEmpty() {
            return firstEquivNode( mKey ) == null;
        }

        @Override
        public Iterator<V> iterator() {
            return new EquivIterator<V>( mKey, mDescending ) {
                @Override
                public V next() {
                    return nextIterNode().mValue;
                }
            };
        }
    }


    private final class IntersectionValues extends AbstractCollection<V> {
        private final K       mKey;
        private final boolean mDescending;

        IntersectionValues( K key, boolean descending ) {
            mKey = key;
            mDescending = descending;
        }

        @Override
        public int size() {
            return iterSize( iterator() );
        }

        @Override
        public boolean isEmpty() {
            return firstIntersectionNode( mRoot, mKey ) == null;
        }

        @Override
        public Iterator<V> iterator() {
            return new IntersectingIterator<V>( mKey, mDescending ) {
                @Override
                public V next() {
                    return nextIterNode().mValue;
                }
            };
        }
    }


    private final class SupersetValues extends AbstractCollection<V> {
        private final K       mKey;
        private final boolean mDescending;

        SupersetValues( K key, boolean descending ) {
            mKey = key;
            mDescending = descending;
        }


        @Override
        public int size() {
            return iterSize( iterator() );
        }

        @Override
        public boolean isEmpty() {
            return firstSupersetNode( mKey ) == null;
        }

        @Override
        public Iterator<V> iterator() {
            return new SupersetIterator<V>( mKey, mDescending ) {
                @Override
                public V next() {
                    return nextIterNode().mValue;
                }
            };
        }
    }


    private final class SubsetValues extends AbstractCollection<V> {
        private final K       mKey;
        private final boolean mDescending;

        SubsetValues( K key, boolean descending ) {
            mKey = key;
            mDescending = descending;
        }


        @Override
        public int size() {
            return iterSize( iterator() );
        }

        @Override
        public boolean isEmpty() {
            return firstSubsetNode( mKey ) == null;
        }

        @Override
        public Iterator<V> iterator() {
            return new SubsetIterator<V>( mKey, mDescending ) {
                @Override
                public V next() {
                    return nextIterNode().mValue;
                }
            };
        }
    }


    private abstract class AbstractEntrySet extends AbstractCollection<Map.Entry<K, V>> implements Set<Map.Entry<K, V>> {

        final boolean mDescending;

        AbstractEntrySet( boolean descending ) {
            mDescending = descending;
        }


        @Override
        public boolean contains( Object obj ) {
            Map.Entry<K, V> e = (Map.Entry<K, V>)obj;
            final K key = e.getKey();
            final V value = e.getValue();

            if( !keyInSubset( key ) ) {
                return false;
            }

            Node node = firstEquivNode( key );

            while( node != null ) {
                if( value == node.mValue || value != null && value.equals( node.mValue ) ) {
                    return true;
                }

                node = nextEquivNode( node, key );
            }

            return false;
        }

        @Override
        public boolean remove( Object obj ) {
            Map.Entry<K, V> e = (Map.Entry<K, V>)obj;
            final K key = e.getKey();
            final V value = e.getValue();

            if( !keyInSubset( key ) ) {
                return false;
            }

            if( !mDescending ) {
                Node node = firstEquivNode( key );

                while( node != null ) {
                    if( value == node.mValue || value != null && value.equals( node.mValue ) ) {
                        removeNode( node );
                        return true;
                    }

                    node = nextEquivNode( node, key );
                }

            } else {
                Node node = lastEquivNode( key );

                while( node != null ) {
                    if( value == node.mValue || value != null && value.equals( node.mValue ) ) {
                        removeNode( node );
                        return true;
                    }

                    node = prevEquivNode( node, key );
                }

            }

            return false;
        }

        @Override
        public boolean isEmpty() {
            return firstNodeInSubset() == null;
        }

        @Override
        public int size() {
            return iterSize( iterator() );
        }


        abstract Node firstNodeInSubset();

        abstract boolean keyInSubset( K key );

    }


    private final class EntrySet extends AbstractEntrySet {

        EntrySet( boolean descending ) {
            super( descending );
        }


        @Override
        public boolean isEmpty() {
            return mSize <= 0;
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public void clear() {
            IntervalMap.this.clear();
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new AllIterator<Map.Entry<K, V>>( mDescending ) {
                @Override
                public Map.Entry<K, V> next() {
                    return nextIterNode();
                }
            };
        }

        @Override
        Node firstNodeInSubset() {
            return firstNode();
        }

        @Override
        boolean keyInSubset( K key ) {
            return true;
        }
    }


    private final class EquivEntrySet extends AbstractEntrySet {
        private final K mKey;

        EquivEntrySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EquivIterator<Map.Entry<K, V>>( mKey, mDescending ) {
                @Override
                public Map.Entry<K, V> next() {
                    return nextIterNode();
                }
            };
        }

        @Override
        Node firstNodeInSubset() {
            return firstEquivNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) == 0 &&
                   mComp.compareMaxes( key, mKey ) == 0;
        }
    }


    private final class IntersectionEntrySet extends AbstractEntrySet {
        private final K mKey;

        IntersectionEntrySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new IntersectingIterator<Map.Entry<K, V>>( mKey, mDescending ) {
                @Override
                public Map.Entry<K, V> next() {
                    return nextIterNode();
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstIntersectionNode( mRoot, mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMinToMax( key, mKey ) < 0 &&
                   mComp.compareMinToMax( mKey, key ) < 0;
        }

    }


    private final class SupersetEntrySet extends AbstractEntrySet {

        private final K mKey;

        SupersetEntrySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new SupersetIterator<Map.Entry<K, V>>( mKey, mDescending ) {
                @Override
                public Map.Entry<K, V> next() {
                    return nextIterNode();
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstSupersetNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) >= 0 &&
                   mComp.compareMaxes( key, mKey ) <= 0;
        }
    }


    private final class SubsetEntrySet extends AbstractEntrySet {
        private final K mKey;

        SubsetEntrySet( K key, boolean descending ) {
            super( descending );
            mKey = key;
        }


        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new SubsetIterator<Map.Entry<K, V>>( mKey, mDescending ) {
                @Override
                public Map.Entry<K, V> next() {
                    return nextIterNode();
                }
            };
        }


        @Override
        Node firstNodeInSubset() {
            return firstSubsetNode( mKey );
        }

        @Override
        boolean keyInSubset( K key ) {
            return mComp.compareMins( key, mKey ) <= 0 &&
                   mComp.compareMaxes( key, mKey ) >= 0;
        }
    }

}
