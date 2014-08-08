package bits.collect;

import java.util.Collection;
import java.util.Set;


/**
 * Primitive long version of {@link java.util.Map}.
 *
 * @see java.util.Map
 */
public interface LongMap<V> {

    void 	      clear();
    boolean       containsKey( long key );
    boolean       containsValue( Object value );
    Set<Entry<V>> entrySet();
    V             get( long key );
    int	          hashCode();
    boolean	      isEmpty();
    LongSet       keySet();
    V             put( long key, V value);
    void          putAll( LongMap<? extends V> m );
    V             remove( long key );
    int           size();
    Collection<V> values();

    static interface Entry<V> {
        long getKey();
        V getValue();
        V setValue( V value );
    }

}
