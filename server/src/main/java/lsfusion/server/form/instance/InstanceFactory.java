package lsfusion.server.form.instance;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.*;
import lsfusion.server.form.instance.filter.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcPropertyRevImplement;
import lsfusion.server.logics.property.PropertyInterface;

public class InstanceFactory {

    public InstanceFactory() {
    }

    private final MAddExclMap<ObjectEntity, ObjectInstance> objectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<GroupObjectEntity, GroupObjectInstance> groupInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<TreeGroupEntity, TreeGroupInstance> treeInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<PropertyObjectEntity, PropertyObjectInstance> propertyObjectInstances = MapFact.mSmallStrongMap();
    private final MAddExclMap<PropertyDrawEntity, PropertyDrawInstance> propertyDrawInstances = MapFact.mSmallStrongMap();


    public ObjectInstance getInstance(ObjectEntity entity) {
        if (!objectInstances.containsKey(entity)) {
            objectInstances.exclAdd(entity, entity.baseClass.newInstance(entity));
        }
        return objectInstances.get(entity);
    }

    public GroupObjectInstance getInstance(GroupObjectEntity entity) {

        if (entity == null) {
            return null;
        }

        if (!groupInstances.containsKey(entity)) {

            ImOrderSet<ObjectInstance> objects = entity.getOrderObjects().mapOrderSetValues(new GetValue<ObjectInstance, ObjectEntity>() { // последействие есть, но "статичное"
                public ObjectInstance getMapValue(ObjectEntity value) {
                    return getInstance(value);
                }
            });

            ImMap<ObjectInstance, CalcPropertyObjectInstance> parentInstances = null;
            if(entity.isParent !=null) {
                parentInstances = entity.isParent.mapKeyValues(new GetValue<ObjectInstance, ObjectEntity>() {
                    public ObjectInstance getMapValue(ObjectEntity value) {
                        return getInstance(value);
                    }}, new GetValue<CalcPropertyObjectInstance, CalcPropertyObjectEntity<?>>() {
                    public CalcPropertyObjectInstance<?> getMapValue(CalcPropertyObjectEntity value) {
                        return getInstance(value);
                    }});
            }

            groupInstances.exclAdd(entity, new GroupObjectInstance(entity, objects, entity.propertyBackground != null ? getInstance(entity.propertyBackground) : null,
                    entity.propertyForeground != null ? getInstance(entity.propertyForeground) : null, parentInstances,
                    getInstance(entity.getProperties())));
        }

        return groupInstances.get(entity);
    }

    public TreeGroupInstance getInstance(TreeGroupEntity entity) {

        if (entity == null) {
            return null;
        }

        if (!treeInstances.containsKey(entity)) {

            ImOrderSet<GroupObjectInstance> groups = entity.getGroups().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() { // тут как бы с последействием, но "статичным"
                public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                    return getInstance(value);
                }
            });
            treeInstances.exclAdd(entity, new TreeGroupInstance(entity, groups));
        }

        return treeInstances.get(entity);
    }

    private <P extends PropertyInterface> ImMap<P, ObjectInstance> getInstanceMap(PropertyObjectEntity<P, ?> entity) {
        return entity.mapping.mapValues(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return value.getInstance(InstanceFactory.this);
            }});
    }

    public <P extends PropertyInterface> CalcPropertyObjectInstance<P> getInstance(CalcPropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new CalcPropertyObjectInstance<>(entity.property, getInstanceMap(entity)));

        return (CalcPropertyObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    private <P extends PropertyInterface> ImRevMap<P, ObjectInstance> getInstanceMap(CalcPropertyRevImplement<P, ObjectEntity> entity) {
        return entity.mapping.mapRevValues(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return InstanceFactory.this.getInstance(value);
            }});
    }

    public <T, P extends PropertyInterface> ImMap<T, CalcPropertyRevImplement<P, ObjectInstance>> getInstance(ImMap<T, CalcPropertyRevImplement<P, ObjectEntity>> entities) {
        return entities.mapValues(new GetValue<CalcPropertyRevImplement<P, ObjectInstance>, CalcPropertyRevImplement<P, ObjectEntity>>() {
            public CalcPropertyRevImplement<P, ObjectInstance> getMapValue(CalcPropertyRevImplement<P, ObjectEntity> entity) {
                return new CalcPropertyRevImplement<>(entity.property, getInstanceMap(entity));
            }});
    }

        // временно
    public <P extends PropertyInterface> PropertyObjectInstance<P, ?> getInstance(PropertyObjectEntity<P, ?> entity) {
        if(entity instanceof CalcPropertyObjectEntity)
            return getInstance((CalcPropertyObjectEntity<P>)entity);
        else
            return getInstance((ActionPropertyObjectEntity<P>)entity);
    }

    public <P extends PropertyInterface> ActionPropertyObjectInstance<P> getInstance(ActionPropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity))
            propertyObjectInstances.exclAdd(entity, new ActionPropertyObjectInstance<>(entity.property, getInstanceMap(entity)));

        return (ActionPropertyObjectInstance<P>) propertyObjectInstances.get(entity);
    }

    public <T extends PropertyInterface> PropertyDrawInstance getInstance(PropertyDrawEntity<T> entity) {

        if (!propertyDrawInstances.containsKey(entity)) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = entity.getColumnGroupObjects().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() {
                public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                    return getInstance(value);
                }
            });

            propertyDrawInstances.exclAdd(entity, new PropertyDrawInstance<>(
                    entity,
                    getInstance(entity.getValueProperty()),
                    getInstance(entity.toDraw),
                    columnGroupObjects,
                    entity.propertyCaption == null ? null : getInstance(entity.propertyCaption),
                    entity.propertyShowIf == null ? null : getInstance(entity.propertyShowIf),
                    entity.propertyReadOnly == null ? null : getInstance(entity.propertyReadOnly),
                    entity.propertyFooter == null ? null : getInstance(entity.propertyFooter),
                    entity.propertyBackground == null ? null : getInstance(entity.propertyBackground),
                    entity.propertyForeground == null ? null : getInstance(entity.propertyForeground)));
        }

        return propertyDrawInstances.get(entity);
    }

    public RegularFilterGroupInstance getInstance(RegularFilterGroupEntity entity) {

        RegularFilterGroupInstance group = new RegularFilterGroupInstance(entity);

        for (RegularFilterEntity filter : entity.getFiltersList()) {
            group.addFilter(getInstance(filter));
        }

        return group;
    }

    public RegularFilterInstance getInstance(RegularFilterEntity entity) {
        return new RegularFilterInstance(entity, entity.filter.getInstance(this));
    }
}
