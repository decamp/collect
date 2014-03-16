/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */ 
package bits.collect;

import java.util.*;
import org.junit.*;

import bits.collect.SemiWeakHashSet;

import static org.junit.Assert.*;


/**
 * @author Philip DeCamp
 */
public class SemiWeakHashSetTest {

    private final Random mRand = new Random( 0 );


    @Test
    public void testVacuum() {
        int strongCount = 0;
        SemiWeakHashSet<double[]> set = new SemiWeakHashSet<double[]>();
        double[] keep = newRandomArray( 100 );
        set.addWeakly( keep );

        for( int i = 0; i < 100000; i++ ) {
            if( mRand.nextBoolean() ) {
                set.add( newRandomArray( 100 ) );
                strongCount++;
            } else {
                set.addWeakly( newRandomArray( 100 ) );
            }
        }

        try {
            Thread.sleep( 50L );
            System.gc();
            Thread.sleep( 50L );
        } catch( InterruptedException ex ) {
            throw new AssertionError( "Interrupted" );
        }
        
        assertEquals( strongCount + 1, set.vacuum() );
        // Without the line below, keep may actually be collected. Pretty tricky, java.
        assertTrue( keep[0] * 0.0 == 0.0 );
    }

    @Test
    public void testContains() {
        SemiWeakHashSet<double[]> set = new SemiWeakHashSet<double[]>();

        double[] el = newRandomArray( 10 );
        set.add( el );
        assertTrue( "contains() failed", set.contains( el ) );
    }

    @Test
    public void testContainsMany() {
        SemiWeakHashSet<double[]> set = new SemiWeakHashSet<double[]>();
        List<double[]> list = new ArrayList<double[]>();
        int strongCount = 0;
        int weakCount = 0;

        for( int i = 0; i < 10000; i++ ) {
            double[] d = newRandomArray( 100 );
            list.add( d );

            if( mRand.nextBoolean() ) {
                set.add( d );
                strongCount++;
            } else {
                set.addWeakly( d );
                weakCount++;
            }
        }


        for( double[] d : set ) {
            if( set.containsWeakly( d ) ) {
                weakCount--;
            } else if( set.containsStrongly( d ) ) {
                strongCount--;
            } else {
                throw new AssertionError( "Failed to find iterator-provided element in set." );
            }
        }

        assertEquals( "Invalid number of strong references.", strongCount, 0 );
        assertEquals( "Invalid number of weak referencese.", weakCount, 0 );
    }



    private double[] newRandomArray( int len ) {
        double[] ret = new double[len];
        for( int i = 0; i < len; i++ )
            ret[i] = mRand.nextDouble();

        return ret;
    }

}
