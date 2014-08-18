package com.saic.uicds.clients.em.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.ws.client.core.WebServiceOperations;
import org.springframework.ws.client.core.WebServiceTemplate;

import org.uicds.resourceProfileService.ResourceProfile;

import com.saic.uicds.clients.em.async.UicdsResourceProfile;
import com.saic.uicds.clients.util.SpringClient;
import com.saic.uicds.clients.util.WebServiceClient;

public class UicdsResourceProfileTest extends AbstractDependencyInjectionSpringContextTests {

    private UicdsResourceProfile uicdsResourceProfile;

    public void setUicdsResourceProfile(UicdsResourceProfile uicdsResourceProfile) {
        this.uicdsResourceProfile = uicdsResourceProfile;
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath:contexts/async-context.xml" };
    }

    @Test
    public void testSetWebServiceTemplate() {
        assertTrue(uicdsResourceProfile != null);
        assertTrue(uicdsResourceProfile.getWebServiceClient() != null);
    }

    @Test
    public void testGetResourceProfileList() {
        ArrayList<ResourceProfile> resourceProfiles = uicdsResourceProfile.getResourceProfileList();
        assertTrue("Array is empty", resourceProfiles.size() > 0);
    }
    
    @Test
    public void testGetDefaultProfile() {
    	String testProfile = "testprofile";
    	UicdsResourceProfile profile = new UicdsResourceProfile();
    	setupWithEmptyResourceProfileList(profile);

    	profile.setResourceProfileIdentifier(testProfile);
    	Collection<String> interests = new HashSet<String>();
    	interests.add("Incident");
    	profile.setInterests(interests);
    	assertEquals("profile id not set correctly",profile.getResourceProfileIdentifier(),testProfile);
    	
    	profile.initialize();
    	ArrayList<ResourceProfile> resourceProfiles = profile.getResourceProfileList();
    	assertTrue("Array is empty", resourceProfiles.size() == 1);
    	assertEquals("wrong resource profile",resourceProfiles.get(0).getID().getStringValue(),testProfile);
    	
    	resetToDefaultResourceProfileList();
    }

	private void setupWithEmptyResourceProfileList(UicdsResourceProfile profile) {
		WebServiceClient wsc = uicdsResourceProfile.getWebServiceClient();
		if (wsc instanceof SpringClient) {
			SpringClient s = (SpringClient)wsc;
			WebServiceOperations wso = s.getWebServiceTemplate();
			if (wso instanceof MockWebServiceOperations) {
				MockWebServiceOperations m = (MockWebServiceOperations)wso;
				ArrayList<String> list = new ArrayList<String>();
				m.setResourceProfileList(list);
				assertTrue("profile list not empty",m.getResourceProfileList().size() == 0);
				s.setWebServiceTemplate(m);
				profile.setWebServiceClient(s);
			}
		}
		else {
			profile.setWebServiceClient(wsc);
		}
	}
	
	private void resetToDefaultResourceProfileList() {
		WebServiceClient wsc = uicdsResourceProfile.getWebServiceClient();
		if (wsc instanceof SpringClient) {
			SpringClient s = (SpringClient)wsc;
			WebServiceOperations wso = s.getWebServiceTemplate();
			if (wso instanceof MockWebServiceOperations) {
				MockWebServiceOperations m = (MockWebServiceOperations)wso;
				ArrayList<String> list = new ArrayList<String>();
				list.add(MockWebServiceOperations.RESOURCE_PROFILE_ID);
				m.setResourceProfileList(list);
			}
		}
	}
}
