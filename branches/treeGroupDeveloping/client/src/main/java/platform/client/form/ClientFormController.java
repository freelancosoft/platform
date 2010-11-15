/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.client.ClientButton;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.logics.*;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientResultAction;
import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClientFormController {

    private final ClientForm form;

    public RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;
    public final ClientFormActionDispatcher actionDispatcher;

    // здесь хранится список всех GroupObjects плюс при необходимости null
//    private List<ClientGroupObject> groupObjects;

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private int ID;

    private ClientFormLayout formLayout;

    private Map<ClientGroupObject, GroupObjectController> controllers;
    private Map<ClientTreeGroup, TreeGroupController> treeControllers;

    private JButton buttonApply;
    private JButton buttonCancel;

    private Color defaultApplyBackground;
    public boolean dataChanged;

    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();

    public boolean isDialogMode() {
        return false;
    }

    public boolean isReadOnlyMode() {
        return form.readOnly;
    }

    public int getID() {
        return ID;
    }

    public KeyStroke getKeyStroke() {
        return form.keyStroke;
    }

    public String getCaption() {
        return form.caption;
    }

    public String getFullCaption() {
        return form.getFullCaption();
    }

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) throws IOException, ClassNotFoundException {

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        this.remoteForm = remoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        this.clientNavigator = clientNavigator;

        actionDispatcher = new ClientFormActionDispatcher(this);

        form = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));
