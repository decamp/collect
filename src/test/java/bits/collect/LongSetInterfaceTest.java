/*
 * Copyright (c) 2014. Massachusetts Institute of Technology
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */
package bits.collect;

import junit.framework.TestCase;
import java.util.*;


/**
 * Originally tests for {@link java.util.Set}. Concrete subclasses of this
 * base class test conformance of concrete {@link java.util.Set} subclasses to that
 * contract.
 * <p/>
 * TODO: Descriptive assertion messages, with hints as to probable
 * fixes.
 * TODO: Add another constructor parameter indicating whether the
 * class under test is ordered, and check the order if so.
 * TODO: Refactor to share code with SetTestBuilder &c.
 *
  * @author George van den Driessche
 */
public abstract class LongSetInterfaceTest extends TestCase {

    protected final boolean supportsAdd;
    protected final boolean supportsRemove;
    protected final boolean supportsClear;
    protected final boolean allowsNullValues;
    protected final boolean supportsIteratorRemove;

    /**
     * Constructor that assigns {@code supportsIteratorRemove} the same value as
     * {@code supportsRemove}.
     */
    protected LongSetInterfaceTest(
            boolean allowsNullValues,
            boolean supportsPut,
            boolean supportsRemove,
            boolean supportsClear )
    {
        this( allowsNullValues, supportsPut, supportsRemove, supportsClear, supportsRemove );
    }

    /**
     * Constructor with an explicit {@code supportsIteratorRemove} parameter.
     */
    protected LongSetInterfaceTest(
            boolean allowsNullValues,
            boolean supportsPut,
            boolean supportsRemove,
            boolean supportsClear,
            boolean supportsIteratorRemove )
    {
        this.supportsAdd = supportsPut;
        this.supportsRemove = supportsRemove;
        this.supportsClear = supportsClear;
        this.allowsNullValues = allowsNullValues;
        this.supportsIteratorRemove = supportsIteratorRemove;
    }


    private static int hash( long v ) {
        return (int)v;
    }

    /**
     * Creates a new, empty instance of the class under test.
     *
     * @return a new, empty set instance.
     * @throws UnsupportedOperationException if it's not possible to make an empty instance of the class under test.
     */
    protected abstract LongSet makeEmptySet() throws UnsupportedOperationException;

    /**
     * Creates a new, non-empty instance of the class under test.
     *
     * @return a new, non-empty set instance.
     * @throws UnsupportedOperationException if it's not possible to make a
     *                                       non-empty instance of the class under test.
     */
    protected abstract LongSet makePopulatedSet() throws UnsupportedOperationException;

    /**
     * Creates a new item that is not expected to be found
     * in {@link #makePopulatedSet()}.
     *
     * @return a item.
     * @throws UnsupportedOperationException if it's not possible to make a item
     *                                       that will not be found in the set.
     */
    protected abstract long getItemNotInPopulatedSet() throws UnsupportedOperationException;

    /**
     * Used by tests that require a set, but don't care whether it's
     * populated or not.
     *
     * @return a new set instance.
     */
    protected LongSet makeEitherSet() {
        try {
            return makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return makeEmptySet();
        }
    }


    /**
     * Checks all the properties that should always hold of a set. Also calls
     * {@link #assertMoreInvariants} to check invariants that are peculiar to
     * specific implementations.
     *
     * @param set the set to check.
     * @see #assertMoreInvariants
     */
    protected final void assertInvariants( LongSet set ) {
        assertEquals( set.size() == 0, set.isEmpty() );
        int expectedHash = 0;
        for( LongIterator it = set.iterator(); it.hasNext(); ) {
            long item = it.next();
            expectedHash ^= (int)(item >> 32 | item);
            assertTrue( set.contains( item ) );
        }
        assertEquals( expectedHash, set.hashCode() );

        long[] arr = set.toArray();
        assertEquals( set.size(), arr.length );
        assertMoreInvariants( set );
    }

    /**
     * Override this to check invariants which should hold true for a particular
     * implementation, but which are not generally applicable to every instance
     * of Set.
     *
     * @param set the set whose additional invariants to check.
     */
    protected void assertMoreInvariants( LongSet set ) {}

