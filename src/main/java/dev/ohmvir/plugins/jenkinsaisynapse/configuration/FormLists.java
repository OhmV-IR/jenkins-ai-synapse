package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

final class FormLists {
    private FormLists() {}

    /**
     * Binds a {@code f:hetero-list} from the submitted form. The browser leaves the field out entirely when the list
     * is empty, which is treated as an empty list so that removing the last item works.
     */
    static <T> List<T> bind(StaplerRequest2 req, Class<T> type, String field) throws ServletException {
        JSONObject form = req.getSubmittedForm();
        Object json = form.opt(field);
        if (json == null) {
            return new ArrayList<>();
        }
        return req.bindJSONToList(type, json);
    }
}
