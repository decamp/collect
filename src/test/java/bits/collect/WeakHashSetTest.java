/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;

import bits.collect.WeakHashSet;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author Philip DeCamp
 */
@SuppressWarnings( { "rawtypes" } )
public class WeakHashSetTest {
    

    @Test
    public void test1() {
        Set<Integer> normSet = new HashSet<Integer>();
        Set<Integer> weakSet = new WeakHashSet<Integer>();

        Random rand = new Random( 0 );

        for( int i = 0; i < 1000000; i++ ) {
            Integer n = rand.nextInt() % 5000000;

            if( rand.nextBoolean() ) {
                normSet.add( n );
                weakSet.add( n );
            } else {
                normSet.remove( n );
                weakSet.remove( n );
            }
        }

        //System.out.println( normSet.equals( weakSet ) );
        //System.out.println( weakSet.equals( normSet ) ); 
        //System.out.println( "---" );

        normSet.clear();
        normSet = null;

        System.out.println( weakSet.size() );
        System.gc();

        try {
            Thread.sleep( 1000L );
        } catch( InterruptedException ex ) {}

        weakSet.add( 4 );
        ((WeakHashSet)weakSet).vacuum();
        System.out.println( weakSet.size() );

    }

    public static <T> boolean compare( Set<T> a, Set<T> b ) {
        assertTrue( "Size mismatch", a.size() == b.size() );

        for( Object o : a ) {
            assertTrue( "Containment mismatch", b.contains( o ) );
        }

        assertTrue( "ContainsAll mismatch", a.containsAll( b ) );

        return true;
    }

}
