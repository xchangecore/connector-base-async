package com.saic.uicds.clients.em.async;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;

import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.uicds.clients.util.WebServiceClient;

public class AsyncClient {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

    private WebServiceClient webServiceClient;

    private String profileID;

    private String uicdsID;

    private static String INCIDENT_EXAMPLE_FILE = "src/test/resources/workproduct/IncidentSample.xml";

    public void setUicdsCore(UicdsCore core) {
        uicdsCore = core;
    }

    public void setWebServiceClient(WebServiceClient client) {
        webServiceClient = client;
    }

    public void setProfileID(String id) {
        profileID = id;
    }

    public void setUicdsID(String id) {
        uicdsID = id;
    }

    /**
     * @param args
     */

    public static void main(String[] args) {

        // Get the spring context and then the AsyncClient object that was
        // configured in it
    	ApplicationContext context = null;

    	try {
    		context = new FileSystemXmlApplicationContext("./async-context.xml");
    		System.err.println("Using local async-context.xml file");

    	} catch (BeansException e) {
    		if (e.getCause() instanceof FileNotFoundException) {
    			System.err.println("Local async-context.xml File not found so using file from jar");
    		} else {
    			System.err.println("Error reading local file context: " + e.getCause().getMessage());
    		}
    	}

    	if (context == null) {
    		context = new ClassPathXmlApplicationContext(new String[] { "contexts/async-context.xml" });
    	}


        AsyncClient asyncClient = (AsyncClient) context.getBean("asyncClient");
        if (asyncClient == null) {
            System.err.println("Could not instantiate asyncClient");
        }

        asyncClient.run(args);
    }

    private void run(String[] args) {
        // set the uri from the system property
        String targetUri = System.getProperty("target.uri");
        if (targetUri != null) {
            webServiceClient.setURI(targetUri);
        }

        // evaluate command line arguments
        for (int i = 0; i < args.length; i++) {

            // if there's a -u switch then there should be a target uri
            if (args[i].equals("-u")) {
                i++;
                if (args[i] == null) {
                    System.out.println("Switch -u must be followed with a target URI");
                    return;
                }
                webServiceClient.setURI(args[i]);
                System.out.println("set URI: " + args[i]);
            } else if (args[i].equals("-p")) {
                i++;
                profileID = args[i];
            } else if (args[i].equals("-i")) {
                i++;
                uicdsID = args[i];
            } else {
                usage();
            }
        }

        // Update the profile and resource ids if specified on the command line
        if (profileID != null) {
        	 uicdsCore.setApplicationProfileID(profileID);
        }
        else {
        	profileID = uicdsCore.getApplicationID();
        }
        if (uicdsID != null) {
        	 uicdsCore.setApplicationID(uicdsID);
        }
        else {
        	uicdsID = uicdsCore.getApplicationID();
        }

        System.out.println("targetUri=" + webServiceClient.getURI() + " profile: " + profileID
            + " UICDS ID: " + uicdsID);

        // Initialize a manager for UICDS incidents (Set before
        // initializeUicdsCore so that it
        // can register as a listener for work products because initializing the
        // core will
        // create local incidents for those already on the core.
        UicdsIncidentManager uicdsIncidentManager = new UicdsIncidentManager();
        uicdsIncidentManager.setUicdsCore(uicdsCore);

        // Initialize the connection to the core
        initializeUicdsCore();

        // process notifications to clear out any notifications left over
        processNotifications(2, 1000);

        // Show the current state of the client
        printStartingState(uicdsIncidentManager);

        // Create an incident (gets all the associated work products)
        log.info("Creating Incident");
        UicdsIncident uicdsIncident = new UicdsIncident();
        uicdsIncident.setUicdsCore(uicdsCore);

        // Create an IncidentDocument to describe the incident
        IncidentDocument incidentDoc = getIncidentSample();
        if (incidentDoc == null) {
            return;
        }

        // Create the incident on the core
        uicdsIncident.createOnCore(incidentDoc.getIncident());

        // Add it to the incident manager
        uicdsIncidentManager.addIncident(uicdsIncident);

        // process notifications to see the incident created
        processNotifications(5, 1000);

        // Get the current incident document
        UICDSIncidentType incidentType = uicdsIncident.getIncidentDocument();

        // Update the incident
        System.out.println("Updating incident: "
            + incidentType.getActivityIdentificationArray(0).getIdentificationIDArray(0).getStringValue());

        // Change the type of incident
        if (incidentType.sizeOfActivityCategoryTextArray() < 1) {
            incidentType.addNewActivityCategoryText();
        }
        incidentType.getActivityCategoryTextArray(0).setStringValue("CHANGED");

        String description = "DEFAULT DESCRIPTION";
        if (incidentType.sizeOfActivityDescriptionTextArray() < 1) {
            incidentType.addNewActivityDescriptionText();
        } else {
            description = incidentType.getActivityDescriptionTextArray(0).getStringValue();
        }
        incidentType.getActivityDescriptionTextArray(0).setStringValue(description + " - ADDITION");

        // Update the incident on the core
        ProcessingStatusType status = uicdsIncident.updateIncident(incidentType);

        // If the request is pending then process requests until the request is
        // accepted or rejected
        // Get the asynchronous completion token
        if (status != null && status.getStatus() == ProcessingStateType.PENDING) {
            IdentifierType incidentUpdateACT = status.getACT();
            System.out.println("Incident update is PENDING");

            // Process notifications from the core until the update request is
            // completed
            // This loop should also process other incoming notification
            // messages such
            // as updates for other work products
            while (!uicdsCore.requestCompleted(incidentUpdateACT)) {
                // Process messages from the core
                uicdsCore.processNotifications();

                // Get the status of the request we are waiting on
                status = uicdsCore.getRequestStatus(incidentUpdateACT);
            }

            // Check the final status of the request
            status = uicdsCore.getRequestStatus(incidentUpdateACT);
            if (status.getStatus() == ProcessingStateType.REJECTED) {
                log.error("UpdateIncident request was rejected: " + status.getMessage());
            }
        } else if (status == null) {
            System.err.println("Processing status for incident update was null");
        } else {
            System.out.println("Incident update was ACCEPTED");

            System.out.println("Close the incident");
            uicdsIncident.closeIncident(uicdsIncident.getIdentification());
            processNotifications(10, 1000);
        }

        // Dump all the work products for the incident
        // uicdsIncident.dumpWorkProducts();
        int i = 0;
        while (i < 30) {
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted: " + e.getMessage());
            }
            uicdsCore.processNotifications();
        }

