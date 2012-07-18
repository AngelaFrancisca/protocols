package module.protocols.domain;

import jvstm.cps.ConsistencyPredicate;
import pt.ist.bennu.core.domain.groups.PersistentGroup;
import pt.ist.fenixWebFramework.services.Service;

public class ProtocolDirNode extends ProtocolDirNode_Base {

    public ProtocolDirNode(ProtocolAuthorizationGroup group, String name, PersistentGroup readers) {
	super();
	setWriters(group);
	setName(name);
	setReadGroup(readers);
	createTrashFolder();
    }

    public ProtocolDirNode(ProtocolAuthorizationGroup group) {
	super();
	setOwnerGroup(group);
	setWriteGroup(group.getAuthorizedWriterGroup());
	setQuota(Long.MAX_VALUE);
	createTrashFolder();
    }

    @Service
    public ProtocolDirNode generateNodeForProtocol(String protocolNumber) {
	return null;
    }

    public void setWriters(ProtocolAuthorizationGroup group) {
	this.setParent(group.getGroupDir());
    }

    @Override
    @ConsistencyPredicate
    public boolean checkParent() {
	return super.checkParent() ? true : hasOwnerGroup();
    }

    @Override
    public void delete() {
	removeWriteGroup();
	removeOwnerGroup();
	removeTrash();
	super.delete();
    }

}
