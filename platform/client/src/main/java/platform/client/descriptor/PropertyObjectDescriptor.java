package platform.client.descriptor;

import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.serialization.ClientSerializationPool;
import platform.client.serialization.ClientIdentitySerializable;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class PropertyObjectDescriptor extends PropertyDescriptorImplement<PropertyObjectInterfaceDescriptor> implements OrderDescriptor, ClientIdentitySerializable {

    public PropertyObjectDescriptor() {
    }

    protected int ID;

    public int getID() {
        return ID;
    }

    public PropertyObjectDescriptor(PropertyDescriptor property, Map<PropertyInterfaceDescriptor, ? extends PropertyObjectInterfaceDescriptor> mapping) {
        super(property, (Map<PropertyInterfaceDescriptor,PropertyObjectInterfaceDescriptor>) mapping);
    }

    public PropertyObjectDescriptor(PropertyDescriptorImplement<PropertyObjectInterfaceDescriptor> implement) {
        super(implement);
    }

    public Set<ObjectDescriptor> getObjects() {
        Set<ObjectDescriptor> result = new HashSet<ObjectDescriptor>();
        for(PropertyObjectInterfaceDescriptor implement : mapping.values())
            if(implement instanceof ObjectDescriptor)
                result.add((ObjectDescriptor)implement);
        return result;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        int result = -1;
        for(ObjectDescriptor object : getObjects()) {
            int groupInd = groupList.indexOf(object.groupTo);
            if(groupInd > result)
                result = groupInd;
        }
        return result>=0?groupList.get(result):null;
    }

    public Set<GroupObjectDescriptor> getGroupObjects() {
        Set<GroupObjectDescriptor> groupObjects = new HashSet<GroupObjectDescriptor>();
        for(ObjectDescriptor object : getObjects())
            groupObjects.add(object.groupTo);
        return groupObjects;
    }

    public List<GroupObjectDescriptor> getGroupObjects(List<GroupObjectDescriptor> groupList) {
        return BaseUtils.filterList(groupList, getGroupObjects());
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeInt(property.getID());

        outStream.writeInt(mapping.size());
        for (Map.Entry<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor> entry : mapping.entrySet()) {
            outStream.writeInt(entry.getKey().getID());
            pool.serializeObject(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        property = (PropertyDescriptor) pool.deserializeObject(inStream);

        mapping = new HashMap<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor>();
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            PropertyInterfaceDescriptor inter = (PropertyInterfaceDescriptor) pool.deserializeObject(inStream);
            PropertyObjectInterfaceDescriptor value = (PropertyObjectInterfaceDescriptor) pool.deserializeObject(inStream);

            mapping.put(inter, value);
        }
    }

    @Override
    public String toString() {
        return property + "[" + BaseUtils.toString(mapping.values(), ",") + "]";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof PropertyObjectDescriptor && property.equals(((PropertyObjectDescriptor) o).property) && mapping.equals(((PropertyObjectDescriptor) o).mapping);
    }

    @Override
    public int hashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
