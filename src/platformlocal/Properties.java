/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.*;

class ObjectValue {
    Integer idObject;
    Class Class;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectValue that = (ObjectValue) o;

        return idObject.equals(that.idObject);
    }

    public int hashCode() {
        return idObject.hashCode();
    }

    ObjectValue(Integer iObject,Class iClass) {idObject=iObject;Class=iClass;}
}

class PropertyImplement<T,P extends PropertyInterface> {

    PropertyImplement(PropertyImplement<T,P> iProperty) {
        Property = iProperty.Property;
        Mapping = new HashMap<P,T>(iProperty.Mapping);
    }

    PropertyImplement(Property<P> iProperty) {
        Property = iProperty;
        Mapping = new HashMap<P,T>();
    }

    Property<P> Property;
    Map<P,T> Mapping;

    public String toString() {
        return Property.toString();
    }
}

interface PropertyInterfaceImplement<P extends PropertyInterface> {

    public SourceExpr mapSourceExpr(Map<P, SourceExpr> JoinImplement,boolean NotNull);
    public ClassSet mapGetValueClass(InterfaceClass<P> ClassImplement);
    public InterfaceClassSet<P> mapGetClassSet(ClassSet ReqValue);


    abstract boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks);

    // для increment'ного обновления
    public boolean mapHasChanges(DataSession Session);
    public SourceExpr mapChangeExpr(DataSession Session, Map<P, SourceExpr> JoinImplement, int Value);
    ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement);
    InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue);
}


class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P> {

    int ID = 0;
    PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, SourceExpr> JoinImplement,boolean NotNull) {
        return JoinImplement.get(this);
    }

    public SourceExpr mapChangeExpr(DataSession Session, Map<P, SourceExpr> JoinImplement, int Value) {
        return null;
    }

    public ClassSet mapGetValueClass(InterfaceClass<P> ClassImplement) {
        return ClassImplement.get(this);
    }

    public InterfaceClassSet<P> mapGetClassSet(ClassSet ReqValue) {
        InterfaceClass<P> ResultClass = new InterfaceClass<P>();
        ResultClass.put((P) this,ReqValue);
        return new InterfaceClassSet<P>(ResultClass);
    }

    public boolean mapHasChanges(DataSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        return false;
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return mapGetValueClass(ClassImplement);
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return mapGetClassSet(ReqValue);
    }
}

class AbstractNode {

    AbstractGroup parent;
    AbstractGroup getParent() { return parent; }

}

class AbstractGroup extends AbstractNode {

    String caption;

    AbstractGroup(String icaption) {
        caption = icaption;
    }

    Collection<AbstractNode> children = new ArrayList<AbstractNode>();
    void add(AbstractNode prop) {
        children.add(prop);
        prop.parent = this;
    }

    boolean hasChild(AbstractNode prop) {
        for (AbstractNode child : children) {
            if (child == prop) return true;
            if (child instanceof AbstractGroup && ((AbstractGroup)child).hasChild(prop)) return true;
        }
        return false;
    }

}

abstract class Property<T extends PropertyInterface> extends AbstractNode implements PropertyClass<T> {

    int ID=0;

    TableFactory TableFactory;

    Property(TableFactory iTableFactory) {
        TableFactory = iTableFactory;
    }

    // чтобы подчеркнуть что не направленный
    Collection<T> Interfaces = new ArrayList<T>();
    // кэшируем здесь а не в JoinList потому как быстрее
    // работает только для JOIN смотри ChangedJoinSelect
    Map<Map<T,SourceExpr>,SourceExpr> SelectCacheJoins = new HashMap<Map<T,SourceExpr>,SourceExpr>();

    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<T, SourceExpr> JoinImplement,boolean NotNull) {

        // не будем проверять что все интерфейсы реализованы все равно null в map не попадет
        SourceExpr JoinExpr = SelectCacheJoins.get(JoinImplement);
        if(JoinExpr==null) {
            if(IsPersistent()) {
                // если persistent читаем из таблицы
                Map<KeyField,T> MapJoins = new HashMap<KeyField,T>();
                Table SourceTable = GetTable(MapJoins);

                // прогоним проверим все ли Implement'ировано
                Join<KeyField,PropertyField> SourceJoin = new Join<KeyField,PropertyField>(SourceTable, NotNull);
                for(KeyField Key : SourceTable.Keys)
                    SourceJoin.Joins.put(Key, JoinImplement.get(MapJoins.get(Key)));

                JoinExpr = SourceJoin.Exprs.get(Field);
            } else
                JoinExpr = ((AggregateProperty<T>)this).calculateSourceExpr(JoinImplement, NotNull);

//            SelectCacheJoins.put(JoinImplement,JoinExpr);
        }

        return JoinExpr;
    }

    // возвращает класс значения
    // если null то не подходит по интерфейсу
    abstract public ClassSet getValueClass(InterfaceClass<T> ClassImplement);

    // возвращает то и только то мн-во интерфейсов которые заведомо дают этот интерфейс (GetValueClass >= ReqValue)
    // если null то когда в принципе дает значение
    abstract public InterfaceClassSet<T> getClassSet(ClassSet ReqValue);

    // получает базовый класс чтобы определять
    ClassSet getBaseClass() {
        ClassSet ResultClass = new ClassSet();
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            ResultClass.or(getValueClass(InterfaceClass));
        return ResultClass;
    }

    InterfaceClassSet<T> getUniversalInterface() {
        InterfaceClass<T> Result = new InterfaceClass<T>();
        for(T Interface : Interfaces)
            Result.put(Interface,ClassSet.universal);
        return new InterfaceClassSet<T>(Result);
    }

    public Type getType() {
        return getBaseClass().getType();
    }

    String caption = "";

    public String toString() {
        return caption;
    }

    // заполняет список, возвращает есть ли изменения
    abstract boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks);

    JoinQuery<T,String> getOutSelect(String Value) {
        JoinQuery<T,String> Query = new JoinQuery<T,String>(Interfaces);
        SourceExpr ValueExpr = getSourceExpr(Query.MapKeys, true);
        Query.add(Value, ValueExpr);
        Query.add(new SourceIsNullWhere(ValueExpr,true));
        return Query;
    }

    void Out(DataSession Session) throws SQLException {
        System.out.println(caption);
        getOutSelect("value").outSelect(Session);
    }

    boolean isObject() {
        // нужно также проверить
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            for(ClassSet Interface : InterfaceClass.values())
                if(Interface.intersect(ClassSet.getUp(Class.data)))
                    return false;

        return true;
    }

    void setChangeType(Map<Property, Integer> RequiredTypes,int ChangeType) {
        // 0 и 0 = 0
        // 0 и 1 = 2
        // 1 и 1 = 1
        // 2 и x = 2

        // значит не изменилось (тогда не надо)
        if(!RequiredTypes.containsKey(this)) return;

        Integer PrevType = RequiredTypes.get(this);
        if(PrevType!=null && !PrevType.equals(ChangeType)) ChangeType = 2;
        RequiredTypes.put(this,ChangeType);
    }

    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<T,KeyField> ChangeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    IncrementChangeTable ChangeTable;

    void FillChangeTable() {
        ChangeTable = TableFactory.GetChangeTable(Interfaces.size(), getType());
        ChangeTableMap = new HashMap<T,KeyField>();
        Iterator<KeyField> io = ChangeTable.Objects.iterator();
        for(T Interface : Interfaces)
            ChangeTableMap.put(Interface,io.next());
    }

    void OutChangesTable(DataSession Session) throws SQLException {
        JoinQuery<T,PropertyField> Query = new JoinQuery<T,PropertyField>(Interfaces);

        Join<KeyField,PropertyField> ChangeJoin = new MapJoin<KeyField,PropertyField,T>(ChangeTable,Query,ChangeTableMap,true);
        ChangeJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID,ChangeTable.Property.Type));

        Query.add(ChangeTable.Value,ChangeJoin.Exprs.get(ChangeTable.Value));
        Query.add(ChangeTable.PrevValue,ChangeJoin.Exprs.get(ChangeTable.PrevValue));

        Query.outSelect(Session);
    }

    PropertyField Field;
    abstract Table GetTable(Map<KeyField,T> MapJoins);

    boolean IsPersistent() {
        return Field!=null && !(this instanceof AggregateProperty && TableFactory.ReCalculateAggr); // для тестирования 2-е условие
    }

    // базовые методы - ничего не делать, его перегружают только Override и Data
    boolean allowChangeProperty(Map<T, ObjectValue> Keys) { return false; }
    void changeProperty(Map<T, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {}

    // заполняет требования к изменениям
    abstract void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes);

    // для каскадного выполнения (запрос)
    boolean XL = false;

    // получает запрос для инкрементных изменений
    abstract Change incrementChanges(DataSession Session, int ChangeType);

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<DataPropertyInterface,?> Query, DataSession Session,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(Table.getClassJoin(Session,Interface.Class),true);
        ClassJoin.Joins.put(Table.Object,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    void joinObjects(JoinQuery<DataPropertyInterface,?> Query,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(TableFactory.ObjectTable.getClassJoin(Interface.Class),true);
        ClassJoin.Joins.put(TableFactory.ObjectTable.Key,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    // тип по умолчанию, если null заполнить кого ждем
    abstract Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait);

    class Change {
        int Type; // && 0 - =, 1 - +, 2 - и новое и предыдущее
        Source<T,PropertyField> Source;
        ChangeClassSet<T> Classes;

        Change(int iType, Source<T, PropertyField> iSource, ChangeClassSet<T> iClasses) {
            Type = iType;
            Source = iSource;
            Classes = iClasses;
        }

        // подгоняет к Type'у
        void correct(int RequiredType) {
            // проверим что вернули что вернули то что надо, "подчищаем" если не то
            // если вернул 2 запишем
            if(Type==2 || (Type!=RequiredType))
                RequiredType = 2;

            if(Type != RequiredType) {
                JoinQuery<T,PropertyField> NewQuery = new JoinQuery<T,PropertyField>(Interfaces);
                SourceExpr NewExpr = (new UniJoin<T,PropertyField>(Source,NewQuery,true)).Exprs.get(ChangeTable.Value);
                // нужно LEFT JOIN'ить старые
                SourceExpr PrevExpr = getSourceExpr(NewQuery.MapKeys,false);
                // по любому 2 нету надо докинуть
                NewQuery.add(ChangeTable.PrevValue,PrevExpr);
                if(Type==1) {
                    // есть 1, а надо по сути 0
                    UnionSourceExpr SumExpr = new UnionSourceExpr(1);
                    SumExpr.Operands.put(NewExpr,1);
                    SumExpr.Operands.put(PrevExpr,1);
                    NewExpr = SumExpr;
                }
                NewQuery.add(ChangeTable.Value,NewExpr);

                Source = NewQuery;
                Type = RequiredType;
            }
        }

        // сохраняет в инкрементную таблицу
        void save(DataSession Session) throws SQLException {

//            System.out.println(caption+" "+Type);
//            Source.outSelect(Session);
//            System.out.println(Classes);

            Map<KeyField,Integer> ValueKeys = new HashMap<KeyField,Integer>();
            ValueKeys.put(ChangeTable.Property,ID);
            Session.deleteKeyRecords(ChangeTable,ValueKeys);

            // откуда читать
            JoinQuery<T,PropertyField> ReadQuery = new JoinQuery<T,PropertyField>(Interfaces);
            Join<KeyField,PropertyField> ReadJoin = new MapJoin<KeyField,PropertyField,T>(ChangeTable,ReadQuery,ChangeTableMap,true);
            ReadJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID,ChangeTable.Property.Type));

            JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(ChangeTable.Keys);
            Join<T,PropertyField> WriteJoin = new MapJoin<T,PropertyField,KeyField>(Source,ChangeTableMap,WriteQuery,true);
            WriteQuery.putDumbJoin(ValueKeys);

            WriteQuery.add(ChangeTable.Value,WriteJoin.Exprs.get(ChangeTable.Value));
            ReadQuery.add(ChangeTable.Value,ReadJoin.Exprs.get(ChangeTable.Value));
            if(Type==2) {
                WriteQuery.add(ChangeTable.PrevValue,WriteJoin.Exprs.get(ChangeTable.PrevValue));
                ReadQuery.add(ChangeTable.PrevValue,ReadJoin.Exprs.get(ChangeTable.PrevValue));
            }

            Session.InsertSelect(new ModifyQuery(ChangeTable,WriteQuery));

            Source = ReadQuery;
        }

        // сохраняет в базу
        void apply(DataSession Session) throws SQLException {

            Map<KeyField,T> MapKeys = new HashMap<KeyField,T>();
            Table SourceTable = GetTable(MapKeys);

            JoinQuery<KeyField,PropertyField> ModifyQuery = new JoinQuery<KeyField,PropertyField>(SourceTable.Keys);

            Join<T,PropertyField> Update = new Join<T,PropertyField>(Source,true);
            for(KeyField Key : SourceTable.Keys)
                Update.Joins.put(MapKeys.get(Key),ModifyQuery.MapKeys.get(Key));

            ModifyQuery.add(Field,Update.Exprs.get(ChangeTable.Value));
            Session.ModifyRecords(new ModifyQuery(SourceTable,ModifyQuery));
        }

        // для отладки, проверяет что у объектов заданные классы

        // связывает именно измененные записи из сессии
        // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
        SourceExpr getExpr(Map<T,SourceExpr> JoinImplement, int Value) {

            // теперь определимся что возвращать
            if(Value==2 && Type==2)
                return new Join<T,PropertyField>(Source,JoinImplement,true).Exprs.get(ChangeTable.PrevValue);

            if(Value==Type || (Value==0 && Type==2))
                return new Join<T,PropertyField>(Source,JoinImplement,true).Exprs.get(ChangeTable.Value);

            if(Value==1 && Type==2) {
                UnionSourceExpr Result = new UnionSourceExpr(1);
                Result.Operands.put(new Join<T,PropertyField>(Source,JoinImplement,true).Exprs.get(ChangeTable.Value),1);
                Result.Operands.put(new Join<T,PropertyField>(Source,JoinImplement,true).Exprs.get(ChangeTable.PrevValue),-1);
                return Result;
            }

            throw new RuntimeException("Тип измененного значения не найден");
        }
    }
}

