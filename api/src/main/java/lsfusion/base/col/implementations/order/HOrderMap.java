package lsfusion.base.col.implementations.order;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.abs.AMWrapOrderMap;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HOrderMap<K, V> extends AMWrapOrderMap<K, V, HMap<K, V>> {
    
    public HOrderMap(AddValue<K, V> addValue) {
        super(new HMap<>(addValue));
    }

    public HOrderMap(HMap<K, V> wrapMap) {
        super(wrapMap);
    }

    public HOrderMap(HOrderMap<K, V> orderMap, AddValue<K, V> addValue) {
        super(new HMap<>(orderMap.wrapMap, addValue));
    }

    public HOrderMap(int size, AddValue<K, V> addValue) {
        super(new HMap<>(size, addValue));
    }

    // ImValueMap
    public HOrderMap(HOrderMap<K, ?> orderMap) {
        super(new HMap<>(orderMap.wrapMap));
    }

    public HOrderMap(HOrderSet<K> hOrderSet) {
        super(new HMap<>(hOrderSet.wrapSet));
    }

    public HOrderMap(HOrderMap<K, V> orderMap, boolean clone) {
        super(new HMap<>(orderMap.wrapMap, clone));
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new HOrderMap<>(this, true);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new HOrderMap<>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        if(wrapMap.size==0)
            return MapFact.EMPTYORDER();
        if(wrapMap.size==1)
            return MapFact.singletonOrder(singleKey(), singleValue());

        if(wrapMap.size < SetFact.useArrayMax) {
            Object[] keys = new Object[wrapMap.size];
            Object[] values = new Object[wrapMap.size];
            for(int i=0;i<wrapMap.size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            return new ArOrderMap<>(new ArMap<>(wrapMap.size, keys, values));
        }
        if(wrapMap.size >= SetFact.useIndexedArrayMin) {
            Object[] keys = new Object[wrapMap.size];
            Object[] values = new Object[wrapMap.size];
            for(int i=0;i<wrapMap.size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            int[] order = new int[wrapMap.size];
            ArSet.sortArray(wrapMap.size, keys, values, order);
            return new ArOrderIndexedMap<>(new ArIndexedMap<>(wrapMap.size, keys, values), order);
        }

        if(wrapMap.indexes.length > wrapMap.size * SetFact.factorNotResize) {
            int[] newIndexes = new int[wrapMap.size];
            System.arraycopy(wrapMap.indexes, 0, newIndexes, 0, wrapMap.size);
            wrapMap.indexes = newIndexes;
        }
        return this;
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new HOrderSet<>(new HSet<>(wrapMap.size, wrapMap.table, wrapMap.indexes));
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        HOrderMap<?, ?> map = (HOrderMap<?, ?>) o;
        serializer.serialize(map.wrapMap, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        HMap<?, ?> map = (HMap<?, ?>) serializer.deserialize(inStream);
        return new HOrderMap<>(map);
    }
}
