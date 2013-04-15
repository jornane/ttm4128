package assignment2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

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
	protected String agent;
	protected final String owlPath;
	protected OntModel ontology;
	protected InfModel inf;
	protected boolean verbose;
	
	public final static Map<String,String> MIBobjectsMap;
	public final static Vector<String> CNMPobjects = new Vector<String>();
	public final static String NS = "http://www.item.ntnu.no/fag/ttm4128/sematicweb-2013#";
	
	static {
		CNMPobjects.add("x5178");
		CNMPobjects.add("x3125");
		CNMPobjects.add("x4912");
		CNMPobjects.add("x5982");
		CNMPobjects.add("x1234");
		CNMPobjects.add("x6742");
	}

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
		
		// Redirect errors away
		PrintStream err = System.err;
		System.setErr(new PrintStream(new ByteArrayOutputStream()));
		ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,null);
		// Reset error redirection
		System.setErr(err);
		
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
	        
	public MibObject getMIBObjectValue(String in) throws NullPointerException {
		Resource nForce = this.inf.getResource(NS+in);
		String mib = mibObjectFinder(this.inf, nForce, null, null);

		if (verbose) {
			System.out.println("MIB Module: "+MIBobjectsMap.get(mib));
			System.out.println("MIB Object: "+mib);
		}
		
		ProcessBuilder pb = new ProcessBuilder(
				"snmpgetnext",
				"-v2c", "-cttm4128",
				agent,
				MIBobjectsMap.get(mib)+"::"+mib
		);
		
		try {
		String snmpgetnextOutput = null;
		Process p = pb.start();
		Scanner scanner = new Scanner(p.getInputStream(), "UTF-8");
		scanner.useDelimiter("\\A");
		if (scanner.hasNext()) snmpgetnextOutput = scanner.next();
		scanner.close();
		return new MibObject(snmpgetnextOutput);
		} catch (IOException e) { return null; }
	}
	
	public static boolean objectValidator(String input) {
		for (int i=0; i<CNMPobjects.size(); i++) {
			if (input.equals(CNMPobjects.get(i))) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		String agent = "129.241.209.30";
		String owlPath = "data/sematicweb-2013-new.owl";
		String cnmpObject = null;
		boolean verbose = true;
		
		for(int i=0;i<args.length;i++) {
			if (i<args.length-1) {
				if ("-a".equals(args[i]))
					agent = args[i+1];
				if ("-o".equals(args[i]))
					owlPath = args[i+1];
				if ("-c".equals(args[i]))
					cnmpObject = args[i+1];
			}
			if ("-v".equals(args[i]))
				verbose = true;
			if ("-q".equals(args[i]))
				verbose = false;
		}
		
		if (cnmpObject == null) {
			System.out.print("Please enter the CNMP Object name: ");
			Scanner reader = new Scanner(System.in);
			cnmpObject = reader.nextLine();
			reader.close();
		}
		if (objectValidator(cnmpObject)) {
			if (verbose)
				System.out.println("Processing...");
			AppClass a = new AppClass(owlPath ,agent);
			a.verbose = verbose;
			System.out.println(a.getMIBObjectValue(cnmpObject));
		} else {
			System.err.println("CNMP Object "+cnmpObject+" is not valid...");
		}
	}
}