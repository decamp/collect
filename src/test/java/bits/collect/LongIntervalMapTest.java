/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;


/**
 * @author Philip DeCamp
 */
public class LongIntervalMapTest {


    public long getSeed() {
        // return System.currentTimeMillis();
        return 2;
    }


    @Test public void testGet() {
        LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
        StupidLongIntervalMap stupid = new StupidLongIntervalMap();

        RandomIter iter = new RandomIter( getSeed(), 0, 100 );
        final int count = 10000;

        for( int i = 0; i < count; i++ ) {
            long[] key = iter.next();
            map.put( key, key );
            stupid.put( key, key );
        }

        assertEquals( map.size(), count );
        int foundCount = 0;

        for( long[] k : stupid.keyList() ) {
            assertNotNull( map.get( k ) );
            assertEquals( k, map.get( k ) );
            foundCount++;
        }

        assertEquals( count, foundCount );
    }


    @Test public void testGetIntersecting() {
        final RandomIter iter = new RandomIter( getSeed(), 0, 100 );
        final int testCount = 5000;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 5 );
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int s = 0; s < subtestCount; s++ ) {
                long[] key = iter.next();

                long[] a = map.getIntersection( key );
                long[] b = stupid.getIntersecting( key );

                if( a != b && (a == null || !a.equals( b )) ) {
                    System.out.println( a );
                    System.out.println( b );
                    map.getIntersection( key );
                }

                assertEquals( a, b );
            }
        }
    }


    @Test public void testGetAllIntersecting() {
        RandomIter iter = new RandomIter( getSeed(), 0, 1000 );

        final int testCount = 200;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 100 );
            final LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            final StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();
                map.put( key, key );
                stupid.put( key, key );
            }

            for( int i = 0; i < testCount; i++ ) {
                long[] key = iter.next();
                Collection<long[]> a = new ArrayList<long[]>( map.intersectionKeySet( key ) );
                List<long[]> b = stupid.getAllIntersecting( key );
                assertDeepEquals( a, b );
            }
        }
    }


    @Test public void testGetSuperset() {
        LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
        StupidLongIntervalMap stupid = new StupidLongIntervalMap();
        RandomIter iter = new RandomIter( getSeed(), 0, 1000 );

        final int testCount = 2000;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            int keyCount = iter.mRand.nextInt( 200 );

            for( int i = 0; i < keyCount; i++ ) {
                long[] key = iter.next();
                map.put( key, key );
                stupid.put( key, key );
            }
        }

        for( int i = 0; i < subtestCount; i++ ) {
            long[] key = iter.next();
            long[] a = map.getSuperset( key );
            long[] b = stupid.getContaining( key );

            assertEquals( a, b );
        }
    }


    @Test public void testGetSubset() {
        LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
        StupidLongIntervalMap stupid = new StupidLongIntervalMap();
        RandomIter iter = new RandomIter( getSeed(), 0, 1000 );

        final int testCount = 2000;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            int keyCount = iter.mRand.nextInt( 200 );

            for( int i = 0; i < keyCount; i++ ) {
                long[] key = iter.next();
                map.put( key, key );
                stupid.put( key, key );
            }
        }

        for( int i = 0; i < subtestCount; i++ ) {
            long[] key = iter.next();
            long[] a = map.getSubset( key );
            long[] b = stupid.getContained( key );

            if( (a == null) != (b == null) || a != null && a[0] != b[0] && a[1] != b[1] ) {
                map.getSubset( key );
            }

            assertEquals( a, b );
        }
    }


    @Test public void testGetAllSupersets() {

        RandomIter iter = new RandomIter( getSeed(), 0, 1000 );
        final int testCount = 1000;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            final LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            final StupidLongIntervalMap stupid = new StupidLongIntervalMap();
            final int keyCount = iter.mRand.nextInt( 200 );

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int i = 0; i < subtestCount; i++ ) {
                long[] key = iter.next();

                Collection<long[]> a = new ArrayList<long[]>( map.supersetKeySet( key ) );
                List<long[]> b = stupid.getAllContaining( key );

                if( a.size() != b.size() ) {
                    System.out.println( a.size() );
                    System.out.println( b.size() );
                }

                assertDeepEquals( a, b );
            }
        }
    }


    @Test public void testGetAllSubsets() {

        RandomIter iter = new RandomIter( getSeed(), 0, 2000 );
        final int testCount = 2000;
        final int subtestCount = 200;

        for( int t = 0; t < testCount; t++ ) {
            final LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            final StupidLongIntervalMap stupid = new StupidLongIntervalMap();
            final int keyCount = iter.mRand.nextInt( 200 );

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int i = 0; i < subtestCount; i++ ) {
                long[] key = iter.next();

                Collection<long[]> a = new ArrayList<long[]>( map.subsetKeySet( key ) );
                List<long[]> b = stupid.getAllContained( key );

                if( a.size() != b.size() ) {
                    System.out.println( a.size() );
                    System.out.println( b.size() );
                }

                assertDeepEquals( a, b );
            }
        }
    }


    @Test public void testRemove() {
        LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
        RandomIter iter = new RandomIter( getSeed(), 0, 10000 );
        Random rand = new Random( 1 );
        List<long[]> list = new ArrayList<long[]>();

        final int count = 10000;

        for( int i = 0; i < count; i++ ) {

            if( rand.nextInt( 10 ) <= 6 ) {
                long[] key = iter.next();
                map.put( key, key );
                list.add( key );

            } else {
                long[] key;

                if( list.size() == 0 ) {
                    key = iter.next();
                } else {
                    int idx = rand.nextInt( list.size() );
                    key = list.get( idx );
                    list.remove( idx );
                }

                long[] val1 = map.get( key );
                int size1 = map.size();
                long[] val2 = map.remove( key );
                int size2 = map.size();

                assertEquals( val1, val2 );

                if( val2 == null ) {
                    assertEquals( size1, size2 );
                } else {
                    assertEquals( size1, size2 + 1 );
                }

                assertEquals( size2, list.size() );
                assertTrue( map.validateMaxStops() );
            }
        }
    }


    @Test public void testRemoveAll() {
        LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
        StupidLongIntervalMap stupid = new StupidLongIntervalMap();

        RandomIter iter = new RandomIter( getSeed(), 0, 10 );
        Random rand = new Random( 1 );
        final int count = 10000;

        for( int i = 0; i < count; i++ ) {
            if( rand.nextInt( 10 ) <= 8 ) {
                long[] key = iter.next();
                map.put( key, key );
                stupid.put( key, key );

            } else {
                long[] key = iter.next();
                List<long[]> a = new ArrayList<long[]>( map.equivKeySet( key ) );
                map.equivEntrySet( key ).clear();
                List<long[]> b = stupid.removeAll( key );
                assertDeepEquals( a, b );
            }
        }
    }


    @Test public void testContainsSupersetUnion() {
        RandomIter iter = new RandomIter( getSeed(), 0, 50 );
        Random rand = new Random( 1 );

        final int count = 10000;


        for( int i = 0; i < count; i++ ) {

            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            long[] key = null;
            long[] lastKey = null;

            for( int j = rand.nextInt( 10 ); j > 0; j-- ) {
                if( lastKey != null && rand.nextBoolean() ) {
                    key = lastKey.clone();
                } else {
                    key = iter.next();
                }

                map.put( key, key );
                stupid.put( key, key );
                lastKey = key;
            }

            for( int j = 0; j < 100; j++ ) {
                key = iter.next();

                boolean ans1 = map.containsSupersetUnion( key );
                boolean ans2 = stupid.containsUnionThatContains( key );

                if( ans1 != ans2 ) {
                    System.out.println( ans1 + "\t" + ans2 );
                    map.containsSupersetUnion( key );
                    stupid.containsUnionThatContains( key );
                }

                assertEquals( ans1, ans2 );

                assertEquals( map.containsSupersetUnion( key ), stupid.containsUnionThatContains( key ) );
            }
        }

    }


    @Test public void testViews() {
        RandomIter iter = new RandomIter( getSeed(), 0, 50 );

        for( int i = 0; i < 10; i++ ) {
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            List<long[]> list = new ArrayList<long[]>();

            for( int j = 0; j < 100; j++ ) {
                long[] key = iter.next();
                map.put( key, key );
                list.add( key );
            }

            long[] key = iter.next();
            assertDeepEquals( map.intersectionKeySet( key ), map.intersectionValues( key ) );
            assertDeepEquals( map.supersetKeySet( key ), map.supersetValues( key ) );

            key = list.get( iter.mRand.nextInt( list.size() ) );
            assertDeepEquals( map.equivKeySet( key ), map.equivValues( key ) );

            key = new long[]{ Long.MIN_VALUE, Long.MAX_VALUE };
            assertDeepEquals( map.intersectionKeySet( key ), map.values() );
        }
    }


    @Test public void testEquivView() {
        RandomIter iter = new RandomIter( getSeed(), 0, 100 );

        final int testCount = 2500;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 5 );
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int s = 0; s < subtestCount; s++ ) {
                long[] key = iter.next();

                Collection<long[]> a = map.equivValues( key );
                List<long[]> b = stupid.getAll( key );

                assertDeepEquals( a, b );

                a = map.descendingEquivValues( key );

                assertDeepReverseEquals( a, b );
            }
        }
    }


    @Test public void testIntersectView() {
        RandomIter iter = new RandomIter( getSeed(), 0, 100 );

        final int testCount = 2500;
        final int subtestCount = 100;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 20 );
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int s = 0; s < subtestCount; s++ ) {
                long[] key = iter.next();

                Collection<long[]> a = map.intersectionValues( key );
                List<long[]> b = stupid.getAllIntersecting( key );

                assertDeepEquals( a, b );

                a = map.descendingIntersectionValues( key );

                assertDeepReverseEquals( a, b );
            }
        }
    }


    @Test public void testSupersetView() {
        RandomIter iter = new RandomIter( getSeed(), 0, 100 );

        final int testCount = 2500;
        final int subtestCount = 200;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 20 );
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int s = 0; s < subtestCount; s++ ) {
                long[] key = iter.next();

                Collection<long[]> a = map.supersetValues( key );
                List<long[]> b = stupid.getAllContaining( key );

                assertDeepEquals( a, b );

                a = map.descendingSupersetValues( key );

                assertDeepReverseEquals( a, b );
            }
        }
    }


    @Test public void testSubsetView() {
        RandomIter iter = new RandomIter( getSeed(), 0, 100 );

        final int testCount = 2500;
        final int subtestCount = 200;

        for( int t = 0; t < testCount; t++ ) {
            final int keyCount = iter.mRand.nextInt( 20 );
            LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int k = 0; k < keyCount; k++ ) {
                long[] key = iter.next();

                map.put( key, key );
                stupid.put( key, key );
            }

            for( int s = 0; s < subtestCount; s++ ) {
                long[] key = iter.next();

                Collection<long[]> a = map.subsetValues( key );
                List<long[]> b = stupid.getAllContained( key );

                assertDeepEquals( a, b );

                a = map.descendingSubsetValues( key );

                assertDeepReverseEquals( a, b );
            }
        }
    }


    @Test public void testSpeed() {
        RandomIter iter = new RandomIter( getSeed(), 0, 10000 );
        final int testCount = 50;
        final int subtestCount = 100;

        long mapNanos = 0;
        long refNanos = 0;


        for( int t = 0; t < testCount; t++ ) {
            // iter.setSparseness(iter.mRand.nextDouble() * 0.5 + 0.75);
            iter.setSparseness( 0.001 );

            final int keyCount = iter.mRand.nextInt( 20000 );
            final LongIntervalMap<long[]> map = new LongIntervalMap<long[]>();
            final StupidLongIntervalMap stupid = new StupidLongIntervalMap();

            for( int i = 0; i < keyCount; i++ ) {
                long[] key = iter.next();
                map.put( key[0], key[1], key );
                stupid.put( key, key );
            }

            List<long[]> testSet = new ArrayList<long[]>();

            for( int i = 0; i < subtestCount; i++ ) {
                testSet.add( iter.next() );
            }


            long startNanos, stopNanos;

            startNanos = System.nanoTime();

            for( long[] key : testSet ) {
                ArrayList<long[]> list = new ArrayList<long[]>();
                list.addAll( map.intersectionKeySet( key[0], key[1] ) );
                list.clear();
                list.addAll( map.supersetKeySet( key[0], key[1] ) );
                list.clear();
                list.addAll( map.equivKeySet( key[0], key[1] ) );
            }

            stopNanos = System.nanoTime();
            mapNanos += (stopNanos - startNanos);

            startNanos = System.nanoTime();

            for( long[] key : testSet ) {
                stupid.getAllIntersecting( key );
                stupid.getAllContaining( key );
                stupid.getAll( key );
            }

            stopNanos = System.nanoTime();
            refNanos += (stopNanos - startNanos);
        }


        double testSec = mapNanos / 1000000000.0;
        double refSec = refNanos / 1000000000.0;

        System.out.println( "IntervalMap time: " + testSec + " seconds" );
        System.out.println( "SortedMap time: " + refSec + " secods" );
    }



    private static void assertDeepEquals( Collection<?> a, Collection<?> b ) {
        assertEquals( a.size(), b.size() );

        Iterator<?> aa = a.iterator();
        Iterator<?> bb = b.iterator();

        while( aa.hasNext() ) {
            assertEquals( aa.next(), bb.next() );
        }
    }


    private static void assertDeepReverseEquals( Collection<?> a, List<?> b ) {
        assertEquals( a.size(), b.size() );

        Iterator<?> aa = a.iterator();
        ListIterator<?> bb = b.listIterator( b.size() );

        while( aa.hasNext() ) {
            assertEquals( aa.next(), bb.previous() );
        }

    }


    private static class RandomIter {

        public final Random mRand;
        private final long mMin;
        private final long mMax;

        private double mSparseness = 1.0;
        private long[] mLast = null;


        public RandomIter( long seed, long min, long max ) {
            mRand = new Random( seed );
            mMin = min;
            mMax = max;
        }


        public void setSparseness( double sparse ) {
            mSparseness = sparse;
        }

        public long[] next() {
            int mode = mRand.nextInt( 5 );

            if( mode == 0 ) {
                long a = Math.abs( mRand.nextLong() % (mMax - mMin) ) + mMin;
                return new long[]{ a, a };
            }

            if( mode <= 2 && mLast != null ) {
                return mLast.clone();
            }

            long a = Math.abs( mRand.nextLong() % (mMax - mMin) ) + mMin;
            long b = (long)(Math.abs( mRand.nextLong() % (mMax - mMin) ) * mSparseness + 0.5);

            return new long[]{ a, a + b };
        }

    }


    private static class StupidLongIntervalMap extends StupidIntervalMap<long[], long[]> {
        public StupidLongIntervalMap() {
            super( SINGLE_INDEX_COMP, LongIntervalMap.LONG_PAIR_COMP );
        }
    }


    private static final Comparator<long[]> SINGLE_INDEX_COMP = new Comparator<long[]>() {
        public int compare( long[] a, long[] b ) {
            if( a[0] < b[0] )
                return -1;

            if( a[0] > b[0] )
                return 1;

            if( a[1] < b[1] )
                return -1;

            if( a[1] > b[1] )
                return 1;

            return 0;
        }
    };


}
