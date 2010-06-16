package platform.server.data.translator;

import platform.server.data.expr.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

public interface MapTranslate {

    KeyExpr translate(KeyExpr expr);
    ValueExpr translate(ValueExpr expr);

    // аналог mapKeys в HashValues - оставляет только трансляцию выражений
    MapValuesTranslate mapValues();

    // для кэша classWhere на самом деле надо
    <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map);

    <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map);

    <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map);

    <K> Map<BaseExpr,K> translateKeys(Map<? extends BaseExpr, K> map);

    <K> Map<K, Expr> translate(Map<K, ? extends Expr> map);

    List<BaseExpr> translateDirect(List<BaseExpr> list);

    Set<BaseExpr> translateDirect(Set<BaseExpr> set);

    Set<KeyExpr> translateKeys(Set<KeyExpr> set);

    List<Expr> translate(List<Expr> list);

    Set<Expr> translate(Set<Expr> set);

    MapTranslate mergeEqual(MapValuesTranslate map);
}
