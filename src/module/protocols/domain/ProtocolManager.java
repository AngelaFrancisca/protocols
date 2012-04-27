package module.protocols.domain;

import java.util.ArrayList;
import java.util.List;

import jvstm.cps.ConsistencyPredicate;
import module.protocols.dto.ProtocolSystemConfigurationBean;
import myorg.domain.ModuleInitializer;
import myorg.domain.MyOrg;
import myorg.domain.groups.AnyoneGroup;
import myorg.domain.groups.PersistentGroup;
import myorg.domain.groups.UnionGroup;
import pt.ist.fenixWebFramework.services.Service;

/**
 * 
 * @author Joao Carvalho (joao.pedro.carvalho@ist.utl.pt)
 * 
 */
public class ProtocolManager extends ProtocolManager_Base implements ModuleInitializer {

    public static ProtocolManager getInstance() {

	ProtocolManager instance = MyOrg.getInstance().getProtocolManager();

	// Should only happen once
	if (instance == null) {
	    instance = createInstance();
	}

	return instance;
    }

    @Service
    private static ProtocolManager createInstance() {

	ProtocolManager manager = new ProtocolManager();
	manager.setAdministrativeGroup(AnyoneGroup.getInstance());

	return manager;
    }

    private ProtocolManager() {
	super();
	setMyOrg(MyOrg.getInstance());
    }

    @ConsistencyPredicate
    protected final boolean checkForDifferentOrganizationalModels() {
	return getInternalOrganizationalModel() == null ? true
		: getInternalOrganizationalModel() != getExternalOrganizationalModel();
    }

    public static PersistentGroup createGroupFor(List<PersistentGroup> groups) {

	PersistentGroup administrativeGroup = getInstance().getAdministrativeGroup();

	if (administrativeGroup != null) {
	    groups = new ArrayList<PersistentGroup>(groups);
	    groups.add(administrativeGroup);
	}

	return new UnionGroup(groups);
    }

    /*
     * (non-Javadoc)
     * 
     * @see myorg.domain.ModuleInitializer#init(myorg.domain.MyOrg)
     */
    @Override
    public void init(MyOrg root) {

    }

    /**
     * @param bean
     */
    @Service
    public void updateFromBean(ProtocolSystemConfigurationBean bean) {
	setInternalOrganizationalModel(bean.getInternalOrganizationalModel());
	setExternalOrganizationalModel(bean.getExternalOrganizationalModel());
	setAdministrativeGroup(bean.getAdministrativeGroup());
    }
}
