/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.collect;

import junit.framework.TestCase;

import java.util.*;
import static java.util.Collections.singleton;

/**
 * Tests representing the contract of {@link java.util.Set}. Concrete subclasses of this
 * base class test conformance of concrete {@link java.util.Map} subclasses to that
 * contract.
 *
 * TODO: Descriptive assertion messages, with hints as to probable
 * fixes.
 * TODO: Add another constructor parameter indicating whether the
 * class under test is ordered, and check the order if so.
 * TODO: Refactor to share code with SetTestBuilder &c.
 *
  * @param <V> the type of mapped values used the maps under test
 *
 * @author George van den Driessche
 */
public abstract class SetInterfaceTest<V> extends TestCase {

    protected final boolean supportsAdd;
    protected final boolean supportsRemove;
    protected final boolean supportsClear;
    protected final boolean allowsNullItems;
    protected final boolean supportsIteratorRemove;

    /**
     * Creates a new, empty instance of the class under test.
     *
     * @return a new, empty map instance.
     * @throws UnsupportedOperationException if it's not possible to make an
     * empty instance of the class under test.
     */
    protected abstract Set<V> makeEmptySet() throws UnsupportedOperationException;

    /**
     * Creates a new, non-empty instance of the class under test.
     *
     * @return a new, non-empty map instance.
     * @throws UnsupportedOperationException if it's not possible to make a
     * non-empty instance of the class under test.
     */
    protected abstract Set<V> makePopulatedSet() throws UnsupportedOperationException;

    /**
     * Creates a new item that is not expected to be found
     * in {@link #makePopulatedSet()}.
     *
     * @return a key.
     * @throws UnsupportedOperationException if it's not possible to make a key
     * that will not be found in the map.
     */
    protected abstract V getItemNotInPopulatedMap() throws UnsupportedOperationException;

    /**
     * Constructor that assigns {@code supportsIteratorRemove} the same value as
     * {@code supportsRemove}.
     */
    protected SetInterfaceTest(
            boolean allowsNullItems,
            boolean supportsAdd,
            boolean supportsRemove,
            boolean supportsClear ) {
        this( allowsNullItems, supportsAdd, supportsRemove,
             supportsClear, supportsRemove );
    }

    /**
     * Constructor with an explicit {@code supportsIteratorRemove} parameter.
     */
    protected SetInterfaceTest(
            boolean allowsNullKeys,
            boolean supportsPut,
            boolean supportsRemove,
            boolean supportsClear,
            boolean supportsIteratorRemove ) {
        this.supportsAdd = supportsPut;
        this.supportsRemove = supportsRemove;
        this.supportsClear = supportsClear;
        this.allowsNullItems = allowsNullKeys;
        this.supportsIteratorRemove = supportsIteratorRemove;
    }

    /**
     * Used by tests that require a map, but don't care whether it's
     * populated or not.
     *
     * @return a new map instance.
     */
    protected Set<V> makeEitherSet() {
        try {
            return makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return makeEmptySet();
        }
    }


