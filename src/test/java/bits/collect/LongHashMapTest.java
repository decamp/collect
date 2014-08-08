package bits.collect;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Philip DeCamp
 */
public class LongHashMapTest extends LongMapInterfaceTest {

    public LongHashMapTest() {
        super( true, true, true, true, true );
    }


    @Override
    protected LongMap makeEmptyMap() throws UnsupportedOperationException {
        return new LongHashMap();
    }

    @Override
    protected LongMap makePopulatedMap() throws UnsupportedOperationException {
        Random rand = new Random( 100 );
        LongMap<Long> ret = new LongHashMap<Long>();

        for( int i = 0; i < 1000; i++ ) {
            long k = rand.nextInt( 500 );
            long v = rand.nextInt( 500 );
            ret.put( k, v );
        }

        return ret;
    }

    @Override
    protected long getKeyNotInPopulatedMap() throws UnsupportedOperationException {
        return Long.MIN_VALUE;
    }

    @Override
    protected Object getValueNotInPopulatedMap() throws UnsupportedOperationException {
        return Long.MIN_VALUE;
    }


    public void testAddRemoveClear() {
        Map<Long, Double> normMap   = new HashMap<Long, Double>();
        LongHashMap<Double> longMap = new LongHashMap<Double>();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            Double d = (double)n.intValue();

            normMap.put( n, d );
            longMap.put( n, d );
        }
        compare( normMap, longMap );

        for( int i = 0; i < 50; i++ ) {
            Long n = (long)rand.nextInt( 100 );
            normMap.remove( n );
            longMap.remove( n );
        }
        compare( normMap, longMap );

        normMap.clear();
        longMap.clear();

        compare( normMap, longMap );
    }


    public void testKeyIterRemove() {
        Map<Long, Double> normMap   = new HashMap<Long, Double>();
        LongHashMap<Double> longMap = new LongHashMap<Double>();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            Double d = (double)n.intValue();
            normMap.put( n, d );
            longMap.put( n, d );
        }


        // Check remove on iterator.
        LongIterator iter = longMap.keySet().iterator();
        while( iter.hasNext() ) {
            long key = iter.next();
            if( rand.nextBoolean() ) {
                iter.remove();
                normMap.remove( key );
            }
        }
        compare( normMap, longMap );
    }


    public void testValueIterRemove() {
        Map<Long, Double> normMap   = new HashMap<Long, Double>();
        LongHashMap<Double> longMap = new LongHashMap<Double>();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            Double d = (double)n.intValue();
            normMap.put( n, d );
            longMap.put( n, d );
        }

        // Check remove on iterator.
        Iterator<Double> iter = longMap.values().iterator();
        while( iter.hasNext() ) {
            Double value = iter.next();
            Long key     = (long)( value.doubleValue() + 0.5 );

            if( rand.nextBoolean() ) {
                iter.remove();
                normMap.remove( key );
            }
        }
        compare( normMap, longMap );
    }


    public void testEntryIterRemove() {
        Map<Long, Double> normMap   = new HashMap<Long, Double>();
        LongHashMap<Double> longMap = new LongHashMap<Double>();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            Double d = (double)n.intValue();
            normMap.put( n, d );
            longMap.put( n, d );
        }

        // Check remove on iterator.
        Iterator<LongMap.Entry<Double>> iter = longMap.entrySet().iterator();
        while( iter.hasNext() ) {
            LongMap.Entry<Double> e = iter.next();
            if( rand.nextBoolean() ) {
                iter.remove();
                normMap.remove( e.getKey() );
            }
        }

        compare( normMap, longMap );
    }


    public void testContainsKeyValue() {
        LongHashMap<Double> longMap = new LongHashMap<Double>();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            Double d = (double)n.intValue();
            longMap.put( n, d );
        }

        // Check remove on iterator.
        Iterator<LongMap.Entry<Double>> iter = longMap.entrySet().iterator();
        while( iter.hasNext() ) {
            LongMap.Entry<Double> e = iter.next();
            assertTrue( "Failed containsKey()",   longMap.containsKey( e.getKey() ) );
            assertTrue( "Failed containsValue()", longMap.containsValue( e.getValue() ) );

            if( rand.nextBoolean() ) {
                iter.remove();
                assertFalse( "Failed containsKey()", longMap.containsKey( e.getKey() ) );
                assertFalse( "Failed containsValue()", longMap.containsValue( e.getValue() ) );
            }
        }
    }



    private static boolean compare( Map<Long,Double> x, LongHashMap y ) {
        assertTrue( "Size mismatch", x.size() == y.size() );

        for( Long k : x.keySet() ) {
            assertTrue( "Get mismatch", x.get( k ) == y.get( k ) );
        }

        for( Map.Entry<Long,Double> entry : x.entrySet() ) {
            assertTrue( "Key mismatch"  , y.containsKey(   entry.getKey()   ) );
            assertTrue( "Value mismatch", y.containsValue( entry.getValue() ) );
        }

        LongIterator iter = y.keySet().iterator();
        while( iter.hasNext() ) {
            long key = iter.next();
            assertTrue( "Keyset masmatch", y.containsKey( key ) && x.containsKey( key ) );
        }

        return true;
    }


}
