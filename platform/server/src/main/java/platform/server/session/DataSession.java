package platform.server.session;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.server.data.*;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.type.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.RemoteForm;
import platform.server.data.where.WhereBuilder;
import platform.server.classes.*;
import platform.server.caches.hash.HashValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.Lazy;

import java.sql.SQLException;
import java.util.*;

import net.jcip.annotations.Immutable;

public class DataSession extends SQLSession implements ChangesSession {

    // mutable для удобства
    public static class UpdateChanges {

        public final Set<Property> properties;
        public final Set<CustomClass> addClasses;
        public final Set<CustomClass> removeClasses;

        public UpdateChanges() {
            properties = new HashSet<Property>();
            addClasses = new HashSet<CustomClass>();
            removeClasses = new HashSet<CustomClass>();            
        }

        public UpdateChanges(SessionChanges changes, RemoteForm<?> form) {
            addClasses = new HashSet<CustomClass>(changes.add.keySet());
            removeClasses = new HashSet<CustomClass>(changes.remove.keySet());
            properties = new HashSet<Property>(form.getUpdateProperties(changes));
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
            addClasses.addAll(changes.addClasses);
            removeClasses.addAll(changes.removeClasses);
        }
    }

    // формы, для которых с момента последнего update уже был restart, соотвественно в значениях - изменения от посл. update (prev) до посл. apply
    public Map<RemoteForm, UpdateChanges> appliedChanges = new HashMap<RemoteForm, UpdateChanges>();

    // формы для которых с момента последнего update не было restart, соответственно в значениях - изменения от посл. update (prev) до посл. изменения
    public Map<RemoteForm, UpdateChanges> incrementChanges = new HashMap<RemoteForm, UpdateChanges>();

    // assert что те же формы что и в increment, соответственно в значениях - изменения от посл. apply до посл. update (prev)
    public Map<RemoteForm, UpdateChanges> updateChanges = new HashMap<RemoteForm, UpdateChanges>();

    public SessionChanges changes = SessionChanges.EMPTY;

    public final BaseClass baseClass;
    public final CustomClass namedObject;
    public final Property<?> name;
    public final CustomClass transaction;
    public final Property<?> date;

    // для отладки
    public static boolean reCalculateAggr = false;

