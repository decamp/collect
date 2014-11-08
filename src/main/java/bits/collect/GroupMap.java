/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;

/** 
 * Something like a {@link java.util.SortedMap} that provides three different indices over its
 * elements.<br/>
 * 1. A red-black tree over all elements in the collection. <br/>
 * 2. A red-black tree over all elements within a given "group" in the collection. <br/>
 * 3. A hash table that provides a mapping between a mKey and each element. <br/>
 * <p>
 * Groups may be null.  Keys and Values may not be null.  Only one mValue is
 * allowed for a given mKey.  A group cannot contain a mValue multiple times,
 * however, a given mValue may be present in the collection multiple times under
 * different keys and different groups.
 * <p>
 * Within the entire collection, values are ordered first by a mValue comparison,
 * and second by a group comparison.  Within a group, elements are ordered only
 * by a mValue comparison.  The user has the option to provide a comparator for
 * the group comparison, mValue comparison, or both.  If a comparator is not
 * provided, the natural ordering will be used.
 * <p>
 * In general, the interface to GroupMap is consistent with Map.  However,
 * most methods have two versions: one that specifies a group and one that
 * doesn't.  While some methods that don't specify a group may use the "null"
 * group by default (<tt>put</tt>), some of the methods that
 * do not specify a group operate over all groups (<tt>size</tt>, 
 * <tt>clear</tt> and <tt>contains</tt>).  See documentation for specific
 * methods for details.
 *
 * @param <G> Group identifiers type
 * @param <K> Key type
 * @param <V> Value type
 * 
 * @author Philip DeCamp  
 */
@SuppressWarnings("unchecked")
public class GroupMap<G,K,V> extends AbstractMap<K,V> {

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR    = 0.75f;
    static final int MAXIMUM_CAPACITY         = 1 << 30;
    
    private Comparator<? super G> mGroupComparator = null;
    private Comparator<? super V> mValueComparator = null;
    
    //Shared values
    private transient int mModCount  = 0;
    private transient Groups mGroups = null;
    
    //Values for group table.
    private GroupNode<K,V>[] mGroupBuckets;
    private transient int mGroupCount = 0;
    private final float mGroupLoadFactor;
    private int mGroupThreshold;
    
    //Values for mKey table.
    private Node<K,V>[] mKeyBuckets;
    private transient int mKeyCount = 0;
    private final float mKeyLoadFactor;
    private int mKeyThreshold;
    
    //Values for complete index.
    private Node<K,V> mAllRoot = null;
    private transient int mAllCount = 0;
    
    

    public GroupMap() {
        this(null, null);
    }
    
    public GroupMap(Comparator<? super G> groupComparator, Comparator<? super V> valueComparator) {
        mGroupComparator = groupComparator;
        mValueComparator = valueComparator;
        mGroupLoadFactor = DEFAULT_LOAD_FACTOR;
        mKeyLoadFactor = DEFAULT_LOAD_FACTOR;
        
        int initCapacity = DEFAULT_INITIAL_CAPACITY;
        int capacity = 1;
        while(capacity < initCapacity)
            capacity <<= 1;
        
        mGroupBuckets = new GroupNode[capacity];
        mGroupThreshold = (int)(capacity * mGroupLoadFactor);
        mKeyBuckets = new Node[capacity];
        mKeyThreshold = (int)(capacity * mKeyLoadFactor);
    }
    
    
    
    /**
     * Gets a mValue mapped to the provided mKey.
     * 
     * @param key Key to lookup.
     * @return mValue associated with mKey, or null if not present.
     */
    @Override
    public V get(Object key) {
        Node<K,V> node = findKeyNode((K)key);
        if(node == null)
            return null;
        
        return node.mValue;
    }

    /**
     * Equivalent to calling <tt>put(null, mKey, mValue)</tt>
     * 
     * @param key
     * @param value
     * @return previous mValue mapped to the provided mKey
     */
    @Override
    public V put(K key, V value) {
        Node<K,V> ret = putKeyNode(null, key, value);
        if(ret == null)
            return null;
        
        return ret.mValue;
    }

    /**
     * Places mValue into a group within the collection and associates the mValue
     * with the provided mKey.  If the provided mKey is already mapped to a mValue
     * in the collection, that mValue will be removed from the collection
     * entirely and returned after the new mValue has been inserted.
     * <p>
     * Importantly, if a comparison-equivalent mValue is already present in the
     * collection, it will be removed silently, even if it uses a different mKey.
     * For that reason, both keys and comparison functions should be consistent
     * with mValue equivalence.
     * 
     * @param group Group into which mValue will be placed.
     * @param key Key to associate with mValue.
     * @param value Value to place into group/collection.
     * @return mValue previously associated with the provided mKey
     */
    public V put(G group, K key, V value) {
        Node<K,V> ret = putKeyNode(group, key, value);
        if(ret == null)
            return null;
        
        return ret.mValue;
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        putAll(null, t);
    }
    
    public void putAll(G group, Map<? extends K, ? extends V> values) {
        for(Map.Entry<? extends K, ? extends V> entry: values.entrySet()) {
            put(group, entry.getKey(), entry.getValue());
        }
    }
        
    @Override
    public V remove(Object key) {
        Node<K,V> ret = removeNodeByKey((K)key);
        if(ret == null)
            return null;
        
        return ret.mValue;
    }
    
    
    /**
     * Removes all elements in the collection.  Removes all groups.
     */
    @Override
    public void clear() {
        mModCount++;
        
        Arrays.fill(mGroupBuckets, null);
        mGroupCount = 0;
        
        Arrays.fill(mKeyBuckets, null);
        mKeyCount = 0;
        
        mAllRoot = null;
        mAllCount = 0;
    }

