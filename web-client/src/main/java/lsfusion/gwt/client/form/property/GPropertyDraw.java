package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GHTMLTextType;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.classes.data.GLongType;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.interop.action.ServerResponse;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;
import static lsfusion.gwt.client.form.event.GKeyStroke.getKeyStroke;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.CHANGE;

public class GPropertyDraw extends GComponent implements GPropertyReader, Serializable {
    public int ID;
    public String nativeSID;
    public String sID;
    public String namespace;
    public String caption;
    public String canonicalName;
    public String propertyFormName;
    public String integrationSID;
    
    public String customRenderFunction;

    public String toolTip;
    public boolean clearText;
    public String tableName;
    public String[] interfacesCaptions;
    public GClass[] interfacesTypes;
    public String creationScript;
    public String creationPath;
    public String path;
    public String formPath;

    public GGroupObject groupObject;
    public String columnsName;
    public ArrayList<GGroupObject> columnGroupObjects;

    public boolean isList;

    public GType baseType;
    public String pattern;
    public String defaultPattern;
    public GClass returnClass;

    public boolean boxed = true;

    public GType externalChangeType;
    public Map<String, GAsyncEventExec> asyncExecMap;

    public GType getExternalChangeType() {
        return externalChangeType;
    }

    public boolean hasColumnGroupObjects() {
        return columnGroupObjects != null && !columnGroupObjects.isEmpty();
    }

    public GType getFilterBaseType() {
        return getDefaultCompare().escapeSeparator() ? baseType.getFilterMatchType() : baseType;
    }