class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    Class Class;

    DataPropertyInterface(int iID,Class iClass) {
        super(iID);
        Class = iClass;
    }
}


class DataProperty<D extends PropertyInterface> extends Property<DataPropertyInterface> {
    Class Value;

    DataProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;

        DefaultMap = new HashMap<DataPropertyInterface,D>();
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    Table GetTable(Map<KeyField,DataPropertyInterface> MapJoins) {
        return TableFactory.GetTable(Interfaces,MapJoins);
    }

    public ClassSet getValueClass(InterfaceClass<DataPropertyInterface> ClassImplement) {
        // пока так потом сделаем перегрузку по классам
        // если не тот класс сразу зарубаем
       for(DataPropertyInterface DataInterface : Interfaces)
            if(!ClassImplement.get(DataInterface).intersect(ClassSet.getUp(DataInterface.Class)))
                return new ClassSet();

        return ClassSet.getUp(Value);
    }

    public InterfaceClassSet<DataPropertyInterface> getClassSet(ClassSet ReqValue) {
        if(ReqValue.intersect(ClassSet.getUp(Value))) {
            InterfaceClass<DataPropertyInterface> ResultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface Interface : Interfaces)
                ResultInterface.put(Interface,ClassSet.getUp(Interface.Class));
            return new InterfaceClassSet<DataPropertyInterface>(ResultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ChangeClassSet<DataPropertyInterface> getChangeClass() {
        return new ChangeClassSet<DataPropertyInterface>(ClassSet.getUp(Value),getClassSet(ClassSet.universal));
    }

    // свойства для "ручных" изменений пользователем
    DataChangeTable DataTable;
    Map<KeyField,DataPropertyInterface> DataTableMap = null;

    void FillDataTable() {
        DataTable = TableFactory.GetDataChangeTable(Interfaces.size(), getType());
        // если нету Map'a построим
        DataTableMap = new HashMap<KeyField,DataPropertyInterface>();
        Iterator<KeyField> io = DataTable.Objects.iterator();
        for(DataPropertyInterface Interface : Interfaces)
            DataTableMap.put(io.next(),Interface);
    }

    void outDataChangesTable(DataSession Session) throws SQLException {
        DataTable.outSelect(Session);
    }

    @Override
    boolean allowChangeProperty(Map<DataPropertyInterface, ObjectValue> Keys) { return true; }

    @Override
    void changeProperty(Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {
        // записываем в таблицу изменений
        Session.changeProperty(this,Keys,NewValue);
    }

    // св-во по умолчанию (при ClassSet подставляется)
    Property<D> DefaultProperty;
    // map интерфейсов на PropertyInterface
    Map<DataPropertyInterface,D> DefaultMap;
    // если нужно еще за изменениями следить и перебивать
    boolean OnDefaultChange;

    // заполняет список, возвращает есть ли изменения, последний параметр для рекурсий
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        if(ChangedProperties.contains(this)) return true;
        // если null то значит полный список запрашивают
        if(Changes==null) return true;

        boolean Changed = Changes.Properties.contains(this);

        if(!Changed)
            for(DataPropertyInterface Interface : Interfaces)
                if(Changes.RemoveClasses.contains(Interface.Class)) Changed = true;

        if(!Changed)
            if(Changes.RemoveClasses.contains(Value)) Changed = true;

        if(DefaultProperty!=null) {
            if(!DefaultLinks.add(this)) return Changed;
            boolean DefaultChanged = DefaultProperty.fillChangedList(ChangedProperties, Changes, DefaultLinks);
            if(!Changed) {
                if(OnDefaultChange)
                    Changed = DefaultChanged;
                else
                    for(DataPropertyInterface Interface : Interfaces)
                        if(Changes.AddClasses.contains(Interface.Class)) Changed = true;
            }
            DefaultLinks.remove(this);
        }

        if(Changed) {
            ChangedProperties.add(this);
            return true;
        } else
            return false;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(DefaultProperty!=null && RequiredTypes.containsKey(DefaultProperty))
            DefaultProperty.setChangeType(RequiredTypes,OnDefaultChange?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    Change incrementChanges(DataSession Session, int ChangeType) {

        // на 3 то есть слева/направо
        UnionQuery<DataPropertyInterface,PropertyField> ResultQuery = new UnionQuery<DataPropertyInterface,PropertyField>(Interfaces,3);
        ChangeClassSet<DataPropertyInterface> ResultClass = new ChangeClassSet<DataPropertyInterface>();

        // Default изменения (пока Add)
        if(DefaultProperty!=null) {
            if(!OnDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface Interface : Interfaces)
                    if(Session.Changes.AddClasses.contains(Interface.Class)) {
                        JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
                        Map<D,SourceExpr> JoinImplement = new HashMap<D,SourceExpr>();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface DataInterface : Interfaces)
                            JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(TableFactory.AddClassTable,Query,Session,Interface);

                        Query.add(ChangeTable.Value,Session.getSourceExpr(DefaultProperty,JoinImplement,true));

                        ResultQuery.add(Query,1);
                        ResultClass.or(DefaultProperty.getChangeClass().mapBack(DefaultMap).and(new ChangeClass<DataPropertyInterface>(Interface,Session.AddChanges.get(Interface.Class))));
                    }
            } else {
                if(Session.PropertyChanges.containsKey(DefaultProperty)) {
                    Property<D>.Change DefaultChange = Session.getChange(DefaultProperty);
                    JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);

                    Map<D,SourceExpr> JoinImplement = new HashMap<D,SourceExpr>();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface DataInterface : Interfaces)
                        JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                    // по изменению св-ва
                    SourceExpr NewExpr = DefaultChange.getExpr(JoinImplement,0);
                    Query.add(ChangeTable.Value,NewExpr);
                    // new, не равно prev
                    Query.add(new FieldExprCompareWhere(new NullEmptySourceExpr(NewExpr),new NullEmptySourceExpr(DefaultChange.getExpr(JoinImplement,2)),FieldExprCompareWhere.NOT_EQUALS));

                    ResultQuery.add(Query,1);
                    ResultClass.or(DefaultChange.Classes.mapBack(DefaultMap));
                }
            }
        }

        boolean DataChanged = Session.Changes.Properties.contains(this);
        JoinQuery<DataPropertyInterface,PropertyField> DataQuery = null;
        SourceExpr DataExpr = null;
        if(DataChanged) {
            DataQuery = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
            // GetChangedFrom
            Join<KeyField,PropertyField> DataJoin = new MapJoin<KeyField,PropertyField,DataPropertyInterface>(DataTable,DataTableMap,DataQuery,true);
            DataJoin.Joins.put(DataTable.Property,new ValueSourceExpr(ID,DataTable.Property.Type));

            DataExpr = DataJoin.Exprs.get(DataTable.Value);
            DataQuery.add(ChangeTable.Value,DataExpr);
            ResultClass.or(Session.DataChanges.get(this));
        }

        for(DataPropertyInterface RemoveInterface : Interfaces) {
            if(Session.Changes.RemoveClasses.contains(RemoveInterface.Class)) {
                // те изменения которые были на удаляемые объекты исключаем
                if(DataChanged) TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,RemoveInterface.Class,DataQuery.MapKeys.get(RemoveInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
                joinChangeClass(TableFactory.RemoveClassTable,Query,Session,RemoveInterface);
                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                Query.add(ChangeTable.Value,new NullJoinSourceExpr(getSourceExpr(Query.MapKeys,true)));

                ResultQuery.add(Query,1);
                ResultClass.or(ChangeClassSet.getNullClass(this).and(new ChangeClass<DataPropertyInterface>(RemoveInterface,Session.RemoveChanges.get(RemoveInterface.Class))));
            }
        }

        if(Session.Changes.RemoveClasses.contains(Value)) {
            // те изменения которые были на удаляемые объекты исключаем
            if(DataChanged) TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,Value,DataExpr);

            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
            Join<KeyField,PropertyField> RemoveJoin = new Join<KeyField,PropertyField>(TableFactory.RemoveClassTable.getClassJoin(Session,Value),true);
            RemoveJoin.Joins.put(TableFactory.RemoveClassTable.Object,getSourceExpr(Query.MapKeys,true));
            Query.add(RemoveJoin);
            Query.add(ChangeTable.Value,new ValueSourceExpr(null,ChangeTable.Value.Type));

            ResultQuery.add(Query,1);
            ResultClass.or(ChangeClassSet.getNullValueClass(this,Session.RemoveChanges.get(Value)));
        }

        // здесь именно в конце так как должна быть последней
        if(DataChanged)
            ResultQuery.add(DataQuery,1);

        return new Change(0,ResultQuery,ResultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    Map<DataPropertyInterface,T> AggregateMap;

    // расчитывает выражение
    abstract SourceExpr calculateSourceExpr(Map<T, SourceExpr> JoinImplement,boolean NotNull);

    // сначала проверяет на persistence
    Table GetTable(Map<KeyField,T> MapJoins) {
        if(AggregateMap==null) {
            AggregateMap = new HashMap<DataPropertyInterface,T>();
            Map<T,Class> Parent = getClassSet(ClassSet.universal).getCommonParent();
            for(T Interface : Interfaces) {
                AggregateMap.put(new DataPropertyInterface(0,Parent.get(Interface)),Interface);
            }
        }

        Map<KeyField,DataPropertyInterface> MapData = new HashMap<KeyField,DataPropertyInterface>();
        Table SourceTable = TableFactory.GetTable(AggregateMap.keySet(),MapData);
        // перекодирукм на MapJoins
        if(MapJoins!=null) {
            for(KeyField MapField : MapData.keySet())
                MapJoins.put(MapField,AggregateMap.get(MapData.get(MapField)));
        }

        return SourceTable;
    }

    Object dropZero(Object Value) {
        if(Value instanceof Integer && Value.equals(0)) return null;
        if(Value instanceof Long && ((Long)Value).intValue()==0) return null;
        if(Value instanceof Double && ((Double)Value).intValue()==0) return null;
        if(Value instanceof Boolean && !((Boolean)Value)) return null;
        return Value;
    }

    // проверяет аггрегацию для отладки
    boolean CheckAggregation(DataSession Session,String Caption) throws SQLException {
        JoinQuery<T, String> AggrSelect;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Кол-во") || caption.equals("OL 269")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<T, Integer>, Map<String, Object>> AggrResult = AggrSelect.executeSelect(Session);
        TableFactory.ReCalculateAggr = true;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Кол-во") || caption.equals("OL 269")) {
            System.out.println("RECALCULATE - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<T, Integer>, Map<String, Object>> CalcResult = AggrSelect.executeSelect(Session);
        TableFactory.ReCalculateAggr = false;

        Iterator<Map.Entry<Map<T,Integer>,Map<String,Object>>> i = AggrResult.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Map<T,Integer>,Map<String,Object>> Row = i.next();
            Map<T, Integer> RowKey = Row.getKey();
            Object RowValue = dropZero(Row.getValue().get("value"));
            Map<String,Object> CalcRow = CalcResult.get(RowKey);
            Object CalcValue = (CalcRow==null?null:dropZero(CalcRow.get("value")));
            if(RowValue==CalcValue || (RowValue!=null && RowValue.equals(CalcValue))) {
                i.remove();
                CalcResult.remove(RowKey);
            }
        }
        // вычистим и отсюда 0
        i = CalcResult.entrySet().iterator();
        while(i.hasNext()) {
            if(dropZero(i.next().getValue().get("value"))==null)
                i.remove();
        }

        if(CalcResult.size()>0 || AggrResult.size()>0) {
            System.out.println("----CheckAggregations "+Caption+"-----");
            System.out.println("----Aggr-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : AggrResult.entrySet())
                System.out.println(Row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : CalcResult.entrySet())
                System.out.println(Row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    void reCalculateAggregation(DataSession Session) throws SQLException {
        PropertyField WriteField = Field;
        Field = null;
        JoinQuery<T,PropertyField> ReCalculateQuery = new JoinQuery<T,PropertyField>(Interfaces);
        ReCalculateQuery.add(WriteField,getSourceExpr(ReCalculateQuery.MapKeys,true));

        Map<KeyField,T> MapTable = new HashMap<KeyField,T>();
        Table AggrTable = GetTable(MapTable);

        JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(AggrTable.Keys);
        WriteQuery.add(WriteField,(new MapJoin<T,PropertyField,KeyField>(ReCalculateQuery,WriteQuery,MapTable,true).Exprs.get(WriteField)));
        Session.ModifyRecords(new ModifyQuery(AggrTable,WriteQuery));

        Field = WriteField;
    }
}

class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    Class ValueClass;
    Object Value;

    ClassProperty(TableFactory iTableFactory, Class iValueClass, Object iValue) {
        super(iTableFactory);
        ValueClass = iValueClass;
        Value = iValue;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // этому св-ву чужого не надо
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        // если Value null то ничего не интересует
        if(Value==null) return false;
        if(ChangedProperties.contains(this)) return true;

        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Changes ==null || Changes.AddClasses.contains(ValueInterface.Class) || Changes.RemoveClasses.contains(ValueInterface.Class)) {
                ChangedProperties.add(this);
                return true;
            }

        return false;
    }

    Change incrementChanges(DataSession Session, int ChangeType) {

        // работает на = и на + ему собсно пофигу, то есть сразу на 2

        // для любого изменения объекта на NEW можно определить PREV и соответственно
        // Set<Class> пришедшие, Set<Class> ушедшие
        // соответственно алгоритм бежим по всем интерфейсам делаем UnionQuery из SS изменений + старых объектов

        // конечный результат, с ключами и выражением
        UnionQuery<DataPropertyInterface,PropertyField> ResultQuery = new UnionQuery<DataPropertyInterface,PropertyField>(Interfaces,3);
        ChangeClassSet<DataPropertyInterface> ResultClass = new ChangeClassSet<DataPropertyInterface>();

        List<DataPropertyInterface> RemoveInterfaces = new ArrayList<DataPropertyInterface>();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.Changes.RemoveClasses.contains(ValueInterface.Class))
                RemoveInterfaces.add(ValueInterface);

        // для RemoveClass без SS все за Join'им (ValueClass пока трогать не будем (так как у значения пока не закладываем механизм изменений))
        for(DataPropertyInterface ChangedInterface : RemoveInterfaces) {
            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface, PropertyField>(Interfaces);
            InterfaceClass<DataPropertyInterface> RemoveClass = new InterfaceClass<DataPropertyInterface>();

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(TableFactory.RemoveClassTable,Query,Session,ChangedInterface);
            RemoveClass.put(ChangedInterface,Session.RemoveChanges.get(ChangedInterface.Class));
            for(DataPropertyInterface ValueInterface : Interfaces)
                if(ValueInterface!=ChangedInterface) {
                    joinObjects(Query,ValueInterface);
                    RemoveClass.put(ValueInterface,ClassSet.getUp(ValueInterface.Class));
                }

            Query.add(ChangeTable.Value,new ValueSourceExpr(null,ChangeTable.Value.Type));
            Query.add(ChangeTable.PrevValue,new ValueSourceExpr(Value,ChangeTable.PrevValue.Type));

            ResultQuery.add(Query,1);
            ResultClass.or(new ChangeClass<DataPropertyInterface>(RemoveClass,new ClassSet()));
        }

        List<DataPropertyInterface> AddInterfaces = new ArrayList();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.Changes.AddClasses.contains(ValueInterface.Class))
                AddInterfaces.add(ValueInterface);

        ListIterator<List<DataPropertyInterface>> il = SetBuilder.buildSubSetList(AddInterfaces).listIterator();
        // пустое подмн-во не надо (как и в любой инкрементности)
        il.next();
        while(il.hasNext()) {
            List<DataPropertyInterface> ChangeProps = il.next();

            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface, PropertyField>(Interfaces);
            InterfaceClass<DataPropertyInterface> AddClass = new InterfaceClass<DataPropertyInterface>();

            for(DataPropertyInterface ValueInterface : Interfaces) {
                if(ChangeProps.contains(ValueInterface)) {
                    joinChangeClass(TableFactory.AddClassTable,Query,Session,ValueInterface);
                    AddClass.put(ValueInterface,Session.AddChanges.get(ValueInterface.Class));
                } else {
                    joinObjects(Query,ValueInterface);
                    AddClass.put(ValueInterface,ClassSet.getUp(ValueInterface.Class));

                    // здесь также надо проверить что не из RemoveClasses (то есть LEFT JOIN на null)
                    if(Session.Changes.RemoveClasses.contains(ValueInterface.Class))
                        TableFactory.RemoveClassTable.excludeJoin(Query,Session,ValueInterface.Class,Query.MapKeys.get(ValueInterface));
                }
            }

            Query.add(ChangeTable.PrevValue,new ValueSourceExpr(null,ChangeTable.PrevValue.Type));
            Query.add(ChangeTable.Value,new ValueSourceExpr(Value,ChangeTable.Value.Type));

            ResultQuery.add(Query,1);
            ResultClass.or(new ChangeClass<DataPropertyInterface>(AddClass,new ClassSet(ValueClass)));
        }

        return new Change(2,ResultQuery,ResultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }

    SourceExpr calculateSourceExpr(Map<DataPropertyInterface, SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "value";

        Source<DataPropertyInterface,String> Source;
        // если null то возвращает EmptySource
        if(Value==null)
            Source = new EmptySource<DataPropertyInterface,String>(Interfaces,ValueString,ValueClass.getType());
        else {
            JoinQuery<DataPropertyInterface,String> Query = new JoinQuery<DataPropertyInterface,String>(Interfaces);

            for(DataPropertyInterface ValueInterface : Interfaces)
                joinObjects(Query,ValueInterface);
            Query.add(ValueString,new ValueSourceExpr(Value,ValueClass.getType()));
            Source = Query;
        }

        return (new Join<DataPropertyInterface,String>(Source,JoinImplement,NotNull)).Exprs.get(ValueString);
    }

    public ClassSet getValueClass(InterfaceClass<DataPropertyInterface> ClassImplement) {
        // аналогично DataProperty\только без перегрузки классов
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(!ClassImplement.get(ValueInterface).intersect(ClassSet.getUp(ValueInterface.Class)))
                return new ClassSet();

        return new ClassSet(ValueClass);
    }

    public InterfaceClassSet<DataPropertyInterface> getClassSet(ClassSet ReqValue) {
        // аналогично DataProperty\только без перегрузки классов
        if(ReqValue.contains(ValueClass)) {
            InterfaceClass<DataPropertyInterface> ResultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface ValueInterface : Interfaces)
                ResultInterface.put(ValueInterface,ClassSet.getUp(ValueInterface.Class));
            return new InterfaceClassSet<DataPropertyInterface>(ResultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ChangeClassSet<DataPropertyInterface> getChangeClass() {
        return new ChangeClassSet<DataPropertyInterface>(new ClassSet(ValueClass),getClassSet(ClassSet.universal));
    }
}

class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    PropertyMapImplement(Property<T> iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<P, SourceExpr> JoinImplement,boolean NotNull) {
        return Property.getSourceExpr(getMapImplement(JoinImplement),NotNull);
    }

    public SourceExpr mapChangeExpr(DataSession Session, Map<P, SourceExpr> JoinImplement, int Value) {
        return Session.getChange(Property).getExpr(getMapImplement(JoinImplement),Value);
    }

    private <V> Map<T, V> getMapImplement(Map<P, V> JoinImplement) {
        Map<T,V> MapImplement = new HashMap<T,V>();
        for(T ImplementInterface : Property.Interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(Mapping.get(ImplementInterface)));
        return MapImplement;
    }

    public ClassSet mapGetValueClass(InterfaceClass<P> ClassImplement) {
        return Property.getValueClass(ClassImplement.mapBack(Mapping));
    }

    public InterfaceClassSet<P> mapGetClassSet(ClassSet ReqValue) {
        return Property.getClassSet(ReqValue).map(Mapping);
    }

    public ChangeClassSet<P> mapGetChangeClass() {
        return Property.getChangeClass().map(Mapping);
    }

    public boolean mapHasChanges(DataSession Session) {
        return Session.getChange(Property)!=null;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        return Property.fillChangedList(ChangedProperties, Changes, DefaultLinks);
    }

    // для OverrideList'а по сути
    void mapChangeProperty(Map<P, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {
        Property.changeProperty(getMapImplement(Keys),NewValue,Session);
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return Session.getChange(Property).Classes.getValueClass(ClassImplement.mapBack(Mapping));
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return Session.getChange(Property).Classes.getClassSet(ReqValue).map(Mapping);
    }

    public ChangeClassSet<P> mapChangeClass(DataSession Session) {
        return Session.getChange(Property).Classes.getChangeClass().map(Mapping);
    }
}

// для четкости пусть будет
class JoinPropertyInterface extends PropertyInterface<JoinPropertyInterface> {
    JoinPropertyInterface(int iID) {
        super(iID);
    }
}

class JoinProperty<T extends PropertyInterface> extends MapProperty<JoinPropertyInterface,T,JoinPropertyInterface,T,PropertyField> {
    PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> Implements;

    JoinProperty(TableFactory iTableFactory, Property<T> iProperty) {
        super(iTableFactory);
        Implements = new PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T>(iProperty);
    }

/*    public ClassSet getValueClass(InterfaceClass<JoinPropertyInterface> ClassImplement) {

        InterfaceClass<T> MapImplement = new InterfaceClass<T>();
        for(T ImplementInterface : Implements.Property.Interfaces) // если null то уже не подходит по интерфейсу
            MapImplement.put(ImplementInterface, Implements.Mapping.get(ImplementInterface).mapGetValueClass(ClassImplement));

        return Implements.Property.getValueClass(MapImplement);
    } */

    InterfaceClassSet<JoinPropertyInterface> getMapClassSet(MapRead<JoinPropertyInterface> Read, InterfaceClass<T> InterfaceImplement) {
        InterfaceClassSet<JoinPropertyInterface> Result = getUniversalInterface();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> MapInterface : Implements.Mapping.entrySet())
           Result = Result.and(Read.getImplementClassSet(MapInterface.getValue(),InterfaceImplement.get(MapInterface.getKey())));
        if(Result.isEmpty()) return null;
        return Result;
    }
    
    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1

        if(!containsImplement(RequiredTypes.keySet())) {
            Implements.Property.setChangeType(RequiredTypes,IncrementType);
        } else {
            int ReqType = (implementAllInterfaces() && IncrementType.equals(0)?0:2);

            Implements.Property.setChangeType(RequiredTypes,ReqType);
            for(PropertyInterfaceImplement Interface : Implements.Mapping.values())
                if(Interface instanceof PropertyMapImplement)
                    (((PropertyMapImplement)Interface).Property).setChangeType(RequiredTypes,(Implements.Property instanceof MultiplyFormulaProperty && IncrementType.equals(1)?1:ReqType));
        }
    }

    // инкрементные св-ва
    Change incrementChanges(DataSession Session, int ChangeType) {

        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL

        UnionQuery<JoinPropertyInterface,PropertyField> ResultQuery = new UnionQuery<JoinPropertyInterface,PropertyField>(Interfaces,3); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)
        ChangeClassSet<JoinPropertyInterface> ResultClass = new ChangeClassSet<JoinPropertyInterface>();

        int QueryIncrementType = ChangeType;
        if(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1)
            ResultQuery.add(getMapQuery(getChangeImplements(Session,1),ChangeTable.Value,ResultClass,true),1);
        else {
            // если нужна 1 и изменились св-ва то придется просто 2-ку считать (хотя это потом можно поменять)
            if(QueryIncrementType==1 && containsImplement(Session.PropertyChanges.keySet()))
                QueryIncrementType = 2;

            // все на Value - PrevValue не интересует, его как раз верхний подгоняет
            ResultQuery.add(getMapQuery(getChange(Session,QueryIncrementType==1?1:0),ChangeTable.Value,ResultClass,false),1);
            if(QueryIncrementType==2) ResultQuery.add(getMapQuery(getPreviousChange(Session),ChangeTable.PrevValue,new ChangeClassSet<JoinPropertyInterface>(),false),1);
        }

        return new Change(QueryIncrementType,ResultQuery,ResultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        if(!containsImplement(ChangedProps)) {
            ToWait.add(Implements.Property);
            return null;
        } else
        if(Implements.Property instanceof MultiplyFormulaProperty)
            return 1;
        else
            return 0;
    }

    Property<T> getMapProperty() {
        return Implements.Property;
    }

    Map<T, PropertyInterfaceImplement<JoinPropertyInterface>> getMapImplements() {
        return Implements.Mapping;
    }

    Collection<JoinPropertyInterface> getMapInterfaces() {
        return Interfaces;
    }

    InterfaceClassSet<JoinPropertyInterface> getMapPropertyInterfaces(InterfaceClassSet<JoinPropertyInterface> InterfaceDumb, InterfaceClassSet<T> ImplementDumb) {
        return InterfaceDumb;
    }

    InterfaceClassSet<T> getMapPropertyImplements(InterfaceClassSet<JoinPropertyInterface> InterfaceDumb, InterfaceClassSet<T> ImplementDumb) {
        return ImplementDumb;
    }

    void putImplementsToQuery(JoinQuery<JoinPropertyInterface, PropertyField> Query, PropertyField Value, MapRead<JoinPropertyInterface> Read, Map<T, SourceExpr> Implements) {
        Query.add(Value,Read.getMapExpr(getMapProperty(),Implements));
    }

    Map<PropertyField, Type> getMapNullProps(PropertyField Value) {
        Map<PropertyField, Type> NullProps = new HashMap<PropertyField, Type>();
        NullProps.put(Value, getType());
        return NullProps;
    }

    PropertyField getDefaultObject() {
        return ChangeTable.Value;
    }

    Source<JoinPropertyInterface, PropertyField> getMapSourceQuery(PropertyField Value) {
        return getMapQuery(getDB(),Value);
    }
}

class GroupPropertyInterface<T extends PropertyInterface> extends PropertyInterface<GroupPropertyInterface<T>> {
    PropertyInterfaceImplement<T> Implement;

    GroupPropertyInterface(int iID,PropertyInterfaceImplement<T> iImplement) {
        super(iID);
        Implement=iImplement;
    }
}

abstract class GroupProperty<T extends PropertyInterface> extends MapProperty<GroupPropertyInterface<T>,T,T,GroupPropertyInterface<T>,Object> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty

    // оператор
    int Operator;

    GroupProperty(TableFactory iTableFactory,Property<T> iProperty,int iOperator) {
        super(iTableFactory);
        GroupProperty = iProperty;
        Operator = iOperator;
    }

    // группировочное св-во собсно должно быть не формулой
    Property<T> GroupProperty;

    @Override // так быстрее
    public ClassSet getValueClass(InterfaceClass<GroupPropertyInterface<T>> ClassImplement) {

        ClassSet Result = new ClassSet();
        // GetClassSet по идее ValueClass'ы проставил
        InterfaceClassSet<T> ValueClassSet = GroupProperty.getUniversalInterface();
        for(Map.Entry<GroupPropertyInterface<T>,ClassSet> Class : ClassImplement.entrySet())
            ValueClassSet = ValueClassSet.and(Class.getKey().Implement.mapGetClassSet(Class.getValue()));
        for(InterfaceClass<T> GroupImplement : ValueClassSet)
            Result.or(GroupProperty.getValueClass(GroupImplement));
        return Result;
    }

    InterfaceClassSet<GroupPropertyInterface<T>> getMapClassSet(MapRead<T> Read, InterfaceClass<T> InterfaceImplement) {
        // сначала делаем and всех classSet'ов, а затем getValueClass
        InterfaceClassSet<GroupPropertyInterface<T>> Result = new InterfaceClassSet<GroupPropertyInterface<T>>();
        InterfaceClassSet<T> GroupClassSet = new InterfaceClassSet<T>(InterfaceImplement);
        for(GroupPropertyInterface<T> Interface : Interfaces)
            GroupClassSet = GroupClassSet.and(Read.getImplementClassSet(Interface.Implement,ClassSet.universal));
        if(GroupClassSet.isEmpty()) return null;
        for(InterfaceClass<T> GroupImplement : GroupClassSet) {
            InterfaceClass<GroupPropertyInterface<T>> ValueClass = new InterfaceClass<GroupPropertyInterface<T>>();
            for(GroupPropertyInterface<T> Interface : Interfaces)
                ValueClass.put(Interface,Read.getImplementValueClass(Interface.Implement,GroupImplement));
            Result.or(new InterfaceClassSet<GroupPropertyInterface<T>>(ValueClass));
        }

        return Result;
    }

    List<GroupPropertyInterface> GetChangedProperties(DataSession Session) {
        List<GroupPropertyInterface> ChangedProperties = new ArrayList<GroupPropertyInterface>();
        // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
        for(GroupPropertyInterface Interface : Interfaces)
            if(Interface.Implement.mapHasChanges(Session)) ChangedProperties.add(Interface);

        return ChangedProperties;
    }

    Property<T> getMapProperty() {
        return GroupProperty;
    }

    Map<GroupPropertyInterface<T>, PropertyInterfaceImplement<T>> getMapImplements() {
        Map<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>> Result = new HashMap<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>>();
        for(GroupPropertyInterface<T> Interface : Interfaces)
            Result.put(Interface,Interface.Implement);
        return Result;
    }

    Collection<T> getMapInterfaces() {
        return GroupProperty.Interfaces;
    }

    InterfaceClassSet<GroupPropertyInterface<T>> getMapPropertyInterfaces(InterfaceClassSet<T> InterfaceDumb, InterfaceClassSet<GroupPropertyInterface<T>> ImplementDumb) {
        return ImplementDumb;
    }

    InterfaceClassSet<T> getMapPropertyImplements(InterfaceClassSet<T> InterfaceDumb, InterfaceClassSet<GroupPropertyInterface<T>> ImplementDumb) {
        return InterfaceDumb;
    }


    void putImplementsToQuery(JoinQuery<T, Object> Query, Object Value, MapRead<T> Read, Map<GroupPropertyInterface<T>, SourceExpr> Implements) {
        Query.addAll(Implements);
        Query.add(Value,Read.getMapExpr(GroupProperty,Query.MapKeys));
    }

    Map<Object, Type> getMapNullProps(Object Value) {
        Map<Object, Type> NullProps = new HashMap<Object,Type>();
        NullProps.put(Value, getType());
        InterfaceClass<GroupPropertyInterface<T>> InterfaceClass = getClassSet(ClassSet.universal).iterator().next();
        for(Map.Entry<GroupPropertyInterface<T>,ClassSet> Interface : InterfaceClass.entrySet())
            NullProps.put(Interface.getKey(),Interface.getValue().getType());
        return NullProps;
    }

    Object getDefaultObject() {
        return "grfield";
    }

    Source<GroupPropertyInterface<T>, Object> getMapSourceQuery(Object Value) {
        return new GroupQuery<Object,GroupPropertyInterface<T>,Object>(Interfaces,getMapQuery(getDB(),Value),Value,Operator);
    }

    Source<GroupPropertyInterface<T>, PropertyField> getGroupQuery(List<MapChangedRead<T>> ReadList, PropertyField Value, ChangeClassSet<GroupPropertyInterface<T>> ResultClass) {
        return new GroupQuery<Object,GroupPropertyInterface<T>,PropertyField>(Interfaces,getMapQuery(ReadList,Value,ResultClass,false),Value,Operator);
    }
}

class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    SumGroupProperty(TableFactory iTableFactory,Property<T> iProperty) {super(iTableFactory,iProperty,1);}

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        GroupProperty.setChangeType(RequiredTypes,1);

        for(GroupPropertyInterface<T> Interface : Interfaces)
            if(Interface.Implement instanceof PropertyMapImplement)
                (((PropertyMapImplement)Interface.Implement).Property).setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession Session, int ChangeType) {

        // конечный результат, с ключами и выражением
        UnionQuery<GroupPropertyInterface<T>,PropertyField> ResultQuery = new UnionQuery<GroupPropertyInterface<T>,PropertyField>(Interfaces,1);
        ChangeClassSet<GroupPropertyInterface<T>> ResultClass = new ChangeClassSet<GroupPropertyInterface<T>>();

        ResultQuery.add(getGroupQuery(getChangeMap(Session,1),ChangeTable.Value,ResultClass),1);
        ResultQuery.add(getGroupQuery(getChangeImplements(Session,0),ChangeTable.Value,ResultClass),1);
        ResultQuery.add(getGroupQuery(getPreviousImplements(Session),ChangeTable.Value,ResultClass),-1);

        return new Change(1,ResultQuery,ResultClass);
     }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 1;
    }
}


