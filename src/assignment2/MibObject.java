package assignment2;

public class MibObject {

	public final String module;
	public final String objectName;
	public final int objectIndex;
	public final String type;
	public final String value;

	public MibObject(String snmpgetOutputLine) {
		// HOST-RESOURCES-MIB::hrSystemNumUsers.0 = Gauge32: 0
		int moduleSep = snmpgetOutputLine.indexOf("::");
		int objectNameIndex = snmpgetOutputLine.indexOf('.');
		int typeIndex = snmpgetOutputLine.indexOf(" = ");
		int valueIndex = snmpgetOutputLine.indexOf(": ");
		
		module = snmpgetOutputLine.substring(0, moduleSep);
		objectName = snmpgetOutputLine.substring(moduleSep+2, objectNameIndex);
		objectIndex = Integer.parseInt(snmpgetOutputLine.substring(objectNameIndex+1, typeIndex));
		type = snmpgetOutputLine.substring(typeIndex+3, valueIndex);
		value = snmpgetOutputLine.substring(valueIndex+2);
	}
	
	public String toString() {
		return module+"::"+objectName+"."+objectIndex+" = "+type+": "+value;
	}
	
}