    public GType getChangeType() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncInput ? ((GAsyncInput) asyncExec).changeType : null;
    }

    public GInputList getInputList() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncInput ? ((GAsyncInput) asyncExec).inputList : null;
    }

    public GGroupObjectValue filterColumnKeys(GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(columnGroupObjects != null ? columnGroupObjects : Collections.emptyList());
    }

    public Boolean loadingReplaceImage;
    public boolean isLoadingReplaceImage() {
        if(loadingReplaceImage != null)
            return loadingReplaceImage;

        return !hasDynamicImage(); // move to server afterwards
    }

    public static class QuickAccessAction implements CellRenderer.ToolbarAction {
        public final String action;
        public final GKeyStroke keyStroke;
        public final int index;
        public final boolean hover;

        @Override
        public boolean isHover() {
            return hover;
        }

        @Override
        public GKeyStroke getKeyStroke() {
            return keyStroke;
        }

        @Override
        public String getImage() {
            return action;
        }

        @Override
        public void setOnPressed(ImageElement actionImgElement, UpdateContext updateContext) {
            setToolbarAction(actionImgElement, index);
//            setToolbarAction(actionImgElement, () -> updateContext.executeContextAction(index));
        }

        @Override
        public boolean matches(CellRenderer.ToolbarAction action) {
            if(!(action instanceof QuickAccessAction))
                return false;

            return isHover() == action.isHover() && index == ((QuickAccessAction) action).index;
        }

        public QuickAccessAction(String action, GKeyStroke keyStroke, int index, boolean hover) {
            this.action = action;
            this.keyStroke = keyStroke;
            this.index = index;
            this.hover = hover;
        }
    }

    private transient QuickAccessAction[] quickAccessSelectedFocusedActions;
    private transient QuickAccessAction[] quickAccessSelectedActions;
    private transient QuickAccessAction[] quickAccessActions;
    // manual caching
    public QuickAccessAction[] getQuickAccessActions(boolean isSelected, boolean isFocused) {
        if(isSelected) {
            if(isFocused) {
                if (quickAccessSelectedFocusedActions == null) {
                    quickAccessSelectedFocusedActions = calculateQuickAccessActions(true, true);
                }
                return quickAccessSelectedFocusedActions;
            }
            if (quickAccessSelectedActions == null) {
                quickAccessSelectedActions = calculateQuickAccessActions(true, false);
            }
            return quickAccessSelectedActions;
        }

        if(quickAccessActions == null) {
            quickAccessActions = calculateQuickAccessActions(false, false);
        }
        return quickAccessActions;
    }

    private QuickAccessAction[] calculateQuickAccessActions(boolean isSelected, boolean isFocused) {
        GInputList inputList = getInputList();

        List<QuickAccessAction> actions = new ArrayList<>();
        if(inputList != null) {
            for (int i = 0; i < inputList.actions.length; i++) {
                List<GQuickAccess> quickAccessList = inputList.actions[i].quickAccessList;

                boolean enable = false;
                boolean hover = true;
                for (GQuickAccess quickAccess : quickAccessList) {
                    switch (quickAccess.mode) {
                        case ALL:
                            enable = true;
                            if (!quickAccess.hover) {
                                hover = false;
                            }
                            break;
                        case SELECTED:
                            if (isSelected) {
                                enable = true;
                                if (!quickAccess.hover) {
                                    hover = false;
                                }
                            }
                            break;
                        case FOCUSED:
                            if (isSelected && isFocused) {
                                enable = true;
                                if (!quickAccess.hover) {
                                    hover = false;
                                }
                            }
                    }
                }

                GInputListAction action = inputList.actions[i];
                if (enable) {
                    actions.add(new QuickAccessAction(action.action, action.keyStroke, action.index, hover));
                }
            }
        }
        return actions.toArray(new QuickAccessAction[0]);
    }

    public GAsyncEventExec getAsyncEventExec(String actionSID) {
        return asyncExecMap.get(actionSID);
    }

    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction; // programmatic or user
    public boolean hasUserChangeAction() { // user
        if(!hasChangeAction)
            return false;

        if (isHtmlText())
            return externalChangeType instanceof GHTMLTextType;

        // if custom render change is the input of some type, then probably it is a programmatic change (i.e. custom renderer uses changeValue to set this value, and should not be replaced with the input)
        return customRenderFunction == null || externalChangeType == null;
    }

    public boolean disableInputList;

    public GEditBindingMap editBindingMap;

    public ImageHolder imageHolder;
    public boolean hasDynamicImage;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;
    public GCompare defaultCompare;

    public GCompare getDefaultCompare() {
        return defaultCompare != null ? defaultCompare : GCompare.EQUALS;
    }

    public GCompare[] getFilterCompares() {
        return baseType.getFilterCompares();
    }

    public boolean hasStaticImage() {
        return imageHolder != null;
    }
    public boolean hasDynamicImage() { // when it's an action and has dynamic image
        return isAction() && hasDynamicImage;
    }

    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showChangeKey;

    public boolean hasKeyBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent)
                return true;
        return false;
    }
    public String getKeyBindingText() {
        assert hasKeyBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke;
            }
        return result;
    }

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GLoadingReader loadingReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;
    public GImageReader imageReader;

    // for pivoting
    public String formula;
    public ArrayList<GPropertyDraw> formulaOperands;

    public String aggrFunc;
    public ArrayList<GLastReader> lastReaders;
    public boolean lastAggrDesc;

    public GPropertyDraw quickFilterProperty;

    public int charWidth;
    public int charHeight;

    public int valueWidth = -1;
    public int valueHeight = -1;

    public int captionWidth = -1;
    public int captionHeight = -1;

    public boolean autoSize;

    public boolean panelCaptionVertical;
    public Boolean panelCaptionLast;
    public GFlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical;
    
    public GFlexAlignment valueAlignment;

    public Boolean changeOnSingleClick;

    public boolean hide;

    private transient CellRenderer cellRenderer;
    
    public boolean notNull;

    public boolean sticky;

    public boolean hasFooter;

    // eventually gets to PropertyDrawEntity.getEventAction (which is symmetrical to this)
    public String getEventSID(Event editEvent, Result<Integer> contextAction) {
        if (editBindingMap != null) { // property bindings
            String actionSID = editBindingMap.getEventSID(editEvent);
            if(actionSID != null)
                return actionSID;
        }

        Integer inputActionIndex = getInputActionIndex(editEvent, false);
        if(inputActionIndex != null) {
            contextAction.set(inputActionIndex);
            return CHANGE;
        }

        GType changeType = getChangeType();
        return GEditBindingMap.getDefaultEventSID(editEvent, contextAction, changeType == null ? null : changeType.getEditEventFilter(), hasEditObjectAction, hasUserChangeAction(), this::getDialogInputActionIndex);
    }

    public Integer getInputActionIndex(Event editEvent, boolean isEditing) {
        GInputList inputList;
        if (KEYDOWN.equals(editEvent.getType()) && (inputList = getInputList()) != null) {
            GKeyStroke keyStroke = null;
            for (int i = 0; i < inputList.actions.length; i++) {
                GInputListAction action = inputList.actions[i];
                if (action.keyStroke != null) {
                    if (keyStroke == null)
                        keyStroke = getKeyStroke(editEvent);
                    if (keyStroke.equals(action.keyStroke) && bindEditing(action.editingBindingMode, isEditing)) {
                        return action.index;
                    }
                }
            }
        }
        return null;
    }

    public Integer getDialogInputActionIndex() {
        GInputList inputList = getInputList();
        if (inputList != null) {
            return getDialogInputActionIndex(inputList.actions);
        }
        return null;
    }

    public Integer getDialogInputActionIndex(GInputListAction[] actions) {
        if (actions != null && (disableInputList || FormsController.isDialogMode())) {
            for (int i = 0; i < actions.length; i++) {
                GInputListAction action = actions[i];
                //addDialogInputAProp from server
                if (action.action.equals("dialog")) {
                    return action.index;
                }
            }
        }
        return null;
    }

    private boolean bindEditing(GBindingMode bindEditing, boolean isEditing) {
        switch (bindEditing) {
            case AUTO:
            case ALL:
                return true;
            case NO:
                return !isEditing;
            case ONLY:
                return isEditing;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + bindEditing);
        }
    }

    public boolean isFilterChange(Event editEvent, Result<Boolean> contextAction) {
        return GEditBindingMap.isDefaultFilterChange(editEvent, contextAction, baseType.getEditEventFilter());
    }

    public GPropertyDraw(){}

    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        return baseType.createPanelRenderer(form, controller, this, columnKey, captionContainer);
    }

    public boolean isAction() {
        return baseType instanceof GActionType;
    }

    public boolean isBoolean() {
        return baseType instanceof GLogicalType;
    }

    public boolean isHtmlText() {
        return baseType instanceof GHTMLTextType;
    }

    public CellRenderer getCellRenderer() {
        if (cellRenderer == null) {
            if (customRenderFunction != null) {
                cellRenderer = new CustomCellRenderer(this, customRenderFunction);
            } else {
                cellRenderer = baseType.createGridCellRenderer(this);
            }
        }
        return cellRenderer;
    }

    public void setUserPattern(String pattern) {
        if(baseType instanceof GFormatType)
            this.pattern = pattern != null ? pattern : defaultPattern;
    }

    public Object parsePaste(String s, GType parseType) {
        if (s == null) {
            return null;
        }
        if(parseType == null)
            return null;
        try {
            return parseType.parseString(s, pattern);
        } catch (ParseException pe) {
            return null;
        }
    }

    public boolean canUseChangeValueForRendering(GType type) {
        return type != null && baseType.getClass() == type.getClass() && !(baseType instanceof GJSONType);
    }

    public String getDynamicCaption(Object caption) {
        return caption == null ? "" : caption.toString().trim();
    }

    public String getCaption() {
        return caption == null ? "" : caption;
    }

    public String getPanelCaption(String caption) {
        if(showChangeKey && hasKeyBinding())
            caption += " (" + getKeyBindingText() + ")";
        return caption;
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return getMessages().propertyEmptyCaption();
        } else {
            return caption;
        }
    }

    public String getFilterCaption() {
        return getNotEmptyCaption() + " (" + (groupObject != null ? groupObject.getCaption() : "") + ")";
    }

    private static ClientMessages getMessages() {
        return ClientMessages.Instance.get();
    }
    
    public static final String TOOL_TIP_FORMAT =
            "<html><b>%s</b><br>%s";

    public static String getDetailedToolTipFormat() {
        return  createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipCanonicalName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipTable() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipSignature() + ":</b> %s (%s)<br>" +
                "<b>" + getMessages().propertyTooltipScript() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a><br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s<a class='lsf-form-property-declaration'></a> &ensp; <a class='lsf-tooltip-form-decl-help'></a>&ensp;" +
                "</html>";
    }  
    
    public static String getDetailedActionToolTipFormat() {
        return  createTooltipHorizontalSeparator() +
                "<b>sID:</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a><br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s<a class='lsf-form-property-declaration'></a> &ensp; <a class='lsf-tooltip-form-decl-help'></a>&ensp;" +
                "</html>";
    }
    
    public static String getChangeKeyToolTipFormat() {
        return createTooltipHorizontalSeparator() + "<b>" + getMessages().propertyTooltipHotkey() + ":</b> %s<br>";
    }
    public String getQuickActionTooltipText(GKeyStroke keyStroke) {
        return keyStroke != null ? ("<b>" + getMessages().propertyTooltipHotkey() + ":</b> " + keyStroke) : null;
    }

    public String getTooltipText(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String keyBindingText = hasKeyBinding() ? GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), getKeyBindingText()) : "";

        if (!MainFrame.showDetailedInfo) {
            return propCaption.isEmpty() ? null : GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, keyBindingText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? escapeLineBreakHTML(creationPath) : "";
            String scriptFormPath = formPath != null ? escapeLineBreakHTML(formPath) : "";
            
            if (isAction()) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass != null ? this.returnClass.toString() : "";
                String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
                String script = creationScript != null ? escapeLineBreakHTML(escapeHTML(creationScript)) : "";
                
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, tableName, ifaceObjects, returnClass, ifaceClasses,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
    }

    private String escapeHTML(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    public ImageDescription getImage() {
        return imageHolder != null ? imageHolder.getImage() : null;
    }

    @Override
    public String getNativeSID() {
        return nativeSID;
    }

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public double getFlex() {
        if (flex == -2) {
            return getValueWidth(null).getValueFlexSize();
        }
        return flex;
    }

    public boolean isPanelCaptionLast() {
        return panelCaptionLast != null ? panelCaptionLast : (isBoolean() && !panelCaptionVertical);
    }

    public GFlexAlignment getPanelCaptionAlignment() {
        return (panelCaptionAlignment != null && panelCaptionAlignment != GFlexAlignment.STRETCH) ? panelCaptionAlignment : GFlexAlignment.CENTER;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }
    
    public Style.TextAlign getTextAlignStyle() {
        if (valueAlignment != null) {
            switch (valueAlignment) {
                case START:
                    return Style.TextAlign.LEFT;
                case CENTER:
                case STRETCH:
                    return Style.TextAlign.CENTER;
                case END:
                    return Style.TextAlign.RIGHT;
            }
        }
        return null;
    }

    public static ArrayList<GGroupObjectValue> getColumnKeys(GPropertyDraw property, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        ArrayList<GGroupObjectValue> columnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        if (property.columnGroupObjects != null) {
            LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<>();
            for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                ArrayList<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                if (columnGroupKeys != null) {
                    groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                }
            }

            columnKeys = GGroupObject.mergeGroupValues(groupColumnKeys);
        }
        return columnKeys;
    }

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.getPropertyController(this).updateProperty(this, getColumnKeys(this, controller.getCurrentGridObjects()), updateKeys, values);
    }

    // not null
    // padding has to be included for grid column for example, and not for panel property (since flex, width, min-width, etc. doesn't include padding)
    public GSize getValueWidthWithPadding(GFont parentFont) {
        return getValueWidth(parentFont).add(getCellRenderer().getWidthPadding() * 2);
    }
    public GSize getValueHeightWithPadding(GFont parentFont) {
        return getValueHeight(parentFont).add(getCellRenderer().getHeightPadding() * 2);
    }

    // nullable
    public GSize getAutoSizeValueWidth(GFont parentFont) {
        assert autoSize;
        if(valueWidth == -1)
            return null;

        return getValueWidth(parentFont);
    }

    // not null
    public GSize getValueWidth(GFont parentFont) {
        if (valueWidth >= 0) {
            return GSize.getValueSize(valueWidth);
        }

        GFont font = this.font != null ? this.font : parentFont;

        if(charWidth != 0)
            return GType.getFullWidthString(GwtSharedUtils.replicate('0', charWidth), font);

        return baseType.getDefaultWidth(font, this);
    }

    public GSize getCaptionWidth() {
        if(captionWidth >= 0)
            return GSize.getValueSize(captionWidth);

        return null;
    }

    public GSize getCaptionHeight() {
        if(captionHeight >= 0)
            return GSize.getValueSize(captionHeight);

        return null;
    }

    public GSize getHeaderCaptionHeight(GGridPropertyTable table) {
        GSize headerHeight = table.getHeaderHeight();
        if(headerHeight != null)
            return headerHeight;

        return getCaptionHeight();
    }

    public GFormatType getFormatType() {
        return (baseType instanceof GObjectType ? GLongType.instance : ((GFormatType)baseType));
    }

    // nullable
    public GSize getAutoSizeValueHeight(GFont parentFont) {
        assert autoSize;
        if(valueHeight == -1)
            return null;

        return getValueHeight(parentFont);
    }

    // not null
    public GSize getValueHeight(GFont parentFont) {
        if (valueHeight >= 0) {
            return GSize.getValueSize(valueHeight);
        }

        GSize lineHeight;
        lineHeight = GFontMetrics.getSymbolHeight(font != null ? font : parentFont);
        int lines = charHeight > 0 ? charHeight : baseType.getDefaultCharHeight();

        GSize height = lineHeight.scale(lines);

        final ImageDescription image = getImage();
        GSize imageHeight;
        if (image != null && (imageHeight = image.getHeight()) != null) {
            height = height.max(imageHeight);
        }
        return height;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    public boolean isFocusable() {
        if(focusable != null)
            return focusable;
        return !hasKeyBinding();
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return sID + " " + caption;
    }

    @Override
    public boolean isAlignCaption() {
        return caption != null && !hasColumnGroupObjects() && !isAction() && !panelCaptionVertical && !isBoolean();
    }
}