class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    MaxGroupProperty(TableFactory iTableFactory,Property<T> iProperty) {super(iTableFactory,iProperty,0);}

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        GroupProperty.setChangeType(RequiredTypes,2);

        for(GroupPropertyInterface<T> Interface : Interfaces)
            if(Interface.Implement instanceof PropertyMapImplement)
                ((PropertyMapImplement)Interface.Implement).Property.setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession Session, int ChangeType) {

        // делаем Full Join (на 3) :
        //      a) ушедшие (previmp и prevmap) = старые (sourceexpr) LJ (prev+change) (вообще и пришедшие <= старых)
        //      b) пришедшие (change) > старых (sourceexpr)
        if(caption.equals("Посл. строка") && BusinessLogics.ChangeDBIteration==10)
            caption = caption;

        ChangeClassSet<GroupPropertyInterface<T>> ResultClass = new ChangeClassSet<GroupPropertyInterface<T>>();

        PropertyField PrevMapValue = new PropertyField("drop",Type.Integer);

        UnionQuery<GroupPropertyInterface<T>,PropertyField> ChangeQuery = new UnionQuery<GroupPropertyInterface<T>,PropertyField>(Interfaces,3);

        ChangeQuery.add(getGroupQuery(getPreviousChange(Session),PrevMapValue,ResultClass),1);
        ChangeQuery.add(getGroupQuery(getChange(Session,0),ChangeTable.Value,ResultClass),1);

        // подозрительные на изменения ключи
        JoinQuery<GroupPropertyInterface<T>,PropertyField> SuspiciousQuery = new JoinQuery<GroupPropertyInterface<T>,PropertyField>(Interfaces);
        UniJoin<GroupPropertyInterface<T>,PropertyField> ChangeJoin = new UniJoin<GroupPropertyInterface<T>,PropertyField>(ChangeQuery,SuspiciousQuery,true);

        SourceExpr NewValue = ChangeJoin.Exprs.get(ChangeTable.Value);
        SourceExpr OldValue = ChangeJoin.Exprs.get(PrevMapValue);
        SourceExpr PrevValue = getSourceExpr(SuspiciousQuery.MapKeys,false);

        SuspiciousQuery.add(ChangeTable.Value,NewValue);
        SuspiciousQuery.add(PrevMapValue,OldValue);
        SuspiciousQuery.add(ChangeTable.PrevValue,PrevValue);

        SuspiciousQuery.add(new FieldOPWhere(
                new FieldExprCompareWhere(NewValue.getNullMinExpr(),PrevValue.getNullMinExpr(),FieldExprCompareWhere.GREATER),
                new FieldExprCompareWhere(OldValue.getNullMinExpr(),PrevValue.getNullMinExpr(),FieldExprCompareWhere.EQUALS),false));

        JoinQuery<GroupPropertyInterface<T>,PropertyField> UpdateQuery = new JoinQuery<GroupPropertyInterface<T>,PropertyField>(Interfaces);
        UniJoin<GroupPropertyInterface<T>, PropertyField> ChangesJoin = new UniJoin<GroupPropertyInterface<T>,PropertyField>(SuspiciousQuery,UpdateQuery,true);
        UpdateQuery.add(ChangeTable.PrevValue,ChangesJoin.Exprs.get(ChangeTable.PrevValue));
        List<MapChangedRead<T>> NewRead = new ArrayList<MapChangedRead<T>>(); NewRead.add(getPrevious(Session)); NewRead.addAll(getChange(Session,0));
        ChangeClassSet<GroupPropertyInterface<T>> NewClass = new ChangeClassSet<GroupPropertyInterface<T>>();
        UpdateQuery.add(ChangeTable.Value,(new UniJoin<GroupPropertyInterface<T>,PropertyField>(getGroupQuery(NewRead,ChangeTable.Value,NewClass),UpdateQuery,false)).Exprs.get(ChangeTable.Value));

        ResultClass.or(ResultClass.and(NewClass));
        return new Change(2,UpdateQuery,ResultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 2;
    }
}

