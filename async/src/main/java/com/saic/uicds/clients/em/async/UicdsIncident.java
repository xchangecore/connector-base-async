/**
 * 
 */
package com.saic.uicds.clients.em.async;

import java.util.HashMap;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CloseIncidentRequestDocument;
import org.uicds.incidentManagementService.CloseIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;
import org.uicds.workProductService.GetAssociatedWorkProductListRequestDocument;
import org.uicds.workProductService.GetAssociatedWorkProductListResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.base.StateType;
import com.saic.precis.x2009.x06.base.ProcessingStateType.Enum;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.Common;

/**
 * UICDS Incident This class represents a UICDS incident and caches all the associated work
 * products. An incident can be created on a UICDS core from a UICDSIncidenType or a local
 * representation of an incident already on a core can be created. When an incident is created all
 * the associated work products are retrieved from the core and stored locally. A UicdsIncident
 * object also registers it self as a WorkProductListener with its UicdsCore. When a
 * WorkProductSummary is received via the listener the full work product will be retrieved and
 * updated or added in the local cache.
 * 
 * @author roger
 */
public class UicdsIncident {

    Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String INCIDENT_WP_TYPE = "Incident";

    public static final String INCIDENT_SERVICE_NS = "http://uicds.org/incident";
    public static final String INCIDENT_ELEMENT_NAME = "Incident";

    private IdentificationType incidentDocID;
    private String incidentID;
    private String name;

    private HashMap<String, UicdsWorkProduct> workProducts = new HashMap<String, UicdsWorkProduct>();

    private UicdsCore uicdsCore;

    /**
     * Default constructor
     */
    public UicdsIncident() {

    }

    /**
     * Constructor Construct and set the UicdsCore to use
     * 
     * @param uicdsCore
     */
    public UicdsIncident(UicdsCore uicdsCore) {

        setUicdsCore(uicdsCore);
    }

    public void setUicdsCore(UicdsCore core) {

        uicdsCore = core;
    }

    public String getIncidentID() {

        return incidentID;
    }

    public String getName() {

        return name;
    }

    public IdentificationType getIdentification() {

        return incidentDocID;
    }

    /**
     * Create an incident on the associated core with the input UICDSIncidentType
     * 
     * @param incident
     * @return
     */
    public String createOnCore(UICDSIncidentType incident) {

        String wpid = null;
        CreateIncidentRequestDocument request = CreateIncidentRequestDocument.Factory.newInstance();
        request.addNewCreateIncidentRequest().setIncident(incident);

        try {
            CreateIncidentResponseDocument response = (CreateIncidentResponseDocument) uicdsCore.marshalSendAndReceive(request);

            Enum status = response.getCreateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus().getStatus();

            // If the create was accepted then get the id and all the associated
            // documents
            if (status == ProcessingStateType.ACCEPTED) {

                // Get the incidents work product id
                IdentificationType workProductID = Common.getIdentificationElement(response.getCreateIncidentResponse().getWorkProductPublicationResponse().getWorkProduct());
                wpid = workProductID.getIdentifier().getStringValue();

                // Get the incident id
                PropertiesType properties = Common.getPropertiesElement(response.getCreateIncidentResponse().getWorkProductPublicationResponse().getWorkProduct());
                if (properties.getAssociatedGroups().sizeOfIdentifierArray() > 0) {
                    incidentID = properties.getAssociatedGroups().getIdentifierArray(0).getStringValue();
                    log.debug("Incident: " + incidentID + " created ...");
                }

                // update all the work products for the incident
                getAssociatedWorkProducts();
            }

        } catch (ClassCastException e) {
            log.error("Error casting response to CreateIncidentResponseDocument");
            wpid = null;

        }

        return wpid;
    }

    /**
     * Get the Incident work product specified by the input id and get all associated work products
     * 
     * @param workProductID
     */
    public boolean createFromWorkProductIdentifier(IdentificationType workProductID) {

        WorkProduct wp = uicdsCore.getWorkProductFromCore(workProductID);
        return createFromWorkProduct(wp);

    }

