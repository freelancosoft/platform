package platform.server.session;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.DataClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ObjectClassProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.*;
import java.sql.SQLException;

import static platform.base.BaseUtils.*;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(List<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(keys, Collections.singletonList("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }

    public void modifyRecord(SQLSession session, Map<K, DataObject> keyFields, ObjectValue propertyValue, Modify type) throws SQLException {
        modifyRecord(session, keyFields, Collections.singletonMap("value", propertyValue), type);
    }

    public void modifyRows(SQLSession session, Map<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, Modify type, QueryEnvironment env) throws SQLException {
        modifyRows(session, new Query<K, String>(mapKeys, expr, "value", where), baseClass, type, env);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        Map<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<P>(mapKeys, join.getExpr("value"), join.getWhere());
    }

    public <B> ClassWhere<B> getClassWhere(Map<K, ? extends B> remapKeys, B mapProp) {
        return getClassWhere("value", remapKeys, mapProp);
    }

    public void fixKeyClasses(ClassWhere<K> classes) {
        table = table.fixKeyClasses(classes.remap(reverse(mapKeys)));
    }

    public void updateAdded(SQLSession sql, BaseClass baseClass, int count) throws SQLException {
        updateAdded(sql, baseClass, "value", count);
    }

    public void updateCurrentClasses(DataSession session) throws SQLException {
        table = table.updateCurrentClasses(session);
    }
}
