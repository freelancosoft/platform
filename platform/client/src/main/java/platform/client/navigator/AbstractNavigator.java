package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public abstract class AbstractNavigator extends JPanel {
    public final RemoteNavigatorInterface remoteNavigator;

    protected final NavigatorTree tree;
    public static final String BASE_ELEMENT_SID = "baseElement";

    public AbstractNavigator(RemoteNavigatorInterface iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new NavigatorTree(this);

        JScrollPane pane = new JScrollPane(tree);
        add(pane);
    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;

    protected java.util.List<ClientNavigatorElement> getNodeElements(ClientNavigatorElement element) throws IOException {
        return element.childrens;
    }

    public java.util.List<ClientNavigatorElement> getTreeElements() throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(remoteNavigator.getNavigatorTree());
    }

    public void nodeChanged(NavigatorTreeNode node) {
        //do nothing
    }
}
