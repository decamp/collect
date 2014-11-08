/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;
import bits.collect.PublicList;
import org.junit.*;
import static org.junit.Assert.*;


/**
 * @author Philip DeCamp
 */
public class PublicListTest {
    
    private final Random mRand = new Random( 2 );
    
    public PublicListTest() {}
    
    
    
    @Test
    public void testAsList() {
        int trials = 20;
        int range  = 5;
        
        List<Integer> a = new ArrayList<Integer>( 4 );
        List<Integer> b = PublicList.create( Integer.class, 4 );
        
        for( int trial = 0; trial < trials; trial++ ) {
            int ops = mRand.nextInt( 500 );
            int seed = mRand.nextInt();
            Random aRand = new Random( seed );
            Random bRand = new Random( seed );
            
            for(int i = 0; i < ops; i++) {
                randomListOp( aRand, a, range, false );
                randomListOp( bRand, b, range, false );
                
                assertEquiv( a, b );
            }
            
            assertEquiv( a, b );
            
            ListIterator<Integer> la = a.listIterator();
            ListIterator<Integer> lb = b.listIterator();
            
            for( int i = 0; i < ops; i++ ) {
                randomIterOp( aRand, la, lb, range );
            }
            
            assertEquiv( a, b );
            
            a.clear();
            b.clear();
            
            assertEquiv( a, b );
        }
    }
    
    
    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testSpeed() {
        final int trials = 20;
        final int range  = 10;
        
        Random rootRand = new Random( 0 );
        
        for( int type = 0; type < 2; type++ ) {
            boolean middleMods = ( type == 0 );
            long[] dur = { 0, 0 };
            List[] lists = { PublicList.create( Integer.class, 10 ),
                             new ArrayList( 10 ) };
            
            for(int i = 0; i < trials; i++) {
                int ops  = 10000;
                int seed = rootRand.nextInt();
                Random[] rands = { new Random( seed ), new Random( seed ) };
                
                for( int j = 0; j < lists.length; j++ ) {
                    Random rand = rands[j];
                    List<Integer> tt = lists[j];
                    tt.clear();
                    
                    long t0 = System.nanoTime();
                    for( int n = 0; n < ops; n++ ) {
                        randomListOp( rand, tt, range, middleMods ); 
                    }
                    long t1 = System.nanoTime();
                    
                    dur[j] += t1 - t0;
                }
            }
            
            if( middleMods ) {
                System.out.println( "Speed with modifications to middle elements: ");
            } else {
                System.out.println( "Speed with modifications to head/tail only: " );
            }
            
            System.out.println( "PublicList   : " + (dur[0] / 1000000000.0) + " seconds" );
            System.out.println( "ArrayList  : " + (dur[1] / 1000000000.0) + " seconds" );
            System.out.println();
        }
    }
    
    
    private static void randomListOp( Random rand, List<Integer> a, int range, boolean middleModifications ) {
        int maxOp = middleModifications ? 7 : 9;
        int op    = rand.nextInt( maxOp );
        
        switch( op ) {
        case 0:
        case 1:
        case 2:
        {
            Integer val = rand.nextInt( range );
            a.add( val );
            break;
        }
        case 3:
        {
            int s = a.size();
            if( s > 0 ) {
                a.remove( s - 1 );
            }
            break;
        }
        
        case 4:
        {
            int s = a.size();
            if( s > 0 ) {
                a.set( rand.nextInt( s ), rand.nextInt( range ) );
            }
            break;
        }
        
        case 5:
        {
            a.add( 0, rand.nextInt( range ) );
            break;
        }
        
        case 6:
        {   
            if( !a.isEmpty() ) {
                a.remove( 0 );
            }
            break;
        }
                
        // Ops with modifications to middle.
        case 7:
        {
            int s = a.size();
            int idx = s == 0 ? 0 : rand.nextInt( s );
            a.add( idx, rand.nextInt( range ) );
            break;
        }
        
        case 8:
        {
            int s = a.size();
            if( s > 0 ) {
                a.remove( rand.nextInt( s ) );
            }
             break;
        }}
    }
    
    
    private static void randomIterOp( Random rand, ListIterator<Integer> a, ListIterator<Integer> b, int range ) {
        int op = rand.nextInt( 9 );

        switch( op ) {
        case 0:
        {
            assertEquals( a.hasNext(), b.hasNext() );
            if( !a.hasNext() ) {
                return;
            }
            assertEquals( a.next(), b.next() );

            Integer val = rand.nextInt( range );
            a.add( val );
            b.add( val );
            break;
        }

        case 1:
        {
            assertEquals( a.hasPrevious(), b.hasPrevious() );
            if( !a.hasPrevious() ) {
                return;
            }
            assertEquals( a.previous(), b.previous() );
            
            Integer val = rand.nextInt( range );
            a.add( val );
            b.add( val );
            break;
        }

        case 2:
        case 3:
        {
            assertEquals( a.hasNext(), b.hasNext() );
            if( !a.hasNext() ) {
                return;
            }
            assertEquals( a.next(), b.next() );

            Integer val = rand.nextInt( range );
            a.add( val );
            b.add( val );
            break;
        }

        case 4:
        case 5:
        {
            assertEquals( a.hasPrevious(), b.hasPrevious() );
            if( !a.hasPrevious() ) {
                return;
            }
            assertEquals( a.previous(), b.previous() );
            break;
        }
        case 6:
        {
            assertEquals( a.previousIndex(), b.previousIndex() );
            assertEquals( a.nextIndex(), b.nextIndex() );
            break;
        }
        case 7:
        {
            assertEquals( a.hasNext(), b.hasNext() );
            if( !a.hasNext() )
                return;

            assertEquals( a.next(), b.next() );

            Integer val = rand.nextInt( range );
            a.set( val );
            b.set( val );
            break;
        }

        case 8:
        {
            assertEquals( a.hasPrevious(), b.hasPrevious() );
            if( !a.hasPrevious() )
                return;

            assertEquals( a.previous(), b.previous() );

            Integer val = rand.nextInt( range );
            a.set( val );
            b.set( val );
            break;
        }}
    }

    
    
    private void assertEquiv( List<Integer> a, List<Integer> b ) {
        assertEquals(a.size(), b.size());
        
        for(int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i), b.get(i));
        }
    }
    
    
    @SuppressWarnings( { "rawtypes", "unused" } )
    private void assertEquiv( Queue<Integer> a, Queue<Integer> b ) {
        assertEquals( a.size(), b.size() );

        Iterator ia = a.iterator();
        Iterator ib = b.iterator();

        while( ia.hasNext() ) {
            assertEquals( ia.next(), ib.next() );
        }
    }

}
