package com.saic.uicds.clients.em.async;

import java.util.Collection;

import org.apache.xmlbeans.XmlObject;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.WebServiceClient;

/**
 * UICDS Core Interface
 * 
 * <P>
 * Represents an interface to a UICDS core. Allows clients to do
 * reqeust/response web service invocations using XmlBeans marshalling. Handles
 * asynchronous operation by keeping track of responses that were pending and
 * watching for notifications that return the status of pending requests. Allows
 * users to check the status of a pending request. Allows WorkProductListener
 * objects to register to receive all WorkProduct notifications through
 * extension of the WorkProductProducer interface.
 * 
 * @author roger
 * 
 */
public interface UicdsCore extends WorkProductProducer {

	/**
	 * Set the WebServiceClient that will process web service requests
	 * represented as XmlBeans objects
	 * 
	 * @param client
	 */
	public void setWebServiceClient(WebServiceClient client);
	
	/**
	 * Set the WebServiceClient that will process web service requests.
	 * @return WebServiceClient
	 */
	public WebServiceClient getWebServiceClient();

	/**
	 * Set the UICDS application id that will be used for registering the
	 * application
	 * 
	 * @param ID
	 */
	public void setApplicationID(String ID);
	
	/**
	 * Get the UICDS application ID that is used for registering the
	 * application (Resource Instance ID);
	 * @return resource instance id
	 */
	public String getApplicationID();

	/**
	 * Set the application specific local id that will be used for local
	 * identification
	 * 
	 * @param ID
	 */
	public void setLocalID(String ID);

	/**
	 * Set the profile id that will be used for getting notifications
	 * 
	 * @param ID
	 */
	public void setApplicationProfileID(String ID);


	/**
	 * Set the set of interests for the profile for this profile
	 * 
	 * @param interests
	 */
	public void setApplicationProfileInterests(Collection<String> interests);
	
	/**
	 * Get the full resource instance ID as is used in the WorkProductProperties element.
	 * 
	 * @return full resource instance id
	 */
	public String getFullResourceInstanceID();
	
	/**
	 * Any initialization needed for the specific implementation
	 */
	public Boolean initialize();

	/**
	 * Any cleanup activities needed to shut down for the specific implementation.
	 */
	public Boolean shutdown();
	
	/**
	 * Send the XmlBeans object to the WebServiceClient to be marshalled and
	 * sent to the server
	 * 
	 * @param arg0
	 * @return
	 */
	public XmlObject marshalSendAndReceive(XmlObject arg0);

	/**
	 * Get an individual work product from the core
	 * 
	 * @param workProductID
	 * @return
	 */
	public WorkProduct getWorkProductFromCore(IdentificationType workProductID);

	/**
	 * Request a GetMessages from the server and process each message.
	 */
	public void processNotifications();

	/**
	 * Check if the request represented by the act has received a notification
	 * of completion
	 * 
	 * @param act
	 * @return
	 */
	public boolean requestCompleted(IdentifierType act);

	/**
	 * Get the current status of a pending request.
	 * 
	 * @param incidentUpdateACT
	 * @return
	 */
	public ProcessingStatusType getRequestStatus(
			IdentifierType incidentUpdateACT);
}