    /**
     * Removes all elements that belong to a given group.
     * 
     * @param group Group to remove from collection.
     */
    public void clear(G group) {
        //Find group.
        final GroupNode<K,V>[] buckets = mGroupBuckets;
        final int hash = (group == null ? 0 : rehash(group.hashCode()));
        final int idx = (hash & (buckets.length - 1));
        
        GroupNode<K,V> prevGroupNode = null;
        GroupNode<K,V> groupNode = null;
        
        {
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
        
        Node<K,V> node = firstNodeGroup(groupNode);
        
        while(node != null) {
            removeNodeAll(node);
            removeNodeHash(node);
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
        GroupNode<K,V> g = findGroupNode(group);
        if(g == null)
            return 0;
        
        return g.mSize;
    }

    
    
    @Override
    public boolean containsKey(Object key) {
        return findKeyNode((K)key) != null;
    }

    /**
     * @param obj Object to find in the collection.
     * @return true iff obj is present in any group within the collection.
     */
    @Override
    public boolean containsValue(Object obj) {
        return findNodeThatContains(obj) != null;
    }

    /**
     * @param group Group to look in
     * @param obj Value to look for in provided group
     * @return true iff provided mValue is a member of the provided group.
     */
    public boolean containsValue(G group, Object obj) {
        return findNodeThatContains(group, obj) != null;
    }
    
    
    public G getGroupForKey(Object key) {
        Node<K,V> node = findKeyNode((K)key);
        return (node == null ? null : (G)node.mGroup.mGroup);
    }
    
    
    public G getGroupForValue(Object value) {
        Node<K,V> node = findNodeThatContains(value);
        return node == null ? null : (G)node.mGroup.mGroup;
    }
    
    
    public V firstValue() {
        Node<K,V> node = firstNodeAll();
        return (node == null ? null : node.mValue);
    }
    
    
    public V firstValue(G group) {
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = firstNodeGroup(groupNode);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> firstEntry() {
        return firstNodeAll();
    }
    
    
    public Map.Entry<K,V> firstEntry(G group) {
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        return firstNodeGroup(groupNode);
    }

    
    public V lastValue() {
        Node<K,V> node = lastNodeAll();
        return (node == null ? null : node.mValue);
    }
    
    
    public V lastValue(G group) {
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = lastNodeGroup(groupNode);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> lastEntry() {
        return lastNodeAll();
    }
    
    
    public Map.Entry<K,V> lastEntry(G group) {
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        return lastNodeGroup(groupNode);
    }
    
    
    public V ceilingValue(V value) {
        Node<K,V> node = ceilingAll(value);
        return (node == null ? null : node.mValue);
    }
    
    
    public V ceilingValue(G group, V value) {
        Node<K,V> node = ceilingGroup(group, value);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> ceilingEntry(V value) {
        return ceilingAll(value);
    }
    
    
    public Map.Entry<K,V> ceilingEntry(G group, V value) {
        return ceilingGroup(group, value);
    }
    
    
    public V floorValue(V value) {
        Node<K,V> node = floorAll(value);
        return (node == null ? null : node.mValue);
    }
    
    
    public V floorValue(G group, V value) {
        Node<K,V> node = floorGroup(group, value);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> floorEntry(V value) {
        return floorAll(value);
    }
    
    
    public Map.Entry<K,V> floorEntry(G group, V value) {
        return floorGroup(group, value);
    }
    
    
    public V higherValue(V value) {
        Node<K,V> node = higherAll(value);
        return (node == null ? null : node.mValue);
    }
    
    
    public V higherValue(G group, V value) {
        Node<K,V> node = higherGroup(group, value);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> higherEntry(V value) {
        return higherAll(value);
    }
    
    
    public Map.Entry<K,V> higherEntry(G group, V value) {
        return higherGroup(group, value);
    }
    
    
    public V lowerValue(V value) {
        Node<K,V> node = lowerAll(value);
        return (node == null ? null : node.mValue);
    }
    
    
    public V lowerValue(G group, V value) {
        Node<K,V> node = lowerGroup(group, value);
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> lowerEntry(V value) {
        return lowerAll(value);
    }
    
    
    public Map.Entry<K,V> lowerEntry(G group, V value) {
        return lowerGroup(group, value);
    }
        

    
    public Set<K> keySet() {
        return new AllKeySet(true, null);
    }
    
    public Set<K> keySet(Comparable<? super V> comp) {
        return new AllKeySet(true, comp);
    }

    public Set<K> groupKeySet(G group) {
        return new GroupKeySet(true, group, null);
    }
    
    public Set<K> groupKeySet(G group, Comparable<? super V> comp) {
        return new GroupKeySet(true, group, comp);
    }
    
    public Set<K> descendingKeySet() {
        return new AllKeySet(false, null);
    }
    
    public Set<K> descendingKeySet(Comparable<? super V> comp) {
        return new AllKeySet(false, comp);
    }

    public Set<K> descendingGroupKeySet(G group) {
        return new GroupKeySet(false, group, null);
    }
    
    public Set<K> descendingGroupKeySet(G group, Comparable<? super V> comp) {
        return new GroupKeySet(false, group, comp);
    }

    
    public Set<Map.Entry<K,V>> entrySet() {
        return new AllEntrySet(true, null);
    }
    
    public Set<Map.Entry<K,V>> entrySet(Comparable<? super V> comp) {
        return new AllEntrySet(true, comp);
    }

    public Set<Map.Entry<K,V>> groupEntrySet(G group) {
        return new GroupEntrySet(true, group, null);
    }

    public Set<Map.Entry<K,V>> groupEntrySet(G group, Comparable<? super V> comp) {
        return new GroupEntrySet(true, group, comp);
    }
    
    public Set<Map.Entry<K,V>> descendingEntrySet() {
        return new AllEntrySet(false, null);
    }
    
    public Set<Map.Entry<K,V>> descendingEntrySet(Comparable<? super V> comp) {
        return new AllEntrySet(false, comp);
    }

    public Set<Map.Entry<K,V>> descendingGroupEntrySet(G group) {
        return new GroupEntrySet(false, group, null);
    }
    
    public Set<Map.Entry<K,V>> descendingGroupEntrySet(G group, Comparable<? super V> comp) {
        return new GroupEntrySet(false, group, comp);
    }


    public Collection<V> values() {
        return new AllValues(true, null);
    }
    
    public Collection<V> values(Comparable<? super V> comp) {
        return new AllValues(true, comp);
    }
    
    public Collection<V> groupValues(G group) {
        return new GroupValues(true, group, null);
    }
    
    public Collection<V> groupValues(G group, Comparable<? super V> comp) {
        return new GroupValues(true, group, comp);
    }
    
    public Collection<V> descendingValues() {
        return new AllValues(false, null);
    }
    
    public Collection<V> descendingValues(Comparable<? super V> comp) {
        return new AllValues(false, comp);
    }
    
    public Collection<V> descendingGroupValues(G group) {
        return new GroupValues(false, group, null);
    }
    
    public Collection<V> descendingGroupValues(G group, Comparable<? super V> comp) {
        return new GroupValues(false, group, comp);
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
        return (ret != null ? ret : (mGroups = new Groups()));
    }
    
        
    
    
    private Node<K,V> findNodeThatContains(Object obj) {
        if(obj == null)
            return null;
        
        final V value = (V)obj;
               
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            Node<K,V> node = firstEquivalentAll(comp, value);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeAll(node);
            }while(node != null && comp.compare(value, node.mValue) == 0);
            
        }else{
            Comparable<V> comp = (Comparable<V>)value;
            Node<K,V> node = firstEquivalentAll(comp);
            
            if(node == null)
                return null;
            
            do{
                if(value.equals(node.mValue))
                    return node;
                
                node = nextNodeAll(node);
            }while(node != null && comp.compareTo(node.mValue) == 0);
        }
        
        return null;   
    }
    
    private Node<K,V> findNodeThatContains(G group, Object obj) {
        if(obj == null)
            return null;
        
        final V value = (V)obj;
        final GroupNode<K,V> groupNode = findGroupNode(group);
        
        if(groupNode == null)
            return null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            Node<K,V> node = firstEquivalentGroup(groupNode, comp, value);
            
            if(node == null)
                return null;

            do{
                if(value.equals(node.mValue))
                    return node;

                node = nextNodeGroup(node);
            }while(node != null && comp.compare(value, node.mValue) == 0);

        }else{
            Comparable<V> comp = (Comparable<V>)value;
            Node<K,V> node = firstEquivalentGroup(groupNode, comp);

            if(node == null)
                return null;

            do{
                if(value.equals(node.mValue))
                    return node;

                node = nextNodeGroup(node);
            }while(node != null && comp.compareTo(node.mValue) == 0);
        }

        return null;   
    }
    
    private GroupNode<K,V> findGroupNode(Object group) {
        final GroupNode<K,V>[] buckets = mGroupBuckets;
        final int hash = (group == null ? 0 : rehash(group.hashCode()));
        final int idx = hash & (buckets.length - 1);
        
        GroupNode<K,V> g = buckets[idx];
        while(g != null) {
            if(hash == g.mHash && (group == g.mGroup || group != null && group.equals(g.mGroup)))
                return g;
            
            g = g.mNext;
        }
        
        return null;
    }
    
    private GroupNode<K,V> getGroupNodeInstance(G group) {
        GroupNode<K,V>[] buckets = mGroupBuckets;
        final int hash = (group == null ? 0 : rehash(group.hashCode()));
        final int idx = hash & (buckets.length - 1);
        
        GroupNode<K,V> g = buckets[idx];
        while(g != null) {
            if(hash == g.mHash && (group == g.mGroup || group != null && group.equals(g.mGroup)))
                return g;
            
            g = g.mNext;
        }
        
        g = new GroupNode<K,V>(group, hash, buckets[idx]);
        buckets[idx] = g;
        
        if(mGroupCount++ >= mGroupThreshold)
            resizeGroups(2 * buckets.length);
        
        return g;
    }
    
    private void removeGroupNode(GroupNode<K,V> groupNode) {
        final GroupNode<K,V>[] buckets = mGroupBuckets;
        final int idx = (groupNode.mHash & (buckets.length - 1));
        
        GroupNode<K,V> parent = null;
        GroupNode<K,V> g = buckets[idx];
        
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
        GroupNode<K,V>[] oldBuckets = mGroupBuckets;
        
        int oldCap = oldBuckets.length;
        if(oldCap >= MAXIMUM_CAPACITY) {
            mGroupThreshold = Integer.MAX_VALUE;
            return;
        }
        
        GroupNode<K,V>[] newBuckets = new GroupNode[newCapacity];
        
        for(int i = 0; i < oldBuckets.length; i++) {
            GroupNode<K,V> groupNode = oldBuckets[i];
            
            while(groupNode != null) {
                GroupNode<K,V> next = groupNode.mNext;
                
                int idx = (groupNode.mHash & (newCapacity - 1));
                groupNode.mNext = newBuckets[idx];
                newBuckets[idx] = groupNode;
                
                groupNode = next;
            }
        }
        
        mGroupBuckets = newBuckets;
        mGroupThreshold = (int)(newCapacity * mGroupLoadFactor);
    }

    
    
    private Node<K,V> findKeyNode(K key) {
        if(key == null)
            return null;
        
        final Node<K,V>[] buckets = mKeyBuckets;
        final int hash = rehash(key.hashCode());
        final int idx = (hash & (buckets.length - 1));
        
        Node<K,V> node = buckets[idx];
        while(node != null) {
            if(hash == node.mHash && (key == node.mKey || key.equals(node.mKey)))
                return node;
            
            node = node.mNext;
        }
        
        return null;
    }
    
    private void removeNodeHash(Node<K,V> node) {
        final Node<K,V>[] buckets = mKeyBuckets;
        final int idx = (node.mHash & (buckets.length - 1));
        
        Node<K,V> parent = null;
        Node<K,V> n = buckets[idx];
        
        while(n != null) {
            if(n == node) {
                mModCount++;
                mKeyCount--;
                
                if(parent == null) {
                    buckets[idx] = n.mNext;
                }else{
                    parent.mNext = n.mNext;
                }
                
                break;
            }
            
            parent = n;
            n = n.mNext;
        }
    }
    
    private void resizeKeys(int newCapacity) {
        Node<K,V>[] oldBuckets = mKeyBuckets;
        
        int oldCap = oldBuckets.length;
        if(oldCap >= MAXIMUM_CAPACITY) {
            mKeyThreshold = Integer.MAX_VALUE;
            return;
        }
        
        Node<K,V>[] newBuckets = new Node[newCapacity];
        
        for(int i = 0; i < oldBuckets.length; i++) {
            Node<K,V> node = oldBuckets[i];
            
            while(node != null) {
                Node<K,V> next = node.mNext;
                
                int idx = (node.mHash & (newCapacity - 1));
                node.mNext = newBuckets[idx];
                newBuckets[idx] = node;
                
                node = next;
            }
        }
        
        mKeyBuckets = newBuckets;
        mKeyThreshold = (int)(newCapacity * mKeyLoadFactor);
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
    
    
    
    private void insertNodeAll(Node<K,V> node, Node<K,V> parent, boolean left) {
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
            
            Node<K,V> grandParent = parent.mAllParent;
            Node<K,V> uncle = (grandParent.mAllLeft == parent ? grandParent.mAllRight: grandParent.mAllLeft);
            
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
    
    private void removeNodeAll(Node<K,V> node) {
        mAllCount--;
        mModCount++;
        
        //If we are deleting a node with two children, swap
        //it with a node that has at most one child.
        if(node.mAllLeft != null && node.mAllRight != null) {
            Node<K,V> swapNode = node.mAllLeft;
            while(swapNode.mAllRight != null)
                swapNode = swapNode.mAllRight;
            
            swapNodesAll(node, swapNode);
        }
        
        //We are now guaranteed that node has no more than one non-null child.
        //We now relabel the node to be deleted "oldParent", the parent of that
        //    deletion node "newParent", and it's child "node".
        Node<K,V> oldParent = node;
        Node<K,V> newParent = node.mAllParent;

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
            Node<K,V> sibling = left ? newParent.mAllRight : newParent.mAllLeft;
            
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
    
    private void rotateLeftAll(Node<K,V> node) {
        Node<K,V> right = node.mAllRight;
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
    
    private void rotateRightAll(Node<K,V> node) {
        Node<K,V> left = node.mAllLeft;
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
    
    private void swapNodesAll(Node<K,V> a, Node<K,V> b) {
        
        if(a.mAllParent == b) { 
            swapNodesAll(b, a);
            return;
        }
        
        {
            boolean tempColor = a.mAllColor;
            a.mAllColor = b.mAllColor;
            b.mAllColor = tempColor;
        }
        
        Node<K,V> tempNode;
        
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
    
    private Node<K,V> firstNodeAll() {
        Node<K,V> node = mAllRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllLeft != null)
            node = node.mAllLeft;
        
        return node;
    }
    
    private Node<K,V> lastNodeAll() {
        Node<K,V> node = mAllRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllRight != null)
            node = node.mAllRight;
        
        return node;
    }
    
    private Node<K,V> nextNodeAll(Node<K,V> node) {
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
    
    private Node<K,V> prevNodeAll(Node<K,V> node) {
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
    
    private Node<K,V> firstEquivalentAll(Comparable<? super V> comp) {
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compareTo(node.mValue);
            
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
    
    private Node<K,V> firstEquivalentAll(Comparator<? super V> comp, V ref) {
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compare(ref, node.mValue);

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
    
    private Node<K,V> lastEquivalentAll(Comparable<? super V> comp) {
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compareTo(node.mValue);
            
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
    
    private Node<K,V> ceilingAll(V value) {
        if(value == null)
            return null;
        
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, node.mValue);
            
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
                int c = comp.compareTo(node.mValue);
                
                if(c <= 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        }
        
        return ret;
    }
    
    private Node<K,V> floorAll(V value) {
        if(value == null)
            return null;
        
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c < 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }
        }
        
        return ret;
    }

    private Node<K,V> higherAll(V value) {
        if(value == null)
            return null;
        
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        }
        
        return ret;
    }

    private Node<K,V> lowerAll(V value) {
        if(value == null)
            return null;
        
        Node<K,V> node = mAllRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c <= 0) {
                    node = node.mAllLeft;
                }else{
                    ret = node;
                    node = node.mAllRight;
                }
            }
        }
        
        return ret;
    }

    

    private void insertNodeGroup(GroupNode<K,V> group, Node<K,V> node, Node<K,V> parent, boolean left) {
        mModCount++;
        group.mSize++;
        
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
            
            Node<K,V> grandParent = parent.mGroupParent;
            Node<K,V> uncle = (grandParent.mGroupLeft == parent ? grandParent.mGroupRight: grandParent.mGroupLeft);
            
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
    
    private void removeNodeGroup(GroupNode<K,V> group, Node<K,V> node) {
        mModCount++;
        group.mSize--;
        
        //If we are deleting a node with two children, swap
        //it with a node that has at most one child.
        if(node.mGroupLeft != null && node.mGroupRight != null) {
            Node<K,V> swapNode = node.mGroupLeft;
            while(swapNode.mGroupRight != null)
                swapNode = swapNode.mGroupRight;
            
            swapNodesGroup(group, node, swapNode);
        }
        
        //We are now guaranteed that node has no more than one non-null child.
        //We now relabel the node to be deleted "oldParent", the parent of that
        //    deletion node "newParent", and it's child "node".
        Node<K,V> oldParent = node;
        Node<K,V> newParent = node.mGroupParent;

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
            Node<K,V> sibling = left ? newParent.mGroupRight : newParent.mGroupLeft;
            
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
    
    private void rotateLeftGroup(GroupNode<K,V> group, Node<K,V> node) {
        Node<K,V> right = node.mGroupRight;
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
    
    private void rotateRightGroup(GroupNode<K,V> group, Node<K,V> node) {
        Node<K,V> left = node.mGroupLeft;
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

    private void swapNodesGroup(GroupNode<K,V> group, Node<K,V> a, Node<K,V> b) {
        
        if(a.mGroupParent == b) { 
            swapNodesGroup(group, b, a);
            return;
        }
        
        {
            boolean tempColor = a.mGroupColor;
            a.mGroupColor = b.mGroupColor;
            b.mGroupColor = tempColor;
        }
        
        Node<K,V> tempNode;
        
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
    
    private Node<K,V> firstNodeGroup(GroupNode<K,V> group) {
        Node<K,V> node = group.mRoot;
        
        if(node == null)
            return null;
        
        while(node.mGroupLeft != null)
            node = node.mGroupLeft;
        
        return node;
    }
    
    private Node<K,V> lastNodeGroup(GroupNode<K,V> group) {
        Node<K,V> node = group.mRoot;
        
        if(node == null)
            return null;
        
        while(node.mGroupRight != null)
            node = node.mGroupRight;
        
        return node;
    }
    
    private Node<K,V> nextNodeGroup(Node<K,V> node) {
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
    
    private Node<K,V> prevNodeGroup(Node<K,V> node) {
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

    private Node<K,V> firstEquivalentGroup(GroupNode<K,V> group, Comparable<? super V> comp) {
        Node<K,V> node = group.mRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compareTo(node.mValue);
            
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
        
    private Node<K,V> firstEquivalentGroup(GroupNode<K,V> group, Comparator<? super V> comp, V ref) {
        Node<K,V> node = group.mRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compare(ref, node.mValue);
            
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
    
    private Node<K,V> lastEquivalentGroup(GroupNode<K,V> group, Comparable<? super V> comp) {
        Node<K,V> node = group.mRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compareTo(node.mValue);
            
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
    
    
    
    private Node<K,V> ceilingGroup(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = groupNode.mRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, node.mValue);
            
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
                int c = comp.compareTo(node.mValue);
                
                if(c <= 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
        }
        
        return ret;
    }
    
    private Node<K,V> floorGroup(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = groupNode.mRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;
            
            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c < 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }
        }
        
        return ret;
    }

    private Node<K,V> higherGroup(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = groupNode.mRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c < 0) {
                    ret = node;
                    node = node.mGroupLeft;
                }else{
                    node = node.mGroupRight;
                }
            }
        }
        
        return ret;
    }

    private Node<K,V> lowerGroup(G group, V value) {
        if(value == null)
            return null;
        
        GroupNode<K,V> groupNode = findGroupNode(group);
        if(groupNode == null)
            return null;
        
        Node<K,V> node = groupNode.mRoot;
        Node<K,V> ret = null;
        
        if(mValueComparator != null) {
            Comparator<? super V> comp = mValueComparator;

            while(node != null) {
                int c = comp.compare(value, node.mValue);
                
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
                int c = comp.compareTo(node.mValue);
                
                if(c <= 0) {
                    node = node.mGroupLeft;
                }else{
                    ret = node;
                    node = node.mGroupRight;
                }
            }
        }
        
        return ret;
    }

    
    
    /****************************
     * GroupMap specific node operations.
     ****************************/
    
    private Node<K,V> removeNodeByKey(K key) {
        if(key == null)
            return null;
        
        final Node<K,V>[] buckets = mKeyBuckets;
        final int hash = rehash(key.hashCode());
        final int idx = (hash & (buckets.length - 1));
        
        Node<K,V> node = buckets[idx];
        while(node != null) {
            if(hash == node.mHash && (key == node.mKey || key.equals(node.mKey))) {
                removeNodeCompletely(node, true);
                return node;
            }
            
            node = node.mNext;
        }
        
        return null;
    }
    
    private Node<K,V> putKeyNode(G group, K key, V value) {
        if(key == null || value == null)
            throw new NullPointerException();
        
        //Find group.
        GroupNode<K,V> groupNode = getGroupNodeInstance(group);
        
        //Find place in bucket table.
        final Node<K,V>[] buckets = mKeyBuckets;
        final int hash = rehash(key.hashCode());
        final int idx = hash & (buckets.length - 1);
        
        Node<K,V> parent = null;
        Node<K,V> ret = buckets[idx];
        Node<K,V> newNode = null;
        
        while(true) {
            if(ret == null) {
                newNode = new Node<K,V>(groupNode, key, value, hash);
                
                mModCount++;
                mKeyCount++;
                newNode.mNext = buckets[idx];
                buckets[idx] = newNode;
                
                if(mKeyCount >= mKeyThreshold)
                    resizeKeys(buckets.length * 2);
                
                break;
                
            }else if(hash == ret.mHash && key == ret.mKey || key.equals(ret.mKey)) {
                if(ret.mGroup == groupNode && ret.mValue == value)
                    return ret;
                
                newNode = new Node<K,V>(groupNode, key, value, hash);
                
                removeNodeAll(ret);
                removeNodeGroup(ret.mGroup, ret);
                mModCount++;
                
                if(parent == null) {
                    buckets[idx] = ret.mNext;
                }else{
                    parent.mNext = ret.mNext;
                }
                
                newNode.mNext = buckets[idx];
                buckets[idx] = newNode;
                break;
            }
            
            parent = ret;
            ret = ret.mNext;
        }
        
        //Insert node into group.
        Node<K,V> node = groupNode.mRoot;
        
        if(node == null) {
            insertNodeGroup(groupNode, newNode, null, LEFT);
            
        }else{
            if(mValueComparator != null) {
                Comparator<? super V> comp = mValueComparator;
                
                while(true) {
                    int c = comp.compare(value, node.mValue);
                    
                    if(c < 0) {
                        if(node.mGroupLeft == null) {
                            insertNodeGroup(groupNode, newNode, node, LEFT);
                            break;
                        }else{
                            node = node.mGroupLeft;
                        }
                            
                    }else if(c > 0) {
                        if(node.mGroupRight == null) {
                            insertNodeGroup(groupNode, newNode, node, RIGHT);
                            break;
                        }else{
                            node = node.mGroupRight;
                        }
                        
                    }else{
                        removeNodeCompletely(node, false);
                        node = groupNode.mRoot;
                        
                        if(node == null) {
                            insertNodeGroup(groupNode, newNode, null, LEFT);
                            break;
                        }
                    }
                }
                
            }else{
                Comparable<? super V> comp = (Comparable<? super V>)value;
                
                while(true) {
                    int c = comp.compareTo(node.mValue);
                    
                    if(c < 0) {
                        if(node.mGroupLeft == null) {
                            insertNodeGroup(groupNode, newNode, node, LEFT);
                            break;
                        }else{
                            node = node.mGroupLeft;
                        }
                        
                    }else if(c > 0) {
                        if(node.mGroupRight == null) {
                            insertNodeGroup(groupNode, newNode, node, RIGHT);
                            break;
                        }else{
                            node = node.mGroupRight;
                        }
                    }else{
                        removeNodeCompletely(node, false);
                        node = groupNode.mRoot;
                        
                        if(node == null) {
                            insertNodeGroup(groupNode, newNode, null, LEFT);
                            break;
                        }
                    }
                }
            }
        }
        
        //Insert node into all.
        node = mAllRoot;
        
        if(node == null) {
            insertNodeAll(newNode, null, LEFT);
            return ret;
        }
        
        if(mValueComparator != null) {
            if(mGroupComparator != null) {
                Comparator<? super V> comp = mValueComparator;
                Comparator<? super G> groupComp = mGroupComparator;
                
                while(true) {
                    int c = comp.compare(value, node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return ret;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return ret;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return ret;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compare(group, nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return ret;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return ret;
                            }else{
                                node = node.mAllRight;
                            }
                            
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return ret;
                            }
                        }
                    }
                }
            
            }else{
                Comparator<? super V> comp = mValueComparator;
                Comparable<? super G> groupComp = (Comparable<? super G>)group;
                
                while(true) {
                    int c = comp.compare(value, node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return ret;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return ret;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return ret;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compareTo(nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return ret;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return ret;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return ret;
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
                    int c = comp.compareTo(node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return ret;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return ret;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return ret;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compare(group, nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return ret;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return ret;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return ret;
                            }
                        }
                    }
                }
            
            }else{
                Comparable<? super V> comp = (Comparable<? super V>)value;
                Comparable<? super G> groupComp = (Comparable<? super G>)group;
                
                while(true) {
                    int c = comp.compareTo(node.mValue);

                    if(c < 0) {
                        if(node.mAllLeft == null) {
                            insertNodeAll(newNode, node, LEFT);
                            return ret;
                        }else{
                            node = node.mAllLeft;
                        }

                    }else if(c > 0) {
                        if(node.mAllRight == null) {
                            insertNodeAll(newNode, node, RIGHT);
                            return ret;
                        }else{
                            node = node.mAllRight;
                        }
                    }else{
                        G nodeGroup = (G)node.mGroup.mGroup;
                        
                        if(group == null) {
                            if(nodeGroup == null)
                                return ret;
                            
                            c = -1;
                        }else if(nodeGroup == null) {
                            c = 1;
                        }else{
                            c = groupComp.compareTo(nodeGroup);
                        }
                        
                        
                        if(c < 0) {
                            if(node.mAllLeft == null) {
                                insertNodeAll(newNode, node, LEFT);
                                return ret;
                            }else{
                                node = node.mAllLeft;
                            }
                            
                        }else if(c > 0) {
                            if(node.mAllRight == null) {
                                insertNodeAll(newNode, node, RIGHT);
                                return ret;
                            }else{
                                node = node.mAllRight;
                            }
                        }else{
                            removeNodeCompletely(node, true);
                            node = mAllRoot;
                            
                            if(node == null) {
                                insertNodeAll(newNode, null, LEFT);
                                return ret;
                            }
                        }
                    }
                }
            }            
        }
    }
        
    private void removeNodeCompletely(Node<K,V> node, boolean disposeGroupIfEmpty) {
        removeNodeHash(node);
        removeNodeAll(node);
        removeNodeGroup(node.mGroup, node);
        
        if(disposeGroupIfEmpty && node.mGroup.mRoot == null) {
            removeGroupNode(node.mGroup);
        }
    }
           
    
    
    
    /***************
     * Node<K,V> structures.
     ***************/
    
    private static final class Node<K,V> implements Map.Entry<K,V> {
        final GroupNode<K,V> mGroup;
        final K mKey;
        final V mValue;
        
        final int mHash;
        Node<K,V> mNext = null;
        
        boolean mGroupColor = RED;
        Node<K,V> mGroupParent = null;
        Node<K,V> mGroupLeft = null;
        Node<K,V> mGroupRight = null;
        
        boolean mAllColor = RED;
        Node<K,V> mAllParent = null;
        Node<K,V> mAllLeft = null;
        Node<K,V> mAllRight = null;
        
        Node(GroupNode<K,V> group, K key, V value, int hash) {
            mGroup = group;
            mKey = key;
            mValue = value;
            mHash = hash;
        }

        
        public K getKey() {
            return mKey;
        }
        
        public V getValue() {
            return  mValue;
        }
        
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }
        
        
        public int hashCode() {
            return (mKey.hashCode() ^ mValue.hashCode());
        }
        
        public boolean equals(Object obj) {
            if(!(obj instanceof Map.Entry))
                return false;
            
            Object key = ((Map.Entry<K,V>)obj).getKey();
            if(mKey == key || mKey.equals(key)) {
                Object value = ((Map.Entry<K,V>)obj).getValue();
                
                return mValue == value || mValue.equals(value);
            }
            
            return false;
        }
        
    }
    
    
    private static class GroupNode<K,V> {
        final Object mGroup;
        final int mHash;
        
        int mSize = 0;
        Node<K,V> mRoot = null;
        GroupNode<K,V> mNext;
        
        GroupNode(Object group, int hash, GroupNode<K,V> next) {
            mGroup = group;
            mHash = hash;
            mNext = next;
        }
    }    
    


    /***************
     * Iterators.
     ***************/
    
    private abstract class AbstractAllIterator {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        private int mIterModCount = mModCount;
        private Node<K,V> mPrev = null;
        private Node<K,V> mNext;
                
        
        protected AbstractAllIterator(boolean forward, Comparable<? super V> comp) {
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
        
        
        public final boolean hasNext() {
            return mNext != null;
        }
        
        public final void remove() {
            if(mPrev == null)
                throw new IllegalStateException();
        
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            removeNodeCompletely(mPrev, true);
            
            mPrev = null;
            mIterModCount = mModCount;
        }

        final Node<K,V> nextNode() {
            if(mNext == null)
                throw new NoSuchElementException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            mPrev = mNext;
        
            if(mForward) {
                mNext = nextNodeAll(mNext);
            }else{
                mNext = prevNodeAll(mNext);
            }
            
            if(mNext != null && mComp != null && mComp.compareTo(mNext.mValue) != 0) { 
                mNext = null;
            }
            
            return mPrev;
        }

    }
    
    
    private abstract class AbstractGroupIterator {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        private int mIterModCount = mModCount;
        private Node<K,V> mPrev = null;
        private Node<K,V> mNext;
                
        
        protected AbstractGroupIterator(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
            
            GroupNode<K,V> groupNode = findGroupNode(group);
            
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
        
        
        public final boolean hasNext() {
            return mNext != null;
        }
        
        public final void remove() {
            if(mPrev == null)
                throw new IllegalStateException();
        
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            removeNodeCompletely(mPrev, true);
            
            mPrev = null;
            mIterModCount = mModCount;
        }

        final Node<K,V> nextNode() {
            if(mNext == null)
                throw new NoSuchElementException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            mPrev = mNext;
        
            if(mForward) {
                mNext = nextNodeGroup(mNext);
            }else{
                mNext = prevNodeGroup(mNext);
            }
            
            if(mNext != null && mComp != null && mComp.compareTo(mNext.mValue) != 0) { 
                mNext = null;
            }
            
            return mPrev;
        }

    }
    
    
    private final class AllKeyIterator extends AbstractAllIterator implements Iterator<K> {
        
        protected AllKeyIterator(boolean forward, Comparable<? super V> comp) {
            super(forward, comp);
        }
        
        public K next() {
            return nextNode().mKey;
        }
        
    }
    
    
    private final class AllEntryIterator extends AbstractAllIterator implements Iterator<Map.Entry<K,V>> {
        
        protected AllEntryIterator(boolean forward, Comparable<? super V> comp) {
            super(forward, comp);
        }
        
        public Map.Entry<K,V> next() {
            return nextNode();
        }
        
    }

    
    private final class AllValueIterator extends AbstractAllIterator implements Iterator<V> {
        
        protected AllValueIterator(boolean forward, Comparable<? super V> comp) {
            super(forward, comp);
        }
        
        public V next() {
            return nextNode().mValue;
        }
        
    }


    private final class GroupKeyIterator extends AbstractGroupIterator implements Iterator<K> {
        
        protected GroupKeyIterator(boolean forward, G group, Comparable<? super V> comp) {
            super(forward, group, comp);
        }
        
        public K next() {
            return nextNode().mKey;
        }
        
    }
    
    
    private final class GroupEntryIterator extends AbstractGroupIterator implements Iterator<Map.Entry<K,V>> {
        
        protected GroupEntryIterator(boolean forward, G group, Comparable<? super V> comp) {
            super(forward, group, comp);
        }
        
        public Map.Entry<K,V> next() {
            return nextNode();
        }
        
    }

    
    private final class GroupValueIterator extends AbstractGroupIterator implements Iterator<V> {
        
        protected GroupValueIterator(boolean forward, G group, Comparable<? super V> comp) {
            super(forward, group, comp);
        }
        
        public V next() {
            return nextNode().mValue;
        }
        
    }

  
    private final class GroupTableIterator implements Iterator<G> {
        GroupNode<K,V> mPrev;
        GroupNode<K,V> mNext;
        int mIterModCount;
        int mIndex;
        
        GroupTableIterator() {
            mIterModCount = mModCount;
            
            if(mGroupCount > 0) {
                GroupNode<K,V>[] buckets = mGroupBuckets;
                
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
            GroupNode<K,V>[] buckets = mGroupBuckets;
            
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

    
    
    /************
     * Views
     ************/
    
    private final class AllKeySet extends AbstractSet<K> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        AllKeySet(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mAllCount;
            }else{
                Iterator<K> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object key) {
            Node<K,V> node = findKeyNode((K)key);
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            return true;
        }

        public boolean remove(Object key) {
            if(key == null)
                return false;
            
            Node<K,V> node = findKeyNode((K)key);
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear();
                return;
            }
            
            Iterator<K> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<K> iterator() {
            return new AllKeyIterator(mForward, mComp);
        }

    }
    
    
    private final class GroupKeySet extends AbstractSet<K> {
        
        private final boolean mForward;
        private final G mGroup;
        private final Comparable<? super V> mComp;
        
        GroupKeySet(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mGroup = group;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return GroupMap.this.size(mGroup);
            }else{
                Iterator<K> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object key) {
            Node<K,V> node = findKeyNode((K)key);
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            G ga = (G)node.mGroup.mGroup;
            G gb = mGroup;
            if(ga != gb && (ga == null || !ga.equals(gb)))
                return false;
            
            return true;
        }

        public boolean remove(Object key) {
            if(key == null)
                return false;
            
            Node<K,V> node = findKeyNode((K)key);
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            G ga = (G)node.mGroup.mGroup;
            G gb = mGroup;
            if(ga != gb && (ga == null || !ga.equals(gb)))
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear(mGroup);
                return;
            }
            
            Iterator<K> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<K> iterator() {
            return new GroupKeyIterator(mForward, mGroup, mComp);
        }

    }
    
    
    private final class AllEntrySet extends AbstractSet<Map.Entry<K,V>> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        AllEntrySet(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mAllCount;
            }else{
                Iterator<Map.Entry<K,V>> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object entry) {
            if(entry == null)
                return false;
            
            Map.Entry<K,V> e = (Map.Entry<K,V>)entry;
            
            Node<K,V> node = findKeyNode(e.getKey());
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;

            V va = e.getValue();
            V vb = node.mValue;
            if(va != vb && (va == null || !va.equals(vb)))
                return false;
            
            return true;
        }

        public boolean remove(Object entry) {
            if(entry == null)
                return false;
            
            Map.Entry<K,V> e = (Map.Entry<K,V>)entry;
            
            Node<K,V> node = findKeyNode(e.getKey());
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
                        
            V va = e.getValue();
            V vb = node.mValue;
            if(va != vb && (va == null || !va.equals(vb)))
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear();
                return;
            }
            
            Iterator<Map.Entry<K,V>> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<Map.Entry<K,V>> iterator() {
            return new AllEntryIterator(mForward, mComp);
        }

    }
    
    
    private final class GroupEntrySet extends AbstractSet<Map.Entry<K,V>> {
        
        private final boolean mForward;
        private final G mGroup;
        private final Comparable<? super V> mComp;
        
        GroupEntrySet(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mGroup = group;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return GroupMap.this.size(mGroup);
            }else{
                Iterator<Map.Entry<K,V>> iter = iterator();
                int count = 0;
                
                while(iter.hasNext()) {
                    iter.next();
                    count++;
                }
                
                return count;
            }
        }
        
        public boolean contains(Object entry) {
            if(entry == null)
                return false;
            
            Map.Entry<K,V> e = (Map.Entry<K,V>)entry;
            
            Node<K,V> node = findKeyNode(e.getKey());
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            G ga = (G)node.mGroup.mGroup;
            G gb = mGroup;
            if(ga != gb && (ga == null || !ga.equals(gb)))
                return false;
            
            V va = node.mValue;
            V vb = e.getValue();
            if(va != vb && (va == null || !va.equals(vb)))
                return false;
            
            return true;
        }

        public boolean remove(Object entry) {
            if(entry == null)
                return false;
            
            Map.Entry<K,V> e = (Map.Entry<K,V>)entry;
            
            Node<K,V> node = findKeyNode(e.getKey());
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            G ga = (G)node.mGroup.mGroup;
            G gb = mGroup;
            if(ga != gb && (ga == null || !ga.equals(gb)))
                return false;
            
            V va = node.mValue;
            V vb = e.getValue();
            if(va != vb && (va == null || !va.equals(vb)))
                return false;
                        
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear(mGroup);
                return;
            }
            
            Iterator<Map.Entry<K,V>> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<Map.Entry<K,V>> iterator() {
            return new GroupEntryIterator(mForward, mGroup, mComp);
        }

    }
    

    private final class AllValues extends AbstractCollection<V> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        AllValues(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mAllCount;
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
            
            Node<K,V> node = findNodeThatContains(value);
            if(node == null)
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear();
                return;
            }
            
            Iterator<V> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<V> iterator() {
            return new AllValueIterator(mForward, mComp); 
        }
        
    }
    
    
    private final class GroupValues extends AbstractCollection<V> {
        
        private final boolean mForward;
        private final G mGroup;
        private final Comparable<? super V> mComp;
        
        GroupValues(boolean forward, G group, Comparable<? super V> comp) {
            mForward = forward;
            mGroup = group;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return GroupMap.this.size(mGroup);
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
            
            Node<K,V> node = findNodeThatContains(mGroup, value);
            if(node == null)
                return false;
            
            removeNodeCompletely(node, true);
            return true;
        }

        public void clear() {
            if(mComp == null) {
                GroupMap.this.clear(mGroup);
                return;
            }
            
            Iterator<V> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }

        public Iterator<V> iterator() {
            return new GroupValueIterator(mForward, mGroup, mComp);
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
            GroupNode<K,V> node = findGroupNode(group);
            if(node == null)
                return false;
            
            GroupMap.this.clear((G)node.mGroup);
            return true;
        }
        
        public void clear() {
            GroupMap.this.clear();
        }
        
        public Iterator<G> iterator() {
            return new GroupTableIterator();
        }
        
    }

}
