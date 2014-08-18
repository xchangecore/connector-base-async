package com.saic.uicds.clients.em.async;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.opengis.context.ViewContextDocument;

import org.apache.xmlbeans.XmlException;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.resourceProfileService.CreateProfileRequestDocument;

public class AsyncTestData {

    public static final String PULL_POINT = "http://clash.us.saic.com:8080/uicds/ws/IncidentCommander@core1.saic.com";
    public static final String INCIDENT_ID = "138";
    public static final String INCIDENT_NAME = "fire";
    public static final String INCIDENT_WORK_PRODUCT_TYPE = "Incident";
    public static final String INCIDENT_WORK_PRODUCT_ID = INCIDENT_WORK_PRODUCT_TYPE + "-22";
    public static final String MAP_WORK_PRODUCT_TYPE = "Map";
    public static final String MAP_WORK_PRODUCT_ID = MAP_WORK_PRODUCT_TYPE + "-767";
  
    private static final String INCIDENT_FILE = "src/test/resources/workproduct/IncidentSample.xml";
    private static final String MAP_FILE = "src/test/resources/workproduct/ViewContext.xml";
    private static final String PROFILE_FILE = "src/test/resources/workproduct/IncidentCommander.xml";

    public static IncidentDocument getIncidentSample() {
        try {
            InputStream in = new FileInputStream(INCIDENT_FILE);
            IncidentDocument incidentDoc = IncidentDocument.Factory.parse(in);
//            UICDSIncidentType incidentType = UICDSIncidentType.Factory.parse(in);
            UICDSIncidentType incidentType = incidentDoc.getIncident();
            incidentType.addNewActivityIdentification().addNewIdentificationID().setStringValue(INCIDENT_ID);
            incidentType.setId(INCIDENT_ID);
//            System.out.println(incidentType);
            return incidentDoc;
        } catch (FileNotFoundException e) {
            System.err.println("File not found " + e.getMessage());
        } catch (XmlException e) {
            System.err.println("error parsing files " + e.getMessage());
        } catch (IOException e) {
            System.err.println("File not found"  + e.getMessage());
        }
        return null;
    }

    public static ViewContextDocument getMapSample() {
        try {
            InputStream in = new FileInputStream(MAP_FILE);
            ViewContextDocument viewContext = ViewContextDocument.Factory.parse(in);
            return viewContext;
        } catch (FileNotFoundException e) {
            System.err.println("File not found " + e.getMessage());
        } catch (XmlException e) {
            System.err.println("error parsing files " + e.getMessage());
        } catch (IOException e) {
            System.err.println("File not found " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unused")
	private static CreateProfileRequestDocument getProfile() {
        try {
            InputStream in = new FileInputStream(PROFILE_FILE);
            CreateProfileRequestDocument profile = CreateProfileRequestDocument.Factory.parse(in);
            return profile;
        } catch (FileNotFoundException e) {
            System.err.println("File not found " + e.getMessage());
        } catch (XmlException e) {
            System.err.println("error parsing files " + e.getMessage());
        } catch (IOException e) {
            System.err.println("File not found " + e.getMessage());
        }
        return null;
    }


}