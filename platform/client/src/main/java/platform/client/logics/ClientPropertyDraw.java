package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientExternalScreen;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.SwingUtils;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.screen.ExternalScreenConstraints;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.Format;
import java.util.Collection;
import java.awt.*;

public class ClientPropertyDraw extends ClientComponent {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientType baseType;

    public String caption;

    public KeyStroke editKey;
    public boolean showEditKey;

    public Boolean focusable;
    public Boolean readOnly;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

    public int getMinimumWidth(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.width;
        }
        return baseType.getMinimumWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMinimumHeight(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.height;
        }
        return getPreferredHeight(comp);
    }

    protected Dimension minimumSize;

    public Dimension getMinimumSize(JComponent comp) {
        if (minimumSize != null)
            return minimumSize;
        return new Dimension(getMinimumWidth(comp), getMinimumHeight(comp));
    }

    public int getPreferredWidth(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.width;
        }
        return baseType.getPreferredWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getPreferredHeight(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.height;
        }
        return baseType.getPreferredHeight(comp.getFontMetrics(design.getFont(comp)));
    }

    protected Dimension preferredSize;

    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null)
            return preferredSize;
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.width;
        }
        return baseType.getMaximumWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMaximumHeight(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.height;
        }
        return getPreferredHeight(comp);
    }

    protected Dimension maximumSize;

    public Dimension getMaximumSize(JComponent comp) {
        if (maximumSize != null)
            return maximumSize;
        return new Dimension(getMaximumWidth(comp), getMaximumHeight(comp));
    }

    protected transient PropertyRendererComponent renderer;

    public PropertyRendererComponent getRendererComponent() {
        if (renderer == null) renderer = baseType.getRendererComponent(getFormat(), caption, design);
        return renderer;
    }

    public CellView getPanelComponent(ClientFormController form) {
        return baseType.getPanelComponent(this, form);
    }

    // на данный момент ClientFormController нужна для 2-х целей : как owner, создаваемых диалогов и как провайдер RemoteFormInterface, для получения того, что мы вообще редактируем

    protected Format format;

    Format getFormat() {
        if (format == null) return baseType.getDefaultFormat();
        return format;
    }

    public String toString() {
        return caption;
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showEditKey && editKey != null)
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(editKey) + ")";
        return fullCaption;
    }

    public boolean checkEquals;
    public boolean askConform;

    protected int ID = 0;
    protected String sID;

    public ClientGroupObject groupObject;
    public ClientGroupObject[] columnGroupObjects;
    public ClientPropertyDraw[] columnDisplayProperties;
    public int[] columnDisplayPropertiesIds;

    public boolean autoHide = false;

    //пришлось сделать "конструктор копирования" для ремаппинга
    protected ClientPropertyDraw(ClientPropertyDraw original) {
        super(original);
        this.baseType = original.baseType;
        this.caption = original.caption;
        this.editKey = original.editKey;
        this.showEditKey = original.showEditKey;
        this.focusable = original.focusable;
        this.readOnly = original.readOnly;
        this.panelLabelAbove = original.panelLabelAbove;
        this.externalScreen = original.externalScreen;
        this.externalScreenConstraints = original.externalScreenConstraints;
        this.minimumSize = original.minimumSize;
        this.preferredSize = original.preferredSize;
        this.maximumSize = original.maximumSize;
        this.renderer = original.renderer;
        this.format = original.format;
        this.checkEquals = original.checkEquals;
        this.ID = original.ID;
        this.sID = original.sID;
        this.groupObject = original.groupObject;
        this.columnGroupObjects = original.columnGroupObjects;
        this.columnDisplayProperties = original.columnDisplayProperties;
        this.columnDisplayPropertiesIds = original.columnDisplayPropertiesIds;
        this.autoHide = original.autoHide = false;
        this.askConform = original.askConform;
    }

    public ClientPropertyDraw(DataInputStream inStream, Collection<ClientContainer> containers, Collection<ClientGroupObject> groups) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        caption = inStream.readUTF();

        baseType = ClientTypeSerializer.deserialize(inStream);

        minimumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        maximumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        preferredSize = (Dimension) new ObjectInputStream(inStream).readObject();

        editKey = (KeyStroke) new ObjectInputStream(inStream).readObject();
        showEditKey = inStream.readBoolean();

        format = (Format) new ObjectInputStream(inStream).readObject();

        focusable = (Boolean) new ObjectInputStream(inStream).readObject();
        readOnly = (Boolean) new ObjectInputStream(inStream).readObject();
        if (readOnly == null) {
            readOnly = true;
        }

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean())
            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());

        if (inStream.readBoolean())
            externalScreenConstraints = (ExternalScreenConstraints) new ObjectInputStream(inStream).readObject();

        ID = inStream.readInt();
        sID = inStream.readUTF();
        if (inStream.readBoolean()) {
            int groupID = inStream.readInt();
            groupObject = getClientGroupObject(groups, groupID);
        }

        if (inStream.readBoolean()) {
            int groupID = inStream.readInt();
            keyBindingGroup = getClientGroupObject(groups, groupID);
        }

        if (inStream.readBoolean()) {
            int length = inStream.readInt();
            columnGroupObjects = new ClientGroupObject[length];
            columnDisplayPropertiesIds = new int[length];
            for (int i = 0; i < length; ++i) {
                int groupID = inStream.readInt();
                columnGroupObjects[i] = getClientGroupObject(groups, groupID);
            }
            for (int i = 0; i < length; ++i) {
                columnDisplayPropertiesIds[i] = inStream.readInt();
            }
        } else {
            //чтобы не проверять везде на null
            columnGroupObjects = new ClientGroupObject[0];
            columnDisplayPropertiesIds = new int[0];
        }

        autoHide = inStream.readBoolean();

        checkEquals = inStream.readBoolean();
        askConform = inStream.readBoolean();
    }

    private ClientGroupObject getClientGroupObject(Collection<ClientGroupObject> groups, int groupID) {
        for (ClientGroupObject group : groups) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    ClientGroupObject keyBindingGroup = null;
    public ClientGroupObject getKeyBindingGroup() {
        return BaseUtils.nvl(keyBindingGroup, getGroupObject());
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {

        if (askConform) {
            int n = JOptionPane.showConfirmDialog(
                    null,
                    baseType.getConformedMessage() + " \"" + caption + "\"?",
                    "LS Fusion",
                    JOptionPane.YES_NO_OPTION);
            if (n != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        
        ClientType changeType = getPropertyChangeType(form);
        if (changeType == null) return null;
        return changeType.getEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createEditorPropertyDialog(ID);
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createClassPropertyDialog(ID, BaseUtils.objectToInt(value));
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        return sID;
    }

    public ClientType getPropertyChangeType(ClientFormController form) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(this.ID)));
        if (inStream.readBoolean()) return null;

        return ClientTypeSerializer.deserialize(inStream);
    }

    public Object parseString(ClientFormController form, String s) throws ParseException {
        ClientType changeType = null;
        try {
            changeType = getPropertyChangeType(form);
            if (changeType == null) throw new ParseException("PropertyView не может быть изменено.", 0);

            return changeType.parseString(s);
        } catch (IOException e) {
            throw new ParseException("Ошибка получения данных о propertyChangeType.", 0);
        }
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return baseType.shouldBeDrawn(form);
    }

    private ClientGroupObject[] keyObjects;
    public ClientGroupObject[] getKeyObjects() {
        if (keyObjects == null) {
            keyObjects = new ClientGroupObject[columnGroupObjects.length + 1];
            keyObjects[0] = groupObject;
            System.arraycopy(columnGroupObjects, 0, keyObjects, 1, columnGroupObjects.length);
        }
        return keyObjects;
    }
}
