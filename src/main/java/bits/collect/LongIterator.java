package bits.collect;

/**
 * @author Philip DeCamp
 */
public interface LongIterator {
    public boolean hasNext();
    public long next();
    public void remove();
}
