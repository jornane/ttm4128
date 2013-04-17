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
	protected final String requestId;

	/**
	 * Instantiate new CNMPRequest
	 * @param objectName @see {@link #objectName}
	 * @param requestId @see {@link #requestId}
	 */
	public CNMPRequest(String objectName, String requestId) {
		this.objectName = objectName;
		this.requestId = requestId;
	}
	
	/**
	 * String formatted as requestID@objectName
	 */
	public String toString() {
		return requestId+'@'+objectName;
	}

}
