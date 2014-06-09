/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;

import java.util.*;
import org.junit.Test;

import bits.collect.ConcurrentBag;
import static org.junit.Assert.*;


public class ConcurrentBagTest {
    
    
    @Test
    @SuppressWarnings( "unchecked" )
    public void testAddRemove() {
        ConcurrentBag<Integer> bag     = new ConcurrentBag<Integer>();
        List<Integer> list             = new ArrayList<Integer>();
        List<Collection<Integer>> both = Arrays.asList( bag, list );
        
        add( both, 0 );
        add( both, 2 );
        add( both, 3 );
        add( both, 4 );
        add( both, 5 );
        add( both, 6 );
        
        assertDeepEqual( bag, list );
        remove( both, 100 );
        assertDeepEqual( bag, list );
        remove( both, 4 );
        assertDeepEqual( bag, list );
        remove( both, bag.head().mItem );
        assertDeepEqual( bag, list );
        remove( both, 3 );
        assertDeepEqual( bag, list );
    }
    

    private static <T> void assertDeepEqual( ConcurrentBag<T> bag, List<T> ref ) {
        assertEquals( bag.size(), ref.size() );
        List<T> list = new ArrayList<T>( bag );
        
        for( T item: ref ) {
            assertTrue( list.remove( item ) );
        }
    }

    
    private static <T> void add( Collection<? extends Collection<? super T>> colls, T item ) {
        for( Collection<? super T> coll: colls ) {
            coll.add( item );
        }
    }
    
    
    private static <T> void remove( Collection<? extends Collection<? super T>> colls, T item ) {
        for( Collection<? super T> coll: colls ) {
            coll.remove( item );
        }
    }
    
}