// КОМБИНАЦИИ (ЛИНЕЙНЫЕ,MAX,OVERRIDE) принимают null на входе, по сути как Relation но работают на Or\FULL JOIN
// соответственно мн-во св-в полностью должно отображаться на интерфейсы

abstract class UnionProperty extends AggregateProperty<PropertyInterface> {

    UnionProperty(TableFactory iTableFactory,int iOperator) {
        super(iTableFactory);
        Operator = iOperator;
    }

    // имплементации св-в (полные)
    List<PropertyMapImplement<PropertyInterface,PropertyInterface>> Operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    int Operator;
    // коэффициенты
    Map<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> Coeffs = new HashMap<PropertyMapImplement<PropertyInterface, PropertyInterface>, Integer>();

    SourceExpr calculateSourceExpr(Map<PropertyInterface, SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "joinvalue";
        UnionQuery<PropertyInterface,String> ResultQuery = new UnionQuery<PropertyInterface,String>(Interfaces,Operator);
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : Operands) {
            JoinQuery<PropertyInterface,String> Query = new JoinQuery<PropertyInterface, String>(Interfaces);
            Query.add(ValueString,Operand.mapSourceExpr(Query.MapKeys,true));
            ResultQuery.add(Query,Coeffs.get(Operand));
        }

