package assignment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.hp.hpl.jena.ontology.OntClass;
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
	public final static Map<String,String> MIBMAP = new HashMap<String, String>();
	public final static String NS = "http://www.item.ntnu.no/fag/ttm4128/sematicweb-2013#";

	static {
		MIBMAP.put("hrSystemDate", "HOST-RESOURCES-MIB");
		MIBMAP.put("hrSystemMaxProcesses", "HOST-RESOURCES-MIB");
		MIBMAP.put("hrSystemNumUsers", "HOST-RESOURCES-MIB");
		MIBMAP.put("hrSystemUptime", "HOST-RESOURCES-MIB");
		MIBMAP.put("snmpEnableAuthenTraps","SNMPv2-MIB");
		MIBMAP.put("snmpInPkts", "SNMPv2-MIB");
		MIBMAP.put("tcpInSegs","TCP-MIB");
		MIBMAP.put("tcpMaxConn","TCP-MIB");		
		MIBMAP.put("udpNoPorts","UDP-MIB");                
		MIBMAP.put("udpOutDatagrams","UDP-MIB");
	}

	public AppClass(String owlPath,String agent) {
		this.agent = agent;
		this.owlPath = owlPath;
		ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,null);
		ontology.read(FileManager.get().open(this.owlPath), null);
		Model schema = FileManager.get().loadModel(this.owlPath);
		Reasoner reason = ReasonerRegistry.getOWLReasoner();
		reason = reason.bindSchema(schema);
		this.inf = ModelFactory.createInfModel(reason,ontology);
		Resource nForce = this.inf.getResource(NS+"hrSystemUptime");
		printStatements(this.inf, nForce, null, null);
	}

	public void printStatements(Model m,Resource s, Property p, Resource o) {
		for (StmtIterator i = m.listStatements(s,p,o);i.hasNext();) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		}
	}        

	public String getValue(String in) throws IOException, NullPointerException {
		String mib = getMibObjectName(in);

		ProcessBuilder pb = new ProcessBuilder(
				"snmpgetnext",
				"-v2c", "-cttm4128", "-Ov",
				agent,
				MIBMAP.get(mib)+"::"+mib
			);
		String a = null;
		Process p = pb.start();
		Scanner scanner = new Scanner(p.getInputStream(), "UTF-8");
		scanner.useDelimiter("\\A");
		if (scanner.hasNext()) a = scanner.next();
		scanner.close();
		return a.substring(a.indexOf(":")+1).trim();
	}

	public String getMibObjectName(String in) throws NullPointerException {
		OntClass cls = ontology.getOntClass(NS+in);
		Resource aliases = inf.getResource(NS+"aliases");                               
		if(cls.hasSuperClass(aliases)) {
			return cls.getSuperClass().getLocalName();
		} else {
			return cls.getLocalName();
		}
	}

	public static void main(String[] args) {
		//BasicConfigurator.configure();
		String agent = "129.241.209.30";
		String owlPath = "data/sematicweb-2013-new.owl";
		AppClass a = new AppClass(owlPath ,agent);
		try {
			System.out.println(a.getValue("defttl"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}        
}