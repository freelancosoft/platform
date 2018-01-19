package lsfusion.server.logics.property.actions.external;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

public abstract class ExternalActionProperty extends SystemActionProperty {

    protected ImOrderSet<PropertyInterface> paramInterfaces;
    protected ImMap<PropertyInterface, Type> paramTypes;
    protected ImList<LCP> targetPropList;

    protected static String getTransformedText(ExecutionContext<PropertyInterface> context, PropertyInterface param) {
        return ScriptingLogicsModule.transformFormulaText((String)context.getKeyObject(param), getParamName("$1"));
    }

    // важно! используется также в regexp'е, то есть не должно быть спецсимволов
    protected static String getParamName(String prmID) {
        return "qxprm" + prmID + "nx";
    }

    public ExternalActionProperty(int exParams, ImList<Type> params, ImList<LCP> targetPropList) {
        super(LocalizedString.NONAME, SetFact.toOrderExclSet(params.size() + exParams, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        this.paramInterfaces = orderInterfaces.subOrder(exParams, orderInterfaces.size());
        paramTypes = paramInterfaces.mapList(params);
        this.targetPropList = targetPropList;
    }

    private Type getParamType(PropertyInterface paramInterface, ObjectValue value) {
        if(value instanceof DataObject)
            return ((DataObject) value).getType();

        Type type = paramTypes.get(paramInterface);
        if(type != null)
            return type;

        return AbstractType.getUnknownTypeNull();
    }

    protected Object format(ExecutionContext<PropertyInterface> context, PropertyInterface paramInterface) {
        ObjectValue value = context.getKeyValue(paramInterface);
        return getParamType(paramInterface, value).format(value.getValue());
    }

    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString) {
        return replaceParams(context, connectionString, null);
    }
    protected String replaceParams(ExecutionContext<PropertyInterface> context, String connectionString, Result<ImOrderSet<PropertyInterface>> rNotUsedParams) {
        ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
        MOrderExclSet<PropertyInterface> mNotUsedParams = rNotUsedParams != null ? SetFact.<PropertyInterface>mOrderExclSetMax(orderInterfaces.size()) : null;
        for (int i = 0, size = orderInterfaces.size(); i < size ; i++) {
            String prmName = getParamName(String.valueOf(i + 1));
            PropertyInterface paramInterface = orderInterfaces.get(i);
            Object replacement = format(context, paramInterface);
            if (replacement instanceof byte[] ||
                    (mNotUsedParams != null && !connectionString.contains(prmName))) {
                if(mNotUsedParams != null)
                    mNotUsedParams.exclAdd(paramInterface);
                continue; // можно не делать continue - это оптимизация
            }
            connectionString = connectionString.replace(prmName, (String)replacement);
        }
        if(rNotUsedParams != null)
            rNotUsedParams.set(mNotUsedParams.immutableOrder());
        try {
            return URIUtil.encodeQuery(connectionString, "UTF-8");
        } catch (URIException e) {
            ServerLoggers.systemLogger.error("ReplaceParams error: ", e);
            return connectionString;
        }
    }
}