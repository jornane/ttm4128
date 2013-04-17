package assignment2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;

public class CNMP2SNMPConverter {
	/** Address of the agent (IP address or hostname, is used for the snmpgetnext command) */
	protected final String agent;
	/** The path to the OWL file containing the inferences */
	protected final String owlPath;
	/** The inference model */
	protected final InfModel inf;
	/** Verbosity of the CLI application */
	protected boolean verbose;
	
	/** Mapping of all CNMP object names to the corresponding MIB module name. */
	public static final Map<String,String> MIBMODULES;

	/** Default namespace */
	public static final String NS = "http://www.item.ntnu.no/fag/ttm4128/sematicweb-2013#";
	/** Default path to the OWL file */
	public static final String OWLPATH = "data/sematicweb-2013-new.owl";
	/** Default agent address */
	public static final String AGENT = "129.241.209.30";
	
	/**
	 * Fill the MIBMODULES map
	 */
	static {
		Map<String, String> mibMap = new HashMap<String,String>();
		mibMap.put("x1234", "HOST-RESOURCES-MIB");
		mibMap.put("x6742", "HOST-RESOURCES-MIB");
		mibMap.put("x4912", "SNMPv2-MIB");
		mibMap.put("x5982", "SNMPv2-MIB");
		mibMap.put("x5178", "TCP-MIB");
		mibMap.put("x3125", "TCP-MIB");
		MIBMODULES = Collections.unmodifiableMap(mibMap);
	}

	/**
	 * Instantiate a new CNMP/SNMP converter
	 * @param owlPath @see {@link #owlPath}
	 * @param agent	@see {@link #agent}
	 */
	public CNMP2SNMPConverter(String owlPath,String agent) {
		this.agent = agent;
		this.owlPath = owlPath;
		
		// Redirect errors away
		PrintStream err = System.err;
		System.setErr(new PrintStream(new ByteArrayOutputStream()));
		OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,null);
		// Reset error redirection
		System.setErr(err);
		
		ontology.read(FileManager.get().open(this.owlPath), null);
		Model schema = FileManager.get().loadModel(this.owlPath);
		Reasoner reason = ReasonerRegistry.getOWLReasoner();
		reason = reason.bindSchema(schema);
		this.inf = ModelFactory.createInfModel(reason, ontology);		
	}

	/**
	 * Retrieves an object name that is not equal to the subject or any generic parent.
	 * @param model	The model containing the inferences.
	 * @param subject	An object name that should have another name inferred.
	 * @param parentClasses	Known names for parent classes (like Thing, SNMP_Object).
	 * @return	The inferred name of the subject, or null if no inference found.
	 * 			Never returns the subject or any of the values in parentClasses.
	 */
	public String resolveInference(Model model, String subject, String... parentClasses) {
		StringBuilder parentPattern = new StringBuilder("("+Pattern.quote(subject));
		for(String parentClass : parentClasses)
			parentPattern.append("|"+Pattern.quote(parentClass));
		parentPattern.append(")");
		for (StmtIterator iterator = model.listStatements(
					model.getResource(NS+subject),
					null,
					(String)null);
				iterator.hasNext();)
		{
			Statement stmt = iterator.nextStatement();
			try {
				String stmtPredicate = stmt.getPredicate().getLocalName();
				if (!"subClassOf".equals(stmtPredicate))
					continue;
				//String stmtSubject = new URI(stmt.getSubject().toString()).getFragment();
				String stmtObject = new URI(stmt.getObject().toString()).getFragment();
				if (stmtObject.matches(parentPattern.toString()))
					continue;
				return stmtObject;
			} catch (URISyntaxException e) {/* do nothing */}
		}
		return null;			
	}
	
	/**
	 * Return the SNMP output for a CNMP object
	 * @param in	Name of the CNMP object
	 * @return	output of snmpgetnext for the corresponding SNMP object
	 * 		Returns NULL if the SNMP name cannot be determined,
	 * 		or if something goes wrong executing snmpgetnext.
	 */
	public MibObject getMIBObjectValue(String in) {
		String mib = resolveInference(this.inf, in, "SNMP_Object", "CNMP_Object", "Thing", "Resource");
		String module = MIBMODULES.get(in);
		if (module == null)
			return null;

		if (verbose) {
			System.out.println("MIB Module: "+module);
			System.out.println("MIB Object: "+mib);
		}
		
		ProcessBuilder pb = new ProcessBuilder(
				"snmpgetnext",
				"-v2c", "-cttm4128",
				agent,
				module+"::"+mib
		);
		
		try {
		String snmpgetnextOutput = null;
		Process p = pb.start();
		Scanner scanner = new Scanner(p.getInputStream(), "UTF-8");
		scanner.useDelimiter("\\A");
		if (scanner.hasNext()) snmpgetnextOutput = scanner.next();
		scanner.close();
		if (snmpgetnextOutput == null)
			return null;
		return new MibObject(snmpgetnextOutput);
		} catch (IOException e) { return null; }
	}
	
	/**
	 * Execute the program
	 * @param args	Commandline arguments
	 */
	public static void main(String[] args) {
		MibObject mib = getMibObject(args);
		if (mib == null)
			System.err.println("Could not find any SNMP object for CNMP object.");
		else
			System.out.println(mib);
	}
	
	/**
	 * Program to get an SNMP MibObject from a CNMP request
	 * @param args	Commandline arguments
	 * @return	the MibObject
	 */
	public static MibObject getMibObject(String[] args) {
		String agent = AGENT;
		String owlPath = OWLPATH;
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
		if (!MIBMODULES.containsKey(cnmpObject))
			System.err.println("No module mapping for "+cnmpObject);
		if (verbose)
			System.out.println("Processing...");
		CNMP2SNMPConverter converter = new CNMP2SNMPConverter(owlPath, agent);
		converter.verbose = verbose;
		return converter.getMIBObjectValue(cnmpObject);
	}
}