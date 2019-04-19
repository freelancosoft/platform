package lsfusion.server.data.expr.formula.conversion;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TextClass;

public class StringTypeConversion implements TypeConversion {
    public final static StringTypeConversion instance = new StringTypeConversion();

    @Override
    public Type getType(Type type1, Type type2) {
        if (type1 instanceof StringClass || type2 instanceof StringClass) {
            if(type1 instanceof TextClass && type2 instanceof TextClass)
                return ((TextClass) type1).rich || ((TextClass) type2).rich ? TextClass.richInstance : TextClass.instance;

            ExtInt length1 = type1 == null ? ExtInt.ZERO : type1.getCharLength();
            ExtInt length2 = type2 == null ? ExtInt.ZERO : type2.getCharLength();

            boolean caseInsensitive =
                    (type1 instanceof StringClass && ((StringClass) type1).caseInsensitive) ||
                            (type2 instanceof StringClass && ((StringClass) type2).caseInsensitive);

            boolean blankPadded =
                    (type1 instanceof StringClass && ((StringClass) type1).blankPadded) &&
                            (type2 instanceof StringClass && ((StringClass) type2).blankPadded);

            return StringClass.get(blankPadded, caseInsensitive, length1.sum(length2));
        }
        return null;
    }
}
