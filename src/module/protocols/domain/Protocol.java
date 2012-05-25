package module.protocols.domain;

import java.util.ArrayList;
import java.util.List;

import module.fileManagement.domain.AbstractFileNode;
import module.fileManagement.domain.DirNode;
import module.fileManagement.domain.Document;
import module.fileManagement.domain.FileNode;
import module.organization.domain.Person;
import module.protocols.domain.util.ProtocolAction;
import module.protocols.domain.util.ProtocolResponsibleType;
import module.protocols.dto.ProtocolCreationBean;
import module.protocols.dto.ProtocolCreationBean.ProtocolResponsibleBean;
import myorg.domain.User;
import myorg.domain.exceptions.DomainException;
import myorg.domain.groups.AnyoneGroup;
import myorg.domain.groups.PersistentGroup;
import myorg.util.BundleUtil;

import org.joda.time.LocalDate;

import pt.ist.fenixWebFramework.rendererExtensions.util.IPresentableEnum;
import pt.ist.fenixWebFramework.services.Service;
import pt.utl.ist.fenix.tools.util.StringNormalizer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

/**
 * 
 * @author Joao Carvalho (joao.pedro.carvalho@ist.utl.pt)
 * 
 */
public class Protocol extends Protocol_Base {

    public static enum RenewTime implements IPresentableEnum {
	YEARS, MONTHS;

	@Override
	public String getLocalizedName() {
	    return BundleUtil.getStringFromResourceBundle("resources/ProtocolsResources", "label.renewTime." + name());
	}
    }

    public Protocol() {
	super();
	setProtocolManager(ProtocolManager.getInstance());
	setActive(Boolean.TRUE);
    }

    public void delete() {
	for (ProtocolHistory history : getProtocolHistories())
	    history.delete();

	removeProtocolManager();
	deleteDomainObject();
    }

    public boolean isActive() {
	if (!getActive())
	    return false;
	else
	    return getCurrentProtocolHistory() != null;
    }

    public ProtocolHistory getCurrentProtocolHistory() {
	for (ProtocolHistory protocolHistory : getProtocolHistories()) {
	    if (protocolHistory.isActive())
		return protocolHistory;
	}
	return null;
    }

    public ProtocolHistory getLastProtocolHistory() {

	return Ordering.from(ProtocolHistory.COMPARATOR_BY_END_DATE).max(getProtocolHistories());

    }

    public List<ProtocolHistory> getCurrentAndFutureProtocolHistories() {

	return Ordering.from(ProtocolHistory.COMPARATOR_BY_BEGIN_DATE).sortedCopy(
		Iterables.filter(getProtocolHistories(), new Predicate<ProtocolHistory>() {

		    @Override
		    public boolean apply(ProtocolHistory history) {
			return !history.isPast();
		    }

		}));

    }

    public boolean hasExternalPartner(final String partnerName) {

	final String name = StringNormalizer.normalize(partnerName);

	return Iterables.any(getProtocolResponsible(), new Predicate<ProtocolResponsible>() {

	    @Override
	    public boolean apply(ProtocolResponsible responsible) {

		if (responsible.getType() != ProtocolResponsibleType.EXTERNAL)
		    return false;

		String unitName = StringNormalizer.normalize(responsible.getUnit().getPartyName().getContent());

		return unitName.equals(name);
	    }
	});
    }

    @Service
    public void renewFor(Integer duration, RenewTime renewTime) {

	LocalDate beginDate = getLastProtocolHistory().getEndDate();
	LocalDate endDate = beginDate;
	if (renewTime == RenewTime.YEARS) {
	    endDate = endDate.plusYears(duration);
	} else if (renewTime == RenewTime.MONTHS) {
	    endDate = endDate.plusMonths(duration);
	}

	new ProtocolHistory(this, beginDate, endDate);
    }

    @Service
    public static Protocol createProtocol(ProtocolCreationBean protocolBean) {

	Protocol protocol = new Protocol();

	protocol.updateFromBean(protocolBean);

	return protocol;
    }

