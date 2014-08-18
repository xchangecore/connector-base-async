/**
 * 
 */
package com.saic.uicds.clients.em.async;

import java.util.HashMap;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.ws.client.core.WebServiceOperations;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.workProductService.WorkProductPublicationResponseDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.base.StateType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.SpringClient;
import com.saic.uicds.clients.util.WebServiceClient;

/**
 * @author roger
 * 
 */
//@Ignore
public class UicdsIndentTest extends AbstractDependencyInjectionSpringContextTests {
	
	@Autowired
	private UicdsCore uicdsCore;

	@Autowired
    private UicdsIncident uicdsIncident;

    final static private String INCIDENT_WPID = "1";
    final static private String OTHER_INCIDENT_WPID = "3";
    final static private String INCIDENT_WP_TYPE = "incident";

    public void setUicdsIncident(UicdsIncident i) {
        uicdsIncident = i;
    }
    
    public void setUicdsCore(UicdsCore core) {
    	uicdsCore = core;
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath:contexts/async-context.xml" };
    }

    /**
     * Test method for
     * {@link com.saic.dctd.uicds.core.UicdsIncident#Incident(x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution)}
     * .
     */
    @Test
    public void testIncident() {
    	assertNotNull(uicdsIncident);
        assertNotNull(uicdsCore);
        IncidentDocument incidentDoc = AsyncTestData.getIncidentSample();
        UICDSIncidentType incidentType = incidentDoc.getIncident();
        uicdsIncident.createOnCore(incidentType);
//        uicdsIncident.dumpWorkProducts();
        assertTrue("incident is null", uicdsIncident != null);
        String id = uicdsIncident.getIncidentID();
        assertNotNull(id);
        assertTrue("incorrect incident id expected "
                + incidentType.getId() + " but got " + id,id.equals(incidentType.getId()));
    }

    @Test
    public void testArchiveUnknownIncident() {
    	IdentificationType wpid = IdentificationType.Factory.newInstance();
    	wpid.addNewChecksum().setStringValue("check");
    	wpid.addNewIdentifier().setStringValue("id");
    	wpid.addNewType().setStringValue(MockWebServiceOperations.INCIDENT_WORK_PRODUCT_TYPE);
    	wpid.addNewVersion().setStringValue("1");
    	wpid.setState(StateType.ACTIVE);
    	
    	WebServiceClient mockWSClient = uicdsCore.getWebServiceClient();
    	if (mockWSClient instanceof SpringClient) {
    		WebServiceOperations wso = ((SpringClient)mockWSClient).getWebServiceTemplate();
    		if (wso instanceof MockWebServiceOperations) {
    			((MockWebServiceOperations)wso).setThrowWorkProductNotFound(true);
    			ProcessingStatusType status = uicdsIncident.archiveIncident(wpid);
    			assertNotNull("null status",status);
    			assertEquals("wrong status",ProcessingStateType.REJECTED,status.getStatus());
    			((MockWebServiceOperations)wso).setThrowWorkProductNotFound(false);
    			
    			
    			((MockWebServiceOperations)wso).setThrowArchiveFailed(true);
    			status = uicdsIncident.archiveIncident(wpid);
    			assertNotNull("null status",status);
    			assertEquals("wrong status",ProcessingStateType.REJECTED,status.getStatus());
    			((MockWebServiceOperations)wso).setThrowArchiveFailed(false);
    		}
    	}
    	
    }

    @Test
    public void testMapProductAdded() {
    	/*
        HashMap<String, UicdsWorkProduct> wps = uicdsIncident.getWorkProductMap();
        assertTrue("work product array wrong size", uicdsIncident.getWorkProductMap().size() > 0);
        boolean foundMap = false;
        for (String wpid : wps.keySet()) {
            UicdsWorkProduct p = wps.get(wpid);
//             System.out.println(wpid + ":" + p.type);
            if (p.getType().startsWith("Map")) {
                foundMap = true;
            }
        }
        assertTrue("Map not found", foundMap);
       */
    }

    @Test
    public void testWorkProductArray() {
    	/*
        HashMap<String, UicdsWorkProduct> wps = uicdsIncident.getWorkProductMap();
        assertNotNull("work product map is null", wps);
        assertTrue("work product map is empty", wps.size() > 0);
        assertTrue("work product map is wrong size", wps.size() == 2);
        boolean foundIncident = false;
        boolean foundMap = false;
        for (String id : wps.keySet()) {
            if (wps.get(id).getType().startsWith(MockWebServiceOperations.MAP_WORK_PRODUCT_TYPE)) {
                foundMap = true;
            }
            if (wps.get(id).getType().startsWith(
                    MockWebServiceOperations.INCIDENT_WORK_PRODUCT_TYPE)) {
                foundIncident = true;
            }
        }
        assertTrue("Map work product not found", foundMap);
        assertTrue("Incident work product not found", foundIncident);
       */
    }

    @Test
    public void testUpdateAllWorkProduts() {
        uicdsIncident.getAssociatedWorkProducts();
        assertTrue("work product array is empty", uicdsIncident.getWorkProductMap().size() > 0);
        UicdsWorkProduct p = uicdsIncident.getWorkProductMap().get(
                MockWebServiceOperations.INCIDENT_WORK_PRODUCT_ID);
        assertTrue("work product is null", p != null);
    }