        return (new Join<PropertyInterface,String>(ResultQuery,JoinImplement,NotNull)).Exprs.get(ValueString);
    }

    public ClassSet getValueClass(InterfaceClass<PropertyInterface> ClassImplement) {
        // в отличии от Relation только когда есть хоть одно св-во
        ClassSet ResultClass = new ClassSet();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : Operands)
            ResultClass.or(Operand.mapGetValueClass(ClassImplement));
        return ResultClass;
    }

    public InterfaceClassSet<PropertyInterface> getClassSet(ClassSet ReqValue) {
        // в отличии от Relation игнорируем null
        InterfaceClassSet<PropertyInterface> Result = new InterfaceClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : Operands)
            Result.or(Operand.mapGetClassSet(ReqValue));
        return Result;
    }

    public ChangeClassSet<PropertyInterface> getChangeClass() {
        ChangeClassSet<PropertyInterface> Result = new ChangeClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : Operands)
            Result.or(Operand.mapGetChangeClass());
        return Result;
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = false;

        for(PropertyMapImplement Operand : Operands)
            Changed = Operand.mapFillChangedList(ChangedProperties, Changes, DefaultLinks) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    List<PropertyMapImplement<PropertyInterface,PropertyInterface>> getChangedProperties(DataSession Session) {

        List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProperties = new ArrayList<PropertyMapImplement<PropertyInterface,PropertyInterface>>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : Operands)
            if(Operand.mapHasChanges(Session)) ChangedProperties.add(Operand);

        return ChangedProperties;
    }

    // определяет ClassSet подмн-ва и что все операнды пересекаются
    ChangeClassSet<PropertyInterface> getChangeClass(DataSession Session,List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProps) {

        ChangeClassSet<PropertyInterface> Result = new ChangeClassSet<PropertyInterface>(new ClassSet(),getUniversalInterface());
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : ChangedProps)// {
//            if(!intersect(Session, Operand,ChangedProps)) return null;
            Result = Result.and(Operand.mapChangeClass(Session));
//        }

        return Result;
    }


    Change incrementChanges(DataSession Session, int ChangeType) {

        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        ChangeClassSet<PropertyInterface> ResultClass = new ChangeClassSet<PropertyInterface>();

        // неструктурно как и все оптимизации
        if(Operator==1 && ChangeType==1) {
            UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,1);

            for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : getChangedProperties(Session)) {
                JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);
                Query.add(ChangeTable.Value,Operand.mapChangeExpr(Session, Query.MapKeys, 1));
                ResultQuery.add(Query,Coeffs.get(Operand));

                ResultClass.or(Operand.mapChangeClass(Session));
            }

            return new Change(1,ResultQuery, ResultClass);
        } else {
            UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);
            ResultQuery.add(getChange(Session,ChangeType==1?1:0,ChangeTable.Value,ResultClass),1);
            if(ChangeType==2) ResultQuery.add(getChange(Session,2,ChangeTable.PrevValue,ResultClass),1);

            return new Change(ChangeType,ResultQuery,ResultClass);
        }
    }

    Source<PropertyInterface,PropertyField> getChange(DataSession Session, int MapType, PropertyField Value, ChangeClassSet<PropertyInterface> ResultClass) {

        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        ListIterator<List<PropertyMapImplement<PropertyInterface,PropertyInterface>>> il = SetBuilder.buildSubSetList(getChangedProperties(Session)).listIterator();
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement<PropertyInterface, PropertyInterface>> ChangedProps = il.next();

            // проверим что все попарно пересекаются по классам, заодно строим InterfaceClassSet<T> св-в
            ChangeClassSet<PropertyInterface> ChangeClass = getChangeClass(Session,ChangedProps);
            if(ChangeClass.isEmpty()) continue;

            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);
            UnionSourceExpr ResultExpr = new UnionSourceExpr(Operator);
            // именно в порядке операндов (для Overrid'а важно)
            for(PropertyMapImplement<PropertyInterface, PropertyInterface> Operand : Operands)
                if(ChangedProps.contains(Operand))
                    ResultExpr.Operands.put(Operand.mapChangeExpr(Session, Query.MapKeys, MapType),Coeffs.get(Operand));
                else { // AND'им как если Join результат
                    ChangeClassSet<PropertyInterface> LeftClass = ChangeClass.and(Operand.mapGetChangeClass());
                    if(!LeftClass.isEmpty()) {
                        SourceExpr OperandExpr = Operand.mapSourceExpr(Query.MapKeys,false);
                        if(Operator==2 && MapType==1) // если Override и 1 то нам нужно не само значение, а если не null то 0, иначе null (то есть не брать значение) {
                            OperandExpr = new CaseWhenSourceExpr(new SourceIsNullWhere(OperandExpr,false),new ValueSourceExpr(null,OperandExpr.getType()),new ValueSourceExpr(OperandExpr.getType().getEmptyValue(),OperandExpr.getType()));
                        ResultExpr.Operands.put(OperandExpr,Coeffs.get(Operand));
                        // значит может изменится Value на другое значение
                        ChangeClass.or(LeftClass);
                    }
                }

            Query.add(Value,ResultExpr);

            ResultQuery.add(Query,1);
            ResultClass.or(ChangeClass);
        }
        if(ResultQuery.Operands.isEmpty())
            return new EmptySource<PropertyInterface,PropertyField>(Interfaces,Collections.singletonMap(Value,getType()));

        return ResultQuery;
    }

    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, Collection<PropertyMapImplement<PropertyInterface,PropertyInterface>> Operands) {
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand : Operands) {
            if(Operand==IntersectOperand) return true;
            if(!intersect(Session, Operand,IntersectOperand)) return false;
        }
        return true;
    }

    // проверяет пересекаются по классам операнды или нет
    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand) {
        return (Session.Changes.AddClasses.size() > 0 && Session.Changes.RemoveClasses.size() > 0) ||
               !Operand.mapGetClassSet(ClassSet.universal).and(IntersectOperand.mapGetClassSet(ClassSet.universal)).isEmpty();
//        return true;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        for(PropertyMapImplement Operand : Operands)
            Operand.Property.setChangeType(RequiredTypes,IncrementType);
    }
}


class SumUnionProperty extends UnionProperty {

    SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory,1);}

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 1;
    }
}

class MaxUnionProperty extends UnionProperty {

    MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory,0);}

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        if(IncrementType.equals(1)) IncrementType = 2;
        super.fillRequiredChanges(IncrementType, RequiredTypes);    //To change body of overridden methods use File | Settings | File Templates.
    }

    Change incrementChanges(DataSession Session, int ChangeType) {
        if(ChangeType==1) ChangeType = 2;
        return super.incrementChanges(Session, ChangeType);    //To change body of overridden methods use File | Settings | File Templates.
    }
}

class OverrideUnionProperty extends UnionProperty {

    OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory,2);}

    private PropertyMapImplement<PropertyInterface,PropertyInterface> getOperand(Map<PropertyInterface, ObjectValue> keys) {

        InterfaceClass<PropertyInterface> changeClass = new InterfaceClass<PropertyInterface>();
        for(PropertyInterface iface : Interfaces)
            changeClass.put(iface,ClassSet.getUp(keys.get(iface).Class));

        for(int i=Operands.size()-1;i>=0;i--) {
            PropertyMapImplement<PropertyInterface,PropertyInterface> operand = Operands.get(i);
            if(!operand.mapGetValueClass(changeClass).isEmpty()) {
                return operand;
            }
        }

        return null;
    }

    @Override
    boolean allowChangeProperty(Map<PropertyInterface, ObjectValue> keys) {
        return getOperand(keys) != null;
    }

    @Override
    void changeProperty(Map<PropertyInterface, ObjectValue> keys, Object newValue, DataSession session) throws SQLException {

        PropertyMapImplement<PropertyInterface,PropertyInterface> operand = getOperand(keys);
        if (operand != null)
            operand.mapChangeProperty(keys,newValue,session);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
/*        for(PropertyMapImplement Operand : Operands)
            if(ChangedProps.contains(Operand.Property))
                ToWait.add(Operand.Property);
        return null;  //To change body of implemented methods use File | Settings | File Templates.*/
        return 0;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        if(IncrementType.equals(1)) IncrementType = 2;
        super.fillRequiredChanges(IncrementType, RequiredTypes);    //To change body of overridden methods use File | Settings | File Templates.
    }

    Change incrementChanges(DataSession Session, int ChangeType) {
        if(ChangeType==1) ChangeType = 2;
        return super.incrementChanges(Session, ChangeType);    //To change body of overridden methods use File | Settings | File Templates.
    }
}


