package lsfusion.base.col.implementations.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.ArSet;
import lsfusion.base.col.implementations.abs.AMOrderSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.ArCol.deserializeIntArray;
import static lsfusion.base.col.implementations.ArCol.serializeIntArray;

public class ArOrderIndexedSet<K> extends AMOrderSet<K> {
    
    public ArIndexedSet<K> arSet; // для дружественных классов
    public int[] order;

    public ArOrderIndexedSet(int size) {
        arSet = new ArIndexedSet<>(size);
        order = new int[size];
    }

    public ArOrderIndexedSet(ArIndexedSet<K> arSet, int[] order) {
        this.arSet = arSet;
        this.order = order;
    }

    public ImSet<K> getSet() {
        return arSet;
    }

    public int size() {
        return arSet.size();
    }

    public K get(int i) {
        return arSet.get(order[i]);
    }

    public boolean add(K key) {
        throw new UnsupportedOperationException();
    }

    public void exclAdd(K key) {
        throw new UnsupportedOperationException();
    }
    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderIndexedMap<>(this);
    }

    private class RevMap<V> implements ImRevValueMap<K, V> {
        private ArIndexedMap<K, V> result = new ArIndexedMap<>(arSet);

        public void mapValue(int i, V value) {
            result.mapValue(order[i], value);
        }

        public ImRevMap<K, V> immutableValueRev() {
            return result.immutableValueRev();
        }

        public V getMapValue(int i) {
            return result.getMapValue(order[i]);
        }

        public K getMapKey(int i) {
            return result.getMapKey(order[i]);
        }

        public int mapSize() {
            return result.mapSize();
        }
    }
    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new RevMap<>();
    }

    public ImOrderSet<K> immutableOrder() {
        if(arSet.size()==0)
            return SetFact.EMPTYORDER();
        if(arSet.size()==1)
            return SetFact.singletonOrder(single());

        if(arSet.size() < SetFact.useArrayMax) {
            Object[] orderArray = new Object[arSet.size()];
            for(int i=0;i<arSet.size();i++)
                orderArray[i] = get(i);
            return new ArOrderSet<>(new ArSet<>(arSet.size(), orderArray));
        }
        
        arSet.shrink();

        return this;
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArOrderIndexedSet<?> set = (ArOrderIndexedSet<?>) o;
        serializer.serialize(set.arSet, outStream);
        serializeIntArray(set.order, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        ArIndexedSet<?> set = (ArIndexedSet<?>) serializer.deserialize(inStream);
        int[] order = deserializeIntArray(inStream, serializer);
        return new ArOrderIndexedSet<>(set, order);
    }
}
