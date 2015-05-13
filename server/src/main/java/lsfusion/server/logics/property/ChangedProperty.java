package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.ChangedDrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.infer.*;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.session.*;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ChangedProperty<T extends PropertyInterface> extends SessionCalcProperty<T> {

    private final IncrementType type;
    private final PrevScope scope;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public ChangedProperty(CalcProperty<T> property, IncrementType type, PrevScope scope) {
        super(property.caption + " (" + type + ")", property);
        this.type = type;
        this.scope = scope;

        property.getOld(scope);// чтобы зарегить old
    }

    public OldProperty<T> getOldProperty() {
        return property.getOld(scope);
    }

    public ChangedProperty<T> getChangedProperty() {
        return this;
    }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
        depends.add(property);
        depends.add(property.getOld(scope));
    }


    @Override
    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        Boolean setOrDropped = getSetOrDropped();
        if(setOrDropped != null) {
            ChangeType type = propChanges.getUsedChange(property);
            Boolean changeSetOrDropped;
            if(type != null && (changeSetOrDropped = type.getSetOrDropped())!=null) {
                if(!setOrDropped.equals(changeSetOrDropped)) { // если SET, а изменение на NULL, или DROPPED и наоборот, считаем что изменение не используется так как все равно всегда NULL будет
                    boolean isFinal = type.isFinal();
                    ImSet<CalcProperty> usedChanges = null;
                    if(isFinal || (usedChanges = property.getUsedChanges(propChanges)).size() == 1) {
                        ServerLoggers.assertLog(isFinal || usedChanges.contains(property), "SHOULD NOT BE");
                        return SetFact.EMPTY();
                    }
                }
            }
        }
        return super.calculateUsedChanges(propChanges);
    }

    protected Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(calcType.isExpr() && propChanges.isEmpty()) // оптимизация для событий
            return Expr.NULL;
        
        WhereBuilder changedIncrementWhere = new WhereBuilder();
        property.getIncrementExpr(joinImplement, changedIncrementWhere, calcType, propChanges, type, scope);
        if(changedWhere!=null) changedWhere.add(changedIncrementWhere.toWhere());
        return ValueExpr.get(changedIncrementWhere.toWhere());
    }

    // для resolve'а следствий в частности
    public PropertyChange<T> getFullChange(Modifier modifier) throws SQLException, SQLHandledException {
        assert scope.onlyDB(); // так как event Apply

        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        Expr expr = property.getExpr(mapKeys, modifier);
        Where where;
        switch(type) {
            case SET:
                where = expr.getWhere();
                break;
            case DROP:
                where = expr.getWhere().not().and(property.getClassProperty().mapExpr(mapKeys, modifier).getWhere());
                break;
            default:
                return null;
        }
        return new PropertyChange<T>(mapKeys, ValueExpr.get(where), Where.TRUE);
    }

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean calcEvents) {
        if(property instanceof IsClassProperty) {
            return getActionChangeProps(); // только у Data и IsClassProperty
        } else
            return super.calculateLinks(calcEvents);
    }

    @Override
    public ClassWhere<Object> calcClassValueWhere(CalcClassType calcType) {
        ClassWhere<Object> result = new ClassWhere<Object>("value", LogicalClass.instance).and(BaseUtils.<ClassWhere<Object>>immutableCast(property.getClassWhere(calcType)));
        if(calcType == CalcClassType.PREVBASE && !type.isNotNullNew())
            result = result.getBase();
        return result; // assert что full
    }

    public Inferred<T> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        Inferred<T> result = property.inferInterfaceClasses(ExClassSet.notNull(commonValue), inferType);
        if(inferType == InferType.PREVBASE && !type.isNotNullNew())
            result = result.getBase(inferType);
        return result;
    }
    public ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.logical;
    }

    @Override
    public boolean supportsDrillDown() {
        return property != null;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new ChangedDrillDownFormEntity(
                canonicalName, getString("logics.property.drilldown.form.data"), this, LM
        );
    }

    public Boolean getSetOrDropped() {
        return type.getSetOrDropped();
    }
}
