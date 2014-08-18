package com.saic.uicds.clients.em.async;

import java.util.ArrayList;

import javax.xml.soap.SOAPException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.apache.xmlbeans.XmlObject;
import org.springframework.oxm.XmlMappingException;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceOperations;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;
import org.uicds.notificationService.GetMatchingMessagesRequestDocument;
import org.uicds.notificationService.GetMatchingMessagesResponseDocument;
import org.uicds.notificationService.GetMessagesRequestDocument;
import org.uicds.notificationService.GetMessagesResponseDocument;
import org.uicds.resourceInstanceService.GetResourceInstanceListRequestDocument;
import org.uicds.resourceInstanceService.GetResourceInstanceListResponseDocument;
import org.uicds.resourceInstanceService.RegisterRequestDocument;
import org.uicds.resourceInstanceService.RegisterResponseDocument;
import org.uicds.resourceProfileService.CreateProfileRequestDocument;
import org.uicds.resourceProfileService.CreateProfileResponseDocument;
import org.uicds.resourceProfileService.GetProfileListRequestDocument;
import org.uicds.resourceProfileService.GetProfileListResponseDocument;
import org.uicds.resourceProfileService.GetProfileRequestDocument;
import org.uicds.resourceProfileService.GetProfileResponseDocument;
import org.uicds.resourceProfileService.ResourceProfile;
import org.uicds.resourceProfileService.ResourceProfileListType;
import org.uicds.workProductService.GetAssociatedWorkProductListRequestDocument;
import org.uicds.workProductService.GetAssociatedWorkProductListResponseDocument;
import org.uicds.workProductService.GetProductRequestDocument;
import org.uicds.workProductService.GetProductResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.base.StateType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;

public class MockWebServiceOperations implements WebServiceOperations {

	public static final String PULL_POINT = "http://clash.us.saic.com:8080/uicds/ws/IncidentCommander@core1.saic.com";
	public static final String INCIDENT_ID = "138";
	public static final String INCIDENT_NAME = "fire";
	public static final String INCIDENT_WORK_PRODUCT_TYPE = "Incident";
	public static final String INCIDENT_WORK_PRODUCT_ID = INCIDENT_WORK_PRODUCT_TYPE
			+ "-22";
	public static final String INCIDENT_WORK_PRODUCT_VERSION = "1";
	public static final String MAP_WORK_PRODUCT_TYPE = "Map";
	public static final String MAP_WORK_PRODUCT_ID = MAP_WORK_PRODUCT_TYPE
			+ "-767";
	public static final String MAP_WORK_PRODUCT_VERSION = "1";
	public static final String RESOURCE_PROFILE_ID = "RMApplication";

	private XmlObject incidentDocument = null;
	private XmlObject mapDocument = null;
	private ArrayList<String> resourceProfileList;

	public static ProcessingStateType.Enum responseProcessingState = ProcessingStateType.ACCEPTED;
	public static XmlObject getMessagesMessage;
	private Integer actIndex = 0;
	
	private boolean throwWorkProductNotFound = false;
	private boolean throwArchiveFailed = false;

	public void setIncidentDocument(XmlObject obj) {
		incidentDocument = obj;
	}

	public void setMapDocument(XmlObject obj) {
		mapDocument = obj;
	}
	

	public ArrayList<String> getResourceProfileList() {
		return resourceProfileList;
	}

	public void setResourceProfileList(ArrayList<String> resourceInstanceList) {
		this.resourceProfileList = resourceInstanceList;
	}
	
	/**
	 * @return the returnEmptyWorkProduct
	 */
	public boolean isThrowWorkProductNotFound() {
		return throwWorkProductNotFound;
	}

	/**
	 * @param returnEmptyWorkProduct the returnEmptyWorkProduct to set
	 */
	public void setThrowWorkProductNotFound(boolean returnEmptyWorkProduct) {
		this.throwWorkProductNotFound = returnEmptyWorkProduct;
	}

	/**
	 * @return the throwArchiveFailed
	 */
	public boolean isThrowArchiveFailed() {
		return throwArchiveFailed;
	}

	/**
	 * @param throwArchiveFailed the throwArchiveFailed to set
	 */
	public void setThrowArchiveFailed(boolean throwArchiveFailed) {
		this.throwArchiveFailed = throwArchiveFailed;
	}

	void setup() {
		incidentDocument = AsyncTestData.getIncidentSample();
		mapDocument = AsyncTestData.getMapSample();
		resourceProfileList = new ArrayList<String>();
		resourceProfileList.add(RESOURCE_PROFILE_ID);
	}

