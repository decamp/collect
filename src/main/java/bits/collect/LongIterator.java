package bits.collect;

/**
 * Primitive long version of {@link java.util.Iterator}.
 *
 * @see java.util.Iterator
 */
public interface LongIterator {
    public boolean hasNext();
    public long next();
    public void remove();
}
