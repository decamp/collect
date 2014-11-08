/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;


/**
 * A HashMap that maintains values in sorted order, as compared to {@link java.util.TreeMap}, which
 * keeps the keys sorted but not the values. This has greater overhead, similar to a {@link java.util.LinkedHashMap},
 * because each object requires insertion into both a hash map for the keys and a red-black tree for the values.
 *
 * @author Philip DeCamp
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
public class TreeValueMap<K,V> extends AbstractMap<K,V> {
    
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR    = 0.75f;
    static final int MAXIMUM_CAPACITY         = 1 << 30;
    
    private Comparator<? super V> mValueComparator = null;
    
    //Shared values
    private transient int mModCount  = 0;
    
    //Values for mKey table.
    private Node<K,V>[] mBuckets;
    private transient int mKeyCount = 0;
    private final float mLoadFactor;
    private int mThreshold;
    
    //Values for complete index.
    private Node<K,V> mRoot = null;
    private transient int mNodeCount = 0;
    

    public TreeValueMap() {
        this( null );
    }
    
    
    public TreeValueMap( Comparator<? super V> optComp ) {
        int initCapacity = DEFAULT_INITIAL_CAPACITY;
        int capacity    = higherPot( Math.max( 1, initCapacity ) - 1 );
        
        mValueComparator = optComp;
        mLoadFactor   = DEFAULT_LOAD_FACTOR;
        mBuckets      = new Node[capacity];
        mThreshold    = (int)(capacity * mLoadFactor);
    }
    
    
    
    @Override
    public V get( Object key ) {
        Node<K,V> node = findKeyNode( (K)key );
        return node == null ? null : node.mValue;
    }

    
    @Override
    public V put( K key, V value ) {
        Node<K,V> ret = putNode( key, value );
        return ret == null ? null : ret.mValue;
    }
    
    
    @Override
    public void putAll( Map<? extends K, ? extends V> values ) {
        for( Map.Entry<? extends K, ? extends V> entry: values.entrySet() ) {
            put( entry.getKey(), entry.getValue());
        }
    }
        
    
    @Override
    public V remove( Object key ) {
        Node<K,V> ret = removeNodeByKey( (K)key );
        if( ret == null ) {
            return null;
        }
        
        return ret.mValue;
    }
    
    
    @Override
    public void clear() {
        mModCount++;
        
        Arrays.fill( mBuckets, null );
        mKeyCount = 0;
        mRoot  = null;
        mNodeCount = 0;
    }

    
    @Override
    public int size() {
        return mNodeCount;
    }

    
    @Override
    public boolean containsKey( Object key ) {
        return findKeyNode( (K)key ) != null;
    }


    @Override
    public boolean containsValue( Object obj ) {
        return findNodeThatContains( obj ) != null;
    }
    
    
    public V firstValue() {
        Node<K,V> node = firstNode();
        return node == null ? null : node.mValue;
    }
    
    
    public Map.Entry<K,V> firstEntry() {
        return firstNode();
    }
    
        
    public V lastValue() {
        Node<K,V> node = lastNode();
        return (node == null ? null : node.mValue);
    }
    
    
    public Map.Entry<K,V> lastEntry() {
        return lastNode();
    }
    
    
    public V ceilingValue( V value ) {
        Node<K,V> node = ceiling( value );
        return node == null ? null : node.mValue;
    }
    
    
    public Map.Entry<K,V> ceilingEntry( V value ) {
        return ceiling( value );
    }
    
    
    public V floorValue( V value ) {
        Node<K,V> node = floor( value );
        return node == null ? null : node.mValue;
    }
    
    
    public Map.Entry<K, V> floorEntry( V value ) {
        return floor( value );
    }


    public V higherValue( V value ) {
        Node<K, V> node = higher( value );
        return node == null ? null : node.mValue;
    }


    public Map.Entry<K, V> higherEntry( V value ) {
        return higher( value );
    }


    public V lowerValue( V value ) {
        Node<K, V> node = lower( value );
        return node == null ? null : node.mValue;
    }


    public Map.Entry<K, V> lowerEntry( V value ) {
        return lower( value );
    }

    
    public Set<K> keySet() {
        return new KeySet(true, null);
    }
    
    
    public Set<K> keySet( Comparable<? super V> comp ) {
        return new KeySet( true, comp );
    }
    
    
    public Set<K> descendingKeySet() {
        return new KeySet(false, null);
    }
    
    
    public Set<K> descendingKeySet(Comparable<? super V> comp) {
        return new KeySet(false, comp);
    }
    
    
    public Set<Map.Entry<K,V>> entrySet() {
        return new EntrySet(true, null);
    }
    
    
    public Set<Map.Entry<K,V>> entrySet(Comparable<? super V> comp) {
        return new EntrySet(true, comp);
    }

    
    public Set<Map.Entry<K,V>> descendingEntrySet() {
        return new EntrySet(false, null);
    }
    
    
    public Set<Map.Entry<K,V>> descendingEntrySet(Comparable<? super V> comp) {
        return new EntrySet(false, comp);
    }



    public Collection<V> values() {
        return new Values(true, null);
    }
    
    
    public Collection<V> values( Comparable<? super V> comp ) {
        return new Values( true, comp );
    }
    
    
    public Collection<V> descendingValues() {
        return new Values(false, null);
    }
    
    
    public Collection<V> descendingValues( Comparable<? super V> comp ) {
        return new Values( false, comp );
    }
    
    
    private Node<K,V> findNodeThatContains( Object obj ) {
        if( obj == null ) {
            return null;
        }
        
        final V value = (V)obj;
               
        if( mValueComparator != null ) {
            Comparator<? super V> comp = mValueComparator;
            Node<K,V> node = firstEquivalent( comp, value );
            
            if( node == null ) {
                return null;
            }
            
            do {
                if( value.equals( node.mValue ) ) {
                    return node;
                }
                node = nextNodeAll( node );
            } while( node != null && comp.compare( value, node.mValue ) == 0 );
        } else {
            Comparable<V> comp = (Comparable<V>)value;
            Node<K,V> node = firstEquivalent( comp );
            if( node == null ) {
                return null;
            }
            
            do{
                if( value.equals( node.mValue ) ) {
                    return node;
                }
                node = nextNodeAll( node );
            } while( node != null && comp.compareTo( node.mValue ) == 0 );
        }
        
        return null;   
    }
    
    
    private Node<K,V> findKeyNode( K key ) {
        if( key == null ) {
            return null;
        }
        
        final Node<K,V>[] buckets = mBuckets;
        final int hash = rehash(key.hashCode());
        final int idx = (hash & (buckets.length - 1));
        
        Node<K,V> node = buckets[idx];
        while( node != null ) {
            if( hash == node.mHash && (key == node.mKey || key.equals( node.mKey ) ) ) {
                return node;
            }
            node = node.mNext;
        }
        
        return null;
    }

    
    private void removeNodeHash( Node<K,V> node ) {
        final Node<K,V>[] buckets = mBuckets;
        final int idx = (node.mHash & (buckets.length - 1));
        
        Node<K,V> parent = null;
        Node<K,V> n = buckets[idx];
        
        while( n != null ) {
            if( n == node ) {
                mModCount++;
                mKeyCount--;
                
                if( parent == null ) {
                    buckets[idx] = n.mNext;
                } else {
                    parent.mNext = n.mNext;
                }
                break;
            }
            
            parent = n;
            n = n.mNext;
        }
    }
    
    
    private void resizeKeys( int newCapacity ) {
        Node<K,V>[] oldBuckets = mBuckets;
        
        int oldCap = oldBuckets.length;
        if( oldCap >= MAXIMUM_CAPACITY ) {
            mThreshold = Integer.MAX_VALUE;
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
        
        mBuckets = newBuckets;
        mThreshold = (int)(newCapacity * mLoadFactor);
    }
    
    
    private static int rehash( int hash ) {
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

    private static final boolean LEFT  = true;
    private static final boolean RIGHT = false;
    private static final boolean BLACK = false;
    private static final boolean RED   = true;
    
    
    
    private void insertNode( Node<K,V> node, Node<K,V> parent, boolean left ) {
        mNodeCount++;
        mModCount++;
        
        if(parent == null) {
            mRoot = node;
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
                rotateLeft(parent);
                parent = node;
                node = parent.mAllLeft;
                left = true;
                
            }else if(left && parent == grandParent.mAllRight) {
                rotateRight(parent);
                parent = node;
                node = parent.mAllRight;
                left = false;
            }

            parent.mAllColor = BLACK;
            grandParent.mAllColor = RED;
            
            if(left) {
                rotateRight(grandParent);
            }else{
                rotateLeft(grandParent);
            }
            
            break;
        }
    }
    
    
    private void removeNode( Node<K,V> node ) {
        mNodeCount--;
        mModCount++;
        
        //If we are deleting a node with two children, swap
        //it with a node that has at most one child.
        if(node.mAllLeft != null && node.mAllRight != null) {
            Node<K,V> swapNode = node.mAllLeft;
            while(swapNode.mAllRight != null)
                swapNode = swapNode.mAllRight;
            
            swapNodes(node, swapNode);
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
            mRoot = node;
            
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
                    rotateLeft(newParent);
                    sibling = newParent.mAllRight;
                }else{
                    rotateRight(newParent);
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
                    rotateRight(sibling);
                    sibling = sibling.mAllParent;
                }   
                    
                sibling.mAllColor = newParent.mAllColor;
                sibling.mAllRight.mAllColor = BLACK;
                rotateLeft(newParent);
                
            }else{
                if(sibling.mAllLeft == null || sibling.mAllLeft.mAllColor == BLACK) {
                    rotateLeft(sibling);
                    sibling = sibling.mAllParent;
                }
                
                sibling.mAllColor = newParent.mAllColor;
                sibling.mAllLeft.mAllColor = BLACK;
                rotateRight(newParent);
                
            }

            newParent.mAllColor = BLACK;
            break;
        }
    }
    
    
    private void rotateLeft( Node<K,V> node ) {
        Node<K,V> right = node.mAllRight;
        if(right == null)
            return;
        
        node.mAllRight = right.mAllLeft;
        if(node.mAllRight != null)
            node.mAllRight.mAllParent = node;
        
        right.mAllLeft = node;
        
        if(node == mRoot) {
            mRoot = right;
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
    
    
    private void rotateRight( Node<K,V> node ) {
        Node<K,V> left = node.mAllLeft;
        if(left == null)
            return;
        
        node.mAllLeft = left.mAllRight;
        left.mAllRight = node;
        
        if(node.mAllLeft != null)
            node.mAllLeft.mAllParent = node;
        
        if(node == mRoot) {
            mRoot = left;
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
    
    
    private void swapNodes( Node<K,V> a, Node<K,V> b ) {
        if(a.mAllParent == b) { 
            swapNodes(b, a);
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
                mRoot = b;
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
                mRoot = b;
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
                mRoot = a;
            }else if(a.mAllParent.mAllLeft == b) {
                a.mAllParent.mAllLeft = a;
            }else{
                a.mAllParent.mAllRight = a;
            }
            
            if(b.mAllParent == null) {
                mRoot = b;
            }else if(b.mAllParent.mAllLeft == a) {
                b.mAllParent.mAllLeft = b;
            }else{
                b.mAllParent.mAllRight = b;
            }
        }        
    }
    
    
    private Node<K,V> firstNode() {
        Node<K,V> node = mRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllLeft != null)
            node = node.mAllLeft;
        
        return node;
    }
    
    
    private Node<K,V> lastNode() {
        Node<K,V> node = mRoot;
        
        if(node == null)
            return null;
        
        while(node.mAllRight != null)
            node = node.mAllRight;
        
        return node;
    }
    
    
    private Node<K,V> nextNodeAll( Node<K,V> node ) {
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
    
    
    private Node<K,V> prevNodeAll( Node<K,V> node ) {
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
    
    
    private Node<K,V> firstEquivalent( Comparable<? super V> comp ) {
        Node<K,V> node = mRoot;
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
    
    
    private Node<K,V> firstEquivalent( Comparator<? super V> comp, V ref ) {
        Node<K,V> node = mRoot;
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
    
    
    private Node<K,V> lastEquivalent( Comparable<? super V> comp ) {
        Node<K,V> node = mRoot;
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
    
    
    @SuppressWarnings( "unused" )
    private Node<K,V> lastEquivalent( Comparator<? super V> comp, V ref ) {
        Node<K,V> node = mRoot;
        Node<K,V> ret = null;
        
        while(node != null) {
            int c = comp.compare(ref, node.mValue);
            
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
    
    
    private Node<K,V> ceiling( V value ) {
        if( value == null ) {
            return null;
        }
        
        Node<K,V> node = mRoot;
        Node<K,V> ret = null;
        
        if( mValueComparator != null ) {
            Comparator<? super V> comp = mValueComparator;
            while( node != null ) {
                int c = comp.compare(value, node.mValue);
            
                if(c <= 0) {
                    ret = node;
                    node = node.mAllLeft;
                }else{
                    node = node.mAllRight;
                }
            }
        } else {
            Comparable<? super V> comp = (Comparable<? super V>)value;
            while( node != null ) {
                int c = comp.compareTo( node.mValue );
                if( c <= 0 ) {
                    ret = node;
                    node = node.mAllLeft;
                } else {
                    node = node.mAllRight;
                }
            }
        }
        
        return ret;
    }
    
    
    private Node<K,V> floor( V value ) {
        if(value == null)
            return null;
        
        Node<K,V> node = mRoot;
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

    
    private Node<K,V> higher( V value ) {
        if(value == null)
            return null;
        
        Node<K,V> node = mRoot;
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

    
    private Node<K,V> lower( V value ) {
        if(value == null)
            return null;
        
        Node<K,V> node = mRoot;
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

    
    
    /****************************
     * GroupMap specific node operations.
     ****************************/
    
    private Node<K,V> removeNodeByKey( K key ) {
        if( key == null ) {
            return null;
        }
        
        final Node<K,V>[] buckets = mBuckets;
        final int hash = rehash(key.hashCode());
        final int idx = (hash & (buckets.length - 1));
        
        Node<K,V> node = buckets[idx];
        while( node != null ) {
            if( hash == node.mHash && ( key == node.mKey || key.equals( node.mKey ) ) ) {
                removeNodeCompletely( node );
                return node;
            }
            
            node = node.mNext;
        }
        
        return null;
    }

    
    private Node<K,V> putNode( K key, V value) {
        if( key == null || value == null ) {
            throw new NullPointerException();
        }
        
        //Find place in bucket table.
        final Node<K,V>[] buckets = mBuckets;
        final int hash = rehash( key.hashCode() );
        final int idx = hash & ( buckets.length - 1 );
        
        Node<K,V> parent  = null;
        Node<K,V> ret     = buckets[idx];
        Node<K,V> newNode = null;
        
        while( true ) {
            if( ret == null ) {
                newNode = new Node<K,V>( key, value, hash );
                
                mModCount++;
                mKeyCount++;
                newNode.mNext = buckets[idx];
                buckets[idx] = newNode;
                
                if( mKeyCount >= mThreshold ) {
                    resizeKeys( buckets.length * 2 );
                }
                
                break;
                
            } else if( hash == ret.mHash && key == ret.mKey || key.equals( ret.mKey ) ) {
                if( ret.mValue == value ) {
                    return ret;
                }
                
                newNode = new Node<K,V>( key, value, hash );
                removeNode( ret );
                mModCount++;
                
                if( parent == null ) {
                    buckets[idx] = ret.mNext;
                } else {
                    parent.mNext = ret.mNext;
                }
                
                newNode.mNext = buckets[idx];
                buckets[idx] = newNode;
                break;
            }
            
            parent = ret;
            ret = ret.mNext;
        }
        
        //Insert node into all.
        Node node = mRoot;
        
        if( node == null ) {
            insertNode( newNode, null, LEFT );
            return ret;
        }
        
        if( mValueComparator != null ) {
            Comparator comp = mValueComparator;
            while( true ) {
                int c = comp.compare( value, node.mValue );

                if( c < 0 ) {
                    if( node.mAllLeft == null ) {
                        insertNode(newNode, node, LEFT);
                        return ret;
                    } else {
                        node = node.mAllLeft;
                    }
                } else {
                    if( node.mAllRight == null ) {
                        insertNode( newNode, node, RIGHT );
                        return ret;
                    } else {
                        node = node.mAllRight;
                    }
                }
            }
        } else {
            Comparable comp = (Comparable)value;
            while( true ) {
                int c = comp.compareTo(node.mValue);
                
                if( c < 0 ) {
                    if( node.mAllLeft == null ) {
                        insertNode( newNode, node, LEFT );
                        return ret;
                    } else {
                        node = node.mAllLeft;
                    }
                } else {
                    if( node.mAllRight == null ) {
                        insertNode( newNode, node, RIGHT );
                        return ret;
                    } else {
                        node = node.mAllRight;
                    }
                }
            }
        }            
    }
    
    
    private void removeNodeCompletely( Node<K,V> node ) {
        removeNodeHash( node );
        removeNode( node );
    }
           
    
    
    /***************
     * Node<K,V> structures.
     ***************/
    
    private static final class Node<K,V> implements Map.Entry<K,V> {
        final K mKey;
        final V mValue;
        
        final int mHash;
        Node<K,V> mNext = null;
        
        boolean mAllColor = RED;
        Node<K,V> mAllParent = null;
        Node<K,V> mAllLeft = null;
        Node<K,V> mAllRight = null;
        
        Node( K key, V value, int hash ) {
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
        
        public Object setValue( Object value ) {
            throw new UnsupportedOperationException();
        }
        
        
        public int hashCode() {
            return (mKey.hashCode() ^ mValue.hashCode());
        }
        
        public boolean equals( Object obj ) {
            if( !( obj instanceof Map.Entry ) ) {
                return false;
            }
            
            Object key = ((Map.Entry<K,V>)obj).getKey();
            if( mKey == key || mKey.equals( key ) ) {
                Object value = ((Map.Entry<K,V>)obj).getValue();
                return mValue == value || mValue.equals(value);
            }
            
            return false;
        }
    
        @Override
        public String toString() {
            return String.format( "%s=%s", mKey.toString(), mValue.toString() );
        }
        
        
    }
    

    /***************
     * Iterators.
     ***************/
    
    private abstract class AbstractIterator {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        private int mIterModCount = mModCount;
        private Node<K,V> mPrev = null;
        private Node<K,V> mNext;
                
        
        protected AbstractIterator(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
            
            if(forward) {
                if(comp == null) {
                    mNext = firstNode();
                }else{
                    mNext = firstEquivalent(comp);
                }
            }else{
                if(comp == null) {
                    mNext = lastNode();
                }else{
                    mNext = lastEquivalent(comp);
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
            
            removeNodeCompletely(mPrev);
            
            mPrev = null;
            mIterModCount = mModCount;
        }

        final Node<K,V> nextNode() {
            if(mNext == null)
                throw new NoSuchElementException();
            
            if(mModCount != mIterModCount)
                throw new ConcurrentModificationException();
            
            mPrev = mNext;
        
            if( mForward ) {
                mNext = TreeValueMap.this.nextNodeAll( mNext );
            } else {
                mNext = prevNodeAll( mNext );
            }
            
            if( mNext != null && mComp != null && mComp.compareTo( mNext.mValue ) != 0 ) { 
                mNext = null;
            }
            
            return mPrev;
        }

    }
    
    
    private final class KeyIterator extends AbstractIterator implements Iterator<K> {
        
        protected KeyIterator( boolean forward, Comparable<? super V> comp ) {
            super( forward, comp );
        }
        
        public K next() {
            return nextNode().mKey;
        }
        
    }
    
    
    private final class EntryIterator extends AbstractIterator implements Iterator<Map.Entry<K,V>> {
        
        protected EntryIterator( boolean forward, Comparable<? super V> comp ) {
            super( forward, comp );
        }
        
        public Map.Entry<K,V> next() {
            return nextNode();
        }
        
    }

    
    private final class ValueIterator extends AbstractIterator implements Iterator<V> {
        
        protected ValueIterator( boolean forward, Comparable<? super V> comp ) {
            super( forward, comp );
        }
        
        public V next() {
            return nextNode().mValue;
        }
        
    }

    
    
    /************
     * Views
     ************/
    
    private final class KeySet extends AbstractSet<K> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        KeySet( boolean forward, Comparable<? super V> comp ) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mNodeCount;
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
        
        public boolean contains( Object key ) {
            Node<K,V> node = findKeyNode((K)key);
            if( node == null || mComp != null && mComp.compareTo(node.mValue) != 0 ) {
                return false;
            }
            
            return true;
        }

        public boolean remove(Object key) {
            if(key == null)
                return false;
            
            Node<K,V> node = findKeyNode((K)key);
            if(node == null || mComp != null && mComp.compareTo(node.mValue) != 0)
                return false;
            
            removeNodeCompletely(node);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                TreeValueMap.this.clear();
                return;
            }
            
            Iterator<K> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<K> iterator() {
            return new KeyIterator(mForward, mComp);
        }

    }
    
    
    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        EntrySet(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mNodeCount;
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
            
            removeNodeCompletely(node);
            return true;
        }
        
        public void clear() {
            if(mComp == null) {
                TreeValueMap.this.clear();
                return;
            }
            
            Iterator<Map.Entry<K,V>> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator(mForward, mComp);
        }

    }
    

    private final class Values extends AbstractCollection<V> {
        
        private final boolean mForward;
        private final Comparable<? super V> mComp;
        
        Values(boolean forward, Comparable<? super V> comp) {
            mForward = forward;
            mComp = comp;
        }
        
        
        public int size() {
            if(mComp == null) {
                return mNodeCount;
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
            
            removeNodeCompletely( node );
            return true;
        }
        
        public void clear() {
            if( mComp == null ) {
                TreeValueMap.this.clear();
                return;
            }
            
            Iterator<V> iter = iterator();
            
            while(iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        
        public Iterator<V> iterator() {
            return new ValueIterator(mForward, mComp); 
        }
        
    }
    
    
    private static int higherPot( int val ) {
        if( val <= 0 ) {
            return 1;
        }
        
        val = (val >>  1) | val;
        val = (val >>  2) | val;
        val = (val >>  4) | val;
        val = (val >>  8) | val;
        val = (val >> 16) | val;
        
        return val + 1;
    }
    
}
