/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

import junit.framework.TestCase;

import java.util.*;

import static bits.collect.LongMap.Entry;
import static java.util.Collections.singleton;


/**
 * Tests representing the contract of {@link java.util.Map}. Concrete subclasses of this
 * base class test conformance of concrete {@link java.util.Map} subclasses to that
 * contract.
 * <p/>
 * TODO: Descriptive assertion messages, with hints as to probable
 * fixes.
 * TODO: Add another constructor parameter indicating whether the
 * class under test is ordered, and check the order if so.
 * TODO: Refactor to share code with SetTestBuilder &c.
 *
 * @param <V> the type of mapped values used the maps under test
 * @author George van den Driessche
 */
public abstract class LongMapInterfaceTest<V> extends TestCase {

    protected final boolean supportsPut;
    protected final boolean supportsRemove;
    protected final boolean supportsClear;
    protected final boolean allowsNullValues;
    protected final boolean supportsIteratorRemove;

    /**
     * Constructor that assigns {@code supportsIteratorRemove} the same value as
     * {@code supportsRemove}.
     */
    protected LongMapInterfaceTest(
            boolean allowsNullValues,
            boolean supportsPut,
            boolean supportsRemove,
            boolean supportsClear )
    {
        this( allowsNullValues, supportsPut, supportsRemove,
              supportsClear, supportsRemove );
    }

    /**
     * Constructor with an explicit {@code supportsIteratorRemove} parameter.
     */
    protected LongMapInterfaceTest(
            boolean allowsNullValues,
            boolean supportsPut,
            boolean supportsRemove,
            boolean supportsClear,
            boolean supportsIteratorRemove )
    {
        this.supportsPut = supportsPut;
        this.supportsRemove = supportsRemove;
        this.supportsClear = supportsClear;
        this.allowsNullValues = allowsNullValues;
        this.supportsIteratorRemove = supportsIteratorRemove;
    }

    private static <V> Entry<V> mapEntry( long key, V value ) {
        return new TestEntry<V>( key, value );
    }

    private static int hash( long v ) {
        return (int)v;
    }

    /**
     * Creates a new, empty instance of the class under test.
     *
     * @return a new, empty map instance.
     * @throws UnsupportedOperationException if it's not possible to make an
     *                                       empty instance of the class under test.
     */
    protected abstract LongMap<V> makeEmptyMap() throws UnsupportedOperationException;

    /**
     * Creates a new, non-empty instance of the class under test.
     *
     * @return a new, non-empty map instance.
     * @throws UnsupportedOperationException if it's not possible to make a
     *                                       non-empty instance of the class under test.
     */
    protected abstract LongMap<V> makePopulatedMap() throws UnsupportedOperationException;

    /**
     * Creates a new key that is not expected to be found
     * in {@link #makePopulatedMap()}.
     *
     * @return a key.
     * @throws UnsupportedOperationException if it's not possible to make a key
     *                                       that will not be found in the map.
     */
    protected abstract long getKeyNotInPopulatedMap() throws UnsupportedOperationException;

    /**
     * Creates a new value that is not expected to be found
     * in {@link #makePopulatedMap()}.
     *
     * @return a value.
     * @throws UnsupportedOperationException if it's not possible to make a value
     *                                       that will not be found in the map.
     */
    protected abstract V getValueNotInPopulatedMap() throws UnsupportedOperationException;

    /**
     * Used by tests that require a map, but don't care whether it's
     * populated or not.
     *
     * @return a new map instance.
     */
    protected LongMap<V> makeEitherMap() {
        try {
            return makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return makeEmptyMap();
        }
    }

