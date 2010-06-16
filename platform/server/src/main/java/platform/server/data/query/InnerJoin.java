package platform.server.data.query;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.translator.MapTranslate;

public interface InnerJoin {
    VariableExprSet getJoinFollows();

    int hashContext(HashContext hashContext);

    InnerJoin translate(MapTranslate translator);

    boolean isIn(VariableExprSet set);
}
