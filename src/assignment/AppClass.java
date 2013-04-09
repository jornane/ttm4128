package assignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	public String getValue(String in) {
		String mib = "";
		int numOfAgents = 0;
		ArrayList<Double> vals = new ArrayList<Double>();
		try {
			mib = getMibObjectName(in);
		} catch(NullPointerException dne) {
			return "Didn't get a proper mibObject when trying "+in;
		}

		ProcessBuilder pb = new ProcessBuilder(
				"snmpgetnext",
				"-v", "2c", "-c", "ttm4128",
				agent,
				MIBMAP.get(mib)+"::"+mib
			);
		String a = null;
		try {
			Process p = pb.start();
			a = procToStr(p);                        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double result = Double.parseDouble((a.substring(a.lastIndexOf(":")+1).trim()));
		vals.add(result);

		if(vals.size()>0) {
			double min = Collections.min(vals);
			double max = Collections.max(vals);
			double sum = 0;
			for(double i: vals) {
				sum+=i;
			}
			double average = sum/vals.size();
			return (String.format("<success>true</success><result><mib>%s</mib><minimum>%s</minimum><maximum>%s</maximum><average>%s</average><agentcount>%s</agentcount></result>",mib,min,max,average,numOfAgents));
		} else {
			return "<success>false</success><error>Could not get values from any agents.</error>";
		}
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

	public String procToStr(Process proc) {
		String retStr = "";
		try {
			BufferedReader bri = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while ((line = bri.readLine()) != null) {
				retStr+=line;
			}
			bri.close();
			proc.waitFor();
		} catch (Exception e) {
			//	e.printStackTrace();
		}
		return retStr;
	}

	public String cmdToStr(String cmd) {
		String[] command = cmd.split(" ");
		Process child;
		try {
			child = Runtime.getRuntime().exec(command);
			return procToStr(child);
		} catch (IOException e) {
			//	e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		//BasicConfigurator.configure();
		String agent = "129.241.209.30";
		String owlPath = "data/sematicweb-2013-new.owl";
		AppClass a = new AppClass(owlPath ,agent);
		System.out.println(a.getValue("defttl"));
	}        
}