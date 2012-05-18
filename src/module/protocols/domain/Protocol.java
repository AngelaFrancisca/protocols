package module.protocols.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import module.organization.domain.Person;
import module.protocols.domain.util.ProtocolAction;
import module.protocols.domain.util.ProtocolResponsibleType;
import module.protocols.dto.ProtocolCreationBean;
import module.protocols.dto.ProtocolCreationBean.ProtocolResponsibleBean;
import myorg.domain.User;

import org.joda.time.LocalDate;

import pt.ist.fenixWebFramework.services.Service;
import pt.utl.ist.fenix.tools.util.CollectionUtils;
import pt.utl.ist.fenix.tools.util.Predicate;
import pt.utl.ist.fenix.tools.util.StringNormalizer;

/**
 * 
 * @author Joao Carvalho (joao.pedro.carvalho@ist.utl.pt)
 * 
 */
public class Protocol extends Protocol_Base {

    public static enum RenewTime {
	YEARS, MONTHS;
    }

    public Protocol() {
	super();
	setProtocolManager(ProtocolManager.getInstance());
	setActive(Boolean.TRUE);
    }

    public Protocol(String protocolNumber, ProtocolAction protocolAction, LocalDate signedDate, String observations,
	    String scientificAreas) {
	this();
	setProtocolNumber(protocolNumber);
	setProtocolAction(protocolAction);
	setSignedDate(signedDate);
	setObservations(observations);
	setScientificAreas(scientificAreas);
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
	List<ProtocolHistory> histories = new ArrayList<ProtocolHistory>(getProtocolHistories());

	Collections.sort(histories, ProtocolHistory.COMPARATOR_BY_END_DATE);

	return histories.get(histories.size() - 1);
    }

    public boolean hasExternalPartner(final String partnerName) {

	final String name = StringNormalizer.normalize(partnerName);

	return CollectionUtils.anyMatches(getProtocolResponsible(), new Predicate<ProtocolResponsible>() {

	    @Override
	    public boolean evaluate(ProtocolResponsible responsible) {

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

	ProtocolAction action = new ProtocolAction(protocolBean.getActionTypes(), protocolBean.getOtherActionTypes());

	Protocol protocol = new Protocol();

	protocol.setProtocolNumber(protocolBean.getProtocolNumber());
	protocol.setSignedDate(protocolBean.getSignedDate());
	protocol.setScientificAreas(protocolBean.getScientificAreas());
	protocol.setProtocolAction(action);
	protocol.setObservations(protocolBean.getObservations());
	protocol.setNational(false);

	for (ProtocolResponsibleBean bean : protocolBean.getInternalResponsibles()) {
	    ProtocolResponsible responsible = new ProtocolResponsible(ProtocolResponsibleType.INTERNAL, bean.getUnit());
	    for (Person p : bean.getResponsibles()) {
		responsible.addPeople(p);
	    }

	    protocol.addProtocolResponsible(responsible);
	}

	for (ProtocolResponsibleBean bean : protocolBean.getExternalResponsibles()) {
	    ProtocolResponsible responsible = new ProtocolResponsible(ProtocolResponsibleType.EXTERNAL, bean.getUnit());
	    for (Person p : bean.getResponsibles()) {
		responsible.addPeople(p);
	    }

	    protocol.addProtocolResponsible(responsible);
	}

	protocol.setAllowedToRead(ProtocolManager.createReaderGroup(protocolBean.getReaders()));

	protocol.setAllowedToWrite(ProtocolManager.createWriterGroup(protocolBean.getWriters()));

	protocol.setVisibilityType(protocolBean.getVisibilityType());

	new ProtocolHistory(protocol, protocolBean.getBeginDate(), protocolBean.getEndDate());

	protocol.addProtocolFiles(new ProtocolFile());

	return protocol;
    }

    public boolean canBeReadByUser(final User user) {

	return getAllowedToRead().isMember(user) || getVisibilityType() != ProtocolVisibilityType.RESTRICTED;
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

}