    public boolean createFromWorkProduct(WorkProduct wp) {

        if (wp != null && wp.sizeOfStructuredPayloadArray() > 0) {
            updateFromWorkProduct(wp);
        }

        // update all the work products for the incident
        getAssociatedWorkProducts();

        return (wp != null && workProducts.size() > 0);
    }

    /**
     * Add a work product to the local cache
     * 
     * @param id
     * @param workProduct
     */
    public void addWorkProduct(String id, WorkProduct workProduct) {

        workProducts.put(id, new UicdsWorkProduct(workProduct));
    }

    /**
     * Delete a work product from the local cache
     * 
     * @param id
     */
    public void deleteWorkProduct(String id) {

        workProducts.remove(id);
    }

    /**
     * Get the default map of work products associatedr this incident
     * 
     * @return UicdsWorkProduct
     */
    public UicdsWorkProduct getDefaultMapWorkProduct() {

        Set<String> wpIds = workProducts.keySet();
        for (String wpId : wpIds) {
            UicdsWorkProduct wp = workProducts.get(wpId);
            if ((wp != null) && (wp.getType().equals(UicdsWorkProduct.MAP_WP_TYPE))) {
                return wp;
            }
        }
        return null;
    }

    /**
     * Get the map of work products for this incident
     * 
     * @return HashMap<Work Product ID, UicdsWorkProduct>
     */
    public HashMap<String, UicdsWorkProduct> getWorkProductMap() {

        return workProducts;
    }

    public boolean containsWorkProduct(String id) {

        return workProducts.containsKey(id);
    }

    /**
     * Get all the work products associated with this incident from the core and add to the cache
     */
    public void getAssociatedWorkProducts() {

        GetAssociatedWorkProductListRequestDocument requestDoc = GetAssociatedWorkProductListRequestDocument.Factory.newInstance();

        requestDoc.addNewGetAssociatedWorkProductListRequest().addNewIdentifier().setStringValue(
            incidentID);

        try {
            GetAssociatedWorkProductListResponseDocument response = (GetAssociatedWorkProductListResponseDocument) uicdsCore.marshalSendAndReceive(requestDoc);

            WorkProductList wpsList = response.getGetAssociatedWorkProductListResponse().getWorkProductList();
            if (wpsList != null && wpsList.sizeOfWorkProductArray() > 0) {
                WorkProduct[] wpsArray = wpsList.getWorkProductArray();
                for (WorkProduct wps : wpsArray) {
                    WorkProduct wp = updateLocalWorkProductFromCore(Common.getIdentificationElement(wps));
                    if (wp != null &&
                        Common.getIdentificationElement(wp).getType().getStringValue().equalsIgnoreCase(
                            INCIDENT_WP_TYPE)) {
                        updateFromWorkProduct(wp);
                    }
                }
            }
        } catch (ClassCastException e) {
            log.error("Error casting reponse to GetAssociatedWorkProductListResponseDocument");
        }

    }

    /**
     * Get an individual work product from the core and update in the cache
     * 
     * @param id
     * @return WorkProduct
     */
    public WorkProduct updateLocalWorkProductFromCore(IdentificationType id) {

        // Only update if it is not ARCHIVE
        if (id.getState() != StateType.ARCHIVE) {

            // Only update if the incoming is a newer version and we already
            // know about the work product
            boolean doUpdate = false;
            if (workProducts.containsKey(id.getIdentifier().getStringValue())) {
                Integer newVersion = Integer.parseInt(id.getVersion().getStringValue());
                WorkProduct currentWP = workProducts.get(id.getIdentifier().getStringValue()).getWorkProduct();
                IdentificationType workProductID = Common.getIdentificationElement(currentWP);
                Integer oldVersion = Integer.parseInt(workProductID.getVersion().getStringValue());
                if (newVersion > oldVersion) {
                    doUpdate = true;
                } else {
                    // Ok if it is equal
                    if (oldVersion > newVersion) {
                        log.error("Received update with older version of work product.  Have: " +
                            oldVersion + " Received: " + newVersion);
                    }
                }
            } else {
                doUpdate = true;
            }

            if (doUpdate) {
                WorkProduct wp = uicdsCore.getWorkProductFromCore(id);
                if (wp != null) {
                    workProducts.put(id.getIdentifier().getStringValue(), new UicdsWorkProduct(wp));
                    if (Common.getIdentificationElement(wp).getType().getStringValue().equalsIgnoreCase(
                        INCIDENT_WP_TYPE)) {
                        updateFromWorkProduct(wp);
                    }
                }
                return wp;
            }
        }
        return workProducts.get(id.getIdentifier().getStringValue()).getWorkProduct();
    }

