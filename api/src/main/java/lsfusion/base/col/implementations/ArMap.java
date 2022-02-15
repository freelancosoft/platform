package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.implementations.order.ArOrderMap;
import lsfusion.base.col.implementations.stored.StoredArIndexedMap;
import lsfusion.base.col.implementations.stored.StoredArMap;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.LIMIT;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.STORED_FLAG;

public class ArMap<K, V> extends AMRevMap<K, V> {
    
    private int size;
    private Object[] keys;
    private Object[] values;

    public ArMap(AddValue<K, V> addValue) {
        super(addValue);

        this.keys = new Object[4];
        this.values = new Object[4];
    }

    public ArMap(int size, Object[] keys, Object[] values) {
        this.size = size;
        this.keys = keys;
        this.values = values;
    }

    public ArMap(ArMap<K, V> map, boolean clone) {
        assert clone;
        if (needSwitchToStored(map)) {
            switchToStored(mapSize(), map.keys, map.values);
        } else {
            size = map.size;
            this.keys = map.keys.clone();
            this.values = map.values.clone();
        }
    }

    public ArMap(ArMap<K, V> map, AddValue<K, V> addValue) {
        super(addValue);

        size = map.size;
        this.keys = map.keys.clone();
        this.values = map.values.clone();
    }

    public ArMap(int size, AddValue<K, V> addValue) {
        super(addValue);

        keys = new Object[size];
        values = new Object[size];
    }

