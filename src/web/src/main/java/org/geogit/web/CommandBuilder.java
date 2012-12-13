package org.geogit.web;

import java.util.Arrays;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.commands.Status;
import org.geogit.web.api.WebAPICommand;
import org.geogit.web.api.commands.Commit;
import org.geogit.web.api.commands.Log;
import org.restlet.Request;
import org.restlet.data.Form;

/**
 *
 */
public class CommandBuilder {

    public static WebAPICommand build(Request request) throws CommandSpecException {
        WebAPICommand command = null;
        String commandName = (String) request.getAttributes().get("command");
        Form options;
        if (request.getEntity().isAvailable()) {
            options = new Form(request.getEntity());
        } else {
            options = new Form();
        }
        Form query = request.getResourceRef().getQueryAsForm();
        options.addAll(query);
        if ("status".equalsIgnoreCase(commandName)) {
            command = buildStatus(options);
        } else if ("log".equalsIgnoreCase(commandName)) {
            command = buildLog(options);
        } else if ("commit".equalsIgnoreCase(commandName)) {
            command = buildCommit(options);
        } else {
            throw new CommandSpecException("'" + commandName + "' is not a geogit command");
        }
        return command;
    }

    static Integer parseInt(Form form, String key, Integer defaultValue) {
        String val = form.getFirstValue(key);
        Integer retval = defaultValue;
        if (val != null) {
            try {
                retval = new Integer(val);
            } catch (NumberFormatException nfe) {
                throw new CommandSpecException("Invalid value '" + val + "' specified for option: " + key);
            }
        }
        return retval;
    }

    static Status buildStatus(Form options) {
        Status command = new Status();
        command.setLimit(parseInt(options, "limit", 50));
        command.setOffset(parseInt(options, "offset", 0));
        return command;
    }

    static Log buildLog(Form options) {
        Log command = new Log();
        command.setLimit(parseInt(options, "limit", 50));
        command.setOffset(parseInt(options, "offset", null));
        command.setPaths(Arrays.asList(options.getValuesArray("path")));
        command.setSince(options.getFirstValue("since"));
        command.setUntil(options.getFirstValue("until"));
        return command;
    }

    static Commit buildCommit(Form options) {
        Commit commit = new Commit();
        commit.setAll(Boolean.valueOf(options.getFirstValue("all", "false")));
        commit.setMessage(options.getFirstValue("message", null));
        return commit;
    }
}