    protected final boolean supportsValuesHashCode( Set<V> set) {
        // get the first non-null value
        for (V value : set) {
            if (value != null) {
                try {
                    value.hashCode();
                } catch (Exception e) {
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
     * @see #assertMoreInvariants
     * @param set the Set to check.
     */
    protected final void assertInvariants( Set<V> set ) {
        assertEquals( set.size() == 0, set.isEmpty() );

        int expectedHash = 0;
        for( V item: set ) {
            expectedHash += item != null ? item.hashCode() : 0;
            assertTrue( set.contains( item ) );
            assertTrue( allowsNullItems || (item != null ) );
        }

        assertEquals( expectedHash, set.hashCode() );

        Object[] setToArray1 = set.toArray();
        assertEquals( set.size(), setToArray1.length );
        assertTrue( Arrays.asList( setToArray1 ).containsAll( set ) );

        Object[] setToArray2 = new Object[ set.size() + 2];
        assertSame( setToArray2, set.toArray( setToArray2 ) );
        assertNull( setToArray2[set.size()] );
        assertTrue( Arrays.asList( setToArray2 ).containsAll( set ) );

        assertMoreInvariants( set );
    }

    /**
     * Override this to check invariants which should hold true for a particular
     * implementation, but which are not generally applicable to every instance
     * of Map.
     *
     * @param set the map whose additional invariants to check.
     */
    protected void assertMoreInvariants( Set<V> set ) {}


    public void testClear() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        if (supportsClear) {
            set.clear();
            assertTrue( set.isEmpty() );
        } else {
            try {
                set.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }


    public void testContains() {
        final Set<V> set;
        final V missingItem;
        try {
            set = makePopulatedSet();
            missingItem = getItemNotInPopulatedMap();
        } catch ( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( set.contains( missingItem ) );
        assertTrue( set.contains( set.iterator().next() ) );

        if ( allowsNullItems ) {
            set.contains( null );
        } else {
            try {
                set.contains( null );
            } catch (NullPointerException optional) {}
        }
        assertInvariants(set);
    }


    public void testRemove() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        if( supportsRemove ) {
            int initialSize = set.size();
            boolean didRemove = set.remove( set.iterator().next() );
            assertTrue( didRemove );
            assertEquals( initialSize - 1, set.size() );
        } else {
            try {
                set.remove( set.iterator().next() );
                fail( "Expected UnsupportedOperationException." );
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }


    public void testRemoveMissing() {
        final Set<V> set;
        final V item;
        try {
            set  = makeEitherSet();
            item = getItemNotInPopulatedMap();
        } catch (UnsupportedOperationException e) {
            return;
        }

        int initialSize = set.size();
        if( supportsRemove ) {
            boolean didRemove = set.remove( item );
            assertFalse( didRemove );
        } else {
            try {
                boolean didRemove = set.remove( item );
                assertFalse( didRemove );
            } catch( UnsupportedOperationException optional ) {}
        }

        assertEquals( initialSize, set.size() );
        assertFalse( set.contains( item ) );
        assertInvariants(set);
    }


    public void testNullItem() {
        if (!allowsNullItems || !supportsAdd || !supportsRemove) {
            return;
        }

        final Set<V> set;
        try {
            set = makeEitherSet();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertInvariants( set );

        set.add( null );
        assertTrue( set.contains( null ) );
        assertInvariants( set );

        set.remove( null );
        assertFalse( set.contains( null ) );
        assertInvariants( set );
    }


    public void testRemoveAll() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        Set<V> entriesToRemove = singleton( set.iterator().next() );

        if (supportsRemove) {
            int initialSize = set.size();
            boolean didRemove = set.removeAll( entriesToRemove );
            assertTrue( didRemove );
            assertEquals(initialSize - entriesToRemove.size(), set.size());
            for ( V item : entriesToRemove) {
                assertFalse( set.contains( item ) );
            }
        } else {
            try {
                set.removeAll( entriesToRemove );
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }


    public void testRemoveAllNullFromEmpty() {
        final Set<V> set;
        try {
            set = makeEmptySet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        if (supportsRemove) {
            try {
                set.removeAll( null );
                fail("Expected NullPointerException.");
            } catch (NullPointerException e) {
                // Expected.
            }
        } else {
            try {
                set.removeAll( null );
                fail("Expected UnsupportedOperationException or NullPointerException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            } catch (NullPointerException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }


    public void testRetainAll() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        Set<V> entriesToRetain = singleton( set.iterator().next());
        if (supportsRemove) {
            boolean shouldRemove = (set.size() > entriesToRetain.size());
            boolean didRemove = set.retainAll(entriesToRetain);
            assertEquals(shouldRemove, didRemove);
            assertEquals( entriesToRetain.size(), set.size() );
            for( V entry : entriesToRetain) {
                assertTrue( set.contains( entry ) );
            }
        } else {
            try {
                set.retainAll(entriesToRetain);
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants( set );
    }


    public void testRetainAllNullFromEmpty() {
        final Set<V> set;
        try {
            set = makeEmptySet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        if (supportsRemove) {
            try {
                set.retainAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch (NullPointerException e) {
                // Expected.
            }
        } else {
            try {
                set.retainAll(null);
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch (UnsupportedOperationException e) {
                // Expected.
            } catch (NullPointerException e) {
                // Expected.
            }
        }
        assertInvariants( set );
    }


    public void testEqualsForEqualSet() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        assertEquals(set, set);
        assertEquals( makePopulatedSet(), set );
        assertFalse( set.equals( Collections.emptySet() ) );
        //no-inspection ObjectEqualsNull
        assertFalse( set.equals( null ) );
    }


    public void testEqualsForLargerSet() {
        if (!supportsAdd ) {
            return;
        }

        final Set<V> set;
        final Set<V> largerSet;
        try {
            set = makePopulatedSet();
            largerSet = makePopulatedSet();
            largerSet.add( getItemNotInPopulatedMap() );
        } catch (UnsupportedOperationException e) {
            return;
        }

        assertFalse( set.equals( largerSet) );
    }


    public void testEqualsForSmallerSet() {
        if (!supportsRemove) {
            return;
        }

        final Set<V> set;
        final Set<V> smallerSet;
        try {
            set = makePopulatedSet();
            smallerSet = makePopulatedSet();
            smallerSet.remove( smallerSet.iterator().next() );
        } catch ( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( set.equals( smallerSet ) );
    }


    public void testEqualsForEmptySet() {
        final Set<V> set;
        try {
            set = makeEmptySet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        assertEquals(set, set);
        assertEquals( makeEmptySet(), set);
        assertEquals( Collections.emptyMap(), set );
        assertFalse( set.equals( Collections.emptySet() ) );
        //noinspection ObjectEqualsNull
        assertFalse( set.equals( null ) );
    }


    public void testContainsForEmptySet() {
        final Set<V> set;
        V unmappedKey = null;
        try {
            set = makeEmptySet();
            unmappedKey = getItemNotInPopulatedMap();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertFalse( set.contains( unmappedKey ) );
    }


    public void testHashCode() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertInvariants(set);
    }


    public void testHashCodeForEmptySet() {
        final Set<V> set;
        try {
            set = makeEmptySet();
        } catch (UnsupportedOperationException e) {
            return;
        }
        assertInvariants(set);
    }


    public void testAddNewItem() {
        final Set<V> set = makeEitherSet();
        final V itemToAdd;
        try {
            itemToAdd = getItemNotInPopulatedMap();
        } catch (UnsupportedOperationException e) {
            return;
        }
        if ( supportsAdd ) {
            int initialSize = set.size();
            assertFalse( set.contains( itemToAdd ) );
            assertTrue( set.add( itemToAdd ) );
            assertTrue( set.contains( itemToAdd ) );
            assertEquals( initialSize + 1, set.size() );
        } else {
            try {
                set.add( itemToAdd );
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }


    public void testAddExistingItem() {
        final Set<V> set;
        final V itemToAdd;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        itemToAdd = set.iterator().next();

        if ( supportsAdd ) {
            int initialSize = set.size();
            assertTrue( set.contains( itemToAdd ) );
            assertFalse( set.add( itemToAdd ) );
            assertTrue( set.contains( itemToAdd ) );
            assertEquals( initialSize, set.size() );
        } else {
            try {
                set.add( itemToAdd );
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants( set );
    }


    public void testAddAllNew() {
        final Set<V> set = makeEitherSet();
        final V valueToPut;
        try {
            valueToPut = getItemNotInPopulatedMap();
        } catch (UnsupportedOperationException e) {
            return;
        }
        final Set<V> mapToPut = Collections.singleton( valueToPut );
        if ( supportsAdd ) {
            int initialSize = set.size();
            set.addAll( mapToPut );
            assertTrue( set.contains( valueToPut ) );
            assertEquals( initialSize + 1, set.size() );
        } else {
            try {
                set.addAll( mapToPut );
                fail("Expected UnsupportedOperationException.");
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }


    public void testAddAllExisting() {
        final Set<V> set;
        final V keyToPut;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        keyToPut = set.iterator().next();
        final Set<V> setToPut = Collections.singleton( keyToPut );
        int initialSize = set.size();
        if ( supportsAdd ) {
            assertFalse( set.addAll( setToPut ) );
            assertTrue( set.contains( keyToPut ) );
            assertEquals( initialSize, set.size() );
        } else {
            try {
                set.addAll( setToPut );
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertEquals( initialSize, set.size() );
        assertInvariants(set);
    }


    public void testSize() {
        assertInvariants( makeEitherSet());
    }


    public void testIteratorRemove() {
        final Set<V> set;
        try {
            set = makePopulatedSet();
        } catch (UnsupportedOperationException e) {
            return;
        }

        Iterator<V> iterator = set.iterator();
        if (supportsIteratorRemove) {
            int initialSize = set.size();
            iterator.next();
            iterator.remove();
            assertEquals( initialSize - 1, set.size() );
            // (We can't assert that the values collection no longer contains the
            // removed value, because the underlying map can have multiple mappings
            // to the same value.)
            assertInvariants(set);
            try {
                iterator.remove();
                fail("Expected IllegalStateException.");
            } catch (IllegalStateException e) {
                // Expected.
            }
        } else {
            try {
                iterator.next();
                iterator.remove();
                fail("Expected UnsupportedOperationException.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
        }
        assertInvariants(set);
    }

}
