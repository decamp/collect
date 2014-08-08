/* 
 * Copyright (c) 2012, Massachusetts Institute of Technology
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
public class GroupMapTest {

    private static Comparator<Integer> ORDER = new Comparator<Integer>() {
        public int compare(Integer a, Integer b) {
            int aa = a.intValue();
            int bb = b.intValue();
            
            if(aa < bb)
                return -1;
            
            if(aa > bb)
                return 1;
            
            return 0;
        }
    };
    
    private final Random mRand = new Random(0);

    
    public GroupMapTest() {}
    

    
    @Test
    public void testOneGroup() {
        for(IntMap map: createMaps()) {
            testOneGroup(map);
        }
    }
    
    
    @Test
    public void testMultiGroup() {
        for(IntMap map: createMaps()) {
            testMultiGroup(map);
        }
    }


    @Test
    public void testIter() {
        for(IntMap map: createMaps()) {
            testIter(false, map);
            testIter(true, map);
        }
    }

    
    @Test
    public void testRandom() {
        for(IntMap set: createMaps()) {
            testRandom(set);
        }
    }

    
    @Test
    public void testViews() {
        for(IntMap map: createMaps()) {
            testViews(map);
        }
    }
    
    
    @Test
    public void testNavigation() {
        for(IntMap map: createMaps()) {
            testNavigation(false, map);
        }
        
        for(IntMap map: createMaps()) {
            testNavigation(true, map);
        }
    }
    

    
    
    public void testOneGroup(IntMap map) {
        Integer channel = 3;
        List<Integer> list = new ArrayList<Integer>(5);
        
        for(int i = 0; i < 5; i++) {
            map.put(channel, toKey(channel, i), i);
            list.add(i);
        }
        
        assertAllSequence(map, list);
        assertChanSequence(map, channel, list);
        
        Iterator<Integer> iter = map.values().iterator();
        iter = map.values().iterator();
        iter.next();
        iter.next();
        iter.next();
        iter.remove();
        list.remove(2);
        
        assertAllSequence(map, list);
        assertChanSequence(map, channel, list);
        
        
        iter = map.groupValues(channel).iterator();
        iter.next();
        iter.remove();
        list.remove(0);
        
        assertAllSequence(map, list);
        assertChanSequence(map, channel, list);
        
        map.clear();
        list.clear();
        
        assertAllSequence(map, list);
        assertChanSequence(map, channel, list);
    }
    
    
    public void testMultiGroup(IntMap set) {
        Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
        List<Integer> allList = new ArrayList<Integer>(15);
                
        for(int c = 0; c < 3; c++) {
            Integer cc = c;
            List<Integer> list = new ArrayList<Integer>(5);
            map.put(cc, list);
            
            for(int i = 0; i < 5; i++) {
                long key = ((cc.longValue() << 32) | i);
                set.put(cc, key, i);
                list.add(i);
                
                if(c == 0) {
                    allList.add(i);
                    allList.add(i);
                    allList.add(i);
                }
            }
        }
        
        assertAllSequence(set, allList);
        assertChanSequence(set, 0, map.get(0));
        assertChanSequence(set, 1, map.get(1));
        assertChanSequence(set, 2, map.get(2));
        
        Integer channel = 1;
        List<Integer> list = map.get(channel);
        
        for(int i = 0; i < 5; i++) {
            int idx = mRand.nextInt(list.size());
            list.remove(idx);
            allList.remove((4 - i) * 3);
            remove(set.groupValues(channel).iterator(), idx);
            assertChanSequence(set, channel, list);
        }
        
        assertAllSequence(set, allList);
        
        channel = 2;
        list = map.get(channel);
        list.clear();
        set.clear(channel);
        
        for(int i = 0; i < 5; i++) {
            allList.remove((4 - i) * 2);
        }
        
        assertAllSequence(set, allList);
        assertChanSequence(set, 0, map.get(0));
        assertChanSequence(set, 1, map.get(1));
        assertChanSequence(set, 2, map.get(2));

    }
    

    @SuppressWarnings( "rawtypes" )
    public void testRandom(IntMap map) {
        ReferenceIntMap ref = new ReferenceIntMap();
        
        for(int i = 0; i < 100000; i++) {
            switch(mRand.nextInt(3)) {
            case 0:
            {
                Integer c = mRand.nextInt(10);
                Integer v = mRand.nextInt(100);
                Long k = toKey(c, v);
                
                map.put(c, k, v);
                ref.put(c, k, v);
                break;
            }
            
            case 1:
            {
                int c = mRand.nextInt(10);
                int v = mRand.nextInt(100);
                Long k = toKey(c, v);
                
                map.remove(k);
                ref.remove(k);
                break;
            }
            
            case 2:
            {
                if(mRand.nextInt(5) == 0) {
                    int c = mRand.nextInt(10);
                    map.clear(c);
                    ref.clear(c);
                }
                
                break;
            }}
        }
        

        assertEquals("Size mismatch", ref.size(), map.size());
        
        Iterator iter1 = ref.iterator();
        Iterator iter2 = map.values().iterator();
        
        while(iter1.hasNext()) {
            assertEquals("Node mismatch", iter1.next(), iter2.next());
        }
    }
    
    
    public void testIter(boolean useChannel, IntMap map) {
        int[] start = new int[]{-1, -1, 3, 3, 4, 20};
        int[] stop = new int[]{-1, 4, 3, 4, 20, 22};
        
        for(int i = 0; i < start.length; i++) {
            
            final int startPoint = start[i];
            final int stopPoint = stop[i];
            
            Comparable<Integer> comp = new Comparable<Integer>() {
                public int compareTo(Integer value) {
                    int v = value.intValue();
                    
                    if(v < startPoint)
                        return 1;
                    
                    if(v >= stopPoint)
                        return -1;
                    
                    return 0;
                }
            };

            
            map.clear();
            List<Integer> list = new ArrayList<Integer>();
            
            if(useChannel) {
                populate(map, 10000, 10, 10);
                
                for(int j = 0; j < 20; j++) {
                    map.put(30, toKey(30, j), j);
                    if(comp.compareTo(j) == 0)
                        list.add(j);
                }
                
                assertIter(map.groupValues(30, comp).iterator(), list);
                assertDescendingIter(map.descendingGroupValues(30, comp).iterator(), list);
                
            }else{
                populate(map, 10000, 0, 1000);
                
                for(int j = 0; j < 20; j++) {
                    map.put(30, toKey(30, j), j);
                    if(comp.compareTo(j) == 0)
                        list.add(j);
                }
                
                assertIter(map.groupValues(30, comp).iterator(), list);
                assertDescendingIter(map.descendingGroupValues(30, comp).iterator(), list);
            }
        }
    }

    
    public void testViews(IntMap map) {
        populate(map, 10000, 10, 10);
        
        doViewTest(map);

        Iterator<?> iter = map.values().iterator();
        iterate(iter, 5);
        iter.remove();
        iterate(iter, 5);
        iter.remove();
        
        doViewTest(map);
        
        iter = map.keySet().iterator();
        iterate(iter, 100);
        iter.remove();
        iterate(iter, 5);
        iter.remove();
        
        doViewTest(map);
        
        iter = map.entrySet().iterator();
        iterate(iter, 25);
        iter.remove();
        iterate(iter, 5);
        iter.remove();
        
        doViewTest(map);
        doGroupViewTest(map);
    }
    
    
    public void testNavigation(boolean useGroup, IntMap map) {
        Integer g = 3;
        
        if(useGroup) {
            populate(map, 10000, 10, 100);
            map.clear(g);
        }   
        
        for(int i = 0; i <= 100; i += 2) {
            map.put(g, toKey(g, i), i); 
        }
        
        if(useGroup) {
            assertEquals("First error", 0, map.firstValue(g));
            assertEquals("First error", 0, map.firstEntry(g).getValue());
            assertEquals("Last error", 100, map.lastValue(g));
            assertEquals("Last error", 100, map.lastEntry(g).getValue());
            
            for(int i = 0; i <= 100; i += 2) {
                assertTrue("Contains error", map.containsValue(i));
                assertTrue("Contains error", map.containsValue(g, i));
                assertEquals("Floor error", i, map.floorValue(g, i));
                assertEquals("Floor error", i, map.floorEntry(g, i).getValue());
                assertEquals("Ceiling error", i, map.ceilingValue(g, i));
                assertEquals("Ceiling error", i, map.ceilingEntry(g, i).getValue());

                if(i == 0) {
                    assertEquals("Lower error", null, map.lowerValue(g, i));
                    assertEquals("Lower error", null, map.lowerEntry(g, i));
                }else{
                    assertEquals("Lower error", (i - 2), map.lowerValue(g, i));
                    assertEquals("Lower error", (i - 2), map.lowerEntry(g, i).getValue());
                }

                if(i == 100) {
                    assertEquals("Higher error", null, map.higherValue(g, i));
                    assertEquals("Higher error", null, map.higherEntry(g, i));
                }else{
                    assertEquals("Higher error", (i + 2), map.higherValue(g, i));
                    assertEquals("Higher error", (i + 2), map.higherEntry(g, i).getValue());
                }
            }

            for(int i = -1; i <= 101; i += 2) {
                assertFalse("Contains error", map.containsValue(g, i));

                if(i < 0) {
                    assertEquals("Floor error", null, map.floorValue(g, i));
                    assertEquals("Floor error", null, map.floorEntry(g, i));
                    assertEquals("Lower error", null, map.lowerValue(g, i));
                    assertEquals("Lower error", null, map.lowerEntry(g, i));
                }else{
                    assertEquals("Floor error", i - 1, map.floorValue(g, i));
                    assertEquals("Floor error", i - 1, map.floorEntry(g, i).getValue());
                    assertEquals("Lower error", i - 1, map.lowerValue(g, i));
                    assertEquals("Lower error", i - 1, map.lowerEntry(g, i).getValue());
                }

                if(i > 100) {
                    assertEquals("Ceiling error", null, map.ceilingValue(g, i));
                    assertEquals("Ceiling error", null, map.ceilingEntry(g, i));
                    assertEquals("Higher error", null, map.higherValue(g, i));
                    assertEquals("Higher error", null, map.higherEntry(g, i));
                }else{
                    assertEquals("Ceiling error", i + 1, map.ceilingValue(g, i));
                    assertEquals("Ceiling error", i + 1, map.ceilingEntry(g, i).getValue());
                    assertEquals("Higher error", i + 1, map.higherValue(g, i));
                    assertEquals("Higher error", i + 1, map.higherEntry(g, i).getValue());
                }
            }
            
        }else{
            assertEquals("First error", 0, map.firstValue());
            assertEquals("First error", 0, map.firstEntry().getValue());
            assertEquals("Last error", 100, map.lastValue());
            assertEquals("Last error", 100, map.lastEntry().getValue());
            
            for(int i = 0; i <= 100; i += 2) {
                assertTrue("Contains error", map.containsValue(i));
                assertTrue("Contains error", map.containsValue(g, i));
                assertEquals("Floor error", i, map.floorValue(i));
                assertEquals("Floor error", i, map.floorEntry(i).getValue());
                assertEquals("Ceiling error", i, map.ceilingValue(i));
                assertEquals("Ceiling error", i, map.ceilingEntry(i).getValue());

                if(i == 0) {
                    assertEquals("Lower error", null, map.lowerValue(i));
                    assertEquals("Lower error", null, map.lowerEntry(i));
                }else{
                    assertEquals("Lower error", i - 2, map.lowerValue(i));
                    assertEquals("Lower error", i - 2, map.lowerEntry(i).getValue());
                }

                if(i == 100) {
                    assertEquals("Higher error", null, map.higherValue(i));
                    assertEquals("Higher error", null, map.higherEntry(i));
                }else{
                    assertEquals("Higher error", i + 2, map.higherValue(i));
                    assertEquals("Higher error", i + 2, map.higherEntry(i).getValue());
                }
            }

            for(int i = -1; i <= 101; i += 2) {
                assertFalse("Contains error", map.containsValue(i));
                assertFalse("Contains error", map.containsValue(g, i));

                if(i < 0) {
                    assertEquals("Floor error", null, map.floorValue(i));
                    assertEquals("Floor error", null, map.floorEntry(i));
                    assertEquals("Lower error", null, map.lowerValue(i));
                    assertEquals("Lower error", null, map.lowerEntry(i));
                }else{
                    assertEquals("Floor error", i - 1, map.floorValue(i));
                    assertEquals("Floor error", i - 1, map.floorEntry(i).getValue());
                    assertEquals("Lower error", i - 1, map.lowerValue(i));
                    assertEquals("Lower error", i - 1, map.lowerEntry(i).getValue());
                }

                if(i > 100) {
                    assertEquals("Ceiling error", null, map.ceilingValue(i));
                    assertEquals("Ceiling error", null, map.ceilingEntry(i));
                    assertEquals("Higher error", null, map.higherValue(i));
                    assertEquals("Higher error", null, map.higherEntry(i));
                }else{
                    assertEquals("Ceiling error", i + 1, map.ceilingValue(i));
                    assertEquals("Ceiling error", i + 1, map.ceilingEntry(i).getValue());
                    assertEquals("Higher error", i + 1, map.higherValue(i));
                    assertEquals("Higher error", i + 1, map.higherEntry(i).getValue());
                }
            }
        }
    }
    
    
    
    
    private void doViewTest(IntMap map) {
        Set<Long> keySet = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<Long, Integer>> entrySet = map.entrySet();

        Assert.assertEquals(map.size(), keySet.size());
        Assert.assertEquals(map.size(), values.size());
        Assert.assertEquals(map.size(), entrySet.size());
       
        
        Iterator<Integer> allIter = map.values().iterator();
        Iterator<Long> keyIter = keySet.iterator();
        Iterator<Integer> valueIter = values.iterator();
        Iterator<Map.Entry<Long, Integer>> entryIter = entrySet.iterator();
                
        while(allIter.hasNext()) {
            Integer v1 = allIter.next();
            Long key = keyIter.next();
            Integer v2 = valueIter.next();
            Map.Entry<Long, Integer> entry = entryIter.next();
            
            assertEquals("Value iter mismatch", v1, v2);
            assertEquals("Node iter mismatch", v1, entry.getValue());
            assertEquals("Key iter mismatch", key, entry.getKey());
            
            assertEquals("Get by mKey error", map.get(key), v1);
        }
    }
    
    
    private void doGroupViewTest(IntMap map) {
        
        Set<Integer> set = map.groups();
        
        for(int i = 0; i < 3; i++) {
            int sum = 0;
            
            for(Integer g: map.groups()) {
                int s = map.size(g);
                assertTrue("Empty group error", s > 0);
                sum += s;
            }
            
            assertEquals("Group size accumulation error", map.size(), sum);
            
            Iterator<?> iter = set.iterator();
            iter.next();
            iter.next();
            iter.remove();
        }
    }
    
    
    
    
    
    private static Long toKey(Integer group, Integer value) {
        if(group == null) {
            return (0xFFFFFFFF00000000L | value.longValue());
        }else{
            return ((group.longValue() << 32) | value.longValue());
        }
    }

    
    @SuppressWarnings( "rawtypes" )    
    private static void assertAllSequence(IntMap map, List values) {
        assertEquals("Size mismatch", map.size(), values.size());
        Iterator<Integer> iter = map.values().iterator();
        
        for(int i = 0; i < values.size(); i++) {
            assertEquals("Sequence mismatch", iter.next(), values.get(i));
        }
        
        for(int i = 0; i < values.size(); i++) {
            assertTrue("Contains error", map.containsValue(values.get(i)));
        }
        
        //assertTrue("ContainsAll error", map.containsAll(values));
    }

    
    @SuppressWarnings( "rawtypes" )
    private static void assertChanSequence(IntMap map, Integer group, List values) {
        assertEquals("Size mismatch", map.size(group), values.size());
        Iterator<Integer> iter = map.groupValues(group).iterator();
        
        for(int i = 0; i < values.size(); i++) {
            assertEquals("Sequence mismatch", iter.next(), values.get(i));
        }
        
        for(int i = 0; i < values.size(); i++) {
            assertTrue("Contains error", map.containsValue(group, values.get(i)));
        }
        
        ///assertTrue("ContainsAll error", map.containsAll(group, values));
    }


    @SuppressWarnings( "rawtypes" )
    private static void assertIter(Iterator iter, List values) {
        for(Object obj: values) {
            assertTrue("Iter size error", iter.hasNext());
            assertEquals("Iter mismatch", iter.next(), obj);
        }
        
        assertFalse("Iter size error", iter.hasNext());
    }

    
    @SuppressWarnings( "rawtypes" )
    private static void assertDescendingIter(Iterator iter, List values) {
        for(int i = values.size() - 1; i >= 0; i--) {
            assertTrue("Iter size error", iter.hasNext());
            assertEquals("Iter mismatch", iter.next(), values.get(i));
        }
        
        assertFalse("Iter size error", iter.hasNext());
    }
    
    
    @SuppressWarnings( "rawtypes" )
    private static void remove(Iterator iter, int idx) {
        for(int i = 0; i <= idx; i++)
            iter.next();
        
        iter.remove();
    }
    
    
    private static List<IntMap> createMaps() {
        List<IntMap> ret = new ArrayList<IntMap>();
        
        ret.add(new IntMap(null, null));
        ret.add(new IntMap(ORDER, null));
        ret.add(new IntMap(null, ORDER));
        ret.add(new IntMap(ORDER, ORDER));
        
        return ret;
    }
    
    
    private static <T> T iterate(Iterator<T> iter, int num) {
        while(--num > 0) {
            iter.next();
        }
        
        return iter.next();
    }
    
    
    private void populate(IntMap map, int num, int maxGroup, int maxValue) {
        for(int i = 0; i < num; i++) {
            int g = mRand.nextInt(maxGroup + 1);
            
            if(g == maxGroup) {
                Integer n = mRand.nextInt(maxValue);
                map.put(null, toKey(null, n), n);
            }else{
                Integer n = mRand.nextInt(maxValue);
                map.put(g, toKey(g, n), n);
            }
        }
    }

    
    static void assertEquals(String msg, Object expected, Object ref) {
        Assert.assertEquals(msg, expected, ref);
    }
    
    
    static void assertEquals(String msg, int expected, Object ref) {
        Assert.assertEquals(msg, expected, ref);
    }

    
    static void assertEquals(String msg, int expected, int actual) {
        Assert.assertEquals(msg, expected, actual);
    }

    
    
    @SuppressWarnings( "rawtypes" )
    static class ReferenceIntMap {        

        private Map<Object, Entry> mMap = new HashMap<Object, Entry>();
        private Set<Entry> mAllSet = new TreeSet<Entry>();
        private Map<Object, Set<Entry>> mGroups = new HashMap<Object, Set<Entry>>();
        
        
        public void put(Object group, Object key, Object value) {
            Entry entry = new Entry(group, key, value);
            
            mMap.put(key, entry);
            mAllSet.add(entry);
            
            Set<Entry> set = mGroups.get(group);
            if(set == null) {
                set = new TreeSet<Entry>();
                mGroups.put(group, set);
            }
            
            set.add(entry);
        }
        
        public void remove(Object key) {
            Entry entry = mMap.remove(key);
            if(entry == null)
                return;
            
            mAllSet.remove(entry);
            Set<Entry> set = mGroups.get(entry.mGroup);
            if(set == null)
                return;
            
            set.remove(entry);
        }
        
        public Iterator iterator() {
            return new EntryValueIter(mAllSet.iterator());
        }
        
        public Iterator iterator(Object group) {
            Set<Entry> set = mGroups.get(group);
            if(set == null)
                return null;
            
            return set.iterator();
        }
        
        public void clear(Object group) {
            Set<Entry> set = mGroups.remove(group);
            if(set == null)
                return;
            
            for(Entry entry: set) {
                mAllSet.remove(entry);
                mMap.remove(entry.mKey);
            }
        }
        
        
        public int size() {
            return mAllSet.size();
        }
    }
   
    
    @SuppressWarnings( "rawtypes" )    
    static class Entry implements Comparable<Entry> {
        
        public final Object mGroup;
        public final Object mKey;
        public final Object mValue;
        
        public Entry(Object group, Object key, Object value) {
            mGroup = group;
            mKey = key;
            mValue = value;
        }
        

        @SuppressWarnings("unchecked")
        public int compareTo(Entry entry) {
            int c = ((Comparable)mValue).compareTo(entry.mValue);
            if(c != 0)
                return c;
            
            if(mGroup == null) {
                if(entry.mGroup == null)
                    return 0;
                
                return -1;
            }
            
            if(entry.mGroup == null)
                return 1;
            
            return ((Comparable)mGroup).compareTo(entry.mGroup);
        }

        
        public boolean equals(Object obj) {
            if(!(obj instanceof Entry))
                return false;
            
            return mGroup.equals(((Entry)obj).mGroup) &&
                   mValue.equals(((Entry)obj).mValue);
            
        }
    }
    
    
    @SuppressWarnings( "rawtypes" )    
    static class EntryValueIter implements Iterator {
        private Iterator<Entry> mIter;
        
        public EntryValueIter(Iterator<Entry> iter) {
            mIter = iter;
        }
        
        
        public boolean hasNext() {
            return mIter.hasNext();
        }
        
        public Object next() {
            return mIter.next().mValue;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }


    
    static final class IntMap extends bits.collect.GroupMap<Integer, Long, Integer> {
        
        public IntMap() {}
        
        public IntMap(Comparator<Integer> groupComp, Comparator<Integer> valueComp) {
            super(groupComp, valueComp);
        }
        
    }

    
}