        System.out.println("Archive the incident");
        uicdsIncident.archiveIncident(uicdsIncident.getIdentification());
        processNotifications(10, 1000);
        
        System.out.println("Remove Resource Instance");
        if (!uicdsCore.shutdown()) {
        	System.err.println("Error shutting down");
        }
    }

    private void printStartingState(UicdsIncidentManager uicdsIncidentManager) {
        log.info("Starting State:");
        HashMap<String, UicdsIncident> incidents = uicdsIncidentManager.getIncidents();
        for (String incidentID : incidents.keySet()) {
            log.info("   Incident ID:   " + incidents.get(incidentID).getIncidentID());
            log.info("   Incident Name: " + incidents.get(incidentID).getName());
            log.info("");
        }
    }

    private void initializeUicdsCore() {
        // The UicdsCore object is created by the Spring Framework as defined in
        // async-context.xml
        // Now set the application specific data for the UicdsCore object

        // Set the UICDS identifier for the application
        uicdsCore.setApplicationID(uicdsID);

        // Set the site local identifier for this application
        uicdsCore.setLocalID(this.getClass().getName());

        // Set the application profile to use for the connection to the core
        uicdsCore.setApplicationProfileID(profileID);

        // Set the Web Service Client that will handle web service invocations
        uicdsCore.setWebServiceClient(webServiceClient);

        // Initialize the connection to the core
        if (!uicdsCore.initialize()) {
            log.error("Initialization failed.  Maybe profile does not exist?");
            return;
        }
    }

    private void processNotifications(int count, int milliSeconds) {
        int iterations = -1;
        while (iterations++ < count) {
            processNotifications(milliSeconds);
        }
    }

    private void processNotifications(int milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted: " + e.getMessage());
        }
        log.info("Processing Notifications ");
        uicdsCore.processNotifications();
    }

    // look for the sample xml file in a relative location
    private static IncidentDocument getIncidentSample() {
        String incidentExampleFile2 = "../" + INCIDENT_EXAMPLE_FILE;
        try {
            InputStream in = new FileInputStream(INCIDENT_EXAMPLE_FILE);
            IncidentDocument incidentDoc = IncidentDocument.Factory.parse(in);
            return incidentDoc;
        } catch (FileNotFoundException e1) {
            try {
                // try again in case this is being executed from the target
                // directory
                InputStream in = new FileInputStream(incidentExampleFile2);
                IncidentDocument incidentDoc = IncidentDocument.Factory.parse(in);
                return incidentDoc;
            } catch (FileNotFoundException e2) {
                System.err.println("File not found as either of the following paths:");
                System.err.println(INCIDENT_EXAMPLE_FILE);
                System.err.println(incidentExampleFile2);
            } catch (XmlException e2) {
                System.err.println("error parsing files " + " " + e2.getMessage());
            } catch (IOException e2) {
                System.err.println("File IO exception: " + incidentExampleFile2 + " "
                    + e2.getMessage());
            }
        } catch (XmlException e1) {
            System.err.println("error parsing files " + " " + e1.getMessage());
        } catch (IOException e1) {
            System.err.println("File IO exception: " + INCIDENT_EXAMPLE_FILE + " "
                + e1.getMessage());
        }
        return null;
    }

    private static void usage() {
        System.out.println("");
        System.out.println("This is the UICDS Asynchronous Example Client.");
        System.out.println("Execution of this client depends on a functioning UICDS server. The default is http://localhost/uicds/core/ws/services");
        System.out.println("To verify that a UICDS server is accessible, use a browser to navigate to http://localhost/uicds/core/ws/services/ResourceProfileService.wsdl\"");
        System.out.println("");
        System.out.println("Usage: java -jar AsyncClient.jar [-u <Server URI>] [-p <Resource-Profile-ID>] [-i <UICDS-ID>]");
        System.out.println("");
        System.out.println("");
    }
}
