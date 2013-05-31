package platform.server.classes;

import platform.base.BaseUtils;
import platform.base.ExtInt;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MOrderExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.formula.ExprSource;
import platform.server.data.expr.formula.FormulaImpl;
import platform.server.data.query.CompileOrder;
import platform.server.data.query.CompileSource;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.ParseException;
import platform.server.data.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

// суррогатный класс, необходимый для реализации оператора ORDER
public class OrderClass extends DataClass<Object> implements FormulaImpl {

    private final ImList<Type> types; // типы придется в явную хранить, так как выводить их из expr'ов не всегда получится (могут быть NULL'ы) и тогда непонятно к чему cast'ить
    private final ImList<Boolean> desc;

    private final static Collection<OrderClass> orders = new ArrayList<OrderClass>();
    public synchronized static OrderClass get(ImList<Type> types, ImList<Boolean> desc) {
        for(OrderClass order : orders)
            if(BaseUtils.hashEquals(order.types, types) && BaseUtils.hashEquals(order.desc, desc))
                return order;

        OrderClass order = new OrderClass(types, desc);
        orders.add(order);
        return order;
    }
    public final static OrderClass value = get(ListFact.<Type>EMPTY(), ListFact.<Boolean>EMPTY());

    private OrderClass(ImList<Type> types, ImList<Boolean> desc) {
        super("ORDER " + types + " " + desc);
        this.types = types;
        this.desc = desc;
    }

    public Type getType(ExprSource source, KeyType keyType) {
        return this;
    }

    public ConcreteClass getStaticClass(ExprSource source) {
        return this;
    }

    public String getSource(CompileSource compile, ExprSource source) {
        assert source.getExprCount() == desc.size();

        if(desc.size() == 0)
            return SQLSyntax.NULL;

        if(desc.size() == 1)
            return source.getSource(0, compile);

        MList<String> mExprs = ListFact.mList(source.getExprCount());
        String resultSource = "";
        for (int i = 0, size = source.getExprCount(); i < size; i++) {
            Type exprType = types.get(i);
            String exprSource = source.getSource(i, compile);
            resultSource = (resultSource.length()==0 ? "" : resultSource + "," ) + "COALESCE(" + exprSource + "," + exprType.getString(exprType.getInfiniteValue(true), compile.syntax) + ")";
            mExprs.add(exprSource);
        }
        resultSource = ConcatenateType.get(types.toArray(new Type[types.size()])).getCast("ROW(" + resultSource + ")", compile.syntax, compile.env);

        return "CASE WHEN " + mExprs.immutableList().toString(new GetValue<String, String>() {
            public String getMapValue(String value) {
                return value + " IS NOT NULL";
            }}, " OR ") + " THEN " + resultSource + " ELSE NULL END";
    }

    public ImOrderMap<String, CompileOrder> getCompileOrders(String source, final CompileOrder order) {
        if(desc.size()<=1)
            return null;

        MOrderExclMap<String, CompileOrder> mResult = MapFact.mOrderExclMap(desc.size());
        for(int i=0,size=desc.size();i<size;i++)
            mResult.exclAdd(ConcatenateType.getDeconcatenateSource(source, i), new CompileOrder(desc.get(i)!=order.desc, null, false));
        return mResult.immutableOrder();
    }

    public DataClass getCompatible(DataClass compClass) {
        if(!(compClass instanceof OrderClass)) return null;

        if(types.isEmpty())
            return this;
        if(compClass.isEmpty())
            return this;

        OrderClass orderClass = (OrderClass) compClass;
        if(desc.size() != orderClass.desc.size())
            return null;

        MList<Type> mCompatible = ListFact.mList(types.size());
        for(int i=0,size=types.size();i<size;i++) {
            Type compType = types.get(i).getCompatible(orderClass.types.get(i));
            if(desc.get(i) != orderClass.desc.get(i) || compType == null)
                return null;
            mCompatible.add(compType);
        }
        return get(mCompatible.immutableList(), desc);
    }

    public byte getTypeID() {
        throw new UnsupportedOperationException();
    }

    protected Class getReportJavaClass() {
        throw new UnsupportedOperationException();
    }

    public Format getReportFormat() {
        throw new UnsupportedOperationException();
    }

    protected void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public int getSQL(SQLSyntax syntax) {
        throw new UnsupportedOperationException();
    }

    public boolean isSafeString(Object value) {
        throw new UnsupportedOperationException();
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new UnsupportedOperationException();
    }

    public Object parseString(String s) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public String getSID() {
        String result = "";
        for(int i=0,size=types.size();i<size;i++)
            result += "_" + types.get(i).getSID() + "_" + (desc.get(i) ? "t" : "f");
        return "OrderClass" + result;
    }

    public Object read(Object value) {
        return value; // может зайти так как сейчас order вынужден быть в запросе
    }

    public Object getDefaultValue() {
        throw new UnsupportedOperationException();
    }
}
