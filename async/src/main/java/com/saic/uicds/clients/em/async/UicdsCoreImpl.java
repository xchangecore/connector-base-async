package com.saic.uicds.clients.em.async;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;
import org.springframework.ws.client.WebServiceClientException;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument.UpdateIncidentRequest;
import org.uicds.notificationService.GetMatchingMessagesRequestDocument;
import org.uicds.notificationService.GetMatchingMessagesResponseDocument;
import org.uicds.notificationService.GetMessagesRequestDocument;
import org.uicds.notificationService.GetMessagesResponseDocument;
import org.uicds.notificationService.WorkProductDeletedNotificationType;
import org.uicds.workProductService.GetProductRequestDocument;
import org.uicds.workProductService.GetProductResponseDocument;
import org.uicds.workProductService.WorkProductPublicationResponseType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.Common;
import com.saic.uicds.clients.util.WebServiceClient;

/**
 * Implemenation of the UicdsCore {@link UicdsCore}
 * 
 * @author roger
 * 
 */
public class UicdsCoreImpl implements UicdsCore {

	Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String WORKPRODUCTSERVICE_NS = "http://uicds.org/WorkProductService";
	private static final String PRECISS_NS = "http://www.saic.com/precis/2009/06/structures";
	private static final String EDXLDE_NS = "urn:oasis:names:tc:emergency:EDXL:DE:1.0";
	private static final String EDXLRM_NS = "urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg";
	private static final String NOTIFICATIONSERVICE_NS = "http://uicds.org/NotificationService";

	private static final String WORKPRODUCT_SUMMARY = "WorkProduct";
	private static final String WORK_PRODUCT_PUBLICATION_RESPONSE = "WorkProductPublicationResponse";
	private static final String WORK_PRODUCT_PROCESSING_STATUS = "WorkProductProcessingStatus";
	private static final String WORK_PRODUCT_DELETED_NOTIFICATION = "WorkProductDeletedNotification";
	private static final String EDXLDE_MESSAGE = "EDXLDistribution";
	private static final String EDXLRM_REQUEST_RESOURCE = "RequestResource";

	private StringBuffer processingStatusXPath;

	private WebServiceClient webServiceClient;

	private HashMap<String, ProcessingStatusType> pendingRequestMap = new HashMap<String, ProcessingStatusType>();
	private HashMap<String, UpdateIncidentRequestDocument> updateRequestMap = new HashMap<String, UpdateIncidentRequestDocument>();

	private ArrayList<WorkProductListener> listenerList = new ArrayList<WorkProductListener>();

	private String applicationID;
	private String applicationProfileID;
	private String localID;
	private Collection<String> applicationProfileInterests = new HashSet<String>();
	private EndpointReferenceType applicationEndpoint;
	private UicdsApplicationInstance applicationInstance;

	@Override
	public void setApplicationID(String ID) {

		applicationID = ID;
	}

	@Override
	public String getApplicationID() {

		return applicationID;
	}

	@Override
	public void setLocalID(String ID) {

		localID = ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.UicdsCore#setApplicationProfileID(java.lang
	 * .String)
	 */
	@Override
	public void setApplicationProfileID(String id) {

		applicationProfileID = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.UicdsCore#setWebServiceClient(com.saic.uicds
	 * .clients.util.WebServiceClient)
	 */
	@Override
	public void setWebServiceClient(WebServiceClient client) {

		webServiceClient = client;
	}

