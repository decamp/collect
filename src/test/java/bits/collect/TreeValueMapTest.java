/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */ 
package bits.collect;

import java.util.*;

import org.junit.Test;

import bits.collect.TreeValueMap;
import static org.junit.Assert.*;

/**
 * @author Philip DeCamp
 */
public class TreeValueMapTest {
    
    @Test
    public void test1() {
        TreeValueMap<String,Integer> map = new TreeValueMap<String,Integer>();
        String chars = "abcdefg";
        Random rand = new Random( 0 );
        
        for( int i = 0; i < chars.length(); i++ ) {
            String key = chars.substring( i, i+1 );
            int value = rand.nextInt( 5 );
            map.put( key, value );
        }
        
        checkOrder( map );
        
        //System.out.println( "###\n\n" );
        //System.out.println( map.size() );
        map.remove( "c" );
        map.remove( "b" );
        //System.out.println( map.size() );
        
        checkOrder( map );
    }

    private static <T extends Comparable<T>> void checkOrder( Map<?,T> map ) {
        Comparable<T> prev = null;
        for( Map.Entry<?,T> e: map.entrySet() ) {
            if( prev != null ) {
                assertTrue( prev.compareTo( e.getValue() ) <= 0 );
            }
            prev = e.getValue();
        }
    }
    
}