    // на ValueMap
    public ArMap(int size, Object[] keys) {
        this.size = size;
        this.keys = keys;
        this.values = new Object[size];
    }
    public ArMap(ArMap<K, ?> map) {
        this(map.size, map.keys);
    }
    public ArMap(ArSet<K> set) {
        if (!set.isStored()) {
            this.size = set.size();
            this.keys = set.getArray();
            this.values = new Object[size];
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }

    public K getKey(int i) {
        if (!isStored()) {
            return (K) keys[i];
        } else {
            return stored().getKey(i);
        }
    }

    public V getValue(int i) {
        if (!isStored()) {
            return (V) values[i];
        } else {
            return stored().getValue(i);
        }
    }

    public void mapValue(int i, V value) {
        if (!isStored()) {
            values[i] = value;
        } else {
            stored().mapValue(i, value);
        }
    }

    public <M> ImValueMap<K, M> mapItValues() {
        if (!isStored()) {
            return new ArMap<>(this);
        } else {
            return stored().mapItValues();
        }
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        if (!isStored()) {
            return new ArMap<>(this);
        } else {
            return stored().mapItRevValues();
        }
    }

    protected MExclMap<K, V> copy() {
        if (!isStored()) {
            return new ArMap<>(this, true);
        } else {
            return stored().copy();
        }
    }

    private void resize(int length) {
        Object[] newKeys = new Object[length];
        System.arraycopy(keys, 0, newKeys, 0, size);
        Object[] newValues = new Object[length];
        System.arraycopy(values, 0, newValues, 0, size);
        keys = newKeys;
        values = newValues;
    }

    @Override
    public void exclAdd(K key, V value) { // не будем проверять, так как очень большое искажение при профайлинге будет
        if (!isStored()) {
            if (size >= keys.length) resize(2 * keys.length);
            keys[size] = key;
            values[size++] = value;
            switchToStoredIfNeeded(size-1, size);
        } else {
            stored().exclAdd(key, value);
        }
    }
    public boolean add(K key, V value) {
        if (!isStored()) {
            for (int i = 0; i < size; i++)
                if (BaseUtils.hashEquals(keys[i], key)) {
                    AddValue<K, V> addValue = getAddValue();
                    V addedValue = addValue.addValue(key, (V) values[i], value);
                    if (addValue.stopWhenNull() && addedValue == null)
                        return false;
                    values[i] = addedValue;
                    return true;
                }
            exclAdd(key, value);
            return true;
        } else {
            return stored().add(key, value);
        }
    }

    public ImMap<K, V> immutable() {
        if (!isStored()) {
            ImMap<K, V> simple = simpleImmutable();
            if (simple != null)
                return simple;

            if (needSwitchToStored(this)) {
                switchToStored(size, keys, values);
                return stored().immutable();
            }
            
            if (keys.length > size * SetFact.factorNotResize)
                resize(size);

            if (size < SetFact.useArrayMax)
                return this;

            // упорядочиваем Set
            ArSet.sortArray(size, keys, values);
            return new ArIndexedMap<>(size, keys, values);
        } else {
            return stored().immutable();
        }
    }

    @Override
    public ImRevMap<V, K> reverse() {
        if (!isStored()) {
            return new ArMap<>(size, values, keys);
        } else {
            return stored().reverse();
        }
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        if (!isStored()) {
            return new ArOrderMap<>(this);
        } else {
            return stored().toOrderMap();
        }
    }

    @Override
    public ImSet<K> keys() {
        if (!isStored()) {
            return new ArSet<>(size, keys);
        } else {
            return stored().keys();
        }
    }

    @Override
    public ImCol<V> values() {
        if (!isStored()) {
            return new ArCol<>(size, values);
        } else {
            return stored().values();
        }
    }

    private ImMap<K, V> merge(int mgSize, Object[] mgKeys, Object[] mgValues, AddValue<K, V> add) {

        int r = 0;
        Object[] rKeys = new Object[size + mgSize];
        Object[] rValues = new Object[size + mgSize];

        boolean[] found = new boolean[mgSize];
        for(int i=0;i<size;i++) {
            K key = (K)keys[i];
            V addedValue = (V)values[i];
            if(!add.exclusive()) {
                for (int j = 0; j < mgSize; j++)
                    if (!found[j] && BaseUtils.hashEquals(key, mgKeys[j])) {
                        found[j] = true;
                        addedValue = add.addValue(key, addedValue, (V) mgValues[j]);
                        if (add.stopWhenNull() && addedValue == null)
                            return null;
                    }
            }
            rKeys[r] = key;
            rValues[r] = addedValue;
            r++;
        }
        for(int j=0;j<mgSize;j++)
            if(!found[j]) {
                rKeys[r] = mgKeys[j];
                rValues[r] = mgValues[j];
                r++;
            }


        if(rKeys.length > r * SetFact.factorNotResize) {
            Object[] newKeys = new Object[r];
            System.arraycopy(rKeys, 0, newKeys, 0, r);
            Object[] newValues = new Object[r];
            System.arraycopy(rValues, 0, newValues, 0, r);
            rKeys = newKeys;
            rValues = newValues;
        }

        if(size < SetFact.useArrayMax)
            return new ArMap<>(r, rKeys, rValues);

        // упорядочиваем Set
        ArSet.sortArray(r, rKeys, rValues);
        return new ArIndexedMap<>(r, rKeys, rValues);
    }

    @Override
    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { // важная оптимизация так как ОЧЕНЬ много раз вызывается
        if(map.isEmpty()) return this;
        
        if (!isStored()) {

            if (add.reversed() && size < map.size()) return ((ImMap<K, V>) map).merge(this, add.reverse());

            ImMap<K, V> result;
            if (map.size() <= SetFact.useIndexedAddInsteadOfMerge) {
                ArMap<K, V> mResult = new ArMap<>(this, add);
                if (add.exclusive()) {
                    mResult.exclAddAll(map);
                } else {
                    if (!mResult.addAll(map))
                        return null;
                }
                result = mResult.immutable();
            } else {
                if (map instanceof ArMap) {
                    ArMap<K, V> arMap = (ArMap<K, V>) map;
                    result = merge(arMap.size, arMap.keys, arMap.values, add);
                } else {
                    int mapSize = map.size();
                    Object[] mapKeys = new Object[mapSize];
                    Object[] mapValues = new Object[mapSize];
                    for (int i = 0; i < mapSize; i++) {
                        mapKeys[i] = map.getKey(i);
                        mapValues[i] = map.getValue(i);
                    }
                    ArSet.sortArray(mapSize, mapKeys, mapValues);
                    result = merge(mapSize, mapKeys, mapValues, add);
                }
            }

            //        assert BaseUtils.nullHashEquals(result, super.merge(map, add));
            return result;
        } else {
            return stored().merge(map, add);
        }
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        if (!isStored()) {
            return merge(map, MapFact.exclusive());
        } else {
            return stored().addExcl(map);
        }
    }

    public Object[] getKeys() {
        return keys;
    }

    public void setKeys(Object[] keys) {
        this.keys = keys;
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArMap<?, ?> map = (ArMap<?, ?>) o;
        serializer.serialize(map.size, outStream);
        ArCol.serializeArray(map.keys, serializer, outStream);
        ArCol.serializeArray(map.values, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] keys = ArCol.deserializeArray(inStream, serializer);
        Object[] values = ArCol.deserializeArray(inStream, serializer);
        return new ArMap<>(size, keys, values);
    }

    public boolean isStored() {
        return size == STORED_FLAG;
    }

    private StoredArMap<K, V> stored() {
        return (StoredArMap<K, V>) keys[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (needSwitchToStored(oldSize, newSize)) {
            switchToStored(size, keys, values);
        }
    }

    private boolean needSwitchToStored(int oldSize, int newSize) {
        // todo [dale]: temp
        return data == null && oldSize <= LIMIT && newSize > LIMIT;
    }

    private boolean needSwitchToStored(ArMap<K, V> map) {
        return map.data == null && !map.isStored() && map.size() > LIMIT;
    }

    private void switchToStored(int size, Object[] keys, Object[] values) {
        StoredArIndexedMap<K, V> storedMap =
                new StoredArIndexedMap<>(StoredArraySerializer.getInstance(), size, (K[])keys, (V[])values);
        this.keys = new Object[]{storedMap};
        this.size = STORED_FLAG;
    }
}
