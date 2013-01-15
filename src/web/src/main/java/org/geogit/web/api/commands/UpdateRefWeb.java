package org.geogit.web.api.commands;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.base.Optional;

public class UpdateRefWeb implements WebAPICommand {

    private String name;

    private ObjectId newValue;

    private boolean delete;

    public void setName(String name) {
        this.name = name;
    }

    public void setNewValue(ObjectId newValue) {
        this.newValue = newValue;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    @Override
    public void run(CommandContext context) {
        if (name == null) {
            throw new CommandSpecException("No name was given.");
        } else if (delete == false && newValue.equals(ObjectId.NULL)) {
            throw new CommandSpecException(
                    "Nothing specified to update with, must specify either deletion or new value to update to.");
        } else if (!name.contains(Ref.HEADS_PREFIX) && !name.contains(Ref.REMOTES_PREFIX)
                && !name.contains(Ref.TAGS_PREFIX)) {
            throw new CommandSpecException(
                    "Name must have full path, these prefixes are supported: " + Ref.HEADS_PREFIX
                            + ", " + Ref.REMOTES_PREFIX + ", " + Ref.TAGS_PREFIX);
        }

        final GeoGIT geogit = context.getGeoGIT();
        Optional<Ref> ref;
        final Optional<ObjectId> oldValue;
        try {
            oldValue = geogit.command(RevParse.class).setRefSpec(name).call();
            ref = geogit.command(UpdateRef.class).setDelete(delete).setName(name)
                    .setNewValue(newValue).call();
        } catch (Exception e) {
            context.setResponseContent(CommandResponse.error("Aborting UpdateRef: "
                    + e.getMessage()));
            return;
        }

        if (ref.isPresent()) {
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeUpdateRefResponse(name, newValue, oldValue.get());
                    out.finish();
                }
            });
        }
    }

}