    /**
     * Get the incident id from a WorkProduct
     * 
     * @param wp
     * @return
     */
    public String getIncidentIDFromWorkProduct(WorkProduct wp) {

        String id = null;
        PropertiesType properties = Common.getPropertiesElement(wp);
        if (properties.getAssociatedGroups().sizeOfIdentifierArray() > 0) {
            id = properties.getAssociatedGroups().getIdentifierArray(0).getStringValue();
        } else {
            log.error("no incident identifier found in work product");
        }
        return id;
    }

    /**
     * Update the information about the incident from the input work product (WorkProduct is an
     * Incident document)
     * 
     * @param wp
     */
    private void updateFromWorkProduct(WorkProduct wp) {

        incidentID = getIncidentIDFromWorkProduct(wp);
        incidentDocID = UicdsWorkProduct.getIdentificationElement(wp);
        IncidentDocument incidentDoc = getIncidentDocumentFromWorkProduct(wp);
        if (incidentDoc == null) {
            WorkProduct wpFromCore = uicdsCore.getWorkProductFromCore(incidentDocID);
            incidentDoc = getIncidentDocumentFromWorkProduct(wpFromCore);
        }
        if (incidentDoc != null && incidentDoc.getIncident() != null &&
            incidentDoc.getIncident().sizeOfActivityNameArray() > 0) {
            name = incidentDoc.getIncident().getActivityNameArray(0).getStringValue();
        } else {
            name = incidentID;
        }

    }

    /**
     * Get the IncidentDocument from the structured payload of a WorkProduct
     * 
     * @param wp
     * @return
     */
    private IncidentDocument getIncidentDocumentFromWorkProduct(WorkProduct wp) {

        IncidentDocument incidentDocument = null;
        if (wp.sizeOfStructuredPayloadArray() == 0) {
            return null;
        }
        XmlObject[] objects = wp.getStructuredPayloadArray(0).selectChildren(
            new QName(INCIDENT_SERVICE_NS, INCIDENT_ELEMENT_NAME));
        if (objects.length > 0) {
            try {
                incidentDocument = IncidentDocument.Factory.parse(objects[0].getDomNode());
            } catch (XmlException e) {
                log.error("Error parsing IncidentDocument from payload: " + e.getMessage());
            }
        }
        return incidentDocument;
    }

    /**
     * Request a close to the incident on the core
     * 
     * @param incident
     * @return
     */
    public ProcessingStatusType closeIncident(IdentificationType incidentIdentification) {

        WorkProduct wp = uicdsCore.getWorkProductFromCore(incidentIdentification);
        if (wp != null) {
            String incidentId = getIncidentIDFromWorkProduct(wp);
            CloseIncidentRequestDocument request = CloseIncidentRequestDocument.Factory.newInstance();
            if (incidentDocID != null) {
                request.addNewCloseIncidentRequest().setIncidentID(incidentId);
                log.debug("Close Incident: " + incidentID);

                try {
                    CloseIncidentResponseDocument response = (CloseIncidentResponseDocument) uicdsCore.marshalSendAndReceive(request);
                    return response.getCloseIncidentResponse().getWorkProductProcessingStatus();
                } catch (ClassCastException e) {
                    log.error("Error casting response to CloseIncidentResponseDocument: " +
                        e.getMessage());
                }
            } else {
                log.error("Incident work product identification element is null");
            }
        }

        ProcessingStatusType status = ProcessingStatusType.Factory.newInstance();
        status.setStatus(ProcessingStateType.REJECTED);
        return status;
    }