// ФОРМУЛЫ

class FormulaPropertyInterface<P extends FormulaPropertyInterface<P>> extends PropertyInterface<P> {
//    Class Class;

    FormulaPropertyInterface(int iID) {
        super(iID);
//        Class = iClass;
    }
}

// вообще Collection
abstract class FormulaProperty<T extends FormulaPropertyInterface> extends AggregateProperty<T> {

    Class Value;

    FormulaProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;
    }

    public ClassSet getValueClass(InterfaceClass<T> ClassImplement) {
        if(ClassImplement.hasEmpty()) return new ClassSet();
        return ClassSet.getUp(Value);
    }

    public InterfaceClassSet<T> getClassSet(ClassSet ReqValue) {

        if(ReqValue.intersect(ClassSet.getUp(Value)))
            return getUniversalInterface();
        else
            return new InterfaceClassSet<T>();
    }

    public ChangeClassSet<T> getChangeClass() {
        return new ChangeClassSet<T>(ClassSet.getUp(Value),getUniversalInterface());
    }

    /*    public ClassSet getValueClass(InterfaceClass<T> ClassImplement) {

        ClassSet Result = new ClassSet();
        for(T Interface : Interfaces) {
            if(!ClassImplement.get(Interface).intersect(ClassSet.getUp(Interface.Class))) return new ClassSet();
            if(!(Interface.Class instanceof BitClass))
                Result.or(ClassSet.getUp(Interface.Class));
        }
        if(Result.isEmpty()) Result.or(new ClassSet(Class.bit));

        return Result;
    }

    InterfaceClass<T> getInterfaceSet() {
        InterfaceClass<T> ResultSet = new InterfaceClass<T>();
        for(T Interface : Interfaces)
            ResultSet.put(Interface,ClassSet.getUp(Interface.Class));
        return ResultSet;
    }

    public InterfaceClassSet<T> getClassSet(ClassSet ReqValue) {

        InterfaceClass<T> ResultSet = getInterfaceSet();
        if(getValueClass(ResultSet).intersect(ReqValue))
            return new InterfaceClassSet<T>(ResultSet);
        else
            return new InterfaceClassSet<T>();
    }

    public ChangeClassSet<T> getChangeClass() {

        InterfaceClass<T> ResultSet = getInterfaceSet();
        return new ChangeClassSet<T>(getValueClass(ResultSet),new InterfaceClassSet<T>(ResultSet));
    }
  */
    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
    }

    // не может быть изменений в принципе
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        return false;
    }

    Change incrementChanges(DataSession Session, int ChangeType) {
        return null;
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return null;
    }
}

class StringFormulaPropertyInterface extends FormulaPropertyInterface<StringFormulaPropertyInterface> {

    StringFormulaPropertyInterface(int iID) {
        super(iID);
    }
}

class StringFormulaProperty extends FormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    StringFormulaProperty(TableFactory iTableFactory, Class iValue, String iFormula) {
        super(iTableFactory,iValue);
        Formula = iFormula;
    }

    SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface, SourceExpr> JoinImplement,boolean NotNull) {

        FormulaSourceExpr Source = new FormulaSourceExpr(Formula,Value.getType());

        for(StringFormulaPropertyInterface Interface : Interfaces)
            Source.Params.put("prm"+(Interface.ID+1),JoinImplement.get(Interface));

        return Source;
    }
}

class WhereStringFormulaProperty extends StringFormulaProperty {

    WhereStringFormulaProperty(TableFactory iTableFactory, String iFormula) {
        super(iTableFactory, Class.bit, iFormula);
    }

/*    public ClassSet getValueClass(InterfaceClass<StringFormulaPropertyInterface> ValueClass) {

        for(StringFormulaPropertyInterface Interface : Interfaces)
            if(!ValueClass.get(Interface).intersect(ClassSet.getUp(Interface.Class))) return new ClassSet();

        return new ClassSet(Class.bit);
    }*/

    SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface, SourceExpr> JoinImplement, boolean NotNull) {

        FormulaSourceWhere Source = new FormulaSourceWhere(Formula);

        for(StringFormulaPropertyInterface Interface : Interfaces)
            Source.Params.put("prm"+(Interface.ID+1),JoinImplement.get(Interface));

        return new FormulaWhereSourceExpr(Source,NotNull);
    }
}


class MultiplyFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    MultiplyFormulaProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory,iValue);
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, SourceExpr> JoinImplement,boolean NotNull) {

        MultiplySourceExpr Source = new MultiplySourceExpr(Value.getType());
        for(FormulaPropertyInterface Interface : Interfaces)
            Source.Operands.add(JoinImplement.get(Interface));

        return Source;
    }
}

// изменения данных
class DataChanges {
    Set<DataProperty> Properties = new HashSet<DataProperty>();

    Set<Class> AddClasses = new HashSet<Class>();
    Set<Class> RemoveClasses = new HashSet<Class>();

    DataChanges copy() {
        DataChanges CopyChanges = new DataChanges();
        CopyChanges.Properties.addAll(Properties);
        CopyChanges.AddClasses.addAll(AddClasses);
        CopyChanges.RemoveClasses.addAll(RemoveClasses);
        return CopyChanges;
    }

    public boolean hasChanges() {
        return !(Properties.isEmpty() && AddClasses.isEmpty() && RemoveClasses.isEmpty());
    }
}

interface PropertyUpdateView {

    Collection<Property> getUpdateProperties();
}

class DataSession  {

    Connection Connection;
    SQLSyntax Syntax;

    DataChanges Changes = new DataChanges();
    Map<PropertyUpdateView,DataChanges> IncrementChanges = new HashMap<PropertyUpdateView,DataChanges>();

    Map<Property, Property.Change> PropertyChanges = new HashMap<Property, Property.Change>();
    <P extends PropertyInterface> Property<P>.Change getChange(Property<P> Property) {
        return PropertyChanges.get(Property);
    }

    Map<Class,ClassSet> AddChanges = new HashMap<Class, ClassSet>();
    Map<Class,ClassSet> RemoveChanges = new HashMap<Class, ClassSet>();
    Map<DataProperty, ChangeClassSet<DataPropertyInterface>> DataChanges = new HashMap<DataProperty, ChangeClassSet<DataPropertyInterface>>();

    TableFactory TableFactory;
    ObjectClass ObjectClass;

    int ID = 0;

