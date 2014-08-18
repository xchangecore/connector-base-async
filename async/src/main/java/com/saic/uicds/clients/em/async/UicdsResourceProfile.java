package com.saic.uicds.clients.em.async;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.uicds.resourceProfileService.CreateProfileRequestDocument;
import org.uicds.resourceProfileService.CreateProfileResponseDocument;
import org.uicds.resourceProfileService.GetProfileListRequestDocument;
import org.uicds.resourceProfileService.GetProfileListResponseDocument;
import org.uicds.resourceProfileService.GetProfileRequestDocument;
import org.uicds.resourceProfileService.GetProfileResponseDocument;
import org.uicds.resourceProfileService.Interest;
import org.uicds.resourceProfileService.ResourceProfile;
import org.uicds.resourceProfileService.GetProfileListRequestDocument.GetProfileListRequest;
import org.uicds.resourceProfileService.ResourceProfile.Interests;

import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.uicds.clients.util.WebServiceClient;

/**
 * UICDS ResourceProfile Service Interface
 * 
 * This class can be used as an interface to get a particular Resourceprofile or
 * to ask for the first available Resourceprofile if a particular one is not needed
 * for testing.
 * 
 * @author roger
 * 
 */
public class UicdsResourceProfile {

	static private String RESOURCEPROFILEQUERYSTRING = "";

	private String resourceProfileIdentifier;
	private Collection<String> interests;
	private ResourceProfile resourceProfile;
	private WebServiceClient webServiceClient;

	
	public ResourceProfile getResourceProfile() {
		return resourceProfile;
	}
	public void setResourceProfile(ResourceProfile resourceProfile) {
		this.resourceProfile = resourceProfile;
	}

	public Collection<String> getInterests() {
		return interests;
	}
	public void setInterests(Collection<String> interests) {
		this.interests = interests;
	}
	public WebServiceClient getWebServiceClient() {
		return webServiceClient;
	}
	public void setWebServiceClient(WebServiceClient webServiceClient) {
		this.webServiceClient = webServiceClient;
	}
	
	public void setResourceProfileIdentifier(String profileIdentifier) {
		resourceProfileIdentifier = profileIdentifier;		
	}
		
	public String getResourceProfileIdentifier() {
		return resourceProfileIdentifier;
	}
	
	public void initialize() {
		if (resourceProfileIdentifier != null) {
			ResourceProfile resourceProfile = getResourceProfile(resourceProfileIdentifier);
			if (resourceProfile == null || resourceProfile.getID() == null) {
				resourceProfile = createResourceProfile(resourceProfileIdentifier, interests);
			}			
		}
		
	}
	
	/**
	 * Get a particular resourceProfile from the core
	 * @param resourceProfileID
	 * @return
	 */
	public ResourceProfile getResourceProfile(String resourceProfileId) {
		IdentifierType resourceProfileIdentifier = IdentifierType.Factory.newInstance();
		resourceProfileIdentifier.setStringValue(resourceProfileId);
		GetProfileRequestDocument request = GetProfileRequestDocument.Factory.newInstance();
		request.addNewGetProfileRequest().setID(resourceProfileIdentifier);
		GetProfileResponseDocument response = (GetProfileResponseDocument) getWebServiceClient().sendRequest(request);
		if (response.getGetProfileResponse() != null && response.getGetProfileResponse().getProfile() != null) {
			return response.getGetProfileResponse().getProfile();
		}
		else {
			return null;
		}
	}

	/**
	 * Get the first resourceProfile in the list of resourceProfiles
	 * @return
	 */
	public ResourceProfile getFirstResourceProfile() {
		ResourceProfile resourceProfile = null;
		if (getResourceProfileList().size() > 0) {
			resourceProfile = getResourceProfileList().get(0);
		}
		return resourceProfile;
	}


	/**
	 * Get the list of resourceProfiles
	 * @return
	 */
	public ArrayList<ResourceProfile> getResourceProfileList() {

		ArrayList<ResourceProfile> resourceProfiles = new ArrayList<ResourceProfile>();

		if (getWebServiceClient() == null) {
			System.err.println("coreServiceTemplate is null");
			return resourceProfiles;
		}

		GetProfileListRequestDocument request = GetProfileListRequestDocument.Factory.newInstance();

		GetProfileListRequest getListRequest = request.addNewGetProfileListRequest();
		getListRequest.setQueryString(RESOURCEPROFILEQUERYSTRING);

		GetProfileListResponseDocument response = (GetProfileListResponseDocument) getWebServiceClient().sendRequest(request);

		if (response != null && response.getGetProfileListResponse() != null
				&& response.getGetProfileListResponse().getProfileList() != null
				&& response.getGetProfileListResponse().getProfileList().sizeOfResourceProfileArray() > 0) {
			ResourceProfile[] resourceProfileList = response.getGetProfileListResponse().getProfileList().getResourceProfileArray();
			// Access each resourceProfile
			for (int i = 0; i < resourceProfileList.length; i++) {
				resourceProfiles.add(resourceProfileList[i]);
			}

			// set the resourceProfile to the first one
			if (resourceProfiles.size() > 0) {
				setResourceProfile(resourceProfiles.get(0));
			}
		}

		return resourceProfiles;
	}

	/**
	 * Create a resourceProfile
	 * @param createResourceProfileRequest
	 * @return
	 */
	public ResourceProfile createResourceProfile(CreateProfileRequestDocument createResourceProfileRequest) {
		CreateProfileResponseDocument response =
			(CreateProfileResponseDocument) getWebServiceClient().sendRequest(createResourceProfileRequest);
		return response.getCreateProfileResponse().getProfile();
	}

	/**
	 * Create a resourceProfile
	 * @param profileIdentifier
	 * @return
	 */
	public ResourceProfile createResourceProfile(String profileIdentifier, Collection<String> profileInterests) {
		CreateProfileRequestDocument request = CreateProfileRequestDocument.Factory.newInstance();
		ResourceProfile profile = request.addNewCreateProfileRequest().addNewProfile();
		profile.addNewID().setStringValue(profileIdentifier);
        Interests interests = profile.addNewInterests();
        if (profileInterests != null) {
        	for (String interestExpression : profileInterests) {
        		Interest interest = interests.addNewInterest();
        		QName topicExpression = new QName(interestExpression);
        		interest.setTopicExpression(topicExpression.toString());
        	}
        }
		
		CreateProfileResponseDocument response =
			(CreateProfileResponseDocument) getWebServiceClient().sendRequest(request);
		return response.getCreateProfileResponse().getProfile();
	}
}