//        form = new ClientForm(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        initializeForm();
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    public ClientFormLayout getComponent() {
        return formLayout;
    }

    void initializeForm() throws IOException {

        formLayout = new ClientFormLayout(form.mainContainer) {
            boolean firstGainedFocus = true;

            @Override
            public void gainedFocus() {

                if (remoteForm == null) // типа если сработал closed, то ничего вызывать не надо
                    return;

                try {
                    remoteForm.gainedFocus();
                    if (clientNavigator != null) {
                        clientNavigator.currentFormChanged();
                    }

/*                    //при старте перемещаем фокус на стандартный (только в первый раз, из-за диалогов)
                    if (firstGainedFocus) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getFocusTraversalPolicy().getDefaultComponent(formLayout).requestFocusInWindow();
                            }
                        });
                        firstGainedFocus = false;
                    }*/

                    // если вдруг изменились данные в сессии
                    ClientExternalScreen.invalidate(getID());
                    ClientExternalScreen.repaintAll(getID());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при активации формы", e);
                }
            }
        };

//        setContentPane(formLayout.getComponent());
//        setComponent(formLayout.getComponent());

//        initializeGroupObjects();

        initializeControllers();

        initializeRegularFilters();

        initializeButtons();

        applyRemoteChanges();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    private void initializeControllers() throws IOException {
        List<ClientGroupObject> groupObjectsFromTrees = new ArrayList<ClientGroupObject>();
        treeControllers = new HashMap<ClientTreeGroup, TreeGroupController>();
        for (ClientTreeGroup treeGroup : form.treeGroups) {
            TreeGroupController controller = new TreeGroupController(treeGroup, this, formLayout);
            treeControllers.put(treeGroup, controller);

            groupObjectsFromTrees.addAll(treeGroup.groups);
        }

        controllers = new HashMap<ClientGroupObject, GroupObjectController>();

        for (ClientGroupObject group : form.groupObjects) {
            if (!groupObjectsFromTrees.contains(group)) {
                GroupObjectController controller = new GroupObjectController(group, form, this, formLayout);
                controllers.put(group, controller);
            }
        }

        for (ClientPropertyDraw properties : form.getPropertyDraws()) {
            if (properties.groupObject == null) {
                GroupObjectController controller = new GroupObjectController(null, form, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }

    //todo: unused, remove later
    private void initializeGroupObjects() throws IOException {

        controllers = new HashMap<ClientGroupObject, GroupObjectController>();
//        groupObjects = new ArrayList<ClientGroupObject>();

        for (ClientGroupObject groupObject : form.groupObjects) {
//            groupObjects.add(groupObject);
            GroupObjectController controller = new GroupObjectController(groupObject, form, this, formLayout);
            controllers.put(groupObject, controller);
        }

        for (ClientPropertyDraw properties : form.getPropertyDraws()) {
            if (properties.groupObject == null) {
//                groupObjects.add(null);
                GroupObjectController controller = new GroupObjectController(null, form, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }

    private void initializeRegularFilters() {

        // Проинициализируем регулярные фильтры

        for (final ClientRegularFilterGroup filterGroup : form.regularFilterGroups) {

            if (filterGroup.filters.size() == 1) {

                final ClientRegularFilter singleFilter = filterGroup.filters.get(0);

                final JCheckBox checkBox = new JCheckBox(singleFilter.toString());

                if (filterGroup.defaultFilter >= 0) {
                    checkBox.setSelected(true);
                    try {
                        setRemoteRegularFilter(filterGroup, singleFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }

                checkBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            if (ie.getStateChange() == ItemEvent.SELECTED)
                                setRegularFilter(filterGroup, singleFilter);
                            if (ie.getStateChange() == ItemEvent.DESELECTED)
                                setRegularFilter(filterGroup, null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, checkBox);
                formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.getID() + singleFilter.getID(), new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        checkBox.setSelected(!checkBox.isSelected());
                    }
                });
            } else {

                final JComboBox comboBox = new JComboBox(
                        BaseUtils.mergeList(Collections.singletonList("(Все)"), filterGroup.filters).toArray());

                if (filterGroup.defaultFilter >= 0) {
                    ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
                    comboBox.setSelectedItem(defaultFilter);
                    try {
                        setRemoteRegularFilter(filterGroup, defaultFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }

                comboBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            if (ie.getStateChange() == ItemEvent.SELECTED) {
                                setRegularFilter(filterGroup,
                                        ie.getItem() instanceof ClientRegularFilter ? (ClientRegularFilter) ie.getItem() : null);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, comboBox);

                for (final ClientRegularFilter singleFilter : filterGroup.filters) {
                    formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.getID() + singleFilter.getID(), new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            comboBox.setSelectedItem(singleFilter);
                        }
                    });
                }
            }

        }
    }

    private void initializeButtons() {

        KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
        KeyStroke altX = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK);
        KeyStroke altDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
        KeyStroke altR = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK);
        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (isDialogMode() && isReadOnlyMode()) ? 0 : InputEvent.ALT_DOWN_MASK);
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        // Добавляем стандартные кнопки

        if (Main.module.isFull()) {
            AbstractAction printAction = new AbstractAction(form.getPrintFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altP) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    print();
                }
            };
            formLayout.addBinding(altP, "altPPressed", printAction);

            JButton buttonPrint = new ClientButton(printAction);
            buttonPrint.setFocusable(false);

            AbstractAction xlsAction = new AbstractAction(form.getXlsFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altX) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    Main.module.runExcel(remoteForm);
                }
            };
            formLayout.addBinding(altX, "altXPressed", xlsAction);

            JButton buttonXls = new ClientButton(xlsAction);
            buttonXls.setFocusable(false);

            if (!isDialogMode()) {
                formLayout.add(form.getPrintFunction(), buttonPrint);
                formLayout.add(form.getXlsFunction(), buttonXls);
            }
        }

        AbstractAction nullAction = new AbstractAction(form.getNullFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altDel) + ")") {

            public void actionPerformed(ActionEvent ae) {
                nullPressed();
            }
        };
        JButton buttonNull = new ClientButton(nullAction);
        buttonNull.setFocusable(false);

        AbstractAction refreshAction = new AbstractAction(form.getRefreshFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altR) + ")") {

            public void actionPerformed(ActionEvent ae) {
                refreshData();
            }
        };
        JButton buttonRefresh = new ClientButton(refreshAction);
        buttonRefresh.setFocusable(false);

        AbstractAction applyAction = new AbstractAction(form.getApplyFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                applyChanges(false);
            }
        };
        buttonApply = new ClientButton(applyAction);
        buttonApply.setFocusable(false);

        AbstractAction cancelAction = new AbstractAction(form.getCancelFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                cancelChanges();
            }
        };
        buttonCancel = new ClientButton(cancelAction);
        buttonCancel.setFocusable(false);

        AbstractAction okAction = new AbstractAction(form.getOkFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };
        JButton buttonOK = new ClientButton(okAction);
        buttonOK.setFocusable(false);

        AbstractAction closeAction = new AbstractAction(form.getCloseFunction().caption + " (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };
        JButton buttonClose = new ClientButton(closeAction);
        buttonClose.setFocusable(false);

        formLayout.addBinding(altR, "altRPressed", refreshAction);
        formLayout.add(form.getRefreshFunction(), buttonRefresh);

        if (!isDialogMode()) {

            formLayout.addBinding(altEnter, "enterPressed", applyAction);
            formLayout.add(form.getApplyFunction(), buttonApply);

            formLayout.addBinding(escape, "escapePressed", cancelAction);
            formLayout.add(form.getCancelFunction(), buttonCancel);

        } else {

            formLayout.addBinding(altDel, "altDelPressed", nullAction);
            formLayout.add(form.getNullFunction(), buttonNull);

            formLayout.addBinding(altEnter, "enterPressed", okAction);
            formLayout.add(form.getOkFunction(), buttonOK);

            formLayout.addBinding(escape, "escapePressed", closeAction);
            formLayout.add(form.getCloseFunction(), buttonClose);
        }
    }

    private boolean ordersInitialized = false;
    private void initializeOrders() throws IOException {
        ordersInitialized = true;
        // Применяем порядки по умолчанию
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : form.defaultOrders.entrySet()) {
            controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.ADD);
            if (!entry.getValue()) {
                controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.DIR);
            }
        }
    }

    private void applyActions(List<ClientAction> actions, boolean before) throws IOException {
        for(ClientAction action : actions)
            if(action.isBeforeApply()==before)
                action.dispatch(actionDispatcher);
    }

    private void applyRemoteChanges() throws IOException {
        RemoteChanges remoteChanges = remoteForm.getRemoteChanges();

        applyActions(remoteChanges.actions, true);

        Log.incrementBytesReceived(remoteChanges.form.length);
        applyFormChanges(new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(remoteChanges.form)), form, controllers));

        applyActions(remoteChanges.actions, false);

        if (clientNavigator != null) {
            clientNavigator.changeCurrentClass(remoteChanges.classID);
        }
    }

    private void applyFormChanges(ClientFormChanges formChanges) {

        if (formChanges.dataChanged != null && buttonApply != null) {
            if (defaultApplyBackground == null)
                defaultApplyBackground = buttonApply.getBackground();

            dataChanged = formChanges.dataChanged;
            if (dataChanged) {
                buttonApply.setBackground(Color.green);
                buttonApply.setEnabled(true);
                buttonCancel.setEnabled(true);
            } else {
                buttonApply.setBackground(defaultApplyBackground);
                buttonApply.setEnabled(false);
                buttonCancel.setEnabled(false);
            }
        }

        for (Map.Entry<ClientGroupObject, ClassViewType> entry : formChanges.classViews.entrySet()) {
            ClassViewType classView = entry.getValue();
            if (classView != ClassViewType.GRID) {
                currentGridObjects.remove(entry.getKey());
            }
        }
        currentGridObjects.putAll(formChanges.gridObjects);

        for (GroupObjectController controller : controllers.values()) {
            controller.processFormChanges(formChanges, currentGridObjects);
        }

        for (TreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(formChanges, currentGridObjects);
        }

        if (!ordersInitialized) {
            try {
                initializeOrders();
            } catch (IOException e) {
                throw new RuntimeException("Не могу проинициализировать порядки по умолчанию");
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                formLayout.getComponent().revalidate();
                ClientExternalScreen.repaintAll(getID());
            }
        });

        // выдадим сообщение если было от сервера
        if (formChanges.message.length() > 0) {
            Log.printFailedMessage(formChanges.message);
        }
    }

    public void changeGroupObject(ClientGroupObject group, ClientGroupObjectValue objectValue) throws IOException {
        if (controllers.containsKey(group) && !objectValue.equals(controllers.get(group).getCurrentObject())) {
            remoteForm.changeGroupObject(group.getID(), objectValue.serialize(group));

            applyRemoteChanges();
        }
    }

    public void expandGroupObject(ClientGroupObject group, ClientGroupObjectValue objectValue) throws IOException {
        remoteForm.expandGroupObject(group.getID(), objectValue.serialize(group));

        applyRemoteChanges();
    }

    public void changeGroupObject(ClientGroupObject groupObject, Scroll changeType) throws IOException {
        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());
        applyRemoteChanges();
    }


    public void changePropertyDraw(ClientPropertyDraw property, Object value, boolean all, ClientGroupObjectValue columnKey) throws IOException {
        // для глобальных свойств пока не может быть отложенных действий
        if (property.getGroupObject() != null) {
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);
        }

        remoteForm.changePropertyDraw(property.getID(), BaseUtils.serializeObject(value), all, columnKey.serialize(property));
        applyRemoteChanges();
    }

    void addObject(ClientObject object, ClientConcreteClass cls) throws IOException {

        remoteForm.addObject(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void changeClass(ClientObject object, ClientConcreteClass cls) throws IOException {

        SwingUtils.stopSingleAction(object.groupObject.getActionID(), true);

        remoteForm.changeClass(object.getID(), (cls == null) ? -1 : cls.ID);
        applyRemoteChanges();
    }

    public void changeGridClass(ClientObject object, ClientObjectClass cls) throws IOException {

        remoteForm.changeGridClass(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void switchClassView(ClientGroupObject groupObject) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.getID());

        applyRemoteChanges();
    }

    public void changeClassView(ClientGroupObject groupObject, ClassViewType show) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.changeClassView(groupObject.getID(), show);

        applyRemoteChanges();
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType, ClientGroupObjectValue columnKey) throws IOException {
        remoteForm.changePropertyOrder(property.getID(), modiType.serialize(), columnKey.serialize(property));
        applyRemoteChanges();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObject, List<ClientPropertyFilter>>();

    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {

        currentFilters.put(groupObject, conditions);

        remoteForm.clearUserFilters();

        for (List<ClientPropertyFilter> listFilter : currentFilters.values())
            for (ClientPropertyFilter filter : listFilter) {
                remoteForm.addFilter(Serializer.serializeClientFilter(filter));
            }

        applyRemoteChanges();
    }

    private void setRemoteRegularFilter(ClientRegularFilterGroup filterGroup, ClientRegularFilter filter) throws IOException {
        remoteForm.setRegularFilter(filterGroup.getID(), (filter == null) ? -1 : filter.getID());
    }

    private void setRegularFilter(ClientRegularFilterGroup filterGroup, ClientRegularFilter filter) throws IOException {

        setRemoteRegularFilter(filterGroup, filter);

        applyRemoteChanges();
    }

    public void changePageSize(ClientGroupObject groupObject, int pageSize) throws IOException {
        remoteForm.changePageSize(groupObject.getID(), pageSize);
    }


    void print() {

        try {
            Main.frame.runReport(remoteForm);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при печати формы", e);
        }
    }

    void refreshData() {

        try {

            remoteForm.refreshData();

            applyRemoteChanges();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при обновлении формы", e);
        }
    }

    void applyChanges(boolean sureApply) {

        try {

            if (dataChanged) {

                if(!sureApply) {
                    String okMessage = "";
                    for (ClientGroupObject group : form.groupObjects) {
                        if (controllers.containsKey(group)) {
                            okMessage += controllers.get(group).getSaveMessage();
                        }
                    }

                    if (!okMessage.isEmpty()) {
                        if (!(SwingUtils.showConfirmDialog(getComponent(), okMessage, null, JOptionPane.QUESTION_MESSAGE, SwingUtils.YES_BUTTON) == JOptionPane.YES_OPTION)) {
                            return;
                        }
                    }
                }

                if (remoteForm.hasClientApply()) {
                    ClientApply clientApply = remoteForm.checkClientChanges();
                    if (clientApply instanceof CheckFailed) // чтобы не делать лишний RMI вызов
                        Log.printFailedMessage(((CheckFailed) clientApply).message);
                    else {
                        Object clientResult = null;
                        try {
                            clientResult = ((ClientResultAction) clientApply).dispatchResult(actionDispatcher);
                        } catch (Exception e) {
                            throw new RuntimeException("Ошибка при применении изменений", e);
                        }
                        remoteForm.applyClientChanges(clientResult);

                        applyRemoteChanges();
                    }
                } else {
                    remoteForm.applyChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при применении изменений", e);
        }
    }

    boolean cancelChanges() {

        try {

            if (dataChanged) {

                if (SwingUtils.showConfirmDialog(getComponent(), "Вы действительно хотите отменить сделанные изменения ?", null, JOptionPane.WARNING_MESSAGE, SwingUtils.NO_BUTTON) == JOptionPane.YES_OPTION) {
                    remoteForm.cancelChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отмене изменений", e);
        }

        return true;
    }

    public void okPressed() {
        applyChanges(false);
    }

    boolean closePressed() {
        return cancelChanges();
    }

    boolean nullPressed() {
        return true;
    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }

    public void closed() {
        // здесь мы сбрасываем ссылку на remoteForm для того, чтобы сборщик мусора быстрее собрал удаленные объекты
        // это нужно, чтобы connection'ы на сервере закрывались как можно быстрее
        if (remoteForm != null) {
            remoteForm = null;
        }
    }
}