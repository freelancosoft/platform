package platform.server.data.where;

import platform.base.BaseUtils;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.where.classes.ClassExprWhere;

@GenericImmutable
public abstract class FormulaWhere<WhereType extends Where> extends AbstractWhere {

    protected final WhereType[] wheres;
    protected FormulaWhere(WhereType[] iWheres) {
        wheres = iWheres;
    }

    abstract String getOp();

    public String getSource(CompileSource compile) {
        if(wheres.length==0) return getOp().equals("AND")? TRUE_STRING : FALSE_STRING;

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where.getSource(compile);
        return "("+result+")";
    }

    public void enumerate(ContextEnumerator enumerator) {
        for(Where where : wheres)
            where.enumerate(enumerator);
    }

    @GenericLazy
    public int hashContext(HashContext hashContext) {
        int result = hashCoeff();
        for(Where where : wheres)
            result += where.hashContext(hashContext);
        return result;
    }

    abstract int hashCoeff();

    static OrObjectWhere[] not(AndObjectWhere[] wheres) {
        OrObjectWhere[] result = new OrObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = wheres[i].not();
        return result;
    }

    static AndObjectWhere[] not(OrObjectWhere[] wheres) {
        AndObjectWhere[] result = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = wheres[i].not();
        return result;
    }

    // из массива or'ов определяет массив and'ов
    static AndObjectWhere[] reverse(OrObjectWhere[] wheres) {
        if(wheres.length==1) {
            if(wheres[0] instanceof ObjectWhere)
                return new AndObjectWhere[]{(ObjectWhere)wheres[0]};
            else // значит OrWhere
                return ((OrWhere)wheres[0]).wheres;
        } else
            return new AndObjectWhere[]{new AndWhere(wheres)};
    }

    static AndObjectWhere[] reverseNot(AndObjectWhere[] wheres) {
        return reverse(not(wheres));
    }

    int height;
    public int getHeight() {
        if(wheres.length==0) return 0;
        if(height==0) {
            int maxHeight = 0;
            for(int i=1;i<wheres.length;i++)
                if(wheres[i].getHeight()>wheres[maxHeight].getHeight())
                    maxHeight = i;
            height = wheres[maxHeight].getHeight()+1;
        }
        return height;
    }

    // отнимает одно мн-во от второго
    WhereType[] substractWheres(WhereType[] substract) {
        if(substract.length>wheres.length) return null;

        WhereType[] rawRestWheres = wheres.clone();
        for(WhereType andWhere : substract) {
            boolean found = false;
            for(int i=0;i<rawRestWheres.length;i++)
                if(rawRestWheres[i]!=null && BaseUtils.hashEquals(andWhere,rawRestWheres[i])) {
                    rawRestWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return null;
        }

        WhereType[] restWheres = newArray(wheres.length-substract.length); int rest=0;
        for(WhereType where : rawRestWheres)
            if(where!=null) restWheres[rest++] = where;
        return restWheres;
    }
    public abstract WhereType[] newArray(int length);

    public ClassExprWhere calculateClassWhere() {
        return getMeanClassWheres().getClassWhere();
    }
}
