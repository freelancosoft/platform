package platform.client.navigator;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientToolBarNavigatorWindow extends ClientNavigatorWindow {

    public int type;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public ClientToolBarNavigatorWindow(DataInputStream inStream) throws IOException {
        super(inStream);

        type = inStream.readInt();
        showSelect = inStream.readBoolean();

        verticalTextPosition = inStream.readInt();
        horizontalTextPosition = inStream.readInt();

        verticalAlignment = inStream.readInt();
        horizontalAlignment = inStream.readInt();
    }

    @Override
    public ToolBarNavigatorView getView(INavigatorController controller) {
        return new ToolBarNavigatorView(this, controller);
    }
}