	@Override
	public WebServiceClient getWebServiceClient() {

		return webServiceClient;
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.em.async.UicdsCore#setApplicationProfileInterests
	 * (java.util.Collection)
	 */
	@Override
	public void setApplicationProfileInterests(Collection<String> interests) {

		applicationProfileInterests.addAll(interests);
	}

	@Override
	public String getFullResourceInstanceID() {

		return applicationInstance.getFullResourceInstanceID();
	}

	public UicdsCoreImpl() {

		// Create an XPath expression to get to the processing status in a
		// notification
		processingStatusXPath = new StringBuffer();
		processingStatusXPath.append("declare namespace ws='");
		processingStatusXPath.append(WORKPRODUCTSERVICE_NS);
		processingStatusXPath.append("' declare namespace s='");
		processingStatusXPath.append(PRECISS_NS);
		processingStatusXPath.append("'  /*/ws:");
		processingStatusXPath.append(WORK_PRODUCT_PUBLICATION_RESPONSE);
		processingStatusXPath.append("/s:");
		processingStatusXPath.append(WORK_PRODUCT_PROCESSING_STATUS);
	}

	/*
	 * (non-Javadoc) Initialize the UicdsProfile and get the endpoint.
	 * 
	 * @see com.saic.uicds.clients.async.UicdsCore#initialize()
	 */
	@Override
	public Boolean initialize() {

		// Register the application with the UICDS Core
		applicationInstance = new UicdsApplicationInstance();
		applicationInstance.setWebServiceClient(webServiceClient);
		if (!applicationInstance.registerApplication(applicationID, localID,
				applicationProfileID, applicationProfileInterests)) {
			return false;
		}

		// Get the endpoint for polling for notification messages
		applicationEndpoint = applicationInstance.getEndpoint();

		// Initiaize to the current state of the core
		initializeCurrentState();

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saic.uicds.clients.em.async.UicdsCore#shutdown()
	 */
	@Override
	public Boolean shutdown() {

		Boolean results = applicationInstance.unregisterApplication();

		return results;
	};

	/**
	 * Get a list of all the notifications for work products that are currently
	 * active on the core that match my resource profile interests and process
	 * those as standard notifications for the listeners to intializes all the
	 * work product listeners.
	 */
	private void initializeCurrentState() {

		// Ask for reevaluation of the resource instance interests
		GetMatchingMessagesRequestDocument request = GetMatchingMessagesRequestDocument.Factory
				.newInstance();
		request.addNewGetMatchingMessagesRequest().addNewID().setStringValue(
				applicationID);

		// Send the request
		GetMatchingMessagesResponseDocument response = GetMatchingMessagesResponseDocument.Factory
				.newInstance();
		try {
			response = (GetMatchingMessagesResponseDocument) webServiceClient
					.sendRequest(request);
		} catch (XmlMappingException e) {
			log
					.error("Exception processing a request to the core while processing notifications: "
							+ e.getMessage());
		} catch (WebServiceClientException e) {
			log
					.error("Exception processing a request to the core while processing notifications: "
							+ e.getMessage());
		}

		if (response != null
				&& response.getGetMatchingMessagesResponse() != null
				&& response.getGetMatchingMessagesResponse()
						.getWorkProductIdentificationList() != null
				&& response.getGetMatchingMessagesResponse()
						.getWorkProductIdentificationList()
						.sizeOfIdentificationArray() > 0) {
			for (IdentificationType workProductID : response
					.getGetMatchingMessagesResponse()
					.getWorkProductIdentificationList()
					.getIdentificationArray()) {
				// if
				// (workProductID.getType().getStringValue().equals(UicdsIncident.INCIDENT_WP_TYPE))
				// {
				WorkProduct workProduct = getWorkProductFromCore(workProductID);
				processWorkProductUpdateNotification(workProduct);
				// }
			}
		} else {
			log.info("GetMatchingMessages response was null");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.UicdsCore#marshalSendAndReceive(org.apache
	 * .xmlbeans.XmlObject)
	 */
	@Override
	public XmlObject marshalSendAndReceive(XmlObject request) {

		XmlObject response = XmlObject.Factory.newInstance();
		try {
			// log.debug("sending request to uicds core");
			response = webServiceClient.sendRequest(request);
			// log.debug("received response from uicds core");

			XmlObject[] statusArray = response.selectPath(processingStatusXPath
					.toString());
			if (statusArray != null && statusArray.length > 0) {
				// log.debug("received at least one status");

				if (statusArray[0] instanceof ProcessingStatusType) {
					// log.debug("received at least one status of the right type");

					if (((ProcessingStatusType) statusArray[0]).getStatus() == ProcessingStateType.PENDING) {
						// System.out.println("size: " + statusArray.length +
						// " act: "
						// + ((ProcessingStatusType)
						// statusArray[0]).getACT().getStringValue());

						// Make a copy of the status to store
						ProcessingStatusType status = (ProcessingStatusType) ((ProcessingStatusType) statusArray[0])
								.copy();
						pendingRequestMap.put(
								((ProcessingStatusType) statusArray[0])
										.getACT().getStringValue(), status);
						/*
						 * For update request on product of shared core, if it
						 * is Rejected, we need to issue update again. But we
						 * will not have access to updateRequestDocument, so
						 * storing it for future use when a reject notification
						 * is caught.
						 */
						if (request instanceof UpdateIncidentRequestDocument)
							updateRequestMap.put(
									((ProcessingStatusType) statusArray[0])
											.getACT().getStringValue(),
									(UpdateIncidentRequestDocument) request);
					}
				}
			}
		} catch (XmlMappingException e) {
			log
					.error("XmlMappingException caught while  processing a request to the core while sending: "
							+ e.getMessage());
			return null;
		} catch (WebServiceClientException e) {
			log
					.error("WebServiceClientException caught while processing a request to the core while sending: "
							+ e.getMessage());
			return null;
		} catch (Exception e) {
			log
					.error("Exception caught while processing a request to the core while sending: "
							+ e.getMessage());
			return null;
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.UicdsCore#getRequestStatus(com.saic.precis
	 * .x2009.x06.base.IdentifierType)
	 */
	@Override
	public ProcessingStatusType getRequestStatus(
			IdentifierType incidentUpdateACT) {

		if (pendingRequestMap.containsKey(incidentUpdateACT.getStringValue())) {
			return pendingRequestMap.get(incidentUpdateACT.getStringValue());
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.em.async.UicdsCore#getWorkProductFromCore(com.
	 * saic.precis.x2009.x06.base.IdentificationType)
	 */
	public WorkProduct getWorkProductFromCore(IdentificationType workProductID) {

		GetProductRequestDocument request = GetProductRequestDocument.Factory
				.newInstance();
		request.addNewGetProductRequest().setWorkProductIdentification(
				workProductID);
		try {
			GetProductResponseDocument response = (GetProductResponseDocument) marshalSendAndReceive(request);
			return response.getGetProductResponse().getWorkProduct();
		} catch (Exception e) {
			log.error("Error casting response to GetProductResponseDocument");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saic.uicds.clients.async.UicdsCore#processNotifications()
	 */
	@Override
	public void processNotifications() {

		if (applicationEndpoint == null
				|| applicationEndpoint.getAddress() == null) {
			log
					.error("No endpoint for application is set so cannot process notifications");
			return;
		}

		// Create the request to get messages for the application
		GetMessagesRequestDocument request = GetMessagesRequestDocument.Factory
				.newInstance();
		BigInteger max = BigInteger.ONE;
		request.addNewGetMessagesRequest().setMaximumNumber(max);
		XmlCursor xc = request.getGetMessagesRequest().newCursor();
		xc.toLastChild();
		xc.toEndToken();
		xc.toNextToken();
		QName to = new QName("http://www.w3.org/2005/08/addressing", "To");
		xc.insertElementWithText(to, applicationEndpoint.getAddress()
				.getStringValue());
		xc.dispose();

		// Send the request
		XmlObject response = XmlObject.Factory.newInstance();
		try {
			response = webServiceClient.sendRequest(request);
		} catch (XmlMappingException e) {
			log
					.error("Exception processing a request to the core while processing notifications: "
							+ e.getMessage());
		} catch (WebServiceClientException e) {
			log
					.error("Exception processing a request to the core while processing notifications: "
							+ e.getMessage());
		}
		if (response instanceof GetMessagesResponseDocument) {
			processNotificationMessages((GetMessagesResponseDocument) response);
		}
	}

	/**
	 * Process the GetMessagesResponseDocument by processing the
	 * WorkProductPublicationResponse and the WorkProductSummary elements and
	 * pass any work product update notifcations to the work product listeners.
	 * 
	 * @param response
	 */
	public void processNotificationMessages(GetMessagesResponseDocument response) {

		// If there are any notifications
		if (response.getGetMessagesResponse().sizeOfNotificationMessageArray() > 0) {

			// System.out.println(response);
			// dumpNotifications(response.getGetMessagesResponse().getNotificationMessageArray());

			// Process each one
			for (NotificationMessageHolderType message : response
					.getGetMessagesResponse().getNotificationMessageArray()) {

				// Process any WorkProductPublicationResponses
				XmlObject[] objects = message.getMessage().selectChildren(
						new QName(WORKPRODUCTSERVICE_NS,
								WORK_PRODUCT_PUBLICATION_RESPONSE));
				if (objects.length > 0) {
					for (XmlObject object : objects) {
						if (((WorkProductPublicationResponseType) object)
								.getWorkProductProcessingStatus() != null) {
							processWorkProductPublicationMessage(((WorkProductPublicationResponseType) object));
						}
						if (((WorkProductPublicationResponseType) object)
								.getWorkProduct() != null) {
							processWorkProductUpdateNotification(((WorkProductPublicationResponseType) object)
									.getWorkProduct());
						}
					}
					continue;
				}

				// Process work product notifications (summaries)
				objects = message.getMessage().selectChildren(
						new QName(PRECISS_NS, WORKPRODUCT_SUMMARY));
				if (objects.length > 0) {
					for (XmlObject object : objects) {
						if (((WorkProduct) object) != null) {
							processWorkProductUpdateNotification(((WorkProduct) object));
						}
					}
					continue;
				}

				// Process work product notifications (summaries)
				objects = message.getMessage().selectChildren(
						new QName(EDXLDE_NS, EDXLDE_MESSAGE));
				if (objects.length > 0) {
					// System.out.println("Got an EDXL-DE Message");
					for (XmlObject object : objects) {
						processEDXLDEMessage((EDXLDistribution) object);
					}
					continue;
				}

				// Process work product deletion notifications
				objects = message.getMessage().selectChildren(
						NOTIFICATIONSERVICE_NS,
						WORK_PRODUCT_DELETED_NOTIFICATION);
				if (objects.length > 0) {
					for (XmlObject object : objects) {
						if (object instanceof WorkProductDeletedNotificationType) {
							processWorkProductDeletions((WorkProductDeletedNotificationType) object);
						}
					}
				} else {
					log
							.error("No WorkProductPublicationResponse, WorkProduct, or EDXLDistribution to process: \n"
									+ message.getMessage());
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void dumpNotifications(
			NotificationMessageHolderType[] notificationMessageArray) {

		// Process each one
		for (NotificationMessageHolderType message : notificationMessageArray) {

			// System.out.println("NOTIFICATION: "+message.xmlText());

			// Process any WorkProductPublicationResponses
			XmlObject[] objects = message.getMessage().selectChildren(
					new QName(WORKPRODUCTSERVICE_NS,
							WORK_PRODUCT_PUBLICATION_RESPONSE));
			if (objects.length > 0) {
				for (XmlObject object : objects) {
					log.debug("WorkProductPublicationResponses: "
							+ ((WorkProductPublicationResponseType) object)
									.getWorkProductProcessingStatus());
				}
				continue;
			}

			// Process work product notifications (summaries)
			objects = message.getMessage().selectChildren(
					new QName(PRECISS_NS, WORKPRODUCT_SUMMARY));
			if (objects.length > 0) {
				for (XmlObject object : objects) {
					log.debug("WorkProductSummary: " + (WorkProduct) object);
				}
				continue;
			}

			// Process work product notifications (summaries)
			objects = message.getMessage().selectChildren(
					new QName(EDXLDE_NS, EDXLDE_MESSAGE));
			if (objects.length > 0) {
				log.debug("Got an EDXL-DE Message");
				continue;
			}

			// Process work product deletion notifications
			objects = message.getMessage().selectChildren(
					NOTIFICATIONSERVICE_NS, WORK_PRODUCT_DELETED_NOTIFICATION);
			if (objects.length > 0) {
				for (XmlObject object : objects) {
					log.debug("WorkProductDeletedNotification: "
							+ (WorkProductDeletedNotificationType) object);
				}
			} else {
				log
						.debug("No WorkProductPublicationResponse, WorkProduct, or EDXLDistribution to process: \n"
								+ message.getMessage());
			}
		}
	}

	/**
	 * Handle the processing of any EDXL-DE messages that were received as a
	 * notifcation.
	 * 
	 * @param edxl
	 */
	private void processEDXLDEMessage(EDXLDistribution edxldeMessage) {

		log.debug("Processing EDXL-DE message: ");
		for (WorkProductListener listener : listenerList) {
			listener.handleEDXLDEMessage(edxldeMessage);
		}
	}

	/**
	 * Call each registered listener and pass them the WorkProduct Update
	 * notification.
	 * 
	 * @param workProduct
	 */
	private synchronized void processWorkProductUpdateNotification(
			WorkProduct workProduct) {

		log.debug("Processing work product summary: ");
		for (WorkProductListener listener : listenerList) {
			listener.handleWorkProductUpdate(workProduct);
		}
	}

	/**
	 * Call each registered listener and pass them the Work Product Deleted
	 * notification.
	 * 
	 * @param workProductDeletedNotification
	 */
	private synchronized void processWorkProductDeletions(
			WorkProductDeletedNotificationType workProductDeletedNotification) {

		log.debug("Processing work product deletion: ");
		for (WorkProductListener listener : listenerList) {
			listener.handleWorkProductDelete(workProductDeletedNotification);
		}
	}

	/**
	 * Check if we are waiting on this request and if so then update the status
	 * of this request
	 * 
	 * @param processingStatus
	 */
	private void processWorkProductPublicationMessage(
			WorkProductPublicationResponseType publicationResponse) {

		// ProcessingStatusType processingStatus) {
		ProcessingStatusType processingStatus = publicationResponse
				.getWorkProductProcessingStatus();
		// See if we are waiting on this response
		if (pendingRequestMap.containsKey(processingStatus.getACT()
				.getStringValue())) {
			// Update the status in our map
			pendingRequestMap.get(processingStatus.getACT().getStringValue())
					.setStatus(processingStatus.getStatus());
			// if for update, status is rejected then try issuing update again
			if (processingStatus.getStatus().equals(
					ProcessingStateType.REJECTED)
					&& (updateRequestMap.containsKey(processingStatus.getACT()
							.getStringValue()))) {
				/*
				 * 1. Retreive the updateRequest document stored 2. Update
				 * request document with latest/recent workProduct
				 * identification information 3. Try issuing update request
				 * again 4. Since the reupdate generates new ACT remove the old
				 * ACT and also the old updateRequest document
				 */
				reUpdateOnCore(updateRequestMap.get(processingStatus.getACT()
						.getStringValue()));
				updateRequestMap.remove(processingStatus.getACT()
						.getStringValue());
				pendingRequestMap.remove(processingStatus.getACT()
						.getStringValue());
			} else if (processingStatus.getStatus().equals(
					ProcessingStateType.ACCEPTED)) {
				// if status is accepted for update/reupdate, then remove the
				// updateRequestDocument stored for reupdate
				if (updateRequestMap.containsKey(processingStatus.getACT()
						.getStringValue()))
					updateRequestMap.remove(processingStatus.getACT()
							.getStringValue());
			}
		}
		for (WorkProductListener listener : listenerList) {
			listener.handleWorkProductPublicationMessage(publicationResponse);
		}
	}

	/*
	 * 1. ReUpdate takes the updateDocument stored during original update 2.
	 * Extracts the igId from the document 3. Gets the latest workProduct 4.
	 * Updates the request with new workProductIdentification info 5. Issues the
	 * update.
	 */
	private void reUpdateOnCore(
			UpdateIncidentRequestDocument updateRequestDocument) {
		if (updateRequestDocument != null) {
			UpdateIncidentRequest request = updateRequestDocument
					.getUpdateIncidentRequest();
			UICDSIncidentType incidentDocument = request.getIncident();
			// Extract ig info from stored original updateRequest document
			gov.niem.niem.niemCore.x20.IdentificationType ig = incidentDocument
					.getActivityIdentificationArray(0);
			String igId = ig.getIdentificationIDArray(0).getStringValue();
			// retrieve the latest workproduct
			WorkProduct workProduct = getIncidentDocument(igId);
			// update the identification information on the request
			request.setWorkProductIdentification(Common
					.getIdentificationElement(workProduct));
			UpdateIncidentRequestDocument newUpdateRequest = UpdateIncidentRequestDocument.Factory
					.newInstance();
			newUpdateRequest.setUpdateIncidentRequest(request);
			// issue update on core with new updated request
			marshalSendAndReceive(newUpdateRequest);
		}

	}

	private WorkProduct getIncidentDocument(String interestGroupID) {

		GetIncidentListRequestDocument listRequest = GetIncidentListRequestDocument.Factory
				.newInstance();
		listRequest.addNewGetIncidentListRequest();
		try {
			GetIncidentListResponseDocument response = (GetIncidentListResponseDocument) marshalSendAndReceive(listRequest);
			if (response.getGetIncidentListResponse().getWorkProductList()
					.sizeOfWorkProductArray() == 0) {
				return null;
			}

			IdentificationType wpid = null;
			for (WorkProduct workProduct : response
					.getGetIncidentListResponse().getWorkProductList()
					.getWorkProductArray()) {
				PropertiesType properties = Common
						.getPropertiesElement(workProduct);
				if (properties != null
						&& properties.getAssociatedGroups()
								.sizeOfIdentifierArray() > 0
						&& properties.getAssociatedGroups().getIdentifierArray(
								0).getStringValue().equals(interestGroupID)) {
					wpid = Common.getIdentificationElement(workProduct);
					break;
				}
			}

			if (wpid != null) {
				WorkProduct wp = getWorkProductFromCore(wpid);
				return wp;
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.UicdsCore#requestCompleted(com.saic.precis
	 * .x2009.x06.base.IdentifierType)
	 */
	@Override
	public boolean requestCompleted(IdentifierType act) {

		if (pendingRequestMap.containsKey(act.getStringValue())) {
			ProcessingStatusType a = pendingRequestMap
					.get(act.getStringValue());
			if (a.getStatus() != ProcessingStateType.PENDING) {
				return true;
			} else {
				return false;
			}
		} else {
			log.error("Unknown ACT status requested: " + act.getStringValue());
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.WorkProductProducer#registerListener(com
	 * .saic.uicds.clients.async.WorkProductListener)
	 */
	@Override
	public synchronized void registerListener(WorkProductListener listener) {

		listenerList.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saic.uicds.clients.async.WorkProductProducer#unregisterListener(com
	 * .saic.uicds.clients.async.WorkProductListener)
	 */
	@Override
	public synchronized void unregisterListener(WorkProductListener listener) {

		listenerList.remove(listener);
	}

}