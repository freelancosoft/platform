package lsfusion.server.data.expr.join;

import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.query.stat.Stat;
import lsfusion.server.data.expr.join.where.InnerJoins;

// нужен пока и в будущем для работы с интервалами, сейчас 3 хака:
// 1. ExprIndexedJoin.fillIntervals - 
// 2. WhereJoins.removeJoin - чтобы не протолкнулся висячий ключ
// 3. canBeKeyJoined - чтобы не протолкнулся висячий ключ

public class ExprIntervalJoin extends ExprStatJoin {

    public ExprIntervalJoin(BaseExpr baseExpr, Stat stat, InnerJoins valueJoins) {
        super(baseExpr, stat, valueJoins, false);
    }

    @Override
    public boolean canBeKeyJoined() {
        return false;
    }
}