    protected final boolean supportsValuesHashCode( LongMap<V> map ) {
        // get the first non-null value
        Collection<V> values = map.values();
        for( V value : values ) {
            if( value != null ) {
                try {
                    value.hashCode();
                } catch( Exception e ) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    /**
     * Checks all the properties that should always hold of a map. Also calls
     * {@link #assertMoreInvariants} to check invariants that are peculiar to
     * specific implementations.
     *
     * @param map the map to check.
     * @see #assertMoreInvariants
     */
    protected final void assertInvariants( LongMap<V> map ) {
        LongSet keySet = map.keySet();
        Collection<V> valueCollection = map.values();
        Set<Entry<V>> entrySet = map.entrySet();

        assertEquals( map.size() == 0, map.isEmpty() );
        assertEquals( map.size(), keySet.size() );
        assertEquals( keySet.size() == 0, keySet.isEmpty() );
        assertEquals( !keySet.isEmpty(), keySet.iterator().hasNext() );

        int expectedKeySetHash = 0;
        for( LongIterator it = keySet.iterator(); it.hasNext(); ) {
            long key = it.next();
            V value = map.get( key );
            expectedKeySetHash ^= (int)(key >> 32 | key);
            assertTrue( map.containsKey( key ) );
            assertTrue( map.containsValue( value ) );
            assertTrue( valueCollection.contains( value ) );
            assertTrue( valueCollection.containsAll( Collections.singleton( value ) ) );
            assertTrue( entrySet.contains( mapEntry( key, value ) ) );
        }
        assertEquals( expectedKeySetHash, keySet.hashCode() );

        assertEquals( map.size(), valueCollection.size() );
        assertEquals( valueCollection.size() == 0, valueCollection.isEmpty() );
        assertEquals(
                !valueCollection.isEmpty(), valueCollection.iterator().hasNext() );
        for( V value : valueCollection ) {
            assertTrue( map.containsValue( value ) );
            assertTrue( allowsNullValues || (value != null) );
        }

        assertEquals( map.size(), entrySet.size() );
        assertEquals( entrySet.size() == 0, entrySet.isEmpty() );
        assertEquals( !entrySet.isEmpty(), entrySet.iterator().hasNext() );
        assertFalse( entrySet.contains( "foo" ) );

        boolean supportsValuesHashCode = supportsValuesHashCode( map );
        if( supportsValuesHashCode ) {
            int expectedEntrySetHash = 0;
            for( LongMap.Entry<V> entry : entrySet ) {
                assertTrue( map.containsKey( entry.getKey() ) );
                assertTrue( map.containsValue( entry.getValue() ) );
                V val = entry.getValue();
                int expectedHash = hash( entry.getKey() ) ^ ( val == null ? 0 : val.hashCode() );
                assertEquals( expectedHash, entry.hashCode() );
                expectedEntrySetHash += expectedHash;
            }
            assertEquals( expectedEntrySetHash, entrySet.hashCode() );
            assertTrue( entrySet.containsAll( new HashSet<LongMap.Entry<V>>( entrySet ) ) );
            assertTrue( entrySet.equals( new HashSet<Entry<V>>( entrySet ) ) );
        }

        Object[] entrySetToArray1 = entrySet.toArray();
        assertEquals( map.size(), entrySetToArray1.length );
        assertTrue( Arrays.asList( entrySetToArray1 ).containsAll( entrySet ) );

        Object[] valuesToArray1 = valueCollection.toArray();
        assertEquals( map.size(), valuesToArray1.length );
        assertTrue( Arrays.asList( valuesToArray1 ).containsAll( valueCollection ) );

        Object[] valuesToArray2 = new Object[map.size() + 2];
        valuesToArray2[map.size()] = "foo";
        assertSame( valuesToArray2, valueCollection.toArray( valuesToArray2 ) );
        assertNull( valuesToArray2[map.size()] );
        assertTrue( Arrays.asList( valuesToArray2 ).containsAll( valueCollection ) );

        if( supportsValuesHashCode ) {
            int expectedHash = 0;
            for( Entry<V> entry : entrySet ) {
                expectedHash += entry.hashCode();
            }
            assertEquals( expectedHash, map.hashCode() );
        }

        assertMoreInvariants( map );
    }

    /**
     * Override this to check invariants which should hold true for a particular
     * implementation, but which are not generally applicable to every instance
     * of Map.
     *
     * @param map the map whose additional invariants to check.
     */
    protected void assertMoreInvariants( LongMap<V> map ) {
    }

    public void testClear() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        if( supportsClear ) {
            map.clear();
            assertTrue( map.isEmpty() );
        } else {
            try {
                map.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testContainsKey() {
        final LongMap<V> map;
        final long unmappedKey;
        try {
            map = makePopulatedMap();
            unmappedKey = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertFalse( map.containsKey( unmappedKey ) );
        assertTrue( map.containsKey( map.keySet().iterator().next() ) );
        assertInvariants( map );
    }

    public void testContainsValue() {
        final LongMap<V> map;
        final V unmappedValue;
        try {
            map = makePopulatedMap();
            unmappedValue = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertFalse( map.containsValue( unmappedValue ) );
        assertTrue( map.containsValue( map.values().iterator().next() ) );
        assertInvariants( map );
    }

    public void testEntrySet() {
        final LongMap<V> map;
        final Set<Entry<V>> entrySet;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( map );

        entrySet = map.entrySet();
        final long unmappedKey;
        final V unmappedValue;
        try {
            unmappedKey = getKeyNotInPopulatedMap();
            unmappedValue = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        for( Entry<V> entry : entrySet ) {
            assertFalse( unmappedKey == entry.getKey() );
            assertFalse( unmappedValue.equals( entry.getValue() ) );
        }
    }

    public void testEntrySetForEmptyMap() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( map );
    }

    public void testEntrySetIteratorRemove() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Iterator<Entry<V>> iterator = entrySet.iterator();
        if( supportsIteratorRemove ) {
            int initialSize = map.size();
            Entry<V> entry = iterator.next();
            iterator.remove();
            assertEquals( initialSize - 1, map.size() );
            assertFalse( entrySet.contains( entry ) );
            assertInvariants( map );
            try {
                iterator.remove();
                fail( "Expected IllegalStateException." );
            } catch( IllegalStateException e ) {
                // Expected.
            }
        } else {
            try {
                iterator.next();
                iterator.remove();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetRemove() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        if( supportsRemove ) {
            int initialSize = map.size();
            boolean didRemove = entrySet.remove( entrySet.iterator().next() );
            assertTrue( didRemove );
            assertEquals( initialSize - 1, map.size() );
        } else {
            try {
                entrySet.remove( entrySet.iterator().next() );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetRemoveMissingKey() {
        final LongMap<V> map;
        final long key;
        try {
            map = makeEitherMap();
            key = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Entry<V> entry
                = mapEntry( key, getValueNotInPopulatedMap() );
        int initialSize = map.size();
        if( supportsRemove ) {
            boolean didRemove = entrySet.remove( entry );
            assertFalse( didRemove );
        } else {
            try {
                boolean didRemove = entrySet.remove( entry );
                assertFalse( didRemove );
            } catch( UnsupportedOperationException optional ) {}
        }
        assertEquals( initialSize, map.size() );
        assertFalse( map.containsKey( key ) );
        assertInvariants( map );
    }

    public void testEntrySetRemoveDifferentValue() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        long key = map.keySet().iterator().next();
        Entry<V> entry
                = mapEntry( key, getValueNotInPopulatedMap() );
        int initialSize = map.size();
        if( supportsRemove ) {
            boolean didRemove = entrySet.remove( entry );
            assertFalse( didRemove );
        } else {
            try {
                boolean didRemove = entrySet.remove( entry );
                assertFalse( didRemove );
            } catch( UnsupportedOperationException optional ) {
            }
        }
        assertEquals( initialSize, map.size() );
        assertTrue( map.containsKey( key ) );
        assertInvariants( map );
    }

    public void testEntrySetRemoveAll() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Set<Entry<V>> entriesToRemove =
                singleton( entrySet.iterator().next() );
        if( supportsRemove ) {
            int initialSize = map.size();
            boolean didRemove = entrySet.removeAll( entriesToRemove );
            assertTrue( didRemove );
            assertEquals( initialSize - entriesToRemove.size(), map.size() );
            for( Entry<V> entry : entriesToRemove ) {
                assertFalse( entrySet.contains( entry ) );
            }
        } else {
            try {
                entrySet.removeAll( entriesToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetRemoveAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        if( supportsRemove ) {
            try {
                entrySet.removeAll( null );
                fail( "Expected NullPointerException." );
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                entrySet.removeAll( null );
                fail( "Expected UnsupportedOperationException or NullPointerException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetRetainAll() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Set<Entry<V>> entriesToRetain =
                singleton( entrySet.iterator().next() );
        if( supportsRemove ) {
            boolean shouldRemove = (entrySet.size() > entriesToRetain.size());
            boolean didRemove = entrySet.retainAll( entriesToRetain );
            assertEquals( shouldRemove, didRemove );
            assertEquals( entriesToRetain.size(), map.size() );
            for( Entry<V> entry : entriesToRetain ) {
                assertTrue( entrySet.contains( entry ) );
            }
        } else {
            try {
                entrySet.retainAll( entriesToRetain );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetRetainAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        if( supportsRemove ) {
            try {
                entrySet.retainAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                entrySet.retainAll( null );
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetClear() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        if( supportsClear ) {
            entrySet.clear();
            assertTrue( entrySet.isEmpty() );
        } else {
            try {
                entrySet.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testEntrySetAddAndAddAll() {
        final LongMap<V> map = makeEitherMap();

        Set<Entry<V>> entrySet = map.entrySet();
        final Entry<V> entryToAdd = mapEntry( 0, null );
        try {
            entrySet.add( entryToAdd );
            fail( "Expected UnsupportedOperationException or NullPointerException." );
        } catch( UnsupportedOperationException e ) {
            // Expected.
        } catch( NullPointerException e ) {
            // Expected.
        }
        assertInvariants( map );

        try {
            entrySet.addAll( singleton( entryToAdd ) );
            fail( "Expected UnsupportedOperationException or NullPointerException." );
        } catch( UnsupportedOperationException e ) {
            // Expected.
        } catch( NullPointerException e ) {
            // Expected.
        }
        assertInvariants( map );
    }

    public void testEntrySetSetValue() {
        // TODO: Investigate the extent to which, in practice, maps that support
        // put() also support Entry.setValue().
        if( !supportsPut ) {
            return;
        }

        final LongMap<V> map;
        final V valueToSet;
        try {
            map = makePopulatedMap();
            valueToSet = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Entry<V> entry = entrySet.iterator().next();
        final V oldValue = entry.getValue();
        final V returnedValue = entry.setValue( valueToSet );
        assertEquals( oldValue, returnedValue );
        assertTrue( entrySet.contains(
                mapEntry( entry.getKey(), valueToSet ) ) );
        assertEquals( valueToSet, map.get( entry.getKey() ) );
        assertInvariants( map );
    }

    public void testEntrySetSetValueSameValue() {
        // TODO: Investigate the extent to which, in practice, maps that support
        // put() also support Entry.setValue().
        if( !supportsPut ) {
            return;
        }

        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Set<Entry<V>> entrySet = map.entrySet();
        Entry<V> entry = entrySet.iterator().next();
        final V oldValue = entry.getValue();
        final V returnedValue = entry.setValue( oldValue );
        assertEquals( oldValue, returnedValue );
        assertTrue( entrySet.contains(
                mapEntry( entry.getKey(), oldValue ) ) );
        assertEquals( oldValue, map.get( entry.getKey() ) );
        assertInvariants( map );
    }

    public void testEqualsForEqualMap() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertEquals( map, map );
        assertEquals( makePopulatedMap(), map );
        assertFalse( map.equals( Collections.emptyMap() ) );
        //no-inspection ObjectEqualsNull
        assertFalse( map.equals( null ) );
    }

    public void testEqualsForLargerMap() {
        if( !supportsPut ) {
            return;
        }

        final LongMap<V> map;
        final LongMap<V> largerMap;
        try {
            map = makePopulatedMap();
            largerMap = makePopulatedMap();
            largerMap.put( getKeyNotInPopulatedMap(), getValueNotInPopulatedMap() );
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( map.equals( largerMap ) );
    }

    public void testEqualsForSmallerMap() {
        if( !supportsRemove ) {
            return;
        }

        final LongMap<V> map;
        final LongMap<V> smallerMap;
        try {
            map = makePopulatedMap();
            smallerMap = makePopulatedMap();
            smallerMap.remove( smallerMap.keySet().iterator().next() );
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( map.equals( smallerMap ) );
    }

    public void testEqualsForEmptyMap() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertEquals( map, map );
        assertEquals( makeEmptyMap(), map );
        assertFalse( map.equals( Collections.emptySet() ) );
        //noinspection ObjectEqualsNull
        assertFalse( map.equals( null ) );
    }

    public void testGet() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        for( Entry<V> entry : map.entrySet() ) {
            assertEquals( entry.getValue(), map.get( entry.getKey() ) );
        }

        long unmappedKey = 0;
        try {
            unmappedKey = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertNull( map.get( unmappedKey ) );
    }

    public void testGetForEmptyMap() {
        final LongMap<V> map;
        long unmappedKey = 0;
        try {
            map = makeEmptyMap();
            unmappedKey = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertNull( map.get( unmappedKey ) );
    }

    public void testHashCode() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( map );
    }

    public void testHashCodeForEmptyMap() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( map );
    }

    public void testPutNewKey() {
        final LongMap<V> map = makeEitherMap();
        final long keyToPut;
        final V valueToPut;
        try {
            keyToPut = getKeyNotInPopulatedMap();
            valueToPut = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        if( supportsPut ) {
            int initialSize = map.size();
            V oldValue = map.put( keyToPut, valueToPut );
            assertEquals( valueToPut, map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( valueToPut ) );
            assertEquals( initialSize + 1, map.size() );
            assertNull( oldValue );
        } else {
            try {
                map.put( keyToPut, valueToPut );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testPutExistingKey() {
        final LongMap<V> map;
        final long keyToPut;
        final V valueToPut;
        try {
            map = makePopulatedMap();
            valueToPut = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        keyToPut = map.keySet().iterator().next();
        if( supportsPut ) {
            int initialSize = map.size();
            map.put( keyToPut, valueToPut );
            assertEquals( valueToPut, map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( valueToPut ) );
            assertEquals( initialSize, map.size() );
        } else {
            try {
                map.put( keyToPut, valueToPut );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testPutNullValue() {
        if( !supportsPut ) {
            return;
        }
        final LongMap<V> map = makeEitherMap();
        final long keyToPut;
        try {
            keyToPut = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        if( allowsNullValues ) {
            int initialSize = map.size();
            final V oldValue = map.get( keyToPut );
            final V returnedValue = map.put( keyToPut, null );
            assertEquals( oldValue, returnedValue );
            assertNull( map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( null ) );
            assertEquals( initialSize + 1, map.size() );
        } else {
            try {
                map.put( keyToPut, null );
                fail( "Expected RuntimeException" );
            } catch( RuntimeException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testPutNullValueForExistingKey() {
        if( !supportsPut ) {
            return;
        }
        final LongMap<V> map;
        final long keyToPut;
        try {
            map = makePopulatedMap();
            keyToPut = map.keySet().iterator().next();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        if( allowsNullValues ) {
            int initialSize = map.size();
            final V oldValue = map.get( keyToPut );
            final V returnedValue = map.put( keyToPut, null );
            assertEquals( oldValue, returnedValue );
            assertNull( map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( null ) );
            assertEquals( initialSize, map.size() );
        } else {
            try {
                map.put( keyToPut, null );
                fail( "Expected RuntimeException" );
            } catch( RuntimeException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testPutAllNewKey() {
        final LongMap<V> map = makeEitherMap();
        final long keyToPut;
        final V valueToPut;
        try {
            keyToPut = getKeyNotInPopulatedMap();
            valueToPut = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        final LongMap<V> mapToPut = new LongHashMap<V>();
        mapToPut.put( keyToPut, valueToPut );

        if( supportsPut ) {
            int initialSize = map.size();
            map.putAll( mapToPut );
            assertEquals( valueToPut, map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( valueToPut ) );
            assertEquals( initialSize + 1, map.size() );
        } else {
            try {
                map.putAll( mapToPut );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testPutAllExistingKey() {
        final LongMap<V> map;
        final long keyToPut;
        final V valueToPut;
        try {
            map = makePopulatedMap();
            valueToPut = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        keyToPut = map.keySet().iterator().next();
        final LongMap<V> mapToPut = new LongHashMap<V>();
        mapToPut.put( keyToPut, valueToPut );
        int initialSize = map.size();
        if( supportsPut ) {
            map.putAll( mapToPut );
            assertEquals( valueToPut, map.get( keyToPut ) );
            assertTrue( map.containsKey( keyToPut ) );
            assertTrue( map.containsValue( valueToPut ) );
        } else {
            try {
                map.putAll( mapToPut );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertEquals( initialSize, map.size() );
        assertInvariants( map );
    }

    public void testRemove() {
        final LongMap<V> map;
        final long keyToRemove;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        keyToRemove = map.keySet().iterator().next();
        if( supportsRemove ) {
            int initialSize = map.size();
            V expectedValue = map.get( keyToRemove );
            V oldValue = map.remove( keyToRemove );
            assertEquals( expectedValue, oldValue );
            assertFalse( map.containsKey( keyToRemove ) );
            assertEquals( initialSize - 1, map.size() );
        } else {
            try {
                map.remove( keyToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testRemoveMissingKey() {
        final LongMap<V> map;
        final long keyToRemove;
        try {
            map = makePopulatedMap();
            keyToRemove = getKeyNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        if( supportsRemove ) {
            int initialSize = map.size();
            assertNull( map.remove( keyToRemove ) );
            assertEquals( initialSize, map.size() );
        } else {
            try {
                map.remove( keyToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testSize() {
        assertInvariants( makeEitherMap() );
    }

    public void testKeySetClear() {
        final LongMap<V> map;
        try {
            map = makeEitherMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        LongSet keySet = map.keySet();
        if( supportsClear ) {
            keySet.clear();
            assertTrue( keySet.isEmpty() );
        } else {
            try {
                keySet.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testKeySetRemoveAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        LongSet keySet = map.keySet();
        if( supportsRemove ) {
            try {
                keySet.removeAll( null );
                fail( "Expected NullPointerException." );
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                keySet.removeAll( null );
                fail( "Expected UnsupportedOperationException or NullPointerException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testKeySetRetainAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        LongSet keySet = map.keySet();
        if( supportsRemove ) {
            try {
                keySet.retainAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                keySet.retainAll( null );
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValues() {
        final LongMap<V> map;
        final Collection<V> valueCollection;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( map );

        valueCollection = map.values();
        final V unmappedValue;
        try {
            unmappedValue = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        for( V value : valueCollection ) {
            assertFalse( unmappedValue.equals( value ) );
        }
    }

    public void testValuesIteratorRemove() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        Iterator<V> iterator = valueCollection.iterator();
        if( supportsIteratorRemove ) {
            int initialSize = map.size();
            iterator.next();
            iterator.remove();
            assertEquals( initialSize - 1, map.size() );
            // (We can't assert that the values collection no longer contains the
            // removed value, because the underlying map can have multiple mappings
            // to the same value.)
            assertInvariants( map );
            try {
                iterator.remove();
                fail( "Expected IllegalStateException." );
            } catch( IllegalStateException e ) {
                // Expected.
            }
        } else {
            try {
                iterator.next();
                iterator.remove();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesRemove() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        if( supportsRemove ) {
            int initialSize = map.size();
            valueCollection.remove( valueCollection.iterator().next() );
            assertEquals( initialSize - 1, map.size() );
            // (We can't assert that the values collection no longer contains the
            // removed value, because the underlying map can have multiple mappings
            // to the same value.)
        } else {
            try {
                valueCollection.remove( valueCollection.iterator().next() );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesRemoveMissing() {
        final LongMap<V> map;
        final V valueToRemove;
        try {
            map = makeEitherMap();
            valueToRemove = getValueNotInPopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        int initialSize = map.size();
        if( supportsRemove ) {
            assertFalse( valueCollection.remove( valueToRemove ) );
        } else {
            try {
                assertFalse( valueCollection.remove( valueToRemove ) );
            } catch( UnsupportedOperationException e ) {
                // Tolerated.
            }
        }
        assertEquals( initialSize, map.size() );
        assertInvariants( map );
    }

    public void testValuesRemoveAll() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        Set<V> valuesToRemove = singleton( valueCollection.iterator().next() );
        if( supportsRemove ) {
            valueCollection.removeAll( valuesToRemove );
            for( V value : valuesToRemove ) {
                assertFalse( valueCollection.contains( value ) );
            }
            for( V value : valueCollection ) {
                assertFalse( valuesToRemove.contains( value ) );
            }
        } else {
            try {
                valueCollection.removeAll( valuesToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesRemoveAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> values = map.values();
        if( supportsRemove ) {
            try {
                values.removeAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                values.removeAll( null );
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesRetainAll() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        Set<V> valuesToRetain = singleton( valueCollection.iterator().next() );
        if( supportsRemove ) {
            valueCollection.retainAll( valuesToRetain );
            for( V value : valuesToRetain ) {
                assertTrue( valueCollection.contains( value ) );
            }
            for( V value : valueCollection ) {
                assertTrue( valuesToRetain.contains( value ) );
            }
        } else {
            try {
                valueCollection.retainAll( valuesToRetain );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesRetainAllNullFromEmpty() {
        final LongMap<V> map;
        try {
            map = makeEmptyMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> values = map.values();
        if( supportsRemove ) {
            try {
                values.retainAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                values.retainAll( null );
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    public void testValuesClear() {
        final LongMap<V> map;
        try {
            map = makePopulatedMap();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        Collection<V> valueCollection = map.values();
        if( supportsClear ) {
            valueCollection.clear();
            assertTrue( valueCollection.isEmpty() );
        } else {
            try {
                valueCollection.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( map );
    }

    private static final class TestEntry<V> implements LongMap.Entry<V> {

        final long mKey;
        V mValue;


        TestEntry( long key, V value ) {
            mKey   = key;
            mValue = value;
        }


        public long getKey() {
            return mKey;
        }


        public V getValue() {
            return mValue;
        }


        public V setValue( V v ) {
            V ret = mValue;
            mValue = v;
            return ret;
        }

        @Override
        public int hashCode() {
            return (int)(mKey) ^ (mValue == null ? 0 : mValue.hashCode());
        }

        @Override
        public boolean equals( Object object ) {
            if( this == object ) {
                return true;
            }

            if( !( object instanceof Entry ) ) {
                return false;
            }

            Entry entry = (Entry)object;
            if( mKey != entry.getKey() ) {
                return false;
            }
            Object val = entry.getValue();
            return mValue == val || mValue != null && mValue.equals( val );
        }

        @Override
        public String toString() {
            return mKey + "=" + mValue;
        }

    }


}
