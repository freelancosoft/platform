package lsfusion.base.col.implementations;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.implementations.order.ArOrderIndexedSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArIndexedSet<K> extends AMSet<K> {

    public int size;
    public Object[] array;

    public ArIndexedSet() {
        this.array = new Object[4];
    }

    public ArIndexedSet(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArIndexedSet(int size) {
        array = new Object[size];
    }

    public ArIndexedSet(ArIndexedSet<K> set) {
        size = set.size;
        array = set.array.clone();
    }

    public int size() {
        return size;
    }

    public K get(int i) {
        return (K) array[i];
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new ArIndexedMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new ArIndexedMap<>(this);
    }

    @Override
    public boolean contains(K element) {
        return ArIndexedMap.findIndex(element, size, array) >= 0;
    }

    @Override
    public K getIdentIncl(K element) {
        return get(ArIndexedMap.findIndex(element, size, array));
    }

    @Override
    public void keep(K element) {
        assert size==0 || array[size-1].hashCode() <= element.hashCode();
        array[size++] = element;
    }

    public boolean add(K element) {
        throw new UnsupportedOperationException();
    }

    public ImSet<K> immutable() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }

        if(size < SetFact.useArrayMax)
            return new ArSet<>(size, array);

        return this;
    }

    public ImSet<K> immutableCopy() {
        return new ArIndexedSet<>(this);
    }

    public ArIndexedMap<K, K> toMap() {
        return new ArIndexedMap<>(size, array, array);
    }

    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    public ImOrderSet<K> toOrderSet() {
        return new ArOrderIndexedSet<>(this, ArSet.genOrder(size));
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArIndexedSet<?> set = (ArIndexedSet<?>) o;
        serializer.serialize(set.size, outStream);
        ArCol.serializeArray(set.array, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] array = ArCol.deserializeArray(inStream, serializer);
        return new ArIndexedSet<>(size, array);
    }
}