    /**
     * Request a close to the incident on the core
     * 
     * @param incident
     * @return
     */
    public ProcessingStatusType archiveIncident(IdentificationType incidentIdentification) {

        WorkProduct wp = uicdsCore.getWorkProductFromCore(incidentIdentification);
        
        ProcessingStatusType rejectStatus = ProcessingStatusType.Factory.newInstance();
    	rejectStatus.setStatus(ProcessingStateType.REJECTED);
    	
        if (wp != null) {
            String incidentId = getIncidentIDFromWorkProduct(wp);
            ArchiveIncidentRequestDocument request = ArchiveIncidentRequestDocument.Factory.newInstance();
            
          
        	
            if (incidentDocID != null) {
                request.addNewArchiveIncidentRequest().setIncidentID("1");
                log.debug("Archive Incident: " + incidentID);
                log.debug("Archive Incident Request: " + request);

                
            	
                try {
                    ArchiveIncidentResponseDocument response = (ArchiveIncidentResponseDocument) uicdsCore.marshalSendAndReceive(request);
                    if(response == null){
                    	
                        return rejectStatus;
                    }
                    return response.getArchiveIncidentResponse().getWorkProductProcessingStatus();
                } catch (ClassCastException e) {
                    log.error("Error casting response to ArchiveIncidentResponseDocument: " +
                        e.getMessage());
                    }
            } else {
                log.error("Incident work product identification element is null");
            }
        }

       
        return rejectStatus;
    }

    /**
     * Request an update to the incident on the core
     * 
     * @param incident
     * @return
     */
    public ProcessingStatusType updateIncident(UICDSIncidentType incident) {

        ProcessingStatusType status = null;
        UpdateIncidentRequestDocument request = UpdateIncidentRequestDocument.Factory.newInstance();
        if (incident != null) {
            if (incident.sizeOfIncidentLocationArray() < 1) {
                log.error("NO location in the incident");
                // log.info(t.xmlText());
            } else {
                if (incident.getIncidentLocationArray(0).sizeOfLocationAreaArray() < 1) {
                    log.error("NO location area in incident");
                }
            }
            if (incidentDocID != null) {
                request.addNewUpdateIncidentRequest().setIncident(incident);
                request.getUpdateIncidentRequest().addNewWorkProductIdentification().set(
                    incidentDocID);

                // log.info("update REQUEST: "+request);
                try {
                    UpdateIncidentResponseDocument response = (UpdateIncidentResponseDocument) uicdsCore.marshalSendAndReceive(request);
                    status = response.getUpdateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus();
                    if (status.getStatus().equals(ProcessingStateType.ACCEPTED)) {
                        WorkProduct wp = response.getUpdateIncidentResponse().getWorkProductPublicationResponse().getWorkProduct();
                        if (wp != null) {
                            // this will update the Identification of the product,
                            // so next update will have the correct Identification
                            updateFromWorkProduct(wp);
                        }
                    }
                } catch (ClassCastException e) {
                    log.error("Error casting response to GetProductResponseDocument");
                    status = ProcessingStatusType.Factory.newInstance();
                    status.setStatus(ProcessingStateType.REJECTED);
                }

            } else {
                log.error("Incident work product identification element is null");
            }
        }
        return status;
    }

    /**
     * Get the UICDSIncidentType from the local cache
     * 
     * @return UICDSIncidentType
     */
    public UICDSIncidentType getIncidentDocument() {

        UICDSIncidentType incident = null;
        for (String wpid : workProducts.keySet()) {
            UicdsWorkProduct wp = workProducts.get(wpid);
            IdentificationType id = UicdsWorkProduct.getIdentificationElement(wp.getWorkProduct());
            if (id != null && id.getType().getStringValue().equalsIgnoreCase(INCIDENT_WP_TYPE)) {
                incident = getIncidentDocumentFromWorkProduct(
                    workProducts.get(wpid).getWorkProduct()).getIncident();
                break;
            }
        }
        return incident;
    }

    /**
     * Print out all the work products associated with this incident
     */
    public void dumpWorkProducts() {

        for (String wpid : workProducts.keySet()) {
            UicdsWorkProduct wp = workProducts.get(wpid);
            log.info(wp.toString());
        }
    }

}