	@Override
	public Object marshalSendAndReceive(Object arg0)
			throws XmlMappingException, WebServiceClientException {

		XmlObject request = (XmlObject) arg0;
		XmlObject response = null;

		if (request instanceof GetProfileListRequestDocument) {
			GetProfileListResponseDocument r = GetProfileListResponseDocument.Factory
					.newInstance();
			ResourceProfileListType list = r.addNewGetProfileListResponse().addNewProfileList();
			for (String pid : resourceProfileList) {
				ResourceProfile p = list.addNewResourceProfile();
				IdentifierType resourceProfileId = IdentifierType.Factory
				.newInstance();
				resourceProfileId.setStringValue(pid);
				p.setID(resourceProfileId);
			}
			response = r;
		}
		
		else if (request instanceof GetProfileRequestDocument) {
			GetProfileResponseDocument r = GetProfileResponseDocument.Factory.newInstance();
			r.addNewGetProfileResponse();
			if (resourceProfileList.size() > 0) {
				for (String pid : resourceProfileList) {
					GetProfileRequestDocument req = (GetProfileRequestDocument)request;
					if (pid.equals(req.getGetProfileRequest().getID().getStringValue())) {
						r.getGetProfileResponse().addNewProfile().addNewID().setStringValue(pid);
					}
				}
			}
			
			response = r;
		}

		else if (request instanceof CreateProfileRequestDocument) {
			CreateProfileRequestDocument req = (CreateProfileRequestDocument)request;
			CreateProfileResponseDocument r = CreateProfileResponseDocument.Factory.newInstance();
			r.addNewCreateProfileResponse().setProfile(req.getCreateProfileRequest().getProfile());
			resourceProfileList.add(req.getCreateProfileRequest().getProfile().getID().getStringValue());
			response = r;
		}
		
		else if (request instanceof CreateIncidentRequestDocument) {
			CreateIncidentResponseDocument r = CreateIncidentResponseDocument.Factory
					.newInstance();

			// set the create as accepted
			ProcessingStatusType status = r.addNewCreateIncidentResponse()
					.addNewWorkProductPublicationResponse()
					.addNewWorkProductProcessingStatus();
			setProcessingStatus(status);

			// Add a simple summary
			WorkProduct summary = r.getCreateIncidentResponse()
					.getWorkProductPublicationResponse().addNewWorkProduct();
			IdentificationType id = IdentificationType.Factory.newInstance();
			id.addNewIdentifier().setStringValue(INCIDENT_WORK_PRODUCT_ID);
			id.addNewType().setStringValue(INCIDENT_WORK_PRODUCT_TYPE);
			id.setState(StateType.ACTIVE);
			UicdsWorkProduct.setIdentifierElement(summary
					.addNewPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), id);

			PropertiesType properties = PropertiesType.Factory.newInstance();
			properties.addNewAssociatedGroups().addNewIdentifier()
					.setStringValue(INCIDENT_ID);
			UicdsWorkProduct.setPropertiesElement(summary.getPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), properties);

