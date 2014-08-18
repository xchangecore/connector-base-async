package com.saic.uicds.clients.em.async;

/**
 * Interface for classes that get UICDS notifications for an endpoint and can
 * distribute those to listeners.  Observable or subject interface.
 * @author roger
 *
 */
public interface WorkProductProducer {
	/**
	 * Register a listener to receive WorkProduct events
	 * @param listener
	 */	
	public void registerListener(WorkProductListener listener);

	/**
	 * Remove a registered listener.
	 * @param listenr
	 */
	public void unregisterListener(WorkProductListener listenr);
}