    @Service
    public void updateFromBean(ProtocolCreationBean protocolBean) {

	for (Protocol protocol : ProtocolManager.getInstance().getProtocols()) {
	    if (protocol.equals(this))
		continue;
	    if (protocol.getProtocolNumber().equals(protocolBean.getProtocolNumber()))
		throw new DomainException("error.protocol.number.already.exists", protocolBean.getProtocolNumber());
	}

	this.setProtocolNumber(protocolBean.getProtocolNumber());
	this.setSignedDate(protocolBean.getSignedDate());
	this.setScientificAreas(protocolBean.getScientificAreas());
	this.setProtocolAction(new ProtocolAction(protocolBean.getActionTypes(), protocolBean.getOtherActionTypes()));
	this.setObservations(protocolBean.getObservations());
	this.setNational(true);

	if (protocolBean.getRemovedResponsibles() != null) {
	    for (ProtocolResponsible responsible : protocolBean.getRemovedResponsibles()) {
		if (responsible != null)
		    this.removeProtocolResponsible(responsible);
	    }
	}

	for (ProtocolResponsibleBean bean : protocolBean.getInternalResponsibles()) {
	    ProtocolResponsible responsible = bean.getProtocolResponsible();
	    if (this.hasProtocolResponsible(responsible))
		responsible.updateFromBean(bean);
	    else {
		ProtocolResponsible newResponsible = new ProtocolResponsible(ProtocolResponsibleType.INTERNAL);
		newResponsible.updateFromBean(bean);

		this.addProtocolResponsible(newResponsible);
	    }
	}

	for (ProtocolResponsibleBean bean : protocolBean.getExternalResponsibles()) {
	    ProtocolResponsible responsible = bean.getProtocolResponsible();
	    if (this.hasProtocolResponsible(responsible))
		responsible.updateFromBean(bean);
	    else {
		ProtocolResponsible newResponsible = new ProtocolResponsible(ProtocolResponsibleType.EXTERNAL);
		newResponsible.updateFromBean(bean);

		this.addProtocolResponsible(newResponsible);
	    }
	}

	for (PersistentGroup group : getReaderGroups())
	    removeReaderGroups(group);

	for (PersistentGroup group : protocolBean.getReaders())
	    addReaderGroups(group);

	this.setAllowedToRead(ProtocolManager.createReaderGroup(protocolBean.getReaders()));

	this.setWriterGroup(protocolBean.getWriters());

	this.setAllowedToWrite(ProtocolManager.createWriterGroup(protocolBean.getWriters()));

	this.setVisibilityType(protocolBean.getVisibilityType());

	ProtocolHistory currentProtocolHistory = getCurrentProtocolHistory();

	if (currentProtocolHistory != null) {
	    currentProtocolHistory.setBeginDate(protocolBean.getBeginDate());
	    currentProtocolHistory.setEndDate(protocolBean.getEndDate());
	} else {
	    new ProtocolHistory(this, protocolBean.getBeginDate(), protocolBean.getEndDate());
	}

	PersistentGroup fileReaderGroup = protocolBean.getVisibilityType() == ProtocolVisibilityType.TOTAL ? AnyoneGroup
		.getInstance() : getAllowedToRead();

	ProtocolDirNode dir = getProtocolDir();

	if (dir != null) {
	    dir.setName(protocolBean.getProtocolNumber());
	    dir.setReadGroup(fileReaderGroup);
	    dir.setWriters(protocolBean.getWriters());
	} else {
	    this.setProtocolDir(new ProtocolDirNode(protocolBean.getWriters(), protocolBean.getProtocolNumber(), fileReaderGroup));
	}

    }

    public boolean canBeReadByUser(final User user) {

	return getVisibilityType() != ProtocolVisibilityType.RESTRICTED || getAllowedToRead().isMember(user);
    }

    public boolean canFilesBeReadByUser(User currentUser) {

	return getAllowedToRead().isMember(currentUser) || getVisibilityType() == ProtocolVisibilityType.TOTAL;

    }

    public boolean canBeWrittenByUser(final User user) {

	return getAllowedToWrite().isMember(user);
    }

    public String getPartners() {
	StringBuilder builder = new StringBuilder();
	boolean first = true;
	for (ProtocolResponsible responsible : getProtocolResponsible()) {
	    if (responsible.getType() == ProtocolResponsibleType.INTERNAL)
		continue;

	    if (first)
		first = false;
	    else
		builder.append(", ");

	    builder.append(responsible.getUnit().getPartyName().getContent());
	}
	return builder.toString();
    }

    public String getAllResponsibles() {
	StringBuilder builder = new StringBuilder();
	for (ProtocolResponsible responsible : getProtocolResponsible()) {

	    for (Person person : responsible.getPeople()) {
		if (builder.length() > 0)
		    builder.append(", ");
		builder.append(person.getName());
	    }

	}
	return builder.toString();
    }

    public List<FileNode> getFiles() {

	return getFilesFromDir(getProtocolDir());

    }

    private List<FileNode> getFilesFromDir(DirNode node) {
	List<FileNode> files = new ArrayList<FileNode>();

	for (AbstractFileNode file : node.getChild()) {
	    if (file instanceof FileNode) {
		FileNode fileNode = (FileNode) file;
		files.add(fileNode);
	    } else if (file instanceof DirNode) {
		files.addAll(getFilesFromDir((DirNode) file));
	    }
	}

	return files;
    }

    @Service
    public void uploadFile(String filename, byte[] contents) {

	Document document = new Document(filename, filename, contents);

	new FileNode(getProtocolDir(), document);

    }

}
