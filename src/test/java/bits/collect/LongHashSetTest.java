package bits.collect;

import java.util.*;


/**
 * @author Philip DeCamp
 */
public class LongHashSetTest extends LongSetInterfaceTest {

    public LongHashSetTest() {
        super( true, true, true, true, true );
    }


    @Override
    protected LongSet makeEmptySet() throws UnsupportedOperationException {
        return new LongHashSet();
    }

    @Override
    protected LongSet makePopulatedSet() throws UnsupportedOperationException {
        Random rand = new Random( 100 );
        LongSet ret = new LongHashSet();

        for( int i = 0; i < 1000; i++ ) {
            long k = rand.nextInt( 500 );
            ret.add( k );
        }

        return ret;
    }

    @Override
    protected long getItemNotInPopulatedSet() throws UnsupportedOperationException {
        return Long.MIN_VALUE;
    }


    public void testAddRemoveClear() {
        Set<Long> normSet   = new HashSet<Long>();
        LongHashSet longSet = new LongHashSet();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            normSet.add( n );
            longSet.add( n );
        }
        compare( normSet, longSet );

        for( int i = 0; i < 50; i++ ) {
            Long n = (long)rand.nextInt( 100 );
            normSet.remove( n );
            longSet.remove( n );
        }
        compare( normSet, longSet );

        normSet.clear();
        longSet.clear();

        compare( normSet, longSet );
    }


    public void testIterRemove() {
        Set<Long> normSet   = new HashSet<Long>();
        LongHashSet longSet = new LongHashSet();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            normSet.add( n );
            longSet.add( n );
        }


        // Check remove on iterator.
        LongIterator iter = longSet.iterator();
        while( iter.hasNext() ) {
            long key = iter.next();
            if( rand.nextBoolean() ) {
                iter.remove();
                normSet.remove( key );
            }
        }
        compare( normSet, longSet );
    }


    public void testContains() {
        LongHashSet longSet = new LongHashSet();
        Random rand = new Random( 0 );

        for( int i = 0; i < 1000; i++ ) {
            Long n   = (long)rand.nextInt( 100 );
            longSet.add( n );
        }

        // Check remove on iterator.
        LongIterator iter = longSet.iterator();
        while( iter.hasNext() ) {
            long e = iter.next();
            assertTrue( "Failed contains()", longSet.contains( e ) );
            if( rand.nextBoolean() ) {
                iter.remove();
                assertFalse( "Failed containsKey()", longSet.contains( e ) );
            }
        }
    }



    private static boolean compare( Set<Long> x, LongHashSet y ) {
        assertTrue( "Size mismatch", x.size() == y.size() );

        for( Long k : x ) {
            assertTrue( "Contains mismatch", y.contains( k ) );
        }

        LongIterator iter = y.iterator();
        while( iter.hasNext() ) {
            long item = iter.next();
            assertTrue( "Contains Mismatch", y.contains( item ) && x.contains( item ) );
        }

        return true;
    }


}
