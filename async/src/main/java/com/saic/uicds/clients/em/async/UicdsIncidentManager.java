/**
 * 
 */
package com.saic.uicds.clients.em.async;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.notificationService.WorkProductDeletedNotificationType;
import org.uicds.workProductService.WorkProductPublicationResponseType;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.Common;

/**
 * @author roger
 * 
 */
public class UicdsIncidentManager
    implements WorkProductListener {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

    private HashMap<String, UicdsIncident> incidents = new HashMap<String, UicdsIncident>();

    public UicdsIncidentManager() {

    }

    public UicdsCore getUicdsCore() {

        return uicdsCore;
    }

    public void setUicdsCore(UicdsCore uicdsCore) {

        this.uicdsCore = uicdsCore;

        // Register as notification listener
        uicdsCore.registerListener(this);
    }

    public synchronized void addIncident(UicdsIncident incident) {

        incidents.put(incident.getIncidentID(), incident);
    }

    public synchronized void removeIncident(String incidentID) {

        incidents.remove(incidentID);
    }

    public UicdsIncident getIncident(String incidentID) {

        return incidents.get(incidentID);
    }

    public HashMap<String, UicdsIncident> getIncidents() {

        return incidents;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.clients.em.async.WorkProductListener#handleWorkProductDelete(org.uicds.notificationService.WorkProductDeletedNotificationType)
     */
    @Override
    public void handleWorkProductDelete(
        WorkProductDeletedNotificationType workProductDeletedNotification) {

        log.info("Processing work product deletion: " + workProductDeletedNotification.xmlText());
        // String incidentID =
        // findIncidentIDFromWorkProductID(workProductDeletedNotification.getWorkProductIdentification().getIdentifier().getStringValue());
        // if (incidentID != null) {
        // incidents.get(incidentID).deleteWorkProduct(
        // workProductDeletedNotification.getWorkProductIdentification().getIdentifier().getStringValue());
        // if
        // (workProductDeletedNotification.getWorkProductIdentification().getType().getStringValue().equals(
        // UicdsIncident.INCIDENT_WP_TYPE)) {
        // log.info("Removing incident: " + incidentID);
        // removeIncident(incidentID);
        // }
        // }
    }

    private String findIncidentIDFromWorkProductID(String workProductID) {

        for (String incidentID : incidents.keySet()) {
            if (incidents.get(incidentID).containsWorkProduct(workProductID)) {
                return incidentID;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.clients.em.async.WorkProductListener#handleWorkProductUpdate(com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct)
     */
    @Override
    public synchronized void handleWorkProductUpdate(WorkProduct workProduct) {

        IdentificationType identification = Common.getIdentificationElement(workProduct);
        PropertiesType properties = Common.getPropertiesElement(workProduct);
        String incidentID = properties.getAssociatedGroups().getIdentifierArray(0).getStringValue();

        // If we know about this incident then process the updated work product
        if (incidents.containsKey(incidentID)) {
            log.info("Updating incident: " + incidentID);
            incidents.get(incidentID).updateLocalWorkProductFromCore(identification);
        }
        // If not then create a new UicdsIncident
        else {
            // If this is an Incident type work product then fully create the incident
            UicdsIncident incident = new UicdsIncident(uicdsCore);
            if (identification.getType().getStringValue().equals(UicdsIncident.INCIDENT_WP_TYPE)) {
                if (incident.createFromWorkProduct(workProduct)) {
                    log.info("Creating incident: " + incidentID);
                } else {
                    incident = null;
                }
            }
            // else create an incident which will later get fully populated
            else {
                log.info("Updating incident: " + incidentID);
                incident.updateLocalWorkProductFromCore(identification);
            }
            if (incident != null) {
                incidents.put(incidentID, incident);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.clients.em.async.WorkProductListener#handleWorkProductPublicationMessage(org.uicds.workProductService.WorkProductPublicationResponseType)
     */
    @Override
    public void handleWorkProductPublicationMessage(
        WorkProductPublicationResponseType workProductPublicationResponse) {

        // Default handling by the UicdsCore is sufficient for this implementation

    }

    /* (non-Javadoc)
     * @see com.saic.uicds.clients.em.async.WorkProductListener#handleEDXLDEMessage(x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution)
     */
    @Override
    public void handleEDXLDEMessage(EDXLDistribution edxldeMessage) {

        // This implementation does not process EDXL-DE messages.

    }

}