    DataSession(DataAdapter Adapter,int iID,TableFactory iTableFactory,ObjectClass iObjectClass) throws SQLException{

        ID = iID;
        Syntax = Adapter;
        TableFactory = iTableFactory;
        ObjectClass = iObjectClass;

        try {
            Connection = Adapter.startConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        TableFactory.fillSession(this);
    }

    void restart(boolean Cancel) throws SQLException {

        if(Cancel)
            for(DataChanges ViewChanges : IncrementChanges.values()) {
                ViewChanges.Properties.addAll(Changes.Properties);
                ViewChanges.AddClasses.addAll(Changes.AddClasses);
                ViewChanges.RemoveClasses.addAll(Changes.RemoveClasses);
            }

        TableFactory.clearSession(this);
        Changes = new DataChanges();
        NewClasses = new HashMap<Integer,Class>();
        BaseClasses = new HashMap<Integer,Class>();

        PropertyChanges = new HashMap<Property, Property.Change>();
        AddChanges = new HashMap<Class, ClassSet>();
        RemoveChanges = new HashMap<Class, ClassSet>();
        DataChanges = new HashMap<DataProperty, ChangeClassSet<DataPropertyInterface>>();
    }

    Map<Integer,Class> NewClasses = new HashMap<Integer,Class>();
    // классы на момент выполнения
    Map<Integer,Class> BaseClasses = new HashMap<Integer,Class>();

    private void putClassChanges(Set<Class> Changes,Class PrevClass,Map<Class,ClassSet> To) {
        for(Class Change : Changes) {
            ClassSet PrevChange = To.get(Change);
            if(PrevChange==null) PrevChange = new ClassSet();
            PrevChange.or(new ClassSet(PrevClass));
            To.put(Change,PrevChange);
        }
    }

    void changeClass(Integer idObject,Class ToClass) throws SQLException {
        if(ToClass==null) ToClass = Class.base;

        Set<Class> AddClasses = new HashSet<Class>();
        Set<Class> RemoveClasses = new HashSet<Class>();
        Class PrevClass = getObjectClass(idObject);
        ToClass.GetDiffSet(PrevClass,AddClasses,RemoveClasses);

        putClassChanges(AddClasses,PrevClass,AddChanges);
        TableFactory.AddClassTable.changeClass(this,idObject,AddClasses,false);
        TableFactory.RemoveClassTable.changeClass(this,idObject,AddClasses,true);

        putClassChanges(RemoveClasses,PrevClass,RemoveChanges);
        TableFactory.RemoveClassTable.changeClass(this,idObject,RemoveClasses,false);
        TableFactory.AddClassTable.changeClass(this,idObject,RemoveClasses,true);

        if(!NewClasses.containsKey(idObject))
            BaseClasses.put(idObject,PrevClass);
        NewClasses.put(idObject,ToClass);

        Changes.AddClasses.addAll(AddClasses);
        Changes.RemoveClasses.addAll(RemoveClasses);

        for(DataChanges ViewChanges : IncrementChanges.values()) {
            ViewChanges.AddClasses.addAll(AddClasses);
            ViewChanges.RemoveClasses.addAll(RemoveClasses);
        }
    }

    void changeProperty(DataProperty Property, Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue) throws SQLException {

        // запишем в таблицу
        // также заодно новые классы считаем
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InterfaceClass<DataPropertyInterface> InterfaceClass = new InterfaceClass<DataPropertyInterface>();
        for(Map.Entry<KeyField,DataPropertyInterface> Field : (Set<Map.Entry<KeyField,DataPropertyInterface>>)Property.DataTableMap.entrySet()) {
            Integer idObject = Keys.get(Field.getValue()).idObject;
            InsertKeys.put(Field.getKey(), idObject);
            InterfaceClass.put(Field.getValue(),getBaseClassSet(idObject));
        }

        InsertKeys.put(Property.DataTable.Property,Property.ID);

        Map<PropertyField,Object> InsertValues = new HashMap<PropertyField,Object>();
        InsertValues.put(Property.DataTable.Value,NewValue);

        ClassSet ValueClass = Property.getBaseClass();
        if(ValueClass.intersect(ClassSet.getUp(ObjectClass)))
            ValueClass = getBaseClassSet((Integer) NewValue);

        UpdateInsertRecord(Property.DataTable,InsertKeys,InsertValues);

        // пометим изменения
        Changes.Properties.add(Property);

        ChangeClassSet<DataPropertyInterface> DataChange = DataChanges.get(Property);
        if(DataChange==null) DataChange = new ChangeClassSet<DataPropertyInterface>();
        DataChange.or(new ChangeClass<DataPropertyInterface>(new InterfaceClassSet<DataPropertyInterface>(InterfaceClass),ValueClass));
        DataChanges.put(Property,DataChange);

        for(DataChanges ViewChanges : IncrementChanges.values())
            ViewChanges.Properties.add(Property);
    }

    Class readClass(Integer idObject) throws SQLException {
        return ObjectClass.findClassID(TableFactory.ObjectTable.GetClassID(this,idObject));
    }

    Class getObjectClass(Integer idObject) throws SQLException {
        Class NewClass = NewClasses.get(idObject);
        if(NewClass==null)
            NewClass = readClass(idObject);
        if(NewClass==null)
            NewClass = Class.base;
        return NewClass;
    }

    ClassSet getBaseClassSet(Integer idObject) throws SQLException {
        if(idObject==null) return new ClassSet();
        Class BaseClass = BaseClasses.get(idObject);
        if(BaseClass==null)
            BaseClass = readClass(idObject);
        return new ClassSet(BaseClass);
    }

    // последний параметр
    List<Property> update(PropertyUpdateView ToUpdate,Collection<Class> UpdateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges ToUpdateChanges = IncrementChanges.get(ToUpdate);
        if(ToUpdateChanges==null) ToUpdateChanges = Changes;

        Collection<Property> ToUpdateProperties = ToUpdate.getUpdateProperties();
        // сначала читаем инкрементные св-ва которые изменились
        List<Property> IncrementUpdateList = BusinessLogics.getChangedList(ToUpdateProperties,ToUpdateChanges);
        List<Property> UpdateList = BusinessLogics.getChangedList(IncrementUpdateList,Changes);

        Map<Property,Integer> RequiredTypes = new HashMap<Property,Integer>();
        // пробежим вперед пометим свойства которые изменились, но неясно на что
        for(Property Property : UpdateList)
            RequiredTypes.put(Property,ToUpdateProperties.contains(Property)?0:null);
        Map<Property, Integer> IncrementTypes = getIncrementTypes(UpdateList, RequiredTypes);

        // запускаем IncrementChanges для этого списка
        for(Property Property : UpdateList) {
            Property.Change Change = Property.incrementChanges(this,IncrementTypes.get(Property));
            // подгоняем тип
            Change.correct(RequiredTypes.get(Property));
//            System.out.println("inctype"+Property.caption+" "+IncrementTypes.get(Property));
            Change.save(this);
/*            System.out.println(Property.caption+" - CHANGES");
            Property.OutChangesTable(this);
            System.out.println(Property.caption+" - CURRENT");
            Property.Out(this);
            Change.checkClasses(this);*/
            PropertyChanges.put(Property,Change);
        }

        UpdateClasses.addAll(ToUpdateChanges.AddClasses);
        UpdateClasses.addAll(ToUpdateChanges.RemoveClasses);

        // сбрасываем лог
        IncrementChanges.put(ToUpdate,new DataChanges());

        return IncrementUpdateList;
    }

    // определяет на что считаться 0,1,2
    private Map<Property, Integer> getIncrementTypes(List<Property> UpdateList, Map<Property, Integer> RequiredTypes) {
        // бежим по списку (в обратном порядке) заполняем требования,
        Collections.reverse(UpdateList);
        // на какие значения читаться Persistent'ам
        Map<Property,Integer> IncrementTypes = new HashMap<Property,Integer>();
        // Waiter'ы св-ва которые ждут определившехся на выполнение св-в : не persistent и не 2
        Set<Property> ToWait = null;
        Map<Property,Set<Property>> Waiters = new HashMap<Property, Set<Property>>();
        for(Property Property : UpdateList) {
            Integer IncType = RequiredTypes.get(Property);
            // сначала проверим на Persistent и на "альтруистические" св-ва
            if(IncType==null || Property.IsPersistent()) {
                ToWait = new HashSet<Property>();
                IncType = Property.getIncrementType(UpdateList, ToWait);
            }
            // если определившееся (точно 0 или 1) запустим Waiter'ов, соответственно вычистим
            if(IncType==null || (!Property.IsPersistent() && !IncType.equals(2))) {
                for(Iterator<Map.Entry<Property,Set<Property>>> ie = Waiters.entrySet().iterator();ie.hasNext();) {
                    Map.Entry<Property,Set<Property>> Wait = ie.next();
                    if(Wait.getValue().contains(Property))
                        if(IncType==null) // докидываем еще Waiter'ов
                            Wait.getValue().addAll(ToWait);
                        else { // нашли нужный тип, remove'ся
                            fillChanges(Wait.getKey(), IncType, RequiredTypes, IncrementTypes);
                            ie.remove();
                        }
                }
            }
            if(IncType!=null)
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            else // св-во не знает пока чего хочет
                Waiters.put(Property, ToWait);
        }
        Collections.reverse(UpdateList);
        // еше могут остаться Waiter'ы, тогда возьмем первую не 2, иначе возьмем 0 (все чтобы еще LJ минимизировать)
        for(Property Property : UpdateList) {
            Integer IncType = IncrementTypes.get(Property);
            if(IncType==null) {
                for(Property WaitProperty : Waiters.get(Property)) {
                    Integer WaitType = IncrementTypes.get(WaitProperty);
                    if(!WaitType.equals(2)) IncType = WaitType;
                }
                if(IncType==null) IncType = 0;
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            }
        }
        return IncrementTypes;
    }

    private void fillChanges(Property Property, Integer incrementType, Map<Property, Integer> requiredTypes, Map<Property, Integer> incrementTypes) {
        incrementTypes.put(Property, incrementType);
        Property.fillRequiredChanges(incrementType, requiredTypes);
    }

    void saveClassChanges() throws SQLException {

        for(Integer idObject : NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
            InsertKeys.put(TableFactory.ObjectTable.Key, idObject);

            Map<PropertyField,Object> InsertProps = new HashMap<PropertyField,Object>();
            Class ChangeClass = NewClasses.get(idObject);
            InsertProps.put(TableFactory.ObjectTable.Class,ChangeClass!=null?ChangeClass.ID:null);

            UpdateInsertRecord(TableFactory.ObjectTable,InsertKeys,InsertProps);
        }
    }

    // записывается в запрос с map'ом
    <P extends PropertyInterface> SourceExpr getSourceExpr(Property<P> Property,Map<P,SourceExpr> JoinImplement,boolean NotNull) {

        if(PropertyChanges.containsKey(Property)) {
            String Value = "joinvalue";

            UnionQuery<P,String> UnionQuery = new UnionQuery<P,String>(Property.Interfaces,3);

            JoinQuery<P,String> SourceQuery = new JoinQuery<P,String>(Property.Interfaces);
            SourceQuery.add(Value,Property.getSourceExpr(SourceQuery.MapKeys,true));
            UnionQuery.add(SourceQuery,1);

            JoinQuery<P,String> NewQuery = new JoinQuery<P,String>(Property.Interfaces);
            NewQuery.add(Value,getChange(Property).getExpr(NewQuery.MapKeys,0));
            UnionQuery.add(NewQuery,1);

            return (new Join<P,String>(UnionQuery,JoinImplement,NotNull)).Exprs.get(Value);
        } else
            return Property.getSourceExpr(JoinImplement,NotNull);
    }

    boolean InTransaction = false;

    void startTransaction() throws SQLException {
        InTransaction = true;

        if(!Syntax.noAutoCommit())
            Execute(Syntax.startTransaction());
    }

    void rollbackTransaction() throws SQLException {
        Execute(Syntax.rollbackTransaction());

        InTransaction = false;
    }

    void commitTransaction() throws SQLException {
        Execute(Syntax.commitTransaction());

        InTransaction = false;
    }

    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.Keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(Syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(Syntax);
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY " + Syntax.getClustered() + " (" + KeyString + ")";

        try {
            Execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        Execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");

        int IndexNum = 1;
        for(List<PropertyField> Index : Table.Indexes) {
            String Columns = "";
            for(PropertyField IndexField : Index)
                Columns = (Columns.length()==0?"":Columns+",") + IndexField.Name;

            Execute("CREATE INDEX "+Table.Name+"_idx_"+(IndexNum++)+" ON "+Table.Name+" ("+Columns+")");
        }
    }

    void CreateTemporaryTable(SessionTable Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.Keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(Syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(Syntax);

        try {
            Execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

        Execute(Syntax.getCreateSessionTable(Table.Name,CreateString,"CONSTRAINT PK_S_" + ID +"_T_" + Table.Name + " PRIMARY KEY " + Syntax.getClustered() + " (" + KeyString + ")"));
    }

    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
//        System.out.println(ExecuteString+Syntax.getCommandEnd());
        try {
            Statement.execute(ExecuteString+Syntax.getCommandEnd());
//        } catch(SQLException e) {
//            if(!ExecuteString.startsWith("DROP") && !ExecuteString.startsWith("CREATE")) {
//            System.out.println(ExecuteString+Syntax.getCommandEnd());
//            e = e;
//            }
        } finally {
            Statement.close();
        }
        if(!InTransaction && Syntax.noAutoCommit())
            Statement.execute(Syntax.commitTransaction()+Syntax.getCommandEnd());

        try {
            Statement.close();
        } catch (SQLException e) {
            e.getErrorCode();
        }
    }

    void InsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        String InsertString = "";
        String ValueString = "";

        // пробежим по KeyFields'ам
        for(KeyField Key : Table.Keys) {
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }

        // пробежим по Fields'ам
        for(PropertyField Prop : PropFields.keySet()) {
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+TypedObject.getString(PropFields.get(Prop),Prop.Type,Syntax);
        }

        Execute("INSERT INTO "+Table.getSource(Syntax)+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>();

        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table,true);
        // сначала закинем KeyField'ы и прогоним Select
        for(KeyField Key : Table.Keys)
            TableJoin.Joins.put(Key,new ValueSourceExpr(KeyFields.get(Key),Key.Type));
        IsRecQuery.add(TableJoin);

        if(IsRecQuery.executeSelect(this).size()>0) {
            Map<PropertyField,TypedObject> TypedPropFields = new HashMap<PropertyField,TypedObject>();
            for(Map.Entry<PropertyField,Object> MapProp : PropFields.entrySet())
                TypedPropFields.put(MapProp.getKey(),new TypedObject(MapProp.getValue(),MapProp.getKey().Type));
            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,new DumbSource<KeyField,PropertyField>(KeyFields,TypedPropFields)));
        } else
            // делаем Insert
            InsertRecord(Table,KeyFields,PropFields);
    }

    void deleteKeyRecords(Table Table,Map<KeyField,Integer> Keys) throws SQLException {
 //       Execute(Table.GetDelete());
        String DeleteWhere = "";
        for(Map.Entry<KeyField,Integer> DeleteKey : Keys.entrySet())
            DeleteWhere = (DeleteWhere.length()==0?"":DeleteWhere+" AND ") + DeleteKey.getKey().Name + "=" + DeleteKey.getValue();

        Execute("DELETE FROM "+Table.getSource(Syntax)+(DeleteWhere.length()==0?"":" WHERE "+DeleteWhere));
    }

    void UpdateRecords(ModifyQuery Modify) throws SQLException {
//        try {
            Execute(Modify.getUpdate(Syntax));
//        } catch(SQLException e) {
//            Execute(Modify.getUpdate(Syntax));
//        }
    }

    void InsertSelect(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertSelect(Syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    void ModifyRecords(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertLeftKeys(Syntax));
        Execute(Modify.getUpdate(Syntax));
    }

    void close() throws SQLException {
        Connection.close();
    }

    public boolean hasChanges() {
        return Changes.hasChanges();
    }
}

class MapRead<P extends PropertyInterface> {
    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement,Map<P,SourceExpr> JoinImplement) {
        return Implement.mapSourceExpr(JoinImplement,true);
    }

    <M extends PropertyInterface> SourceExpr getMapExpr(Property<M> MapProperty,Map<M,SourceExpr> JoinImplement) {
        return MapProperty.getSourceExpr(JoinImplement,true);
    }

    // разные классы считывает

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement,InterfaceClass<P> ClassImplement) {
        return Implement.mapGetValueClass(ClassImplement);
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        return Implement.mapGetClassSet(ReqValue);
    }

    <M extends PropertyInterface> ChangeClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        return MapProperty.getChangeClass();
    }
}

