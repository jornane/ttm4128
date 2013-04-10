package assignment1;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Program for outputting CPU usage log lines at a certain interval
 * (see {@link #SLEEPMILLIS})
 * when SNMP traps indicate that CPU usage is above a certain threshold.
 * (threshold configured in the SNMP daemon)
 */
public class SnmpCpuMonitor implements Runnable {

	/**
	 * Magic bytes for finding the last element of the CPU OID.
	 */
	public static final String CPUOIDMAGIC = ", HOST-RESOURCES-MIB::hrDeviceDescr.";
	/**
	 * Magic bytes for finding the trigger name (cpuHigh or cpuLow).
	 */
	public static final String TRIGGERMAGIC = ", DISMAN-EVENT-MIB::mteHotTrigger.0 = STRING: cpu";

	/**
	 * Bash program to show one line of log output in CSV format.
	 */
	public static final String BASHPROGRAM = ""
			+ "/usr/bin/snmpwalk -v 2c -c public -Ov 127.1 HOST-RESOURCES-MIB::hrProcessorLoad | "
			+  "/usr/bin/awk '/^INTEGER: ([0-9]+)$/{print $2;}' | "
			+  "/usr/bin/xargs echo `snmpget -v 2c -c public -Ov 127.1 HOST-RESOURCES-MIB::hrSystemDate.0 | "
			+   "/usr/bin/awk '/^STRING: /{print $2;}'` | "
			+  "/usr/bin/awk '{ gsub(/ /, \",\"); print }'";

	/**
	 * Mapping of last element of CPU OID to boolean indicating whether the cpu load is high
	 * Used to keep track of the CPUs in order to start logging when one CPU is high
	 * and stop logging when any CPU is high.
	 */
	private static final Map<Integer,Boolean> CPUS = new HashMap<Integer,Boolean>();

	/**
	 * Sleep time between calling of {@link #BASHPROGRAM}.
	 */
	public static final long SLEEPMILLIS = 5000;

	/**
	 * The thread running {@link #BASHPROGRAM}
	 * It is created and started when any value of {@link #CPUS} is true,
	 * and interrupted when all values are false.
	 */
	private static Thread logger = null;

	/**
	 * Indicate that an snmp trap happened.
	 * The function will subsequently either call the start or stop logging function.
	 * @param high	whether the referred cpu is high (true) or low (false)
	 * @param cpuNum	the last element of the OID of the CPU
	 */
	synchronized static void trap(boolean high, int cpuNum) {
		if (high) startLogging();
		CPUS.put(cpuNum, high);
		Collection<Boolean> val = CPUS.values();
		val.retainAll(Arrays.asList(new Boolean[]{Boolean.TRUE}));
		if (val.size() > 0)
			startLogging();
		else
			stopLogging();
	}

	/**
	 * Start logging.
	 * This will start a new logging thread if one is not already running.
	 */
	private static void startLogging() {
		if (logger == null || logger.isInterrupted() || !logger.isAlive()) {
			logger = new Thread(new SnmpCpuMonitor());
			logger.start();
		}
	}

	/**
	 * Stop logging.
	 * This will interrupt the current logging thread.
	 * If the current logging thread is still running,
	 * an interrupt will cause it to stop.
	 */
	private static void stopLogging() {
		if (logger != null)
			logger.interrupt();
	}

	@Override
	/**
	 * Keeps running {@link #BASHPROGRAM} with {@link #SLEEPMILLIS} seconds in between.
	 * Will stop when an interrupt is received.
	 */
	public void run() {
		try {
			while(!Thread.interrupted()) {
				Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", BASHPROGRAM});
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = in.readLine();
				while (line != null) {
					System.out.println(line);
					line = in.readLine();
				}
				Thread.sleep(SLEEPMILLIS);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		} catch (InterruptedException e) {/* do nothing*/}
	}

	/**
	 * Run the program; the program accepts no arguments,
	 * and expects the snmptrapd log as it's standard input.
	 * @param args	none
	 */
	public static void main(String[] args) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while(line != null) {
			try {
				line = in.readLine();
				int hrDeviceDescrIndex = line.indexOf(CPUOIDMAGIC)+CPUOIDMAGIC.length();
				int hrDeviceDescr = Integer.parseInt(
						line.substring(
								hrDeviceDescrIndex, line.indexOf(' ', hrDeviceDescrIndex)
							)
					);
				int mteHotTriggerIndex = line.indexOf(TRIGGERMAGIC)+TRIGGERMAGIC.length();
				boolean high = line.substring(
						mteHotTriggerIndex, line.indexOf(
								' ', mteHotTriggerIndex
							)
					).toLowerCase().contains("high");
				trap(high, hrDeviceDescr);
			} catch (Exception e) {/* fail silently */}
		}
	}

}