    public DataSession(DataAdapter adapter, BaseClass baseClass, CustomClass namedObject, Property name, CustomClass transaction, Property date) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        super(adapter);

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.transaction = transaction;
        this.date = date;
    }

    public void restart(boolean cancel) throws SQLException {

        // apply
        //      по кому был restart : добавляем changes -> applied
        //      по кому не было restart : to -> applied (помечая что был restart)

        // cancel
        //    по кому не было restart :  from -> в applied (помечая что был restart)

        if(!cancel)
            for(Map.Entry<RemoteForm,UpdateChanges> appliedChange : appliedChanges.entrySet())
                appliedChange.getValue().add(new UpdateChanges(changes, (RemoteForm<?>) appliedChange.getKey()));

        assert Collections.disjoint(appliedChanges.keySet(),(cancel?updateChanges:incrementChanges).keySet());
        appliedChanges.putAll(cancel?updateChanges:incrementChanges);
        incrementChanges = new HashMap<RemoteForm, UpdateChanges>();
        updateChanges = new HashMap<RemoteForm, UpdateChanges>();

        newClasses = new HashMap<DataObject, ConcreteObjectClass>();

        changes.dropTables(this);
        changes = SessionChanges.EMPTY;
    }

    public <P extends PropertyInterface> void changeSingleProperty(Property<P> property, Modifier<? extends Changes> modifier, DataObject key, Object value) throws SQLException {
        property.getChangeProperty(Collections.singletonMap(BaseUtils.single(property.interfaces),key)).execute(this,modifier,value);
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier) throws SQLException {

        DataObject object = new DataObject(IDTable.instance.generateID(this, IDTable.OBJECT),baseClass.unknown);

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);
        
        if(customClass.isChild(namedObject))
            changeSingleProperty(name,modifier,object,customClass.caption+" "+object.object);

        if(customClass.isChild(transaction))
            changeSingleProperty(date,modifier,object,DateConverter.dateToInt(new Date()));

        return object;
    }    

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changes = new SessionChanges(changes, addClasses, removeClasses, change, this);

        newClasses.put(change,toClass);

        // по тем по кому не было restart'а new -> to
        for(UpdateChanges incrementChange : incrementChanges.values()) {
            incrementChange.addClasses.addAll(addClasses);
            incrementChange.removeClasses.addAll(removeClasses);
        }
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, boolean externalID) throws SQLException {
        changes = new SessionChanges(changes, property, keys, newValue, this);

        // по тем по кому не было restart'а new -> to
        SessionChanges propertyChanges = changes.getSessionChanges(property);
        for(Map.Entry<RemoteForm,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((RemoteForm<?>) incrementChange.getKey()).getUpdateProperties(propertyChanges));
    }

    public ConcreteClass getCurrentClass(DataObject value) {
        ConcreteClass newClass;
        if((newClass = newClasses.get(value))==null)
            return value.objectClass;
        else
            return newClass;
    }

    public <T> Map<T, ConcreteClass> getCurrentClasses(Map<T, DataObject> map) {
        Map<T, ConcreteClass> result = new HashMap<T, ConcreteClass>();
        for(Map.Entry<T,DataObject> entry : map.entrySet())
            result.put(entry.getKey(),getCurrentClass(entry.getValue()));
        return result;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, this, baseClass));
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(value, type);
    }

    // узнает список изменений произошедших без него
    public Collection<Property> update(RemoteForm<?> form, Collection<CustomClass> updateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        UpdateChanges incrementChange = incrementChanges.get(form);
        if(incrementChange!=null) // если не было restart
            //    to -> from или from = changes, to = пустому
            updateChanges.get(form).add(incrementChange);
            //    возвращаем to
        else { // иначе
            incrementChange = appliedChanges.remove(form);
            if(incrementChange==null) // совсем не было
                incrementChange = new UpdateChanges();
            UpdateChanges formChanges = new UpdateChanges(changes, form);
            // from = changes (сбрасываем пометку что не было restart'а)
            updateChanges.put(form, formChanges);
            // возвращаем applied + changes
            incrementChange.add(formChanges);
        }
        incrementChanges.put(form,new UpdateChanges());

        updateClasses.addAll(incrementChange.addClasses);
        updateClasses.addAll(incrementChange.removeClasses);
        return incrementChange.properties;
    }

    @Immutable
    private static class Increment extends Modifier<Increment.UsedChanges> {

        Map<Property, IncrementChangeTable> tables = new HashMap<Property, IncrementChangeTable>(); 

        public final DataSession session;

        public SessionChanges getSession() {
            return session.changes;
        }

        private Increment(DataSession session) {
            this.session = session;
        }

        static class UsedChanges extends Changes<UsedChanges> {
            final Map<Property, IncrementChangeTable> increment;

            private UsedChanges() {
                 increment = new HashMap<Property, IncrementChangeTable>();
            }
            private final static UsedChanges EMPTY = new UsedChanges();

            public UsedChanges(Increment modifier) {
                 super(modifier);
                 increment = new HashMap<Property, IncrementChangeTable>(modifier.tables);
            }

            @Override
            public boolean hasChanges() {
                return super.hasChanges() || !increment.isEmpty();
            }

            private UsedChanges(UsedChanges changes, SessionChanges merge) {
                super(changes, merge);
                increment = changes.increment;
            }
            public UsedChanges addChanges(SessionChanges changes) {
                return new UsedChanges(this, changes);
            }

            private UsedChanges(UsedChanges changes, UsedChanges merge) {
                super(changes, merge);
                increment = BaseUtils.merge(changes.increment, merge.increment);
            }
            public UsedChanges add(UsedChanges changes) {
                return new UsedChanges(this, changes);
            }

            @Override
            public boolean equals(Object o) {
                return this==o || o instanceof UsedChanges && increment.equals(((UsedChanges)o).increment) && super.equals(o);
            }

            @Override
            @Lazy
            public int hashValues(HashValues hashValues) {
                return super.hashValues(hashValues) * 31 + MapValuesIterable.hash(increment,hashValues);
            }

            @Override
            @Lazy
            public Set<ValueExpr> getValues() {
                Set<ValueExpr> result = new HashSet<ValueExpr>();
                result.addAll(super.getValues());
                MapValuesIterable.enumValues(result, increment);
                return result;
            }

            public UsedChanges(Property property, IncrementChangeTable table) {
                increment = Collections.singletonMap(property, table);
            }

            private UsedChanges(UsedChanges usedChanges, Map<ValueExpr,ValueExpr> mapValues) {
                super(usedChanges, mapValues);
                increment = MapValuesIterable.translate(usedChanges.increment, mapValues);
            }

            public UsedChanges translate(Map<ValueExpr,ValueExpr> mapValues) {
                return new UsedChanges(this, mapValues);
            }
        }

        public UsedChanges fullChanges() {
            return new UsedChanges(this);
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
                Join<PropertyField> incrementJoin = incrementTable.join(BaseUtils.join(BaseUtils.reverse(BaseUtils.join(property.mapTable.mapKeys, incrementTable.mapKeys)), joinImplement));
                changedWhere.add(incrementJoin.getWhere());
                return incrementJoin.getExpr(incrementTable.changes.get(property));
            } else
                return null;
        }

        public UsedChanges used(Property property, UsedChanges usedChanges) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null)
                return new UsedChanges(property, incrementTable);
            else
                return usedChanges;
        }

        public UsedChanges newChanges() {
            return UsedChanges.EMPTY;
        }

        public IncrementChangeTable read(Collection<Property> properties,BaseClass baseClass) throws SQLException {
            // создаем таблицу
            IncrementChangeTable changeTable = new IncrementChangeTable(properties);
            session.createTemporaryTable(changeTable);

            // подготавливаем запрос
            Query<KeyField,PropertyField> changesQuery = new Query<KeyField, PropertyField>(changeTable);
            WhereBuilder changedWhere = new WhereBuilder();
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                changesQuery.properties.put(change.getValue(),
                        change.getKey().getIncrementExpr(BaseUtils.join(changeTable.mapKeys, changesQuery.mapKeys), this, changedWhere));
            changesQuery.and(changedWhere.toWhere());

            // подготовили - теперь надо сохранить в курсор и записать классы
            changeTable = changeTable.writeRows(session, changesQuery, baseClass);

            for(Property property : properties)
                tables.put(property,changeTable);

            return changeTable;
        }

        private Expr getNameExpr(Expr expr) {
            return session.name.getSingleExpr(this,expr);
        }

        public <T extends PropertyInterface> String check(Property<T> property) throws SQLException {
            if(property.isFalse) {
                Query<T,String> changed = new Query<T,String>(property);

                WhereBuilder changedWhere = new WhereBuilder();
                Expr valueExpr = property.getExpr(changed.mapKeys,this,changedWhere);
                changed.and(valueExpr.getWhere());
                changed.and(changedWhere.toWhere()); // только на измененные смотрим

                // сюда надо name'ы вставить
                for(T propertyInterface : property.interfaces)
                   changed.properties.put("int"+propertyInterface.ID,getNameExpr(changed.mapKeys.get(propertyInterface)));

                OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(session);
                if(result.size()>0) {
                    String resultString = property.toString() + '\n';
                    for(Map.Entry<Map<T,Object>,Map<String,Object>> row : result.entrySet()) {
                        String objects = "";
                        for(T propertyInterface : property.interfaces)
                            objects = (objects.length()==0?"":objects+", ") + row.getKey().get(propertyInterface)+" "+BaseUtils.nullString((String) row.getValue().get("int"+propertyInterface.ID)).trim();
                        resultString += "    " + objects + '\n';
                    }

                    return resultString;
                }
            }

            return null;
        }
    }

    public String apply(final BusinessLogics<?> BL) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        startTransaction();

        Increment increment = new Increment(this);
        Collection<IncrementChangeTable> temporary = new ArrayList<IncrementChangeTable>();

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property<?> property : BL.getAppliedProperties()) {
            if(property.hasChanges(increment)) {
                String constraintResult = increment.check(property);
                if(constraintResult!=null) {
                    // откатим транзакцию
                    rollbackTransaction();
                    return constraintResult;
                }

                if(property.isStored()) // сохраним изменения в таблицы
                    temporary.add(increment.read(Collections.<Property>singleton(property),baseClass));
            }
        }

        // записываем в базу
        for(Collection<Property> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable,Property>(){public ImplementTable group(Property key) {return key.mapTable.table;}},
                increment.tables.keySet()).values()) {
            IncrementChangeTable changeTable;
            if(groupTable.size()==1) // временно так - если одна берем старую иначе группой
                changeTable = increment.tables.get(groupTable.iterator().next());
            else {
                changeTable = increment.read(groupTable,baseClass);
                temporary.add(changeTable);
            }

            Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(changeTable.table);
            Join<PropertyField> join = changeTable.join(BaseUtils.join(BaseUtils.reverse(changeTable.mapKeys), modifyQuery.mapKeys));
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                modifyQuery.properties.put(change.getKey().field,join.getExpr(change.getValue()));
            modifyQuery.and(join.getWhere());
            modifyRecords(new ModifyQuery(changeTable.table, modifyQuery));
        }

        for(Map.Entry<DataObject,ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(this,newClass.getKey());

        commitTransaction();

        for(IncrementChangeTable addTable : temporary)
            dropTemporaryTable(addTable);

        restart(false);

        return null;
    }

    private final Map<Integer,Integer> viewIDs = new HashMap<Integer, Integer>();
    public int generateViewID(int ID) {
        Integer idCounter;
        synchronized(viewIDs) {
            idCounter = viewIDs.get(ID);
            if(idCounter==null) idCounter = 0;
            viewIDs.put(ID,idCounter+1);
        }
        return (ID << 6) + idCounter;
    }
}
