package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.porcelain.RemoveOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;

public class RemoveWebOp extends AbstractWebAPICommand {

	private String path;
	
	private boolean recursive;
	
    /**
     * Mutator for the path variable
     * 
     * @param path - the path to the feature to be removed
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Mutator for the recursive variable
     * 
     * @param recursive - true to remove a tree and all features under it
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
	
	@Override
	public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, remove requires a transaction to preserve the stability of the repository.");
        }
		if (this.path == null) {
			throw new CommandSpecException("No path was specified for removal.");
		}

		final CommandLocator geogit = this.getCommandLocator(context);
		RemoveOp command = geogit.command(RemoveOp.class);

        NodeRef.checkValidPath(path);

        Optional<NodeRef> node = geogit.command(FindTreeChild.class)
                .setParent(geogit.getWorkingTree().getTree()).setIndex(true)
                .setChildPath(path).call();
        if(!node.isPresent()) {
        	throw new CommandSpecException(path + " did not match any feature or tree.");
        }
        NodeRef nodeRef = node.get();
        if (nodeRef.getType() == TYPE.TREE) {
            if(!recursive) {
            	throw new CommandSpecException("Recursive option must be used to remove a tree.");
            }
        }
		
		command.addPathToRemove(path).call();
		context.setResponseContent(new CommandResponse() {
			@Override
			public void write(ResponseWriter out) throws Exception {
				out.start();
				out.writeElement("Deleted", path);
				out.finish();
			}
		});
	}

}
