package com.saic.uicds.clients.em.async;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.resourceInstanceService.GetResourceInstanceListRequestDocument;
import org.uicds.resourceInstanceService.GetResourceInstanceListResponseDocument;
import org.uicds.resourceInstanceService.RegisterRequestDocument;
import org.uicds.resourceInstanceService.RegisterResponseDocument;
import org.uicds.resourceInstanceService.ResourceInstance;
import org.uicds.resourceInstanceService.GetResourceInstanceListResponseDocument.GetResourceInstanceListResponse;
import org.uicds.resourceInstanceService.UnregisterRequestDocument;
import org.uicds.resourceInstanceService.UnregisterResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import com.saic.uicds.clients.util.WebServiceClient;

public class UicdsApplicationInstance {

	Logger log = LoggerFactory.getLogger(this.getClass());

	private WebServiceClient webServiceClient;

	private ResourceInstance resourceInstance;
	
	private UicdsResourceProfile resourceProfile;

	public WebServiceClient getWebServiceClient() {
		return webServiceClient;
	}

	public void setWebServiceClient(WebServiceClient webServiceClient) {
		this.webServiceClient = webServiceClient;
	}

	public ResourceInstance getResourceInstance() {
		return resourceInstance;
	}
	
	public String getFullResourceInstanceID() {
		if (resourceInstance != null) {
			if (resourceInstance.getID()!= null && resourceInstance.getID().getStringValue() != null
				&& resourceInstance.getSourceIdentification() != null 
				&& resourceInstance.getSourceIdentification().getCoreID() != null) {
				return resourceInstance.getID().getStringValue() + "@" + resourceInstance.getSourceIdentification().getCoreID();
			}
		}
		return null;
	}

	public Boolean registerApplication(String applicationID, String localID,
			String resourceProfileID, Collection<String> interests) {
		
		if (applicationID == null || applicationID.isEmpty()) {
			log.error("UicdsApplicationInstance received a null or empty application identifier");
			return false;
		}
		else if (resourceProfileID == null || resourceProfileID.isEmpty()) {
			log.error("UicdsApplicationInstance received a null or empty resource profile identifier");
			return false;
		}
		
		// default the local identifier if not set
		if (localID == null) {
			localID = applicationID;
		}

		// Get the resource instance if it already exists
		resourceInstance = getExistingResourceInstance(applicationID);
		
		// Initialize a Resource Profile (will create named profile with interest if doesn't exist)
		resourceProfile = new UicdsResourceProfile();
		resourceProfile.setWebServiceClient(webServiceClient);
		resourceProfile.setResourceProfileIdentifier(resourceProfileID);
		resourceProfile.setInterests(interests);
		resourceProfile.initialize();

		// Create the resource instance if it doesn't exist
		if (resourceInstance == null) {
			RegisterRequestDocument request = RegisterRequestDocument.Factory
					.newInstance();
			request.addNewRegisterRequest().addNewID().setStringValue(
					applicationID);
			request.getRegisterRequest().addNewLocalResourceID()
					.setStringValue(localID);
			request.getRegisterRequest().addNewResourceProfileID()
					.setStringValue(resourceProfileID);

			RegisterResponseDocument response = null;
			try {
				response = (RegisterResponseDocument) webServiceClient
						.sendRequest(request);
			} catch (Exception e) {
				log.error("Register Request failed: " + e.getMessage());
				return false;
			}
			resourceInstance = response.getRegisterResponse()
					.getResourceInstance();
		}
		return true;
	}

	public boolean unregisterApplication() {
		UnregisterRequestDocument request = UnregisterRequestDocument.Factory.newInstance();
		request.addNewUnregisterRequest().addNewID().set(resourceInstance.getID());
		
		UnregisterResponseDocument response = null;
		try {
			response = (UnregisterResponseDocument) webServiceClient
			.sendRequest(request);
			return true;
		} catch (Exception e) {
			log.error("Unregister Request failed: " + e.getMessage());
			return false;
		}
	}
	
	private ResourceInstance getExistingResourceInstance(String applicationID) {
		ResourceInstance response = null;

		GetResourceInstanceListResponse list = getResourceInstanceList();
		if (list != null
				&& list.getResourceInstanceList() != null
				&& list.getResourceInstanceList().sizeOfResourceInstanceArray() > 0) {
			for (ResourceInstance resource : list.getResourceInstanceList()
					.getResourceInstanceArray()) {
				if (resource.getID().getStringValue().equals(applicationID)) {
					response = resource;
				}
			}
		}

		return response;
	}

	private GetResourceInstanceListResponse getResourceInstanceList() {
		GetResourceInstanceListRequestDocument listRequest = GetResourceInstanceListRequestDocument.Factory
				.newInstance();
		listRequest.addNewGetResourceInstanceListRequest().setQueryString("");
		GetResourceInstanceListResponseDocument response = GetResourceInstanceListResponseDocument.Factory
				.newInstance();
		response.addNewGetResourceInstanceListResponse();
		try {
			response = (GetResourceInstanceListResponseDocument) webServiceClient
					.sendRequest(listRequest);
		} catch (Exception e) {
			log.error("Register Request failed");
		}
		return response.getGetResourceInstanceListResponse();
	}

	public EndpointReferenceType getEndpoint() {
		if (resourceInstance.getEndpoints().sizeOfEndpointArray() > 0) {
			return resourceInstance.getEndpoints().getEndpointArray(0);
		} else {
			return null;
		}
	}
}
