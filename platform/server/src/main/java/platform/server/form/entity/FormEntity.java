package platform.server.form.entity;

import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public abstract class FormEntity<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    private final static Logger logger = Logger.getLogger(FormEntity.class.getName());

    public boolean isReadOnly() {
        return false;
    }

    public List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
    public List<PropertyDrawEntity> propertyDraws = new ArrayList<PropertyDrawEntity>();

    public Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();


    public void addFixedFilter(FilterEntity filter) {
        fixedFilters.add(filter);
    }

    public OrderedMap<OrderEntity<?>, Boolean> fixedOrders = new OrderedMap<OrderEntity<?>, Boolean>();

    public void addFixedOrder(OrderEntity order, boolean descending) {
        fixedOrders.put(order, descending);
    }

    public List<RegularFilterGroupEntity> regularFilterGroups = new ArrayList<RegularFilterGroupEntity>();
    public void addRegularFilterGroup(RegularFilterGroupEntity group) {
        regularFilterGroups.add(group);
    }

    protected RegularFilterGroupEntity addSingleRegularFilterGroup(FilterEntity ifilter, String iname, KeyStroke ikey) {

        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
        filterGroup.addFilter(new RegularFilterEntity(genID(), ifilter, iname, ikey));
        addRegularFilterGroup(filterGroup);

        return filterGroup;
    }

    // счетчик идентификаторов
    private int IDCount = 0;
    public int genID() {
        return IDCount++;
    }

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, String caption, Object... groups) {

        GroupObjectEntity groupObject = new GroupObjectEntity(genID());
        ObjectEntity object = new ObjectEntity(genID(), baseClass, caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyDraw(groups, object);

        return object;
    }

    protected void addGroup(GroupObjectEntity group) {
        groups.add(group);
    }

    protected void addPropertyDraw(ObjectEntity object, Object... groups) {
        addPropertyDraw(groups, object);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, Object... groups) {
        addPropertyDraw(groups, object1, object2);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, Object... groups) {
        addPropertyDraw(groups, object1, object2, object3);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, Object... groups) {
        addPropertyDraw(groups, object1, object2, object3, object4);
    }

    private void addPropertyDraw(Object[] groups, ObjectEntity... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) continue;

            if (group instanceof AbstractNode) {
                boolean upClasses = false;
                if ((i+1)<groups.length && groups[i+1] instanceof Boolean) upClasses = (Boolean)groups[i+1];
                addPropertyDraw((AbstractNode)group, upClasses, objects);
            }
            else if (group instanceof LP)
                this.addPropertyDraw((LP)group, objects);
        }
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, ObjectEntity... objects) {
        ValueClass[] valueClasses = new ValueClass[objects.length]; int ic = 0;
        for(ObjectEntity object : objects)
            valueClasses[ic++] = object.baseClass;

        for(Property property : group.getProperties(valueClasses))
            if (property.interfaces.size() == objects.length)
                addPropertyDraw(property, upClasses, groupObject, objects);
    }

    <P extends PropertyInterface<P>> void addPropertyDraw(Property<P> property, boolean upClasses, GroupObjectEntity groupObject, ObjectEntity... objects) {

        for (List<P> mapping : new ListPermutations<P>(property.interfaces)) {

            Map<P, AndClassSet> propertyInterface = new HashMap<P, AndClassSet>();
            int interfaceCount = 0;
            for (P iface : mapping) {
                ValueClass propertyClass = objects[interfaceCount++].baseClass;
                propertyInterface.put(iface, propertyClass.getUpSet());
            }

            if ((upClasses && property.anyInInterface(propertyInterface)) || (!upClasses && property.allInInterface(propertyInterface)))
                addPropertyDraw(new LP<P>(property, mapping), groupObject, objects);
        }
    }

    public PropertyDrawEntity addPropertyDraw(LP property, ObjectEntity... objects) {
        return addPropertyDraw(property, null, objects);
    }

    <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P> property, GroupObjectEntity groupObject, ObjectEntity... objects) {

        return addPropertyDraw(groupObject, new PropertyObjectEntity<P>(property, objects));
    }

    public GroupObjectEntity getApplyObject(Collection<ObjectEntity> objects) {
        GroupObjectEntity result = null;
        for(GroupObjectEntity group : groups)
            for(ObjectEntity object : group)
                if(objects.contains(object)) {
                    result = group;
                    break;
                }
        return result;
    }

    <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P> propertyImplement) {

        PropertyDrawEntity<P> propertyDraw = new PropertyDrawEntity<P>(genID(), propertyImplement, (groupObject == null) ? getApplyObject(propertyImplement.getObjectInstances()) : groupObject);
        propertyImplement.property.proceedDefaultDraw(propertyDraw, this);

        if (propertyImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (PropertyDrawEntity property : propertyDraws)
                if (BaseUtils.nullEquals(property.getSID(), propertyImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            propertyDraw.setSID(propertyImplement.property.sID + ((foundSID) ? propertyDraw.getID() : ""));
        }


        int count = 0;
        for (PropertyDrawEntity property : propertyDraws){
          if (property.shouldBeLast){
                propertyDraws.add(count, propertyDraw);
                count = -1;
                break;
          }
          count++;
        }

        if (count >= 0)
            propertyDraws.add(propertyDraw);
        

        assert richDesign == null;

        return propertyDraw;
    }

    public PropertyObjectEntity addPropertyObject(LP property, PropertyObjectInterfaceEntity... objects) {

        return new PropertyObjectEntity(property,objects);
    }


    protected PropertyObjectEntity getPropertyObject(LP<?> lp) {
        return getPropertyDraw(lp).propertyObject;
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?> lp) {
        return getPropertyDraw(lp.property);
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp, object).propertyObject;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp, groupObject).propertyObject;
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp.property, object.groupTo);
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp.property, groupObject);
    }

    protected PropertyDrawEntity getPropertyDraw(PropertyObjectEntity property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : propertyDraws)
            if (propertyDraw.propertyObject.equals(property))
                resultPropertyDraw = propertyDraw;

        return resultPropertyDraw;
    }

    public PropertyObjectEntity getPropertyObject(Property property) {
        return getPropertyDraw(property).propertyObject;
    }

    protected PropertyDrawEntity getPropertyDraw(Property property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity<?> propertyDraw : propertyDraws)
            if (propertyDraw.propertyObject.property == property)
                resultPropertyDraw = propertyDraw;

        return resultPropertyDraw;
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, ObjectEntity object) {
        return getPropertyDraw(group, object.groupTo);
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, GroupObjectEntity groupObject) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : propertyDraws)
            if (group.hasChild(propertyDraw.propertyObject.property) && propertyDraw.toDraw.equals(groupObject))
                resultPropertyDraw = propertyDraw;

        return resultPropertyDraw;
    }

    public void addHintsNoUpdate(AbstractGroup group) {

        for (Property property : group.getProperties(null))
            addHintsNoUpdate(property);
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> hintsSave = new HashSet<Property>();

    protected void addHintsNoUpdate(LP<?> prop) {
        addHintsNoUpdate(prop.property);
    }

    protected void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    void addHintsSave(Property prop) {
        hintsSave.add(prop);
    }

    private static String genSID(int iID) {
        return "form" + iID;
    }

    public String sID;
    public boolean isPrintForm;

    protected FormEntity(int ID, String caption) { this(ID, caption, false); }
    FormEntity(int iID, String caption, boolean iisPrintForm) { this(null, iID, caption, iisPrintForm); }
    protected FormEntity(NavigatorElement parent, int iID, String caption) { this(parent, iID, caption, false); }
    protected FormEntity(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) { this(parent, iID, caption, genSID(iID), iisPrintForm); }
    protected FormEntity(NavigatorElement parent, int iID, String caption, String isID, boolean iisPrintForm) {
        super(parent, iID, caption);
        logger.info("Initializing form "+caption+"...");

        sID = isID;
        isPrintForm = iisPrintForm;
    }

    public FormView richDesign;
    public DefaultFormView createDefaultRichDesign() { return new DefaultFormView(this); }
    public FormView getRichDesign() { if (richDesign == null) return new DefaultFormView(this); else return richDesign; }

    protected GroupObjectHierarchy groupHierarchy;

    public GroupObjectHierarchy.ReportHierarchy getReportHierarchy() {
        if (groupHierarchy == null) {
            FormGroupHierarchyCreator creator = new FormGroupHierarchyCreator(this);
            groupHierarchy = creator.createHierarchy();
        }
        return groupHierarchy.createReportHierarchy();
    }

    public GroupObjectHierarchy getGroupHierarchy() {
        return groupHierarchy;
    }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();

    public byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(isPrintForm);
    }

    public Map<ObjectEntity, List<PropertyObjectEntity>> autoActions = new HashMap<ObjectEntity, List<PropertyObjectEntity>>();
    public void addAutoAction(ObjectEntity object, PropertyObjectEntity... actions) {
        addAutoAction(object, false, actions);
    }
    public void addAutoAction(ObjectEntity object, boolean drop, PropertyObjectEntity... actions) {
        List<PropertyObjectEntity> propertyActions = autoActions.get(object);
        if(propertyActions==null || drop) {
            propertyActions = new ArrayList<PropertyObjectEntity>();
            autoActions.put(object, propertyActions);
        }

        for(PropertyObjectEntity action : actions)
            propertyActions.add(action);
    }

    public boolean hasClientApply() {
        return false;
    }

    public ClientAction getClientApply(FormInstance<T> form) {
        return null; // будем возвращать именно null, чтобы меньше данных передавалось        
    }

    public String checkClientApply(Object result) {
        return null;
    }

    public Map<PropertyDrawEntity, GroupObjectEntity> forceDefaultDraw = new HashMap<PropertyDrawEntity, GroupObjectEntity>();
}
