package bits.collect;

import java.util.*;


/**
 * Brute-force interval map. For testing purposes only.
 *
 * @param <K>
 * @param <V>
 */
@SuppressWarnings( "unused" )
class StupidIntervalMap<K, V> {

    private final SortedMap<K, List<V>> mMap;
    private final IntervalComparator<? super K> mComp;
    private int mSize = 0;


    public StupidIntervalMap( Comparator<? super K> comp, IntervalComparator<? super K> intComp ) {
        mMap = new TreeMap<K, List<V>>( comp );
        mComp = intComp;
    }



    public void put( K key, V value ) {
        List<V> list = mMap.get( key );
        if( list == null ) {
            list = new ArrayList<V>();
            mMap.put( key, list );
        }

        list.add( value );
        mSize++;
    }


    public V get( K key ) {
        List<V> list = mMap.get( key );
        return (list == null ? null : list.get( 0 ));
    }


    public List<V> getAll( K key ) {
        List<V> ret = mMap.get( key );
        if( ret == null ) {
            return new ArrayList<V>();
        }

        return new ArrayList<V>( ret );
    }


    public int keyCount( K key ) {
        List<V> list = mMap.get( key );
        return (list == null ? 0 : list.size());
    }


    public V getIntersecting( K key ) {
        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMinToMax( key, e.getKey() );
            int c1 = mComp.compareMinToMax( e.getKey(), key );

            if( c0 < 0 && c1 < 0 ) {
                return e.getValue().get( 0 );
            }

            if( c1 >= 0 ) {
                break;
            }
        }

        return null;
    }


    public V getContaining( K key ) {
        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMins( key, e.getKey() );
            int c1 = mComp.compareMaxes( key, e.getKey() );

            if( c0 >= 0 && c1 <= 0 ) {
                return e.getValue().get( 0 );
            }

            if( c0 < 0 ) {
                break;
            }
        }

        return null;
    }


    public V getContained( K key ) {
        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMins( key, e.getKey() );
            int c1 = mComp.compareMaxes( key, e.getKey() );

            if( c0 <= 0 && c1 >= 0 ) {
                return e.getValue().get( 0 );
            }
        }

        return null;
    }


    public List<V> getAllIntersecting( K key ) {
        List<V> ret = new ArrayList<V>();

        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMinToMax( key, e.getKey() );
            int c1 = mComp.compareMinToMax( e.getKey(), key );

            if( c0 < 0 && c1 < 0 ) {
                ret.addAll( e.getValue() );
            }

            if( c1 >= 0 ) {
                break;
            }
        }

        return ret;
    }


    public List<V> getAllContaining( K key ) {
        List<V> ret = new ArrayList<V>();

        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMins( key, e.getKey() );
            int c1 = mComp.compareMaxes( key, e.getKey() );

            if( c0 >= 0 && c1 <= 0 ) {
                ret.addAll( e.getValue() );
            }

            if( c0 < 0 ) {
                break;
            }
        }

        return ret;
    }


    public List<V> getAllContained( K key ) {
        List<V> ret = new ArrayList<V>();

        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMins( key, e.getKey() );
            int c1 = mComp.compareMaxes( key, e.getKey() );

            if( c0 <= 0 && c1 >= 0 ) {
                ret.addAll( e.getValue() );
            }
        }

        return ret;
    }


    public V remove( K key ) {
        List<V> list = mMap.get( key );
        if( list == null ) {
            return null;
        }

        V ret = list.remove( 0 );
        if( list.isEmpty() ) {
            mMap.remove( key );
        }

        return ret;
    }


    public List<V> removeAll( K key ) {
        List<V> list = mMap.remove( key );

        if( list == null ) {
            return new ArrayList<V>();
        }

        return list;
    }


    public List<K> keyList() {
        List<K> ret = new ArrayList<K>( mSize );

        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            for( V v : e.getValue() ) {
                ret.add( e.getKey() );
            }
        }

        return ret;
    }


    public boolean containsUnionThatContains( K key ) {
        List<K> all = new ArrayList<K>();

        for( Map.Entry<K, List<V>> e : mMap.entrySet() ) {
            int c0 = mComp.compareMinToMax( key, e.getKey() );
            int c1 = mComp.compareMinToMax( e.getKey(), key );

            if( c0 < 0 && c1 < 0 ) {
                all.add( e.getKey() );
            } else if( c1 >= 0 ) {
                break;
            }
        }

        if( all.isEmpty() || mComp.compareMins( key, all.get( 0 ) ) < 0 ) {
            return false;
        }

        K left = all.get( 0 );

        for( int i = 1; i < all.size(); i++ ) {
            K current = all.get( i );
            int c0 = mComp.compareMinToMax( current, left );
            int c1 = mComp.compareMaxes( current, left );

            if( c0 > 0 ) {
                continue;
            }

            if( c1 > 0 ) {
                left = current;
            }
        }


        return mComp.compareMaxes( left, key ) >= 0;
    }


    public int size() {
        return mSize;
    }

}