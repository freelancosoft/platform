package platform.server.data.where.classes;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.*;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

// упрощенный Where
public class PackClassWhere extends DataWhere {

    ClassExprWhere packWhere;

    public PackClassWhere(ClassExprWhere packWhere) {
        this.packWhere = packWhere;

        assert !packWhere.isFalse();
        assert !packWhere.isTrue();
    }

    protected DataWhereSet calculateFollows() {
        return new DataWhereSet(packWhere.getExprFollows());
    }

    public void enumerate(ContextEnumerator enumerator) {
//        throw new RuntimeException("Not supported");
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        throw new RuntimeException("Not supported");
    }

    public int hashContext(HashContext hashContext) {
        return System.identityHashCode(this);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return false;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return packWhere.toString();

        throw new RuntimeException("Not supported");
    }

    @Override
    public String toString() {
        return packWhere.toString();
    }

    public Where translate(MapTranslate translator) {
        throw new RuntimeException("Not supported");
    }
    public Where translateQuery(QueryTranslator translator) {
        throw new RuntimeException("Not supported");
    }

    public InnerJoins groupInnerJoins() {
        throw new RuntimeException("Not supported");
    }
    public ClassExprWhere calculateClassWhere() {
        return packWhere;
    }
}
