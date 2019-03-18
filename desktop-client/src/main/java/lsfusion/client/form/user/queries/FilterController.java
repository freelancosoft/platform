package lsfusion.client.form.user.queries;

import lsfusion.client.form.layout.view.ClientFormLayout;
import lsfusion.client.form.object.table.GroupObjectLogicsSupplier;
import lsfusion.client.form.filter.ClientFilter;

public abstract class FilterController extends QueryController {

    private ClientFilter filter;

    public FilterController(GroupObjectLogicsSupplier logicsSupplier, ClientFilter filter) {
        super(logicsSupplier);
        this.filter = filter;
    }

    protected QueryView createView() {
        return new FilterView(this);
    }

    public void addView(ClientFormLayout layout) {
        layout.add(filter, getView());
    }
}