class MapChangedRead<P extends PropertyInterface> extends MapRead<P> {

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, Collection<PropertyInterfaceImplement<P>> iImplementChanged) {
        Session = iSession;
        MapChanged = iMapChanged;
        MapType = iMapType;
        ImplementType = iImplementType;
        ImplementChanged = iImplementChanged;
    }

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, PropertyInterfaceImplement<P> iImplementChanged) {
        this(iSession,iMapChanged,iMapType,iImplementType,Collections.singleton(iImplementChanged));
    }

    DataSession Session;

    boolean MapChanged;
    // 0 - J P
    // 1 - просто NULL
    // 2 - NULL JOIN P (то есть Join'им но null'им)
    int MapType;

    Collection<PropertyInterfaceImplement<P>> ImplementChanged;
    int ImplementType;

    // проверяет изменились ли вообще то что запрашивается
    <M extends PropertyInterface> boolean check(Property<M> MapProperty) {
        for(PropertyInterfaceImplement<P> Implement : ImplementChanged)
            if(!Implement.mapHasChanges(Session)) return false;
        return !(MapChanged && !Session.PropertyChanges.containsKey(MapProperty));
    }

    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement, Map<P, SourceExpr> JoinImplement) {
        if(ImplementChanged.contains(Implement))
            return Implement.mapChangeExpr(Session, JoinImplement, ImplementType);
        else
            return super.getImplementExpr(Implement, JoinImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    <M extends PropertyInterface> SourceExpr getMapExpr(Property<M> MapProperty, Map<M, SourceExpr> JoinImplement) {
        if(MapChanged) {
            SourceExpr MapExpr = Session.getChange(MapProperty).getExpr(JoinImplement,MapType==3?0:MapType);
            if(MapType==3)
                return new NullJoinSourceExpr(MapExpr);
            else
                return MapExpr;    
        } else {
            if(MapType==1)
                return new NullMapSourceExpr<M>(JoinImplement,MapProperty.getType());
            else {
                SourceExpr MapExpr = MapProperty.getSourceExpr(JoinImplement,true);
                if(MapType==2)
                    return new NullJoinSourceExpr(MapExpr);
                else
                    return MapExpr;
            }
        }
    }

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement, InterfaceClass<P> ClassImplement) {
        if(ImplementChanged.contains(Implement) && ImplementType!=2) {
            return Implement.mapChangeValueClass(Session, ClassImplement);
        } else
            return super.getImplementValueClass(Implement, ClassImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        if(ImplementChanged.contains(Implement)) // если ImplementType=2 то плевать какой класс
            return Implement.mapChangeClassSet(Session, ImplementType==2?ClassSet.universal:ReqValue);
        else
            return super.getImplementClassSet(Implement, ReqValue);    //To change body of overridden methods use File | Settings | File Templates.
    }

    <M extends PropertyInterface> ChangeClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        if(MapChanged) {
            ChangeClassSet<M> MapChange = Session.getChange(MapProperty).Classes;
            if(MapType>=2) // если старые затираем возвращаем ссылку на nullClass
                return ChangeClassSet.getNullClass(MapChange);
            else
                return MapChange;
        } else {
            if(MapType==2) // если 2 то NullClass
                return ChangeClassSet.getNullClass(MapProperty);
            else
            if(MapType==1)
                return new ChangeClassSet<M>(new ClassSet(),MapProperty.getUniversalInterface());
            else
                return super.getMapChangeClass(MapProperty);
        }
    }
}

// св-ва которые связывают другие св-ва друг с другом
// ClassInterface = T - Join, M - Group
// ImplementClass = M - Join, T - Group
// ObjectMapClass = PropertyField - Join, Object - Group
abstract class MapProperty<T extends PropertyInterface,M extends PropertyInterface,IN extends PropertyInterface,IM extends PropertyInterface,OM> extends AggregateProperty<T> {

    MapProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    // получает св-во для Map'а
    // Join - return Implements.Property
    // Group - return GroupProperty
    abstract Property<M> getMapProperty();

    // получает список имплементаций
    // Join - return Implements.Mapping
    // Group бежит по GroupPropertyInterface и возвращает сформированный Map
    abstract Map<IM,PropertyInterfaceImplement<IN>> getMapImplements();

    // получает список интерфейсов
    // Join - return Interfaces
    // Group - return GroupProperty.Interfaces
    abstract Collection<IN> getMapInterfaces();

    // транслирует\выбирает нужный класс - трансляторы для выбора
    // для Join - возвращает интерфейсы
    // для Group - наоборот
    abstract InterfaceClassSet<T> getMapPropertyInterfaces(InterfaceClassSet<IN> InterfaceDumb, InterfaceClassSet<IM> ImplementDumb);
    abstract InterfaceClassSet<M> getMapPropertyImplements(InterfaceClassSet<IN> InterfaceDumb, InterfaceClassSet<IM> ImplementDumb);

    // по кдассам функционал - могут возвращать null'ы
    abstract InterfaceClassSet<T> getMapClassSet(MapRead<IN> Read,InterfaceClass<M> InterfaceImplement);
    ChangeClassSet<T> getMapChangeClass(MapRead<IN> Read) {
        ChangeClassSet<T> Result = new ChangeClassSet<T>();
        boolean Empty = true;
        for(ChangeClass<M> Change : Read.getMapChangeClass(getMapProperty()))
            for(InterfaceClass<M> InterfaceChange : Change.Interface) {
                InterfaceClassSet<T> ResultInterface = getMapClassSet(Read, InterfaceChange);
                if(ResultInterface!=null) {
                    Empty = false;
                    Result.or(new ChangeClass<T>(ResultInterface,Change.Value));
                }
            }
        if(Empty) return null;
        return Result;
    }

    public InterfaceClassSet<T> getClassSet(ClassSet ReqValue) {
        MapRead<IN> Read = new MapRead<IN>();
        InterfaceClassSet<T> Result = new InterfaceClassSet<T>();
        for(InterfaceClass<M> InterfaceImplement : getMapProperty().getClassSet(ReqValue))
            Result.or(getMapClassSet(Read,InterfaceImplement));
        return Result;
    }
    public ChangeClassSet<T> getChangeClass() {
        return getMapChangeClass(new MapRead<IN>());
    }
    public ClassSet getValueClass(InterfaceClass<T> ClassImplement) {
        return getChangeClass().getValueClass(ClassImplement);
    }


    // "сохраняет" имплементации в запрос
    // Join - закидываем в Value getExpr'ы (Changed,SourceExpr) map'a импллементаций
    // Group - закидываем в запрос map имплементаций
    //          закидываем в Value getMapExpr'ы (Changed,SourceExpr) map'а интерфейсов (Query.MapKeys)
    abstract void putImplementsToQuery(JoinQuery<IN,OM> Query,OM Value,MapRead<IN> Read,Map<IM,SourceExpr> Implements);

    // ВЫПОЛНЕНИЕ ИТЕРАЦИИ
    JoinQuery<IN,OM> getMapQuery(MapRead<IN> Read,OM Value) {

        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        JoinQuery<IN,OM> Query = new JoinQuery<IN,OM>(getMapInterfaces());

        // далее создается для getMapImplements - map <ImplementClass,SourceExpr> имплементаций - по getExpr'ы (Changed,SourceExpr) с переданным map интерфейсов
        Map<IM,SourceExpr> ImplementSources = new HashMap<IM,SourceExpr>();
        for(Map.Entry<IM,PropertyInterfaceImplement<IN>> Implement : getMapImplements().entrySet())
            ImplementSources.put(Implement.getKey(),Read.getImplementExpr(Implement.getValue(),Query.MapKeys));

        putImplementsToQuery(Query,Value,Read,ImplementSources);
        return Query;
    }

    // получает св-ва для запроса
    abstract Map<OM, Type> getMapNullProps(OM Value);

    // ВЫПОЛНЕНИЕ СПИСКА ИТЕРАЦИЙ

    Source<IN,OM> getMapQuery(List<MapChangedRead<IN>> ReadList, OM Value, ChangeClassSet<T> ReadClass, boolean Sum) {

        // делаем getQuery для всех итераций, после чего Query делаем Union на 3, InterfaceClassSet на AND(*), Value на AND(*)
        UnionQuery<IN, OM> ListQuery = new UnionQuery<IN, OM>(getMapInterfaces(),Sum?1:3);
        for(MapChangedRead<IN> Read : ReadList)
            if(Read.check(getMapProperty())) {
                ChangeClassSet<T> ChangeClass = getMapChangeClass(Read);
                if(ChangeClass!=null) {
                    ReadClass.or(ChangeClass);
                    ListQuery.add(getMapQuery(Read,Value),1);
                }
            }
        if(ListQuery.Operands.isEmpty())
            return new EmptySource<IN,OM>(getMapInterfaces(),getMapNullProps(Value));

        return ListQuery;
    }

    // get* получают списки итераций чтобы потом отправить их на выполнение:

    List<MapChangedRead<IN>> getImplementSet(DataSession Session, List<PropertyInterfaceImplement<IN>> SubSet, int ImplementType, boolean NotNull) {
        List<MapChangedRead<IN>> Result = new ArrayList<MapChangedRead<IN>>();
        if(ImplementType==2) // DEBUG
            throw new RuntimeException("по идее не должно быть");
        if(!(NotNull && SubSet.size()==1)) { // сначала "зануляем" ( пропускаем NotNull только одной размерности, теоретически можно доказать(
            if(implementAllInterfaces()) // просто без Join'a
                Result.add(new MapChangedRead<IN>(Session, false, 1, ImplementType, SubSet));
            else {
                Result.add(new MapChangedRead<IN>(Session, true, 3, 2, SubSet));
                Result.add(new MapChangedRead<IN>(Session, false, 2, 2, SubSet));
            }
        }
        // затем Join'им
        Result.add(new MapChangedRead<IN>(Session, false, 0, ImplementType, SubSet));

        return Result;
    }

    // новое состояние
    List<MapChangedRead<IN>> getChange(DataSession Session,int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values())) {
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, 0, false));
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        }
        return ChangedList;
    }

    // новое состояние с измененным основным значением
    // J - C (0,1) - SS+ (0)
    List<MapChangedRead<IN>> getChangeMap(DataSession Session, int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        return ChangedList;
    }
    // новое значение для имплементаций, здесь если не все имплементации придется извращаться и exclude'ать все не измененные выражения
    // LJ - P - SS (0,1)
    List<MapChangedRead<IN>> getChangeImplements(DataSession Session,int ImplementType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, ImplementType, true));

        return ChangedList;
    }
    // предыдущие значения по измененным объектам
    // J - P - L(2)
    List<MapChangedRead<IN>> getPreviousImplements(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(PropertyInterfaceImplement<IN> Implement : getMapImplements().values())
            ChangedList.add(new MapChangedRead<IN>(Session,false,0,2,Implement));
        return ChangedList;
    }
    // предыдущие значения
    List<MapChangedRead<IN>> getPreviousChange(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = getPreviousImplements(Session);
        ChangedList.add(new MapChangedRead<IN>(Session,true,2,0,new ArrayList<PropertyInterfaceImplement<IN>>()));
        return ChangedList;
    }
    // чтобы можно было бы использовать в одном списке
    MapChangedRead<IN> getPrevious(DataSession Session) {
        return new MapChangedRead<IN>(Session,false,0,0,new ArrayList<PropertyInterfaceImplement<IN>>());
    }
    // значение из базы (можно и LJ)
    // J - P - P
    MapRead<IN> getDB() {
        return new MapRead<IN>();
    }

    // получает источник для данных
    abstract OM getDefaultObject();
    abstract Source<T,OM> getMapSourceQuery(OM Value);

    SourceExpr calculateSourceExpr(Map<T, SourceExpr> JoinImplement, boolean NotNull) {
        OM Value = getDefaultObject();
        return (new Join<T,OM>(getMapSourceQuery(Value),JoinImplement,NotNull)).Exprs.get(Value);
    }

    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Set<DataProperty> DefaultLinks) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = getMapProperty().fillChangedList(ChangedProperties, Changes, DefaultLinks);

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            Changed = Implement.mapFillChangedList(ChangedProperties, Changes, DefaultLinks) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    boolean containsImplement(Collection<Property> Properties) {
        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && Properties.contains(((PropertyMapImplement)Implement).Property))
                return true;
        return false;
    }

    boolean implementAllInterfaces() {

        if(getMapProperty() instanceof WhereStringFormulaProperty) return false;

        Set<PropertyInterface> ImplementInterfaces = new HashSet<PropertyInterface>();
        for(PropertyInterfaceImplement<IN> InterfaceImplement : getMapImplements().values()) {
            if(InterfaceImplement instanceof PropertyMapImplement)
                ImplementInterfaces.addAll(((PropertyMapImplement<?,IN>)InterfaceImplement).Mapping.values());
        }

        return ImplementInterfaces.size()==getMapInterfaces().size();
    }

}
