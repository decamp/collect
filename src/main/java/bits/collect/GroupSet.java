/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;

/** 
 * Something like a {@link java.util.SortedSet} that provides two different indices over its
 * elements, each based on a Red-Black tree algorithm.  Each element is 
 * stored in a tree that contains all elements in the GroupSet, and stored
 * again in a tree containing only the elements for a given "group".
 * <p>
 * Groups may be null, elements may not be null.  The same element may be added
 * to a given group only once, but may be added to the GroupSet multiple times
 * under different groups.
 * <p>
 * Within a given group, elements are sorted by a comparator or, if none is 
 * provided, by their natural ordering.  As in SortedSet, the comparison used 
 * should be consistent with equals.
 * <p>
 * Within the GroupSet, elements are ordered first by the mValue comparison, and
 * then by a group comparator (or, if no group comparator is provided, by the
 * natural ordering of the groups).  Because groups are both ordered and stored
 * in a hash table, it is very important that the group comparison is 
 * consistent with equals.
 * <p>
 * In general, the interface to GroupSet is consistent with Set.  However, most
 * methods have two versions: one that specifies a group and one that doesn't.
 * While some methods that don't specify a group may use the "null" group by 
 * default (<tt>add</tt> and <tt>remove</tt>), some of the methods that do not 
 * specify a group operate over all groups (<tt>size</tt>, <tt>clear</tt> and 
 * <tt>contains</tt>).  See method documentation for specific details. 
 *
 * @param <G>  Group type
 * @param <V>  Element type 
 * @author Philip DeCamp  
 */
@SuppressWarnings("unchecked")
public class GroupSet<G,V> extends AbstractSet<V> {

    static final int DEFAULT_INITIAL_GROUP_CAPACITY = 16;
    static final float DEFAULT_GROUP_LOAD_FACTOR = 0.75f;
    static final int MAXIMUM_GROUP_CAPACITY = 1 << 30;
    
    private final Comparator<? super G> mGroupComparator;
    private final Comparator<? super V> mValueComparator;
    
    //Common variables.
    private transient int mModCount = 0;
    private transient Groups mGroups = null;
    
    //Group table variables.
    private GroupNode[] mGroupBuckets;
    private final float mGroupLoadFactor;
    private int mGroupThreshold;
    private transient int mGroupCount = 0;
    
    //All node tree variables.
    private Node mAllRoot = null;
    private transient int mAllCount = 0;
    
    

    public GroupSet() {
        this(null, null);
    }
    
    public GroupSet(Comparator<? super G> groupComparator, Comparator<? super V> valueComparator) {
        mGroupComparator = groupComparator;
        mValueComparator = valueComparator;
        mGroupLoadFactor = DEFAULT_GROUP_LOAD_FACTOR;
        
        int initCapacity = DEFAULT_INITIAL_GROUP_CAPACITY;
        int capacity = 1;
        while(capacity < initCapacity)
            capacity <<= 1;
        
        mGroupBuckets = new GroupNode[capacity];
        mGroupThreshold = (int)(capacity * mGroupLoadFactor);
    }

    
    /**
     * Equivalent to calling <tt>add(null, mValue)</tt>
     * 
     * @param value Value to add to set.
     * @return true iff collection is modified as a result.
     */
    @Override
    public boolean add(V value) {
        return add(null, value);
    }
    