			response = r;
		}

		else if (request instanceof GetAssociatedWorkProductListRequestDocument) {
			GetAssociatedWorkProductListResponseDocument r = GetAssociatedWorkProductListResponseDocument.Factory
					.newInstance();

			// Add incident id
			WorkProduct iwps = r.addNewGetAssociatedWorkProductListResponse()
					.addNewWorkProductList().addNewWorkProduct();
			IdentificationType id = IdentificationType.Factory.newInstance();
			id.addNewIdentifier().setStringValue(INCIDENT_WORK_PRODUCT_ID);
			id.addNewType().setStringValue(INCIDENT_WORK_PRODUCT_TYPE);
			id.addNewVersion().setStringValue(INCIDENT_WORK_PRODUCT_VERSION);
			UicdsWorkProduct.setIdentifierElement(iwps.addNewPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), id);

			PropertiesType properties = PropertiesType.Factory.newInstance();
			properties.addNewAssociatedGroups().addNewIdentifier()
					.setStringValue(INCIDENT_ID);
			UicdsWorkProduct.setPropertiesElement(iwps.getPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), properties);

			// Add map product id
			WorkProduct mwps = r.getGetAssociatedWorkProductListResponse()
					.getWorkProductList().addNewWorkProduct();
			id = IdentificationType.Factory.newInstance();
			id.addNewIdentifier().setStringValue(MAP_WORK_PRODUCT_ID);
			id.addNewType().setStringValue(MAP_WORK_PRODUCT_TYPE);
			id.addNewVersion().setStringValue(MAP_WORK_PRODUCT_VERSION);
			UicdsWorkProduct.setIdentifierElement(mwps.addNewPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), id);

			properties = PropertiesType.Factory.newInstance();
			properties.addNewAssociatedGroups().addNewIdentifier()
					.setStringValue(INCIDENT_ID);
			UicdsWorkProduct.setPropertiesElement(mwps.getPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), properties);

			response = r;
		}

		else if (request instanceof GetProductRequestDocument) {
			
			if (throwWorkProductNotFound) {
				throw new WebServiceFaultException("Work Product does not exist");
			}

			// Get the request
			GetProductRequestDocument d = (GetProductRequestDocument) request;

			// Create a response
			GetProductResponseDocument r = GetProductResponseDocument.Factory
					.newInstance();
			r.addNewGetProductResponse().addNewWorkProduct()
					.addNewPackageMetadata();

			// Set id
			UicdsWorkProduct.setIdentifierElement(r.getGetProductResponse()
					.getWorkProduct().getPackageMetadata()
					.addNewPackageMetadataExtensionAbstract(), d
					.getGetProductRequest().getWorkProductIdentification());

			// Set the content and properties
			if (d.getGetProductRequest().getWorkProductIdentification()
					.getType().getStringValue().startsWith(
							INCIDENT_WORK_PRODUCT_TYPE)) {
				setIncidentDataContent(r.getGetProductResponse()
						.getWorkProduct());
			} else if (d.getGetProductRequest().getWorkProductIdentification()
					.getType().getStringValue().startsWith(
							MAP_WORK_PRODUCT_TYPE)) {
				setMapContent(r.getGetProductResponse().getWorkProduct());
			}

			response = r;
		}

		else if (request instanceof UpdateIncidentRequestDocument) {
			UpdateIncidentResponseDocument r = UpdateIncidentResponseDocument.Factory
					.newInstance();
			ProcessingStatusType status = r.addNewUpdateIncidentResponse()
					.addNewWorkProductPublicationResponse()
					.addNewWorkProductProcessingStatus();
			setProcessingStatus(status);
			response = r;
		}

		else if (request instanceof GetMessagesRequestDocument) {
			GetMessagesResponseDocument r = GetMessagesResponseDocument.Factory
					.newInstance();
			r.addNewGetMessagesResponse().addNewNotificationMessage().set(
					getMessagesMessage);
			response = r;
		}

		else if (request instanceof RegisterRequestDocument) {
			RegisterRequestDocument req = (RegisterRequestDocument) request;
			RegisterResponseDocument r = RegisterResponseDocument.Factory
					.newInstance();
			EndpointReferenceType endpoint = r.addNewRegisterResponse()
					.addNewResourceInstance().addNewEndpoints()
					.addNewEndpoint();
			endpoint.addNewAddress().setStringValue(
					req.getRegisterRequest().getID().getStringValue());
			response = r;
		} else if (request instanceof GetResourceInstanceListRequestDocument) {
			GetResourceInstanceListResponseDocument r = GetResourceInstanceListResponseDocument.Factory
					.newInstance();
			r.addNewGetResourceInstanceListResponse();
			response = r;
		}

		else if (request instanceof GetMatchingMessagesRequestDocument) {
			GetMatchingMessagesResponseDocument r = GetMatchingMessagesResponseDocument.Factory
					.newInstance();
			r.addNewGetMatchingMessagesResponse();
			response = r;
		}
		
		else if (request instanceof ArchiveIncidentRequestDocument) {
			
			if (throwArchiveFailed) {
				throw new WebServiceFaultException("Archive failed");
			}
			
			ArchiveIncidentResponseDocument r = ArchiveIncidentResponseDocument.Factory.newInstance();
			r.addNewArchiveIncidentResponse();
			r.getArchiveIncidentResponse().addNewWorkProductProcessingStatus().setStatus(ProcessingStateType.ACCEPTED);
			response = r;
		}

		return response;
	}

	private void setProcessingStatus(ProcessingStatusType status) {
		status.setStatus(responseProcessingState);
		if (responseProcessingState == ProcessingStateType.PENDING) {
			IdentifierType act = IdentifierType.Factory.newInstance();
			act.setStringValue(actIndex.toString());
			status.setACT(act);
			actIndex++;
		}
	}

	private void setIncidentDataContent(WorkProduct workProduct) {
		// Set the properties
		PropertiesType properties = PropertiesType.Factory.newInstance();
		properties.addNewAssociatedGroups().addNewIdentifier().setStringValue(
				INCIDENT_ID);

		if (workProduct.getPackageMetadata() == null) {
			workProduct.addNewPackageMetadata();
		}
		UicdsWorkProduct.setPropertiesElement(workProduct.getPackageMetadata()
				.addNewPackageMetadataExtensionAbstract(), properties);

		if (incidentDocument instanceof UICDSIncidentType) {
			((UICDSIncidentType) incidentDocument).setId(INCIDENT_ID);
		}
		workProduct.addNewStructuredPayload().addNewStructuredPayloadMetadata();
		workProduct.getStructuredPayloadArray(0).set(incidentDocument);

		workProduct.getStructuredPayloadArray(0)
				.addNewStructuredPayloadMetadata().addNewCommunityPedigreeURI()
				.setStringValue("URI");
		// System.out.println("HERE: "+incidentDocument);
	}

	private void setMapContent(WorkProduct workProduct) {
		// Set the properties
		PropertiesType properties = PropertiesType.Factory.newInstance();
		properties.addNewAssociatedGroups().addNewIdentifier().setStringValue(
				INCIDENT_ID);

		if (workProduct.getPackageMetadata() == null) {
			workProduct.addNewPackageMetadata();
		}
		UicdsWorkProduct.setPropertiesElement(workProduct.getPackageMetadata()
				.addNewPackageMetadataExtensionAbstract(), properties);

		workProduct.addNewStructuredPayload().addNewStructuredPayloadMetadata();
		workProduct.getStructuredPayloadArray(0).set(mapDocument);
	}

	@Override
	public Object marshalSendAndReceive(String arg0, Object arg1)
			throws XmlMappingException, WebServiceClientException {
		return null;
	}

	@Override
	public Object marshalSendAndReceive(Object arg0,
			WebServiceMessageCallback arg1) throws XmlMappingException,
			WebServiceClientException {
		return null;
	}

	@Override
	public Object marshalSendAndReceive(String arg0, Object arg1,
			WebServiceMessageCallback arg2) throws XmlMappingException,
			WebServiceClientException {
		return null;
	}

	@Override
	public Object sendAndReceive(WebServiceMessageCallback arg0,
			WebServiceMessageExtractor arg1) throws WebServiceClientException {
		return null;
	}

	@Override
	public boolean sendAndReceive(WebServiceMessageCallback arg0,
			WebServiceMessageCallback arg1) throws WebServiceClientException {
		return false;
	}

	@Override
	public Object sendAndReceive(String arg0, WebServiceMessageCallback arg1,
			WebServiceMessageExtractor arg2) throws WebServiceClientException {
		return null;
	}

	@Override
	public boolean sendAndReceive(String arg0, WebServiceMessageCallback arg1,
			WebServiceMessageCallback arg2) throws WebServiceClientException {
		return false;
	}

	@Override
	public Object sendSourceAndReceive(Source arg0, SourceExtractor arg1)
			throws WebServiceClientException {
		return null;
	}

	@Override
	public Object sendSourceAndReceive(String arg0, Source arg1,
			SourceExtractor arg2) throws WebServiceClientException {
		return null;
	}

	@Override
	public Object sendSourceAndReceive(Source arg0,
			WebServiceMessageCallback arg1, SourceExtractor arg2)
			throws WebServiceClientException {
		return null;
	}

	@Override
	public Object sendSourceAndReceive(String arg0, Source arg1,
			WebServiceMessageCallback arg2, SourceExtractor arg3)
			throws WebServiceClientException {
		return null;
	}

	@Override
	public boolean sendSourceAndReceiveToResult(Source arg0, Result arg1)
			throws WebServiceClientException {
		return false;
	}

	@Override
	public boolean sendSourceAndReceiveToResult(String arg0, Source arg1,
			Result arg2) throws WebServiceClientException {
		return false;
	}

	@Override
	public boolean sendSourceAndReceiveToResult(Source arg0,
			WebServiceMessageCallback arg1, Result arg2)
			throws WebServiceClientException {
		return false;
	}

	@Override
	public boolean sendSourceAndReceiveToResult(String arg0, Source arg1,
			WebServiceMessageCallback arg2, Result arg3)
			throws WebServiceClientException {
		return false;
	}

}
