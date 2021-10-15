package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.implementations.order.HOrderSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HSet<T> extends AMSet<T> {
    public int size;
    protected Object[] table;

    public int[] indexes;

    public static final float loadFactor = 0.3f;

    public HSet() {
        table = new Object[8];

        indexes = new int[(int)(table.length * loadFactor)];
    }

    public HSet(int size, Object[] table, int[] indexes) {
        this.size = size;
        this.table = table;
        this.indexes = indexes;
    }

    public HSet(int size) {
        int initialCapacity = (int)(size/loadFactor) + 1;

        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        table = new Object[capacity];

        indexes = new int[size];
    }

    public HSet(HSet<T> set) {
        size = set.size;

        table = set.table.clone();

        indexes = set.indexes.clone();
    }

    public int size() {
        return size;
    }

    public boolean contains(T where) {
        // копися с hashSet'а
        for(int i= MapFact.colHash(where.hashCode()) & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(BaseUtils.hashEquals(table[i], where))
                return true;
        return false;
    }

    @Override
    public T getIdentIncl(T element) {
        for(int i= MapFact.colHash(element.hashCode()) & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(BaseUtils.hashEquals(table[i], element))
                return (T) table[i];
        assert false;
        return null;
    }

    /*    public HSet(HSet<T>[] sets) {
        HSet<T> minSet = sets[0];
        for(int i=1;i<sets.length;i++)
            if(sets[i].size<minSet.size)
                minSet = sets[i];

        table = new Object[minSet.table.length];
        htable = new int[table.length];

        indexes = new int[(int)(table.length * loadFactor)];

        for(int i=0;i<minSet.size;i++) {
            T element = minSet.get(i); int hash = minSet.htable[minSet.indexes[i]];
            boolean all = true;
            for(HSet<T> set : sets)
                if(set!=minSet && !set.contains(element,hash)) {
                    all = false;
                    break;
                }
            if(all)
                add(element,hash);
        }
    }*/

    private void resize(int length) {
        int[] newIndexes = new int[(int)(length * loadFactor)+1];

        Object[] newTable = new Object[length];
        for(int i=0;i<size;i++) {
            Object object = table[indexes[i]];

            // копися с hashSet'а
            int newHash = MapFact.colHash(object.hashCode()) & (length-1);
            while(newTable[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);
            newTable[newHash] = object;

            newIndexes[i] = newHash;
        }

        table = newTable;

        indexes = newIndexes;
    }

    public boolean add(T where) {
        if(size>=indexes.length) resize(2*table.length);
        // копися с hashSet'а
        int i= MapFact.colHash(where.hashCode()) & (table.length-1);
        while(table[i]!=null) {
            if(BaseUtils.hashEquals(table[i], where))
                return true;
            i=(i==table.length-1?0:i+1);
        }
        table[i] = where;
        indexes[size++] = i;
        return false;
    }

    public T get(int i) {
        return (T) table[indexes[i]];
    }

    public <M> ImValueMap<T, M> mapItValues() {
        return new HMap<>(this);
    }

    public <M> ImRevValueMap<T, M> mapItRevValues() {
        return new HMap<>(this);
    }

    public ImSet<T> immutable() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(size < SetFact.useArrayMax) {
            Object[] array = new Object[size];
            for(int i=0;i<size;i++)
                array[i] = get(i);
            return new ArSet<>(size, array);
        }
        if(size >= SetFact.useIndexedArrayMin) {
            Object[] array = new Object[size];
            for(int i=0;i<size;i++)
                array[i] = get(i);
            ArSet.sortArray(size, array);
            return new ArIndexedSet<>(size, array);
        }

        if(indexes.length > size * SetFact.factorNotResize) {
            int[] newIndexes = new int[size];
            System.arraycopy(indexes, 0, newIndexes, 0, size);
            indexes = newIndexes;
        }
        return this;
    }

    public ImSet<T> immutableCopy() {
        return new HSet<>(this);
    }

    @Override
    public HMap<T, T> toMap() {
        return new HMap<>(size, table, table, indexes);
    }

    @Override
    public ImRevMap<T, T> toRevMap() {
        return toMap();
    }

    @Override
    public ImOrderSet<T> toOrderSet() {
        return new HOrderSet<>(this);
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        HSet<?> set = (HSet<?>) o;
        serializer.serialize(set.size, outStream);
        ArCol.serializeArray(set.table, serializer, outStream);
        ArCol.serializeIntArray(set.indexes, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int) serializer.deserialize(inStream);
        Object[] array = ArCol.deserializeArray(inStream, serializer);
        int[] indexes = ArCol.deserializeIntArray(inStream, serializer);
        return new HSet<>(size, array, indexes);
    }
}
