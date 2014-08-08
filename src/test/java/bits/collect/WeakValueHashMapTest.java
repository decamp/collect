/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */
package bits.collect;

import java.util.*;

import bits.collect.WeakValueHashMap;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * @author Philip DeCamp
 */
@SuppressWarnings( "rawtypes" )
public class WeakValueHashMapTest {

    @Test
    public void test1() {
        Map<Integer, Double> normMap = new HashMap<Integer, Double>();
        Map<Integer, Double> weakMap = new WeakValueHashMap<Integer, Double>();
        Random rand = new Random( System.currentTimeMillis() );

        for( int i = 0; i < 1000; i++ ) {
            Integer n = rand.nextInt() % 1000;
            Double d = (double)n.intValue();
            normMap.put( n, d );
            weakMap.put( n, d );
        }
        
        //System.out.println( normMap.equals( weakMap ) );
        //System.out.println( normMap.keySet().equals( weakMap.keySet() ) );
        //System.out.println( normMap.entrySet().equals( weakMap.entrySet() ) );
        //System.out.println( "---" );
        compare( normMap, weakMap );

        normMap.remove( 5 );
        weakMap.remove( 5 );

        compare( normMap, weakMap );
        weakMap.size();
        
        for( int i = 0; i < 10; i++ ) {
            Iterator iter = normMap.entrySet().iterator();
            iter.next();
            iter.remove();
        }
        
        System.gc();

        try {
            Thread.sleep( 2000L );
        } catch( InterruptedException ex ) {}

        weakMap.put( 3, 3.0 );
        //System.out.println( weakMap.size() );
    }


    private static boolean compare( Map x, Map y ) {
        assertTrue( "Size mismatch", x.size() == y.size() );
        
        for( Object k : x.keySet() ) {
            assertTrue( "Get mismatch", x.get( k ) == y.get( k ) );
        }
        
        for( Object entry : x.entrySet() ) {
            assertTrue( "Node mismatch", y.entrySet().contains( entry ) );
            assertTrue( "Key mismatch", y.containsKey( ((Map.Entry)entry).getKey() ) );
            assertTrue( "Value mismatch", y.containsValue( ((Map.Entry)entry).getValue() ) );
        }
        
        for( Object key : y.keySet() ) {
            assertTrue( "Keyset masmatch", y.containsKey( key ) && x.containsKey( key ) );
        }
        
        return true;
    }

}
