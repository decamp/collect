/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause 
 */
package bits.collect;

import java.util.*;
import org.junit.*;

import bits.collect.GroupSet;

import static org.junit.Assert.*;


/**
 * @author Philip DeCamp
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
public class GroupSetTest {

    private static Comparator<Integer> ORDER = new Comparator<Integer>() {
        @Override
        public int compare( Integer a, Integer b ) {
            int aa = a.intValue();
            int bb = b.intValue();

            if( aa < bb ) {
                return -1;
            }

            if( aa > bb ) {
                return 1;
            }

            return 0;
        }
    };

    private final Random mRand = new Random( 0 );


    public GroupSetTest() {}



    @Test
    public void testOneChannel() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testOneChannel( set );
        }
    }


    @Test
    public void testMultiChannel() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testMultiChannel( set );
        }
    }


    @Test
    public void testIter() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testIter( false, set );
            testIter( true, set );
        }
    }


    @Test
    public void testViews() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testViews( set );
        }
    }


    @Test
    public void testNavigation() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testNavigation( false, set );
        }

        for( GroupSet<Integer, Integer> set : createSets() ) {
            testNavigation( true, set );
        }
    }


    @Test
    public void testRandom() {
        for( GroupSet<Integer, Integer> set : createSets() ) {
            testRandom( set );
            // testTime(set);
            // testRefTime();
        }
    }



    public void testOneChannel( GroupSet<Integer, Integer> set ) {
        Integer channel = 3;
        List<Integer> list = new ArrayList<Integer>( 5 );

        for( int i = 0; i < 5; i++ ) {
            set.add( channel, i );
            list.add( i );
        }

        assertAllSequence( set, list );
        assertChanSequence( set, channel, list );

        Iterator<Integer> iter = set.iterator();
        iter = set.iterator();
        iter.next();
        iter.next();
        iter.next();
        iter.remove();
        list.remove( 2 );

        assertAllSequence( set, list );
        assertChanSequence( set, channel, list );


        iter = set.groupSubset( channel ).iterator();
        iter.next();
        iter.remove();
        list.remove( 0 );

        assertAllSequence( set, list );
        assertChanSequence( set, channel, list );

        set.clear();
        list.clear();

        assertAllSequence( set, list );
        assertChanSequence( set, channel, list );
    }


    public void testMultiChannel( GroupSet<Integer, Integer> set ) {
        Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
        List<Integer> allList = new ArrayList<Integer>( 15 );

        for( int c = 0; c < 3; c++ ) {
            Integer cc = c;
            List<Integer> list = new ArrayList<Integer>( 5 );
            map.put( cc, list );

            for( int i = 0; i < 5; i++ ) {
                set.add( cc, i );
                list.add( i );

                if( c == 0 ) {
                    allList.add( i );
                    allList.add( i );
                    allList.add( i );
                }
            }
        }

        assertAllSequence( set, allList );
        assertChanSequence( set, 0, map.get( 0 ) );
        assertChanSequence( set, 1, map.get( 1 ) );
        assertChanSequence( set, 2, map.get( 2 ) );

        Integer channel = 1;
        List<Integer> list = map.get( channel );

        for( int i = 0; i < 5; i++ ) {
            int idx = mRand.nextInt( list.size() );
            list.remove( idx );
            allList.remove( (4 - i) * 3 );
            remove( set.groupSubset( channel ).iterator(), idx );
            assertChanSequence( set, channel, list );
        }

        assertAllSequence( set, allList );

        channel = 2;
        list = map.get( channel );
        list.clear();
        set.clear( channel );

        for( int i = 0; i < 5; i++ ) {
            allList.remove( (4 - i) * 2 );
        }

        assertAllSequence( set, allList );
        assertChanSequence( set, 0, map.get( 0 ) );
        assertChanSequence( set, 1, map.get( 1 ) );
        assertChanSequence( set, 2, map.get( 2 ) );

    }


    public void testRandom( GroupSet<Integer, Integer> set ) {
        ReferenceGroupSet ref = new ReferenceGroupSet();


        for( int i = 0; i < 100000; i++ ) {
            switch( mRand.nextInt( 4 ) ) {
            case 0:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                set.add( c, v );
                ref.add( c, v );

                break;
            }

            case 1:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                set.remove( c, v );
                ref.remove( c, v );

                break;
            }

            case 2:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                    ref.add( c, v );
                }

                set.addAll( c, list );

                break;
            }

            case 3:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                    ref.remove( c, v );
                }

                set.removeAll( c, list );

                break;
            }

            case 4:
            {
                if( mRand.nextInt( 5 ) == 0 ) {
                    int c = mRand.nextInt( 10 );
                    set.clear( c );
                    ref.clear( c );
                }

                break;
            }
            }
        }


        assertEquals( "Size mismatch", ref.size(), set.size() );

        Iterator<?> iter1 = ref.iterator();
        Iterator<?> iter2 = set.iterator();

        while( iter1.hasNext() ) {
            assertEquals( "Node mismatch", iter1.next(), iter2.next() );
        }
    }


    public void testTime( GroupSet<Integer, Integer> set ) {

        for( int i = 0; i < 100000; i++ ) {
            switch( mRand.nextInt( 4 ) ) {
            case 0:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                set.add( c, v );
                break;
            }

            case 1:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                set.remove( c, v );
                break;
            }

            case 2:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                }

                set.addAll( c, list );
                break;
            }

            case 3:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                }

                set.removeAll( c, list );
                break;
            }

            case 4:
            {
                if( mRand.nextInt( 5 ) == 0 ) {
                    int c = mRand.nextInt( 10 );
                    set.clear( c );
                }

                break;
            }
            }
        }
    }


    public void testIter( boolean useChannel, GroupSet<Integer, Integer> set ) {
        int[] start = new int[]{ -1, -1, 3, 3, 4, 20 };
        int[] stop = new int[]{ -1, 4, 3, 4, 20, 22 };

        for( int i = 0; i < start.length; i++ ) {

            final int startPoint = start[i];
            final int stopPoint = stop[i];

            Comparable<Integer> comp = new Comparable<Integer>() {
                @Override
                public int compareTo( Integer value ) {
                    int v = value.intValue();

                    if( v < startPoint ) {
                        return 1;
                    }

                    if( v >= stopPoint ) {
                        return -1;
                    }

                    return 0;
                }
            };


            set.clear();
            List<Integer> list = new ArrayList<Integer>();

            if( useChannel ) {
                populate( set, 10000, 10, 10 );

                for( int j = 0; j < 20; j++ ) {
                    set.add( 30, j );
                    if( comp.compareTo( j ) == 0 ) {
                        list.add( j );
                    }
                }

                assertIter( set.groupSubset( 30, comp ).iterator(), list );
                assertDescendingIter( set.descendingGroupSubset( 30, comp ).iterator(), list );

            } else {
                populate( set, 10000, 0, 1000 );

                for( int j = 0; j < 20; j++ ) {
                    set.add( 30, j );
                    if( comp.compareTo( j ) == 0 ) {
                        list.add( j );
                    }
                }

                assertIter( set.groupSubset( 30, comp ).iterator(), list );
                assertDescendingIter( set.descendingGroupSubset( 30, comp ).iterator(), list );
            }
        }
    }


    public void testRefTime() {
        ReferenceGroupSet ref = new ReferenceGroupSet();

        for( int i = 0; i < 100000; i++ ) {
            switch( mRand.nextInt( 4 ) ) {
            case 0:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                ref.add( c, v );
                break;
            }

            case 1:
            {
                int c = mRand.nextInt( 10 );
                int v = mRand.nextInt( 100 );

                ref.remove( c, v );
                break;
            }

            case 2:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                    ref.add( c, v );
                }

                break;
            }

            case 3:
            {
                int c = mRand.nextInt( 10 );
                List<Integer> list = new ArrayList<Integer>( 8 );

                for( int j = 0; j < 8; j++ ) {
                    int v = mRand.nextInt( 100 );
                    list.add( v );
                    ref.remove( c, v );
                }

                break;
            }

            case 4:
            {
                if( mRand.nextInt( 5 ) == 0 ) {
                    int c = mRand.nextInt( 10 );
                    ref.clear( c );
                }

                break;
            }
            }
        }
    }


    public void testViews( GroupSet<Integer, Integer> set ) {
        populate( set, 10000, 10, 10 );
        doGroupViewTest( set );
    }


    public void testNavigation( boolean useGroup, GroupSet<Integer, Integer> set ) {
        Integer chan = 3;

        if( useGroup ) {
            populate( set, 10000, 10, 100 );
            set.clear( chan );
        }

        for( int i = 0; i <= 100; i += 2 ) {
            set.add( chan, i );
        }

        if( useGroup ) {
            assertEquals( "First error", 0, set.first( chan ) );
            assertEquals( "Last error", 100, set.last( chan ) );

            for( int i = 0; i <= 100; i += 2 ) {
                assertTrue( "Contains error", set.contains( i ) );
                assertTrue( "Contains error", set.contains( chan, i ) );
                assertEquals( "Floor error", i, set.floor( chan, i ) );
                assertEquals( "Ceiling error", i, set.ceiling( chan, i ) );

                if( i == 0 ) {
                    assertEquals( "Lower error", null, set.lower( chan, i ) );
                } else {
                    assertEquals( "Lower error", i - 2, set.lower( chan, i ) );
                }

                if( i == 100 ) {
                    assertEquals( "Higher error", null, set.higher( chan, i ) );
                } else {
                    assertEquals( "Higher error", i + 2, set.higher( chan, i ) );
                }
            }

            for( int i = -1; i <= 101; i += 2 ) {
                assertFalse( "Contains error", set.contains( chan, i ) );

                if( i < 0 ) {
                    assertEquals( "Floor error", null, set.floor( chan, i ) );
                    assertEquals( "Lower error", null, set.lower( chan, i ) );
                } else {
                    assertEquals( "Floor error", i - 1, set.floor( chan, i ) );
                    assertEquals( "Lower error", i - 1, set.lower( chan, i ) );
                }

                if( i > 100 ) {
                    assertEquals( "Ceiling error", null, set.ceiling( chan, i ) );
                    assertEquals( "Higher error", null, set.higher( chan, i ) );
                } else {
                    assertEquals( "Ceiling error", i + 1, set.ceiling( chan, i ) );
                    assertEquals( "Higher error", i + 1, set.higher( chan, i ) );
                }
            }

        } else {
            assertEquals( "First error", 0, set.first() );
            assertEquals( "Last error", 100, set.last() );

            for( int i = 0; i <= 100; i += 2 ) {
                assertTrue( "Contains error", set.contains( i ) );
                assertTrue( "Contains error", set.contains( chan, i ) );
                assertEquals( "Floor error", i, set.floor( i ) );
                assertEquals( "Ceiling error", i, set.ceiling( i ) );

                if( i == 0 ) {
                    assertEquals( "Lower error", null, set.lower( i ) );
                } else {
                    assertEquals( "Lower error", i - 2, set.lower( i ) );
                }

                if( i == 100 ) {
                    assertEquals( "Higher error", null, set.higher( i ) );
                } else {
                    assertEquals( "Higher error", i + 2, set.higher( i ) );
                }
            }

            for( int i = -1; i <= 101; i += 2 ) {
                assertFalse( "Contains error", set.contains( i ) );
                assertFalse( "Contains error", set.contains( chan, i ) );

                if( i < 0 ) {
                    assertEquals( "Floor error", null, set.floor( i ) );
                    assertEquals( "Lower error", null, set.lower( i ) );
                } else {
                    assertEquals( "Floor error", i - 1, set.floor( i ) );
                    assertEquals( "Lower error", i - 1, set.lower( i ) );
                }

                if( i > 100 ) {
                    assertEquals( "Ceiling error", null, set.ceiling( i ) );
                    assertEquals( "Higher error", null, set.higher( i ) );
                } else {
                    assertEquals( "Ceiling error", i + 1, set.ceiling( i ) );
                    assertEquals( "Higher error", i + 1, set.higher( i ) );
                }
            }
        }
    }



    private void doGroupViewTest( GroupSet<Integer, Integer> map ) {
        Set<Integer> set = map.groups();

        for( int i = 0; i < 3; i++ ) {
            int sum = 0;

            for( Integer g : map.groups() ) {
                int s = map.size( g );
                assertTrue( "Empty group error", s > 0 );
                sum += s;
            }

            assertEquals( "Group size accumulation error", map.size(), sum );

            Iterator<?> iter = set.iterator();
            iter.next();
            iter.next();
            iter.remove();
        }
    }



    static void assertEquals( String msg, Object expected, Object ref ) {
        Assert.assertEquals( msg, expected, ref );
    }


    static void assertEquals( String msg, int expected, Object ref ) {
        Assert.assertEquals( msg, expected, ref );
    }


    static void assertEquals( String msg, int expected, int actual ) {
        Assert.assertEquals( msg, expected, actual );
    }


    private static void assertAllSequence( GroupSet set, List values ) {
        assertEquals( "Size mismatch", set.size(), values.size() );
        Iterator<Integer> iter = set.iterator();

        for( int i = 0; i < values.size(); i++ ) {
            assertEquals( "Sequence mismatch", iter.next(), values.get( i ) );
        }

        for( int i = 0; i < values.size(); i++ ) {
            if( !set.contains( values.get( i ) ) ) {
                assertTrue( "Contains error", set.contains( values.get( i ) ) );
            }
        }

        assertTrue( "ContainsAll error", set.containsAll( values ) );
    }


    private static void assertChanSequence( GroupSet set, Object channel, List values ) {
        assertEquals( "Size mismatch", set.size( channel ), values.size() );
        Iterator<Integer> iter = set.groupSubset( channel ).iterator();

        for( int i = 0; i < values.size(); i++ ) {
            assertEquals( "Sequence mismatch", iter.next(), values.get( i ) );
        }

        for( int i = 0; i < values.size(); i++ ) {
            assertTrue( "Contains error", set.contains( channel, values.get( i ) ) );
        }

        assertTrue( "ContainsAll error", set.containsAll( channel, values ) );
    }


    private static void assertIter( Iterator iter, List values ) {
        for( Object obj : values ) {
            assertTrue( "Iter size error", iter.hasNext() );
            assertEquals( "Iter mismatch", iter.next(), obj );
        }

        assertFalse( "Iter size error", iter.hasNext() );
    }


    private static void assertDescendingIter( Iterator iter, List values ) {
        for( int i = values.size() - 1; i >= 0; i-- ) {
            assertTrue( "Iter size error", iter.hasNext() );
            assertEquals( "Iter mismatch", iter.next(), values.get( i ) );
        }

        assertFalse( "Iter size error", iter.hasNext() );
    }


    private static void remove( Iterator iter, int idx ) {
        for( int i = 0; i <= idx; i++ ) {
            iter.next();
        }

        iter.remove();
    }


    private static List<GroupSet<Integer, Integer>> createSets() {
        List<GroupSet<Integer, Integer>> ret = new ArrayList<GroupSet<Integer, Integer>>();

        ret.add( new GroupSet<Integer, Integer>( null, null ) );
        ret.add( new GroupSet<Integer, Integer>( ORDER, null ) );
        ret.add( new GroupSet<Integer, Integer>( null, ORDER ) );
        ret.add( new GroupSet<Integer, Integer>( ORDER, ORDER ) );

        return ret;
    }


    private void populate( GroupSet<Integer, Integer> set, int num, int maxGroup, int maxValue ) {
        for( int i = 0; i < num; i++ ) {
            int g = mRand.nextInt( maxGroup + 1 );

            if( g == maxGroup ) {
                set.add( null, mRand.nextInt( maxValue ) );
            } else {
                set.add( g, mRand.nextInt( maxValue ) );
            }
        }
    }


    private static class ReferenceGroupSet {

        private Set<Entry> mAllSet = new TreeSet<Entry>();
        private Map<Object, TreeSet<Object>> mGroups = new HashMap<Object, TreeSet<Object>>();


        public void add( Object key, Object value ) {
            mAllSet.add( new Entry( key, value ) );
            TreeSet<Object> set = mGroups.get( key );
            if( set == null ) {
                set = new TreeSet<Object>();
                mGroups.put( key, set );
            }

            set.add( value );
        }

        public void remove( Object key, Object value ) {
            mAllSet.remove( new Entry( key, value ) );
            TreeSet<Object> set = mGroups.get( key );

            if( set == null ) {
                return;
            }

            set.remove( value );
        }


        public Iterator iterator() {
            return new EntryValueIter( mAllSet.iterator() );
        }
        
        
        public void clear( Object key ) {
            TreeSet<Object> set = mGroups.remove( key );
            if( set == null ) {
                return;
            }

            for( Object v : set ) {
                mAllSet.remove( new Entry( key, v ) );
            }
        }


        public int size() {
            return mAllSet.size();
        }
    }


    private static class Entry implements Comparable<Entry> {

        public final Object mKey;
        public final Object mValue;

        public Entry( Object key, Object value ) {
            mKey = key;
            mValue = value;
        }


        @Override
        public int compareTo( Entry entry ) {
            int c = ((Comparable)mValue).compareTo( entry.mValue );
            if( c != 0 ) {
                return c;
            }

            return ((Comparable)mKey).compareTo( entry.mKey );
        }


        @Override
        public boolean equals( Object obj ) {
            if( !(obj instanceof Entry) ) {
                return false;
            }

            return mKey.equals( ((Entry)obj).mKey ) &&
                   mValue.equals( ((Entry)obj).mValue );

        }
    }


    private static class EntryValueIter implements Iterator {
        private Iterator<Entry> mIter;

        public EntryValueIter( Iterator<Entry> iter ) {
            mIter = iter;
        }


        @Override
        public boolean hasNext() {
            return mIter.hasNext();
        }

        @Override
        public Object next() {
            return mIter.next().mValue;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