    /**
     * Adds a mValue to the set and to the group within the set.
     * 
     * @param group Group into which mValue should be placed.
     * @param value Value to add to set.
     * @return true iff collection is modified as a result.
     */
    public boolean add(G group, V value) {
        if(value == null)
            return false;
        
        //Insert node into group.
        GroupNode groupNode = getGroupNodeInstance(group);
        Node node = groupNode.mRoot;
        Node newNode = null;
        
        if(node == null) {
            newNode = new Node(groupNode, value);
            insertNodeGroup(groupNode, newNode, null, LEFT);
            
        }else{
            if(mValueComparator != null) {
                Comparator<? super V> comp = mValueComparator;
                
                while(true) {
                    int c = comp.compare(value, (V)node.mValue);
                    
                    if(c < 0) {
                        if(node.mGroupLeft == null) {
                            newNode = new Node(groupNode, value);
                            insertNodeGroup(groupNode, newNode, node, LEFT);
                            break;
                        }else{
                            node = node.mGroupLeft;
                        }
                            
                    }else if(c > 0) {
                        if(node.mGroupRight == null) {
                            newNode = new Node(groupNode, value);
                            insertNodeGroup(groupNode, newNode, node, RIGHT);
                            break;
                        }else{
                            node = node.mGroupRight;
                        }
                        
                    }else{
                        return false;
                    }
                }        
                
            }else{
                Comparable<? super V> comp = (Comparable<? super V>)value;
                
                while(true) {
                    int c = comp.compareTo((V)node.mValue);
                    
                    if(c < 0) {
                        if(node.mGroupLeft == null) {
                            newNode = new Node(groupNode, value);
                            insertNodeGroup(groupNode, newNode, node, LEFT);
                            break;
                        }else{
                            node = node.mGroupLeft;
                        }
                        
                    }else if(c > 0) {
                        if(node.mGroupRight == null) {
                            newNode = new Node(groupNode, value);
                            insertNodeGroup(groupNode, newNode, node, RIGHT);
                            break;
                        }else{
                            node = node.mGroupRight;
                        }
                    }else{
                        return false;
                    }
                }
            }
        }
        
        //Insert node into all.
        node = mAllRoot;
        
        if(node == null) {
            insertNodeAll(newNode, null, LEFT);
            return true;
        }
        
        if(mValueComparator != null) {
            if(mGroupComparator != null) {
                Comparator<? super V> comp = mValueComparator;
                Comparator<? super G> groupComp = mGroupComparator;
                
                while(true) {
                    int c = comp.compare(value, (V)node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return true;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return true;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return true;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compare(group, nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return true;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return true;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return true;
                            }
                        }
                    }
                }
            
            }else{
                Comparator<? super V> comp = mValueComparator;
                Comparable<? super G> groupComp = (Comparable<? super G>)group;
                
                while(true) {
                    int c = comp.compare(value, (V)node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return true;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return true;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return true;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compareTo(nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return true;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return true;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return true;
                            }
                        }
                    }
                }
            }
        }else{
            if(mGroupComparator != null) {
                Comparable<? super V> comp = (Comparable<? super V>)value;
                Comparator<? super G> groupComp = mGroupComparator;
                
                while(true) {
                    int c = comp.compareTo((V)node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return true;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return true;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return true;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compare(group, nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return true;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return true;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return true;
                            }
                        }
                    }
                }
            
            }else{
                Comparable<? super V> comp = (Comparable<? super V>)value;
                Comparable<? super G> groupComp = (Comparable<? super G>)group;
                
                while(true) {
                    int c = comp.compareTo((V)node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return true;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return true;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return true;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compareTo(nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return true;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return true;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return true;
                            }
                        }
                    }
                }
            }            
        }
    }
    
    /**
     * Equivalent to calling <tt>addAll(null, values)</tt>
     * 
     * @param values Values to add to set.
     * @return true iff collection is modified as a result.
     */
    @Override
    public boolean addAll(Collection<? extends V> values) {
        return addAll(null, values);
    }

    /**
     * Adds collection of values into set and into one of the groups within
     * the set.
     * 
     * @param group Group into which values will be added.
     * @param values Values to add to set.
     * @return true iff collection is modified as a result.
     */
    public boolean addAll(G group, Collection<? extends V> values) {
        boolean modified = false;
        
        for(V value: values) {
            if(value == null)
                throw new NullPointerException();
            
            modified |= add(group, value);
        }
        
        return modified;
    }
    
    /**
     * Removes a mValue from the "null" group.
     * 
     * @param obj Value to remove from the collection.
     * @return true iff the collection is modified as a result.
     */
    @Override
    public boolean remove(Object obj) {
        return remove(null, obj);
    }
    
    /**
     * Removes a mValue from a given group.
     * 
     * @param group Group from which mValue should be removed.
     * @param obj Object to remove from group.
     * @return true iff the collection is modified as a result.
     */
    public boolean remove(G group, Object obj) {
        if(obj == null)
            return false;
        
        V value = (V)obj;
        
        //Find group.
        final GroupNode[] buckets = mGroupBuckets;
        final int idx;
        GroupNode prevGroupNode = null;
        GroupNode groupNode = null;
        
        {
            final int hash = (group == null ? 0 : rehash(group.hashCode()));
            idx = hash & (buckets.length - 1);
            groupNode = buckets[idx];
            
            while(groupNode != null) {
                if(hash == groupNode.mHash && (group == groupNode.mGroup || group != null && group.equals(groupNode.mGroup)))
                    break;
                
                prevGroupNode = groupNode;
                groupNode = groupNode.mNext;
            }
            
            if(groupNode == null)
                return false;
        }
        
        //Find node.
        Node node = groupNode.mRoot;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(true) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c < 0) {
                    if(node.mGroupLeft == null)
                        return false;
                    
                    node = node.mGroupLeft;
                    
                }else if(c > 0) {
                    if(node.mGroupRight == null)
                        return false;
                    
                    node = node.mGroupRight;
                    
                }else{
                    removeNodeGroup(groupNode, node);
                    removeNodeAll(node);
                    break;
                }
            }
            
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(true) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c < 0) {
                    if(node.mGroupLeft == null)
                        return false;
                    
                    node = node.mGroupLeft;
                    
                }else if(c > 0) {
                    if(node.mGroupRight == null)
                        return false;
                    
                    node = node.mGroupRight;
                    
                }else{
                    removeNodeGroup(groupNode, node);
                    removeNodeAll(node);
                    break;
                }
            }
        }
        
        //Remove group if empty.
        if(groupNode.mCount == 0) {
            mModCount++;
            mGroupCount--;
            
            if(prevGroupNode == null) {
                buckets[idx] = groupNode.mNext;
            }else{
                prevGroupNode.mNext = groupNode.mNext;
            }
        }
        
        return true;
    }
    
    
    @Override
    public boolean removeAll(Collection<?> values) {
        return removeAll(null, values);
    }
    
    
    public boolean removeAll(G group, Collection<?> values) {
        boolean modified = false;
        
        for(Object value: values) {
            modified |= remove(group, value);
        }
        
        return modified;
    }

    /**
     * Removes all elements in the collection.  Removes all groups.
     */
    @Override
    public void clear() {
        mModCount++;
        mAllRoot = null;
        Arrays.fill(mGroupBuckets, null);
        mGroupCount = 0;
        mAllCount = 0;
    }

    /**
     * Removes all elements that belong to a given group.
     * 
     * @param group Group to remove from collection.
     */
    public void clear(G group) {
        //Find group.
        final GroupNode[] buckets = mGroupBuckets;
        final int idx;
        GroupNode prevGroupNode = null;
        GroupNode groupNode = null;
        
        {
            final int hash = (group == null ? 0 : rehash(group.hashCode()));
            idx = hash & (buckets.length - 1);
            groupNode = buckets[idx];
            
            while(groupNode != null) {
                if(hash == groupNode.mHash && (group == groupNode.mGroup || group != null && group.equals(groupNode.mGroup)))
                    break;
                
                prevGroupNode = groupNode;
                groupNode = groupNode.mNext;
            }
            
            if(groupNode == null)
                return;
        }
        
        mModCount++;
        mGroupCount--;
        
        if(prevGroupNode == null) {
            buckets[idx] = groupNode.mNext;
        }else{
            prevGroupNode.mNext = groupNode.mNext;
        }
        
        Node node = firstNodeGroup(groupNode);
        while(node != null) {
            removeNodeAll(node);
            node = nextNodeGroup(node);
        }
    }
    
    /**
     * @return number of elements in the entire collection
     */
    @Override
    public int size() {
        return mAllCount;
    }
    
    /**
     * @return number of elements in a given group within the collection.
     */
    public int size(G group) {
        GroupNode g = findGroupNode(group);
        if(g == null)
            return 0;
        
        return g.mCount;
    }

    /**
     * @param obj Object to find in the collection.
     * @return true iff obj is present in any group within the collection.
     */
    @Override
    public boolean contains(Object obj) {
        return findNodeThatContains(obj) != null;
    }

    /**
     * @param group Group to look in
     * @param obj Value to look for in provided group
     * @return true iff provided mValue is a member of the provided group.
     */
    public boolean contains(G group, Object obj) {
        return findNodeThatContains(group, obj) != null;
    }

    
    @Override
    public boolean containsAll(Collection<?> c) {
        for(Object obj: c) {
            if(!contains(obj))
                return false;
        }
        
        return true;
    }
    
    
    public boolean containsAll(G group, Collection<?> c) {
        for(Object obj: c) {
            if(!contains(group, obj))
                return false;
        }
        
        return true;
    }
    
    
    public G groupContaining(Object obj) {
        Node node = findNodeThatContains(obj);
        return node == null ? null : (G)node.mGroup.mGroup;
    }
    
    
    
    public V first() {
        Node node = firstNodeAll();
        return (node == null ? null : (V)node.mValue);
    }
    
    public V first(G group) {
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = firstNodeGroup(groupNode);
        return (node == null ? null : (V)node.mValue);
    }
    
    public V first(Comparable<? super V> comp) {
        Node node = firstEquivalentAll(comp);
        return (node == null ? null : (V)node.mValue);
    }
    
    public V first(G group, Comparable<? super V> comp) {
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = firstEquivalentGroup(groupNode, comp);
        return (node == null ? null : (V)node.mValue);
    }
    
    public V last() {
        Node node = lastNodeAll();
        return (node == null ? null : (V)node.mValue);
    }

    public V last(G group) {
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = lastNodeGroup(groupNode);
        return (node == null ? null : (V)node.mValue);
    }

    public V last(Comparable<? super V> comp) {
        Node node = lastEquivalentAll(comp);
        return (node == null ? null : (V)node.mValue);
    }
    
    public V last(G group, Comparable<? super V> comp) {
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = lastEquivalentGroup(groupNode, comp);
        return (node == null ? null : (V)node.mValue);
    }
    
    public V ceiling(V value) {
        if(value == null)
            return null;
        
        Node node = mAllRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
            
                if(c <= 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c <= 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }
    
    public V ceiling(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = groupNode.mRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
            
                if(c <= 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c <= 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }
    
    public V floor(V value) {
        if(value == null)
            return null;
        
        Node node = mAllRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c < 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }

        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c < 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }
    
    public V floor(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = groupNode.mRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c < 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }

        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c < 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }

    public V higher(V value) {
        if(value == null)
            return null;
        
        Node node = mAllRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
            
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }

    public V higher(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = groupNode.mRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
            
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }

    public V lower(V value) {
        if(value == null)
            return null;
        
        Node node = mAllRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c <= 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }
            
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c <= 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }

    public V lower(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node node = groupNode.mRoot;
        Node ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, (V)node.mValue);
                
                if(c <= 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }
            
        }else{
            Comparable<? super V> comp = (Comparable<? super V>)value;
            
            while(node != null) {
                int c = comp.compareTo((V)node.mValue);
                
                if(c <= 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }
        }
        
        return (ret == null ? null : (V)ret.mValue);
    }
    

    /**
     * @return iterator over all values in the collection.
     */
    @Override
    public Iterator<V> iterator() {
        return new AllIterator(true, null);
    }
    
    
    public Set<V> subset(Comparable<? super V> comp) {
        return new AllSubset(true, comp);
    }
    
    public Set<V> groupSubset(G group) {
        return new GroupSubset(true, group, null);
    }
    
    public Set<V> groupSubset(G group, Comparable<? super V> comp) {
        return new GroupSubset(true, group, comp);
    }
    
    public Set<V> descendingSubset(Comparable<? super V> comp) {
        return new AllSubset(false, comp);
    }
    
    public Set<V> descendingSet() {
        return new AllSubset(false, null);
    }
    
    public Set<V> descendingGroupSubset(G group) {
        return new GroupSubset(false, group, null);
    }
    
    public Set<V> descendingGroupSubset(G group, Comparable<? super V> comp) {
        return new GroupSubset(false, group, comp);
    }
    
    /**
     * Provides a view to the set of groups contained in this collection.  The
     * returned view does not support <tt>add</tt> or <tt>remove</tt>, although
     * the iterator for this view supports <tt>remove</tt> (removing a group
     * with the iterator will cause all elements in that group to be removed).
     * 
     * @return A view of the groups within this set.
     */
    public Set<G> groups() {
        Groups ret = mGroups;
        return (ret != null ? ret : (ret = new Groups()));
    }
    
    
    
    private Node findNodeThatContains(Object obj) {
        if(obj == null)
            return null;

        final V value = (V)obj;
               
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            Node node = firstEquivalentAll(comp, value);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeAll(node);
            }while(node != null && comp.compare(value, (V)node.mValue) == 0);
            
        }else{
            Comparable<V> comp = (Comparable<V>)value;
            Node node = firstEquivalentAll(comp);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeAll(node);
            }while(node != null && comp.compareTo((V)node.mValue) == 0);
        }
        
        return null;
    }
    
    private Node findNodeThatContains(G group, Object obj) {
        if(obj == null)
            return null;

        final V value = (V)obj;
        
        GroupNode gn = findGroupNode(group);
        if(gn == null)
            return null;
               
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            Node node = firstEquivalentGroup(gn, comp, value);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeGroup(node);
            }while(node != null && comp.compare(value, (V)node.mValue) == 0);
            
        }else{
            Comparable<V> comp = (Comparable<V>)value;
            Node node = firstEquivalentGroup(gn, comp);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeGroup(node);
            }while(node != null && comp.compareTo((V)node.mValue) == 0);
        }
        
        return null;
    }
    
    private GroupNode findGroupNode(Object group) {
        GroupNode[] buckets = mGroupBuckets;
        final int hash = (group == null ? 0 : rehash(group.hashCode()));
        final int idx = hash & (buckets.length - 1);
        
        GroupNode g = buckets[idx];
        while(g != null) {
            if(hash == g.mHash && group == g.mGroup || group != null && group.equals(g.mGroup))
                return g;
            
            g = g.mNext;
        }
        
        return null;
    }
    
    private GroupNode getGroupNodeInstance(G group) {
        GroupNode[] buckets = mGroupBuckets;
        final int hash = (group == null ? 0 : rehash(group.hashCode()));
        final int idx = hash & (buckets.length - 1);
        
        GroupNode g = buckets[idx];
        while(g != null) {
            if(hash == g.mHash && (group == g.mGroup || group != null && group.equals(g.mGroup)))
                return g;
            
            g = g.mNext;
        }
        
        g = new GroupNode(group, hash, buckets[idx]);
        buckets[idx] = g;
        
        if(mGroupCount++ >= mGroupThreshold)
            resizeGroups(2 * buckets.length);
        
        return g;
    }
    
    private void removeGroupNode(GroupNode groupNode) {
        final GroupNode[] buckets = mGroupBuckets;
        final int idx = (groupNode.mHash & (buckets.length - 1));
        
        GroupNode parent = null;
        GroupNode g = buckets[idx];
        
        while(g != null) {
            if(groupNode == g) {
                mModCount++;
                mGroupCount--;
                
                if(parent == null) {
                    buckets[idx] = g.mNext;
                }else{
                    parent.mNext = g.mNext;
                }
                
                break;
            }
            
            parent = g;
            g = g.mNext;
        }
    }
    
    private void resizeGroups(int newCapacity) {
        GroupNode[] oldBuckets = mGroupBuckets;
        
        int oldCap = oldBuckets.length;
        if(oldCap >= MAXIMUM_GROUP_CAPACITY) {
            mGroupThreshold = Integer.MAX_VALUE;
            return;
        }
        
        GroupNode[] newBuckets = new GroupNode[newCapacity];
        
        for(int i = 0; i < oldBuckets.length; i++) {
            GroupNode groupNode = oldBuckets[i];
            
            while(groupNode != null) {
                GroupNode next = groupNode.mNext;
                
                int idx = (groupNode.mHash & (newCapacity - 1));
                groupNode.mNext = newBuckets[idx];
                newBuckets[idx] = groupNode;
                
                groupNode = next;
            }
        }
        
        mGroupBuckets = newBuckets;
        mGroupThreshold = (int)(newCapacity * mGroupLoadFactor);
    }
    
    private void removeNodeCompletely(Node node, boolean disposeGroupIfEmpty) {
        removeNodeAll(node);
        removeNodeGroup(node.mGroup, node);
        
        if(disposeGroupIfEmpty && node.mGroup.mRoot == null) {
            removeGroupNode(node.mGroup);
        }
    }
    
    private static int rehash(int hash) {
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }
    
    
    
    /*************************************************************
     * Fundamental Red-Black Tree Operations
     * 
     * These operations do not look at the mValue portion of any
     * node, and thus may be easily transferred to other classes.  These methods
     * only rely on two member variables: mRoot, mSize and mModCount.
     *************************************************************/

    private static final boolean LEFT = true;
    private static final boolean RIGHT = false;
    private static final boolean BLACK = false;
    private static final boolean RED = true;
    
    
    private void insertNodeAll(Node node, Node parent, boolean left) {
        mAllCount++;
        mModCount++;
        
        if(parent == null) {
            mAllRoot = node;
            node.mAllColor = BLACK;
            return;
        }
        
        node.mAllParent = parent;
        
        if(left) {
            parent.mAllLeft = node;
        }else{
            parent.mAllRight = node;
        }
        
        while(true) {
            if(parent == null) {
                node.mAllColor = BLACK;
                return;
            }

            node.mAllColor = RED;
            
            if(parent.mAllColor == BLACK) {
                return;
            }
            
            Node grandParent = parent.mAllParent;
            Node uncle = (grandParent.mAllLeft == parent ? grandParent.mAllRight: grandParent.mAllLeft);
            
            if(uncle != null && uncle.mAllColor == RED) {
                parent.mAllColor = BLACK;
                uncle.mAllColor = BLACK;
                grandParent.mAllColor = RED;
                
                node = grandParent;
                parent = grandParent.mAllParent;
                left = (parent == null || parent.mAllLeft == node);
                
                continue;
            }

            
            if(!left && parent == grandParent.mAllLeft) {
                rotateLeftAll(parent);
                parent = node;
                node = parent.mAllLeft;
                left = true;
                
            }else if(left && parent == grandParent.mAllRight) {
                rotateRightAll(parent);
                parent = node;
                node = parent.mAllRight;
                left = false;
            }

            parent.mAllColor = BLACK;
            grandParent.mAllColor = RED;
            
            if(left) {
                rotateRightAll(grandParent);
            }else{
                rotateLeftAll(grandParent);
            }
            
            break;
        }
    }
    
    private void removeNodeAll(Node node) {
        mAllCount--;
        mModCount++;
        
        //If we are deleting a node with two children, swap
        //it with a node that has at most one child.
        if(node.mAllLeft != null && node.mAllRight != null) {
            Node swapNode = node.mAllLeft;
            while(swapNode.mAllRight != null)
                swapNode = swapNode.mAllRight;
            
            swapNodesAll(node, swapNode);
        }
        
        //We are now guaranteed that node has no more than one non-null child.
        //We now relabel the node to be deleted "oldParent", the parent of that
        //    deletion node "newParent", and it's child "node".
        Node oldParent = node;
        Node newParent = node.mAllParent;

        node = (node.mAllLeft == null ? node.mAllRight: node.mAllLeft);

        //Set parent of child node to be newParent.
        if(node != null)
            node.mAllParent = newParent;
        
        //Set child of newParent to node.
        if(newParent == null) {
            mAllRoot = node;
            
        }else{
            //left = newParent.mLeft == oldParent;
            if(newParent.mAllLeft == oldParent) {
                newParent.mAllLeft = node;
            }else{
                newParent.mAllRight = node;
            }
        }
        
        //If oldParent was RED, the constraints will be maintained.
        if(oldParent.mAllColor == RED)
            return;
        
        //If the oldParent is BLACK and the node is RED, we swap colors.
        if(node != null && node.mAllColor == RED) {
            node.mAllColor = BLACK;
            return;
        }
        
        //If both oldParent and child are black, we're in a world of pain and 
        //must rebalance the tree.
        while(true) {
            
            //Case 1: node is new root.  We're done.
            if(newParent == null)
                return;
                
            //Case 2: Sibling is RED.  Reverse newParent and sibling colors and 
            //rotate at newParent.  (If tree was balanced before, 
            //sibling is guaranteed to be non-null.)
            boolean left = node == newParent.mAllLeft;
            Node sibling = left ? newParent.mAllRight : newParent.mAllLeft;
            
            if(sibling.mAllColor == RED) {
                newParent.mAllColor = RED;
                sibling.mAllColor = BLACK;
                
                if(left) {
                    rotateLeftAll(newParent);
                    sibling = newParent.mAllRight;
                }else{
                    rotateRightAll(newParent);
                    sibling = newParent.mAllLeft;
                }
            } 
            
            
            if((sibling.mAllLeft == null || sibling.mAllLeft.mAllColor == BLACK) &&
               (sibling.mAllRight == null || sibling.mAllRight.mAllColor == BLACK))
            {
                if(newParent.mAllColor == BLACK) {
                    //Case 3: newParent, sibling, and sibling's children are black.
                    //Repaint sibling red and reiterate through loop.
                    sibling.mAllColor = RED;
                    node = newParent;
                    newParent = node.mAllParent;
                    continue;
                }else{
                    //Case 4: sibling and sibling's children are black, but 
                    //newParent is red.  In this case, swap colors between
                    //newParent and sibling.
                    sibling.mAllColor = RED;
                    newParent.mAllColor = BLACK;
                    return;
                }
            }

            //Case 5: sibling is black but has at least one red child.
            //Here we perform a series of rotations to balance out the tree.
            if(left) {
                if(sibling.mAllRight == null || sibling.mAllRight.mAllColor == BLACK) {
                    rotateRightAll(sibling);
                    sibling = sibling.mAllParent;
                }   
                    
                sibling.mAllColor = newParent.mAllColor;
                sibling.mAllRight.mAllColor = BLACK;
                rotateLeftAll(newParent);
                
            }else{
                if(sibling.mAllLeft == null || sibling.mAllLeft.mAllColor == BLACK) {
                    rotateLeftAll(sibling);
                    sibling = sibling.mAllParent;
                }
                
                sibling.mAllColor = newParent.mAllColor;
                sibling.mAllLeft.mAllColor = BLACK;
                rotateRightAll(newParent);
                
            }

            newParent.mAllColor = BLACK;
            break;
        }
    }
    
    private void rotateLeftAll(Node node) {
        Node right = node.mAllRight;
        if(right == null)
            return;
        
        node.mAllRight = right.mAllLeft;
        if(node.mAllRight != null)
            node.mAllRight.mAllParent = node;
        
        right.mAllLeft = node;
        
        if(node == mAllRoot) {
            mAllRoot = right;
            right.mAllParent = null;
            node.mAllParent = right;
        }else{
            right.mAllParent = node.mAllParent;
            node.mAllParent = right;
            
            if(node == right.mAllParent.mAllLeft) {
                right.mAllParent.mAllLeft = right;
            }else{
                right.mAllParent.mAllRight = right;
            }
        }
    }
    
    private void rotateRightAll(Node node) {
        Node left = node.mAllLeft;
        if(left == null)
            return;
        
        node.mAllLeft = left.mAllRight;
        left.mAllRight = node;
        
        if(node.mAllLeft != null)
            node.mAllLeft.mAllParent = node;
        
        if(node == mAllRoot) {
            mAllRoot = left;
            left.mAllParent = null;
            node.mAllParent = left;
        }else{
            left.mAllParent = node.mAllParent;
            node.mAllParent = left;
            
            if(node == left.mAllParent.mAllRight) {
                left.mAllParent.mAllRight = left;
            }else{
                left.mAllParent.mAllLeft = left;
            }
        }
    }
    
    private Node firstNodeAll() {
        Node node = mAllRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllLeft != null)
            node = node.mAllLeft;
        
        return node;
    }
    
    private Node lastNodeAll() {
        Node node = mAllRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllRight != null)
            node = node.mAllRight;
        
        return node;
    }
    
    private Node nextNodeAll(Node node) {
        if(node.mAllRight != null) {
            node = node.mAllRight;
            
            while(node.mAllLeft != null)
                node = node.mAllLeft;
            
        }else{
            while(node.mAllParent != null && node.mAllParent.mAllRight == node)
                node = node.mAllParent;
            
            node = node.mAllParent;
        }
        
        return node;
    }
    
    private Node prevNodeAll(Node node) {
        if(node.mAllLeft != null) {
            node = node.mAllLeft;
            
            while(node.mAllRight != null)
                node = node.mAllRight;
        
        }else{
            while(node.mAllParent != null && node.mAllParent.mAllLeft == node)
                node = node.mAllParent;
            
            node = node.mAllParent;
        }
        
        return node;
    }
    
    private Node firstEquivalentAll(Comparable<? super V> comp) {
        Node node = mAllRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compareTo((V)node.mValue);
            
            if(c < 0) {
                node = node.mAllLeft;
                
            }else if(c > 0) {
                node = node.mAllRight;
                
            }else{
                ret = node;
                node = node.mAllLeft;
            }
        }
        
        return ret;
    }
    
    private Node firstEquivalentAll(Comparator<? super V> comp, V ref) {
        Node node = mAllRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compare(ref, (V)node.mValue);

            if(c < 0) {
                node = node.mAllLeft;
                
            }else if(c > 0) {
                node = node.mAllRight;
                
            }else{
                ret = node;
                node = node.mAllLeft;
            }
        }
        
        return ret;
    }
    
    private Node lastEquivalentAll(Comparable<? super V> comp) {
        Node node = mAllRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compareTo((V)node.mValue);
            
            if(c < 0) {
                node = node.mAllLeft;
                
            }else if(c > 0) {
                node = node.mAllRight;
                
            }else{
                ret = node;
                node = node.mAllRight;
            }
        }
        
        return ret;
    }

    private void swapNodesAll(Node a, Node b) {
        
        if(a.mAllParent == b) { 
            swapNodesAll(b, a);
            return;
        }
        
        {
            boolean tempColor = a.mAllColor;
            a.mAllColor = b.mAllColor;
            b.mAllColor = tempColor;
        }
        
        Node tempNode;
        
        if(a.mAllLeft == b) {
            
            a.mAllLeft = b.mAllLeft;
            b.mAllLeft = a;
            if(a.mAllLeft != null)
                a.mAllLeft.mAllParent = a;
            
            tempNode = a.mAllRight;
            a.mAllRight = b.mAllRight;
            b.mAllRight = tempNode;
            if(a.mAllRight != null)
                a.mAllRight.mAllParent = a;
            if(b.mAllRight != null)
                b.mAllRight.mAllParent = b;
            
            b.mAllParent = a.mAllParent;
            a.mAllParent = b;
            
            if(b.mAllParent == null) {
                mAllRoot = b;
            }else if(b.mAllParent.mAllLeft == a) {
                b.mAllParent.mAllLeft = b;
            }else{
                b.mAllParent.mAllRight = b;
            }
            
        }else if(a.mAllRight == b) {
            a.mAllRight = b.mAllRight;
            b.mAllRight = a;
            if(a.mAllRight != null)
                a.mAllRight.mAllParent = a;
            
            tempNode = a.mAllLeft;
            a.mAllLeft = b.mAllLeft;
            b.mAllLeft = tempNode;
            if(a.mAllLeft != null)
                a.mAllLeft.mAllParent = a;
            if(b.mAllLeft != null)
                b.mAllLeft.mAllParent = b;
            
            b.mAllParent = a.mAllParent;
            a.mAllParent = b;
            
            if(b.mAllParent == null) {
                mAllRoot = b;
            }else if(b.mAllParent.mAllLeft == a) {
                b.mAllParent.mAllLeft = b;
            }else{
                b.mAllParent.mAllRight = b;
            }
            
        }else{
            tempNode = a.mAllLeft;
            a.mAllLeft = b.mAllLeft;
            b.mAllLeft = tempNode;
            if(a.mAllLeft != null)
                a.mAllLeft.mAllParent = a;
            if(b.mAllLeft != null)
                b.mAllLeft.mAllParent = b;
            
            tempNode = a.mAllRight;
            a.mAllRight = b.mAllRight;
            b.mAllRight = tempNode;
            if(a.mAllRight != null)
                a.mAllRight.mAllParent = a;
            if(b.mAllRight != null)
                b.mAllRight.mAllParent = b;
            
            tempNode = a.mAllParent;
            a.mAllParent = b.mAllParent;
            b.mAllParent = tempNode;
            
            if(a.mAllParent == null) {
                mAllRoot = a;
            }else if(a.mAllParent.mAllLeft == b) {
                a.mAllParent.mAllLeft = a;
            }else{
                a.mAllParent.mAllRight = a;
            }
            
            if(b.mAllParent == null) {
                mAllRoot = b;
            }else if(b.mAllParent.mAllLeft == a) {
                b.mAllParent.mAllLeft = b;
            }else{
                b.mAllParent.mAllRight = b;
            }
        }        
    }

    

    private void insertNodeGroup(GroupNode group, Node node, Node parent, boolean left) {
        mModCount++;
        group.mCount++;
        
        if(parent == null) {
            group.mRoot = node;
            node.mGroupColor = BLACK;
            return;
        }
        
        node.mGroupParent = parent;
        
        if(left) {
            parent.mGroupLeft = node;
        }else{
            parent.mGroupRight = node;
        }
        
        while(true) {
            if(parent == null) {
                node.mGroupColor = BLACK;
                return;
            }

            node.mGroupColor = RED;
            
            if(parent.mGroupColor == BLACK) {
                return;
            }
            
            Node grandParent = parent.mGroupParent;
            Node uncle = (grandParent.mGroupLeft == parent ? grandParent.mGroupRight: grandParent.mGroupLeft);
            
            if(uncle != null && uncle.mGroupColor == RED) {
                parent.mGroupColor = BLACK;
                uncle.mGroupColor = BLACK;
                grandParent.mGroupColor = RED;
                
                node = grandParent;
                parent = grandParent.mGroupParent;
                left = (parent == null || parent.mGroupLeft == node);
                
                continue;
            }

            
            if(!left && parent == grandParent.mGroupLeft) {
                rotateLeftGroup(group, parent);
                parent = node;
                node = parent.mGroupLeft;
                left = true;
                
            }else if(left && parent == grandParent.mGroupRight) {
                rotateRightGroup(group, parent);
                parent = node;
                node = parent.mGroupRight;
                left = false;
            }

            parent.mGroupColor = BLACK;
            grandParent.mGroupColor = RED;
            
            if(left) {
                rotateRightGroup(group, grandParent);
            }else{
                rotateLeftGroup(group, grandParent);
            }
            
            break;
        }
    }
    
    private void removeNodeGroup(GroupNode group, Node node) {
        mModCount++;
        group.mCount--;
        
        //If we are deleting a node with two children, swap
        //it with a node that has at most one child.
        if(node.mGroupLeft != null && node.mGroupRight != null) {
            Node swapNode = node.mGroupLeft;
            while(swapNode.mGroupRight != null)
                swapNode = swapNode.mGroupRight;
            
            swapNodesGroup(group, node, swapNode);
        }
        
        //We are now guaranteed that node has no more than one non-null child.
        //We now relabel the node to be deleted "oldParent", the parent of that
        //    deletion node "newParent", and it's child "node".
        Node oldParent = node;
        Node newParent = node.mGroupParent;

        node = (node.mGroupLeft == null ? node.mGroupRight: node.mGroupLeft);

        //Set parent of child node to be newParent.
        if(node != null)
            node.mGroupParent = newParent;
        
        //Set child of newParent to node.
        if(newParent == null) {
            group.mRoot = node;
            
        }else{
            //left = newParent.mGroupLeft == oldParent;
            if(newParent.mGroupLeft == oldParent) {
                newParent.mGroupLeft = node;
            }else{
                newParent.mGroupRight = node;
            }
        }
        
        //If oldParent was RED, the constraints will be maintained.
        if(oldParent.mGroupColor == RED)
            return;
        
        //If the oldParent is BLACK and the node is RED, we swap colors.
        if(node != null && node.mGroupColor == RED) {
            node.mGroupColor = BLACK;
            return;
        }
        
        //If both oldParent and child are black, we're in a world of pain and 
        //must rebalance the tree.
        while(true) {
            
            //Case 1: node is new root.  We're done.
            if(newParent == null)
                return;
                
            //Case 2: Sibling is RED.  Reverse newParent and sibling colors and 
            //rotate at newParent.  (If tree was balanced before, 
            //sibling is guaranteed to be non-null.)
            boolean left = node == newParent.mGroupLeft;
            Node sibling = left ? newParent.mGroupRight : newParent.mGroupLeft;
            
            if(sibling.mGroupColor == RED) {
                newParent.mGroupColor = RED;
                sibling.mGroupColor = BLACK;
                
                if(left) {
                    rotateLeftGroup(group, newParent);
                    sibling = newParent.mGroupRight;
                }else{
                    rotateRightGroup(group, newParent);
                    sibling = newParent.mGroupLeft;
                }
            } 
            
            
            if((sibling.mGroupLeft == null || sibling.mGroupLeft.mGroupColor == BLACK) &&
               (sibling.mGroupRight == null || sibling.mGroupRight.mGroupColor == BLACK))
            {
                if(newParent.mGroupColor == BLACK) {
                    //Case 3: newParent, sibling, and sibling's children are black.
                    //Repaint sibling red and reiterate through loop.
                    sibling.mGroupColor = RED;
                    node = newParent;
                    newParent = node.mGroupParent;
                    continue;
                }else{
                    //Case 4: sibling and sibling's children are black, but 
                    //newParent is red.  In this case, swap colors between
                    //newParent and sibling.
                    sibling.mGroupColor = RED;
                    newParent.mGroupColor = BLACK;
                    return;
                }
            }

            //Case 5: sibling is black but has at least one red child.
            //Here we perform a series of rotations to balance out the tree.
            if(left) {
                if(sibling.mGroupRight == null || sibling.mGroupRight.mGroupColor == BLACK) {
                    rotateRightGroup(group, sibling);
                    sibling = sibling.mGroupParent;
                }   
                    
                sibling.mGroupColor = newParent.mGroupColor;
                sibling.mGroupRight.mGroupColor = BLACK;
                rotateLeftGroup(group, newParent);
                
            }else{
                if(sibling.mGroupLeft == null || sibling.mGroupLeft.mGroupColor == BLACK) {
                    rotateLeftGroup(group, sibling);
                    sibling = sibling.mGroupParent;
                }
                
                sibling.mGroupColor = newParent.mGroupColor;
                sibling.mGroupLeft.mGroupColor = BLACK;
                rotateRightGroup(group, newParent);
                
            }

            newParent.mGroupColor = BLACK;
            break;
        }
    }
    
    private void rotateLeftGroup(GroupNode group, Node node) {
        Node right = node.mGroupRight;
        if(right == null)
            return;
        
        node.mGroupRight = right.mGroupLeft;
        if(node.mGroupRight != null)
            node.mGroupRight.mGroupParent = node;
        
        right.mGroupLeft = node;
        
        if(node == group.mRoot) {
            group.mRoot = right;
            right.mGroupParent = null;
            node.mGroupParent = right;
        }else{
            right.mGroupParent = node.mGroupParent;
            node.mGroupParent = right;
            
            if(node == right.mGroupParent.mGroupLeft) {
                right.mGroupParent.mGroupLeft = right;
            }else{
                right.mGroupParent.mGroupRight = right;
            }
        }
    }
    
    private void rotateRightGroup(GroupNode group, Node node) {
        Node left = node.mGroupLeft;
        if(left == null)
            return;
        
        node.mGroupLeft = left.mGroupRight;
        left.mGroupRight = node;
        
        if(node.mGroupLeft != null)
            node.mGroupLeft.mGroupParent = node;
        
        if(node == group.mRoot) {
            group.mRoot = left;
            left.mGroupParent = null;
            node.mGroupParent = left;
        }else{
            left.mGroupParent = node.mGroupParent;
            node.mGroupParent = left;
            
            if(node == left.mGroupParent.mGroupRight) {
                left.mGroupParent.mGroupRight = left;
            }else{
                left.mGroupParent.mGroupLeft = left;
            }
        }
    }
    
    private Node firstNodeGroup(GroupNode group) {
        Node node = group.mRoot;
        
        if(node == null)
            return null;
        
        while(node.mGroupLeft != null)
            node = node.mGroupLeft;
        
        return node;
    }
    
    private Node lastNodeGroup(GroupNode group) {
        Node node = group.mRoot;
        
        if(node == null)
            return null;
        
        while(node.mGroupRight != null)
            node = node.mGroupRight;
        
        return node;
    }
    
    private Node nextNodeGroup(Node node) {
        if(node.mGroupRight != null) {
            node = node.mGroupRight;
            
            while(node.mGroupLeft != null)
                node = node.mGroupLeft;
            
        }else{
            while(node.mGroupParent != null && node.mGroupParent.mGroupRight == node)
                node = node.mGroupParent;
            
            node = node.mGroupParent;
        }
        
        return node;
    }
    
    private Node prevNodeGroup(Node node) {
        if(node.mGroupLeft != null) {
            node = node.mGroupLeft;
            
            while(node.mGroupRight != null)
                node = node.mGroupRight;
            
        }else{
            while(node.mGroupParent != null && node.mGroupParent.mGroupLeft == node)
                node = node.mGroupParent;
            
            node = node.mGroupParent;
        }
        
        return node;
    }

    private Node firstEquivalentGroup(GroupNode group, Comparable<? super V> comp) {
        Node node = group.mRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compareTo((V)node.mValue);
            
            if(c < 0) {
                node = node.mGroupLeft;
                
            }else if(c > 0) {
                node = node.mGroupRight;
                
            }else{
                ret = node;
                node = node.mGroupLeft;
            }
        }
        
        return ret;
    }
        
    private Node firstEquivalentGroup(GroupNode group, Comparator<? super V> comp, V ref) {
        Node node = group.mRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compare(ref, (V)node.mValue);
            
            if(c < 0) {
                node = node.mGroupLeft;
                
            }else if(c > 0) {
                node = node.mGroupRight;
                
            }else{
                ret = node;
                node = node.mGroupLeft;
            }
        }
        
        return ret;
    }
    
    private Node lastEquivalentGroup(GroupNode group, Comparable<? super V> comp) {
        Node node = group.mRoot;
        Node ret = null;
        
        while(node != null) {
            int c = comp.compareTo((V)node.mValue);
            
            if(c < 0) {
                node = node.mGroupLeft;
                
            }else if(c > 0) {
                node = node.mGroupRight;
                
            }else{
                ret = node;
                node = node.mGroupRight;
            }
        }
        
        return ret;
    }
    
    private void swapNodesGroup(GroupNode group, Node a, Node b) {
        
        if(a.mGroupParent == b) { 
            swapNodesGroup(group, b, a);
            return;
        }
        
        {
            boolean tempColor = a.mGroupColor;
            a.mGroupColor = b.mGroupColor;
            b.mGroupColor = tempColor;
        }
        
        Node tempNode;
        
        if(a.mGroupLeft == b) {
            
            a.mGroupLeft = b.mGroupLeft;
            b.mGroupLeft = a;
            if(a.mGroupLeft != null)
                a.mGroupLeft.mGroupParent = a;
            
            tempNode = a.mGroupRight;
            a.mGroupRight = b.mGroupRight;
            b.mGroupRight = tempNode;
            if(a.mGroupRight != null)
                a.mGroupRight.mGroupParent = a;
            if(b.mGroupRight != null)
                b.mGroupRight.mGroupParent = b;
            
            b.mGroupParent = a.mGroupParent;
            a.mGroupParent = b;
            
            if(b.mGroupParent == null) {
                group.mRoot = b;
            }else if(b.mGroupParent.mGroupLeft == a) {
                b.mGroupParent.mGroupLeft = b;
            }else{
                b.mGroupParent.mGroupRight = b;
            }
            
        }else if(a.mGroupRight == b) {
            a.mGroupRight = b.mGroupRight;
            b.mGroupRight = a;
            if(a.mGroupRight != null)
                a.mGroupRight.mGroupParent = a;
            
            tempNode = a.mGroupLeft;
            a.mGroupLeft = b.mGroupLeft;
            b.mGroupLeft = tempNode;
            if(a.mGroupLeft != null)
                a.mGroupLeft.mGroupParent = a;
            if(b.mGroupLeft != null)
                b.mGroupLeft.mGroupParent = b;
            
            b.mGroupParent = a.mGroupParent;
            a.mGroupParent = b;
            
            if(b.mGroupParent == null) {
                group.mRoot = b;
            }else if(b.mGroupParent.mGroupLeft == a) {
                b.mGroupParent.mGroupLeft = b;
            }else{
                b.mGroupParent.mGroupRight = b;
            }
            
        }else{
            tempNode = a.mGroupLeft;
            a.mGroupLeft = b.mGroupLeft;
            b.mGroupLeft = tempNode;
            if(a.mGroupLeft != null)
                a.mGroupLeft.mGroupParent = a;
            if(b.mGroupLeft != null)
                b.mGroupLeft.mGroupParent = b;
            
            tempNode = a.mGroupRight;
            a.mGroupRight = b.mGroupRight;
            b.mGroupRight = tempNode;
            if(a.mGroupRight != null)
                a.mGroupRight.mGroupParent = a;
            if(b.mGroupRight != null)
                b.mGroupRight.mGroupParent = b;
            
            tempNode = a.mGroupParent;
            a.mGroupParent = b.mGroupParent;
            b.mGroupParent = tempNode;
            
            if(a.mGroupParent == null) {
                group.mRoot = a;
            }else if(a.mGroupParent.mGroupLeft == b) {
                a.mGroupParent.mGroupLeft = a;
            }else{
                a.mGroupParent.mGroupRight = a;
            }
            
            if(b.mGroupParent == null) {
                group.mRoot = b;
            }else if(b.mGroupParent.mGroupLeft == a) {
                b.mGroupParent.mGroupLeft = b;
            }else{
                b.mGroupParent.mGroupRight = b;
            }
        }        
    }
    
    
        

    /**********
     * Node structures
     **********/
    
    private static class Node {
        final GroupNode mGroup;
        final Object mValue;
        
        boolean mGroupColor = RED;
        Node mGroupParent = null;
        Node mGroupLeft = null;
        Node mGroupRight = null;
        
        boolean mAllColor = RED;
        Node mAllParent = null;
        Node mAllLeft = null;
        Node mAllRight = null;
        
        Node(GroupNode group, Object value) {
            mGroup = group;
            mValue = value;
        }

    }
    
    
    private static class GroupNode {
        final Object mGroup;
        final int mHash;
        
        int mCount = 0;
        Node mRoot = null;
        GroupNode mNext;
        
        GroupNode(Object group, int hash, GroupNode next) {
            mGroup = group;
            mHash = hash;
            mNext = next;
        }
    }    

    

    /**********
     * Iterators
     **********/
    
    private final class AllIterator implements Iterator<V> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        private int mIterModCount = mModCount;
        private Node mPrev = null;
        private Node mNext;
     
        
        AllIterator(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
    
            if(forward) {
                if(comp == null) {
                    mNext = firstNodeAll();
                }else{
                    mNext = firstEquivalentAll(comp);
                }
            }else{
                if(comp == null) {
                    mNext = lastNodeAll();
                }else{
                    mNext = lastEquivalentAll(comp);
                }
            }
        }
        
        
        public boolean hasNext() {
            return mNext != null;
        }
        
        public V next() {
            if(mNext == null)
                throw new NoSuchElementException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            V ret = (V)mNext.mValue;
            mPrev = mNext;
            
            if(mForward) {
                mNext = nextNodeAll(mNext);
            }else{
                mNext = prevNodeAll(mNext);
            }
            
            if(mNext != null && mComp != null && mComp.compareTo((V)mNext.mValue) != 0)
                mNext = null;
            
            return ret;
        }
        
        public void remove() {
            if(mPrev == null)
                throw new IllegalStateException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            removeNodeCompletely(mPrev, true);
            
            mPrev = null;
            mIterModCount = mModCount;
        }

    }
    
    
    private final class GroupIterator implements Iterator<V> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        private int mIterModCount = mModCount;
        private Node mPrev = null;
        private Node mNext;
     
        
        GroupIterator(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
    
            GroupNode groupNode = findGroupNode(group);
            
            if(groupNode == null) {
                mNext = null;
                
            }else if(forward) {
                if(comp == null) {
                    mNext = firstNodeGroup(groupNode);
                }else{
                    mNext = firstEquivalentGroup(groupNode, comp);
                }
                
            }else{
                if(comp == null) {
                    mNext = lastNodeGroup(groupNode);
                }else{
                    mNext = lastEquivalentGroup(groupNode, comp);
                }
            }
        }
        
        
        public boolean hasNext() {
            return mNext != null;
        }
        
        public V next() {
            if(mNext == null)
                throw new NoSuchElementException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            V ret = (V)mNext.mValue;
            mPrev = mNext;
            
            if(mForward) {
                mNext = nextNodeGroup(mNext);
            }else{
                mNext = prevNodeGroup(mNext);
            }
            
            if(mNext != null && mComp != null && mComp.compareTo((V)mNext.mValue) != 0)
                mNext = null;
            
            return ret;
        }
        
        public void remove() {
            if(mPrev == null)
                throw new IllegalStateException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            removeNodeCompletely(mPrev, true);
            
            mPrev = null;
            mIterModCount = mModCount;
        }

    }
        
        
    private final class GroupTableIterator implements Iterator<G> {
        GroupNode mPrev;
        GroupNode mNext;
        int mIterModCount;
        int mIndex;
        
        GroupTableIterator() {
            mIterModCount = mModCount;
            
            if(mGroupCount > 0) {
                GroupNode[] buckets = mGroupBuckets;
                
                while(mIndex < buckets.length) {
                    mNext = buckets[mIndex++];
                    
                    if(mNext != null)
                        break;
                }
            }
        }
        
        
        public final boolean hasNext() {
            return mNext != null;
        }

        public final G next() {
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            if(mNext == null)
                throw new NoSuchElementException();
            
            mPrev = mNext;
            mNext = mNext.mNext;
            GroupNode[] buckets = mGroupBuckets;
            
            while(mNext == null) {
                if(mIndex >= buckets.length)
                    return (G)mPrev.mGroup;
                
                mNext = buckets[mIndex++];
            }
            
            return (G)mPrev.mGroup;    
        }
        
        public final void remove() {
            if(mPrev == null)
                throw new IllegalStateException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();

            clear((G)mPrev.mGroup);
            
            mPrev = null;
            mIterModCount = mModCount;
        }
    }
    
    
    
    /********
     * Views
     ********/
    
    private final class AllSubset extends AbstractSet<V> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        AllSubset(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return GroupSet.this.size();
            }else{
                Iterator<V> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object value) {
            if(mComp != null && mComp.compareTo((V)value) != 0)
                return false;
            
            return findNodeThatContains(value) != null;
        }
        
        public boolean remove(Object value) {
            if(mComp != null && mComp.compareTo((V)value) != 0)
                return false;
            
            Node node = findNodeThatContains(value);
            if(node == null)
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupSet.this.clear();
                return;
            }
            
            Iterator<V> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<V> iterator() {
            return new AllIterator(mForward, mComp);
        }
        
    }
    
    
    private final class GroupSubset extends AbstractSet<V> {
        
        private final boolean mForward;
        private final G mGroup;
        private final Comparable<? super V> mComp;
        
        GroupSubset(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mGroup = group;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return GroupSet.this.size(mGroup);
            }else{
                Iterator<V> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object value) {
            if(mComp != null && mComp.compareTo((V)value) != 0)
                return false;
            
            return findNodeThatContains(mGroup, value) != null;
        }
        
        public boolean remove(Object value) {
            if(mComp != null && mComp.compareTo((V)value) != 0)
                return false;
            
            Node node = findNodeThatContains(mGroup, value);
            if(node == null)
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupSet.this.clear(mGroup);
                return;
            }
            
            Iterator<V> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<V> iterator() {
            return new GroupIterator(mForward, mGroup, mComp);
        }
        
    }
    
    
    private final class Groups extends AbstractSet<G> {
        
        public int size() {
            return mGroupCount;
        }
        
        public boolean contains(Object group) {
            return findGroupNode(group) != null;
        }
        
        public boolean remove(Object group) {
            GroupNode node = findGroupNode(group);
            if(node == null)
                return false;
            
            GroupSet.this.clear((G)node.mGroup);
            return true;
        }
        
        public void clear() {
            GroupSet.this.clear();
        }
        
        public Iterator<G> iterator() {
            return new GroupTableIterator();
        }
        
    }
    
}