    public void testClear() {
        final LongSet set;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        if( supportsClear ) {
            set.clear();
            assertTrue( set.isEmpty() );
        } else {
            try {
                set.clear();
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set);
    }

    public void testContains() {
        final LongSet set;
        final long unusedItem;
        try {
            set = makePopulatedSet();
            unusedItem = getItemNotInPopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertFalse( set.contains( unusedItem ) );
        assertTrue( set.contains( set.iterator().next() ) );
        assertInvariants( set );
    }

    public void testEqualsForEqualSet() {
        final LongSet set;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertEquals( set, set );
        assertEquals( makePopulatedSet(), set );
        assertFalse( set.equals( Collections.emptySet() ) );
        //no-inspection ObjectEqualsNull
        assertFalse( set.equals( null ) );
    }

    public void testEqualsForLargerSet() {
        if( !supportsAdd ) {
            return;
        }

        final LongSet set;
        final LongSet largerSet;
        try {
            set = makePopulatedSet();
            largerSet = makePopulatedSet();
            largerSet.add( getItemNotInPopulatedSet() );
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( set.equals( largerSet ) );
    }

    public void testEqualsForSmallerSet() {
        if( !supportsRemove ) {
            return;
        }

        final LongSet set;
        final LongSet smallerSet;
        try {
            set = makePopulatedSet();
            smallerSet = makePopulatedSet();
            smallerSet.remove( smallerSet.iterator().next() );
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertFalse( set.equals( smallerSet ) );
    }

    public void testEqualsForEmptySet() {
        final LongSet set;
        try {
            set = makeEmptySet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        assertEquals( set, set );
        assertEquals( makeEmptySet(), set );
        assertFalse( set.equals( Collections.emptySet() ) );
        //noinspection ObjectEqualsNull
        assertFalse( set.equals( null ) );
    }

    public void testContainsForEmptySet() {
        final LongSet set;
        long unusedItem = 0;
        try {
            set = makeEmptySet();
            unusedItem = getItemNotInPopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertFalse( set.contains( unusedItem ) );
    }

    public void testHashCode() {
        final LongSet set;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( set );
    }

    public void testHashCodeForEmptySet() {
        final LongSet set;
        try {
            set = makeEmptySet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        assertInvariants( set );
    }

    public void testAdd() {
        final LongSet set = makeEitherSet();
        final long itemToAdd;
        try {
            itemToAdd = getItemNotInPopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        if( supportsAdd ) {
            int initialSize = set.size();
            assertFalse( set.contains( itemToAdd ) );
            assertTrue( set.add( itemToAdd ) );
            assertTrue( set.contains( itemToAdd ) );
            assertEquals( initialSize + 1, set.size() );
        } else {
            try {
                set.add( itemToAdd );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testAddExisting() {
        final LongSet set;
        final long itemToAdd;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        itemToAdd = set.iterator().next();
        if( supportsAdd ) {
            int initialSize = set.size();
            assertFalse( set.add( itemToAdd ) );
            assertEquals( initialSize, set.size() );
        } else {
            try {
                set.add( itemToAdd );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testAddAllNew() {
        final LongSet set = makeEitherSet();
        final long itemToPut;

        try {
            itemToPut = getItemNotInPopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        final LongSet setToAdd = new LongHashSet();
        setToAdd.add( itemToPut );

        if( supportsAdd ) {
            int initialSize = set.size();
            set.addAll( setToAdd );
            assertTrue( set.contains( itemToPut ) );
            assertEquals( initialSize + 1, set.size() );
        } else {
            try {
                set.addAll( setToAdd );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testAddAllExisting() {
        final LongSet set;
        final long itemToAdd;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        itemToAdd = set.iterator().next();
        final LongSet setToAdd = new LongHashSet();
        setToAdd.add( itemToAdd );
        int initialSize = set.size();
        if( supportsAdd ) {
            set.addAll( setToAdd );
            assertTrue( set.contains( itemToAdd ) );
        } else {
            try {
                set.addAll( setToAdd );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertEquals( initialSize, set.size() );
        assertInvariants( set );
    }

    public void testRemove() {
        final LongSet set;
        final long itemToRemove;
        try {
            set = makePopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        itemToRemove = set.iterator().next();
        if( supportsRemove ) {
            int initialSize = set.size();
            assertTrue( set.remove( itemToRemove ) );
            assertEquals( initialSize - 1, set.size() );
        } else {
            try {
                set.remove( itemToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testRemoveMissingItem() {
        final LongSet set;
        final long itemToRemove;
        try {
            set = makePopulatedSet();
            itemToRemove = getItemNotInPopulatedSet();
        } catch( UnsupportedOperationException e ) {
            return;
        }
        if( supportsRemove ) {
            int initialSize = set.size();
            assertFalse( set.remove( itemToRemove ) );
            assertEquals( initialSize, set.size() );
        } else {
            try {
                set.remove( itemToRemove );
                fail( "Expected UnsupportedOperationException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testSize() {
        assertInvariants( makeEitherSet() );
    }

    public void testRemoveAllNullFromEmpty() {
        final LongSet set;
        try {
            set = makeEmptySet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        if( supportsRemove ) {
            try {
                set.removeAll( null );
                fail( "Expected NullPointerException." );
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                set.removeAll( null );
                fail( "Expected UnsupportedOperationException or NullPointerException." );
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }

    public void testRetainAllNullFromEmpty() {
        final LongSet set;
        try {
            set = makeEmptySet();
        } catch( UnsupportedOperationException e ) {
            return;
        }

        if( supportsRemove ) {
            try {
                set.retainAll( null );
                // Returning successfully is not ideal, but tolerated.
            } catch( NullPointerException e ) {
                // Expected.
            }
        } else {
            try {
                set.retainAll( null );
                // We have to tolerate a successful return (Sun bug 4802647)
            } catch( UnsupportedOperationException e ) {
                // Expected.
            } catch( NullPointerException e ) {
                // Expected.
            }
        }
        assertInvariants( set );
    }


}
