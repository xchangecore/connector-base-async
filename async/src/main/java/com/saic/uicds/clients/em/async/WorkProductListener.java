package com.saic.uicds.clients.em.async;

import org.uicds.notificationService.WorkProductDeletedNotificationType;
import org.uicds.workProductService.WorkProductPublicationResponseType;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;

/**
 * Interface of a class that listens for WorkProductSummary notifications
 * 
 * @author roger
 * 
 */
public interface WorkProductListener {
    /**
     * Implementers will receive this call when a WorkProduct is delivered as a notification.
     * 
     * @param workProduct
     */
    public void handleWorkProductUpdate(WorkProduct workProduct);

    /**
     * Implementers will receive this call when a WorkProductPublicationMessage is delivered as a
     * notification.
     * 
     */
    public void handleWorkProductPublicationMessage(
        WorkProductPublicationResponseType workProductPublicationResponse);

    /**
     * Implementers will receive all Work Product Deleted notifications.
     * 
     * @param workProductDeletedNotification
     */
    public void handleWorkProductDelete(
        WorkProductDeletedNotificationType workProductDeletedNotification);

    /**
     * Implementers will receive all EDXL-DE notification messages.
     * 
     * @param edxldeMessage
     */
    public void handleEDXLDEMessage(EDXLDistribution edxldeMessage);
}