    @Test
    public void testCreateFromWorkProduct() {
        UicdsIncident u = new UicdsIncident(uicdsCore);
//        u.setWebServiceClient(uicdsIncident.getWebServiceClient());
        
        IdentificationType id = IdentificationType.Factory.newInstance();
        id.addNewIdentifier().setStringValue(MockWebServiceOperations.INCIDENT_WORK_PRODUCT_ID);
        id.addNewType().setStringValue(MockWebServiceOperations.INCIDENT_WORK_PRODUCT_TYPE);

        boolean created = u.createFromWorkProductIdentifier(id);
        assertTrue("incident not created",created);
        assertNotNull("incident id is null", u.getIncidentID());
        assertTrue("wrong incident id " + u.getIncidentID(), u.getIncidentID().equals(
                MockWebServiceOperations.INCIDENT_ID));
    }

    @Test
    public void testUpdateIncident() {
    	UICDSIncidentType incident = uicdsIncident.getIncidentDocument();
    	if (incident.sizeOfActivityCategoryTextArray() < 1) {
    		incident.addNewActivityCategoryText();
    	}
    	incident.getActivityCategoryTextArray(0).setStringValue("CHANGED");
        assertTrue(uicdsIncident.updateIncident(incident).getStatus() == ProcessingStateType.ACCEPTED);
    }

    @Test
    public void testUpdateIncidentWithPending() {
    	MockWebServiceOperations.responseProcessingState = ProcessingStateType.PENDING;
    	UICDSIncidentType incident = uicdsIncident.getIncidentDocument();
    	if (incident.sizeOfActivityCategoryTextArray() < 1) {
    		incident.addNewActivityCategoryText();
    	}
    	incident.getActivityCategoryTextArray(0).setStringValue("CHANGED");
    	ProcessingStatusType status = uicdsIncident.updateIncident(incident);
        assertTrue(status.getStatus() == ProcessingStateType.PENDING);
        assertNotNull(status.getACT());
        assertNotNull(uicdsCore.getRequestStatus(status.getACT()));
        
        // Set the response to be ACCEPTED
        XmlObject message = getAcceptedMessagesResponse(status, uicdsIncident.getIdentification());
        MockWebServiceOperations.getMessagesMessage = message;
        
        // process get messages
        uicdsCore.processNotifications();
        
        status = uicdsCore.getRequestStatus(status.getACT());
        assertTrue(status.getStatus() == ProcessingStateType.ACCEPTED);
        
    	MockWebServiceOperations.responseProcessingState = ProcessingStateType.ACCEPTED;
    }

    private XmlObject getAcceptedMessagesResponse(ProcessingStatusType status, IdentificationType workProductIdentifier) {
    	// create a notification message
        NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        
        // add the message
        Message m = t.addNewMessage();
        XmlCursor xc = m.newCursor();
        xc.toNextToken();
        XmlCursor ec = null;
        
        // add in an acceptance work product publication response of the work product
        WorkProductPublicationResponseDocument doc = WorkProductPublicationResponseDocument.Factory.newInstance();
        status.setStatus(ProcessingStateType.ACCEPTED);
        
        // add a work product summary
        doc.addNewWorkProductPublicationResponse().setWorkProductProcessingStatus(status);
        WorkProduct workProduct = doc.getWorkProductPublicationResponse().addNewWorkProduct();
        UicdsWorkProduct.setIdentifierElement(workProduct.addNewPackageMetadata().addNewPackageMetadataExtensionAbstract(), workProductIdentifier);
        
        PropertiesType properties = PropertiesType.Factory.newInstance();
        properties.addNewAssociatedGroups().addNewIdentifier().setStringValue(MockWebServiceOperations.INCIDENT_ID);
        UicdsWorkProduct.setPropertiesElement(workProduct.getPackageMetadata().addNewPackageMetadataExtensionAbstract(), properties);
        
        if (doc != null) {
            ec = doc.newCursor();
        }
        if (ec != null) {
            ec.toFirstContentToken();
            ec.moveXml(xc);
            ec.dispose();
        }
        xc.dispose();

        return t;
	}

	/**
     * Test method for
     * {@link com.saic.dctd.uicds.core.UicdsIncident#addWorkProduct(x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution)}
     * .
     */

    @Test
    public void testAddWorkProduct() {
        UICDSIncidentType incidentType = UICDSIncidentType.Factory.newInstance();
        incidentType.addNewActivityName().setStringValue("NEW");
        
        IdentificationType id = IdentificationType.Factory.newInstance();
        WorkProduct wp = WorkProduct.Factory.newInstance();
        id.addNewIdentifier().setStringValue(OTHER_INCIDENT_WPID);
        id.addNewType().setStringValue(INCIDENT_WP_TYPE);
        UicdsWorkProduct.setIdentifierElement(wp.addNewPackageMetadata().addNewPackageMetadataExtensionAbstract(),id);
        
        PropertiesType properties = PropertiesType.Factory.newInstance();
        properties.addNewAssociatedGroups().addNewIdentifier().setStringValue(incidentType.getId());
        UicdsWorkProduct.setPropertiesElement(wp.getPackageMetadata().addNewPackageMetadataExtensionAbstract(), properties);
        
        uicdsIncident.addWorkProduct(INCIDENT_WPID, wp);
        assertTrue("work product array is empty", uicdsIncident.getWorkProductMap().size() > 0);
    }

    @Test
    public void testDeleteWorkProduct() {
        uicdsIncident.deleteWorkProduct(INCIDENT_WPID);
        assertTrue("work product array has wrong number of items ",
                uicdsIncident.getWorkProductMap().size() == 2);
    }

}
