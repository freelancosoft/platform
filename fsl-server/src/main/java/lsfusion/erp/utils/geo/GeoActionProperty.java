package lsfusion.erp.utils.geo;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;

import java.sql.SQLException;

public class GeoActionProperty extends ScriptingActionProperty {

    public GeoActionProperty(ScriptingLogicsModule LM) throws ScriptingModuleErrorLog.SemanticError {
        super(LM);
    }

    public GeoActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingModuleErrorLog.SemanticError {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
    }

    protected boolean isYandex(ExecutionContext context, DataObject mapProvider) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {
        boolean isYandex = true;
        if (mapProvider != null) {
            String providerName = ((String) findProperty("staticName[Object]").read(context, mapProvider));
            isYandex = providerName == null || providerName.contains("yandex");
        }
        return isYandex;
    }
}
