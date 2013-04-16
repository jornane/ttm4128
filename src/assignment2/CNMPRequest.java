package assignment2;

/**
 * A request for the value of a CNMP object.
 * The request contains both the objectName (xnnnn)
 * and a requestID to reference the request.
 */
public class CNMPRequest {

	/** CNMP object name (xnnnn) */
	protected final String objectName;
	/** Numeric request ID */
	protected final String requestID;

	/**
	 * Instantiate new CNMPRequest
	 * @param objectName @see {@link #objectName}
	 * @param requestID @see {@link #requestID}
	 */
	public CNMPRequest(String objectName, String requestID) {
		this.objectName = objectName;
		this.requestID = requestID;
	}
	
	/**
	 * String formatted as requestID@objectName
	 */
	public String toString() {
		return requestID+'@'+objectName;
	}

}
