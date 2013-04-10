package assignment;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;

public class AppClass {
	private String agent;
	private final String owlPath;
	private OntModel ontology;
	private InfModel inf;
	public final static Map<String,String> MIBobjectsMap;
	public final static String NS = "http://www.item.ntnu.no/fag/ttm4128/sematicweb-2013#";	

	static {
		Map<String, String> mibMap = new HashMap<String,String>();
		mibMap.put("hrSystemDate", "HOST-RESOURCES-MIB");
		mibMap.put("hrSystemMaxProcesses", "HOST-RESOURCES-MIB");
		mibMap.put("hrSystemNumUsers", "HOST-RESOURCES-MIB");
		mibMap.put("hrSystemUptime", "HOST-RESOURCES-MIB");
		mibMap.put("snmpEnableAuthenTraps","SNMPv2-MIB");
		mibMap.put("snmpInPkts", "SNMPv2-MIB");
		mibMap.put("tcpInSegs","TCP-MIB");
		mibMap.put("tcpMaxConn","TCP-MIB");		
		mibMap.put("udpNoPorts","UDP-MIB");                
		mibMap.put("udpOutDatagrams","UDP-MIB");
		MIBobjectsMap = Collections.unmodifiableMap(mibMap);
	}

	public AppClass(String owlPath,String agent) {
		this.agent = agent;
		this.owlPath = owlPath;
		ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,null);
		ontology.read(FileManager.get().open(this.owlPath), null);
		Model schema = FileManager.get().loadModel(this.owlPath);
		Reasoner reason = ReasonerRegistry.getOWLReasoner();
		reason = reason.bindSchema(schema);
		this.inf = ModelFactory.createInfModel(reason, ontology);		
	}

	public String mibObjectFinder(Model model, Resource resource, Property p, String s) {
		for (StmtIterator iterator = model.listStatements(resource,p,s); iterator.hasNext();) {						
			Statement stmt = iterator.nextStatement();
			for (Map.Entry<String, String> entry : MIBobjectsMap.entrySet()) {
				if (PrintUtil.print(stmt).contains(entry.getKey())) {					
					return entry.getKey();
				}
			}		
		}
		return "MIB-Object not found.";			
	}
	        
	public String getMIBobjectValue(String in) throws NullPointerException {
		Resource nForce = this.inf.getResource(NS+in);
		String mib = mibObjectFinder(this.inf, nForce, null, null);

		System.out.println("MIB Module: "+MIBobjectsMap.get(mib));
		System.out.println("MIB Object: "+mib);
		
		ProcessBuilder pb = new ProcessBuilder(
				"snmpgetnext",
				"-v2c", "-cttm4128", "-Ov",
				agent,
				MIBobjectsMap.get(mib)+"::"+mib
		);
		
		try {
		String a = null;
		Process p = pb.start();
		Scanner scanner = new Scanner(p.getInputStream(), "UTF-8");
		scanner.useDelimiter("\\A");
		if (scanner.hasNext()) a = scanner.next();
		scanner.close();
		return a.substring(a.indexOf(":")+1).trim();
		} catch (IOException e) { return null; }
	}
	
	public static void main(String[] args) {
		String agent = "129.241.209.30";
		String owlPath = "data/sematicweb-2013-new.owl";
		AppClass a = new AppClass(owlPath ,agent);
		String cnmpObject = "x5982";
		System.out.println(a.getMIBobjectValue(cnmpObject));
	}
}