package org.sar.ppi;

/**
 * Ppi Main class.
 */
public class Ppi {

	/**
	 * The main to call to run the app. Usage:
	 * {@code java org.sar.Ppi <process-class-name> <runner-class-name> [<nb-proc> [<scenario>]]}
	 *
	 * This function sets the defaults values, then call the Runner's init method
	 * which should then call Ppi.main().
	 *
	 * @param args cli args.
	 * @throws PpiException on every internal error (TODO more Exceptions)
	 */
	public static void main(String[] args) throws PpiException {
		Class<? extends NodeProcess> processClass;
		Runner runner;
		int nbProcs = 5;
		String scenario = null;

		if (args.length < 2 || args.length > 4) {
			System.out.println("Usage: ppirun <process-class-name> <runner-class-name> [<nb-proc> [<scenario>]]");
			return;
		}
		try {
			processClass = Class.forName(args[0]).asSubclass(NodeProcess.class);
		} catch (ClassNotFoundException e) {
			throw new PpiException("Could not find the process class " + args[0], e);
		}
		try {
			Class<? extends Runner> rClass = Class.forName(args[1]).asSubclass(Runner.class);
			runner = rClass.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new PpiException("Failed to intanciate the Runner", e);
		}
		if (args.length >= 3) {
			try {
				nbProcs = new Integer(args[2]);
			} catch (NumberFormatException e) {
				throw new PpiException("Not a valid number for <nb-proc> param", e);
			}
		}
		if (args.length >= 4) {
			scenario = args[3];
		}
		main(processClass, runner, nbProcs, scenario);
	}

	/**
	 * Higher level main for easier from java invocations.
	 *
	 * @param pClass the class to execute by Ppi.
	 * @param runner the runner to use for this execution.
	 * @param nbProcs the number of processes to run.
	 * @param scenario the name of the scenario file.
	 * @throws PpiException if pClass instanciation fails.
	 */
	public static void main(Class<? extends NodeProcess> pClass, Runner runner, int nbProcs, String scenario)
			throws PpiException {
		try {
			runner.run(pClass, nbProcs, scenario);
		} catch (ReflectiveOperationException e) {
			throw new PpiException("Failed to intantiate the process class " + pClass.getName(), e);
		}
	}

}