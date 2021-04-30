package mesquite.oregonstate.TabulateLociInSets;

import mesquite.lib.CommandChecker;
import mesquite.lib.Debugg;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteString;
import mesquite.lib.Parser;
import mesquite.lib.StringUtil;
import mesquite.lib.duties.UtilitiesAssistant;

public class TabulateLociInSets extends UtilitiesAssistant {
	String lociListPath = null;
	String lociListFile = null;
	String lociList = "";
	Parser lociListParser = null;
	

	static final int maxLoci = 5000;
	static int numSets = 3;
	String[] loci = new String[maxLoci];
	String[] locusSetNames = new String[numSets];
	boolean[][] locusInSet = new boolean[maxLoci][numSets];
	int numLoci = 0;

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		initializeValues();
		addMenuItem(null, "Tabulate Loci In Sets [DRM Lab]...", makeCommand("tabulate", this));
		return true;
	}
	/*.................................................................................................................*/
	public void initializeValues(){
		numLoci=0;
		for (int i=0; i<maxLoci; i++) {
			loci[i] = "";
			for (int j=0; j<numSets; j++)
				locusInSet[i][j]=false;
		}
		locusSetNames[0] = "M204";
		locusSetNames[1] = "M304";
		locusSetNames[2] = "M404";
	}

	/*.................................................................................................................*/
	public int getCurrentLocus(String locus) {
		for (int i=0; i<numLoci; i++)
			if (StringUtil.notEmpty(loci[i]) && loci[i].equalsIgnoreCase(locus)) {
				return i;
			}
		return -1;
	}
	/*.................................................................................................................*/
	public int getCurrentLocusSet(String locusSet) {
		for (int i=0; i<numSets; i++)
			if (StringUtil.notEmpty(locusSetNames[i]) && locusSetNames[i].equalsIgnoreCase(locusSet)) {
				return i;
			}
		return -1;
	}
	/*.................................................................................................................*/
	public void processLocusLine(String locus, String locusSet) {
		int currentLocus = getCurrentLocus(locus);
		int currentLocusSet = getCurrentLocusSet(locusSet);
		if (currentLocusSet>=0) {
			if (currentLocus>=0) {
				locusInSet[currentLocus][currentLocusSet]=true;
			} else if (numLoci<maxLoci) {  // not found
				loci[numLoci] = locus;
				currentLocus = numLoci;
				numLoci++;
				locusInSet[currentLocus][currentLocusSet]=true;
			}
		}

	}
	
	/*.................................................................................................................*/
	public String getLocusTable() {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<numLoci; i++) {
			sb.append(loci[i]);
			for (int j=0; j<numSets; j++) {
				sb.append("\t");
				if (locusInSet[i][j])
					sb.append("1");
				else
					sb.append("0");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/*.................................................................................................................*/
	public String getOverallStatistics() {
		StringBuffer sb = new StringBuffer();
		sb.append("Total number of loci: " + numLoci +"\n");
		int[] totalLociInSet = new int[numSets];
		int[] uniqueLociInSet = new int[numSets];
		for (int j=0; j<numSets; j++) {
			totalLociInSet[j] = 0;
			uniqueLociInSet[j] = 0;
		}
		for (int i=0; i<numLoci; i++) {
			for (int j=0; j<numSets; j++) {
				if (locusInSet[i][j])
					totalLociInSet[j]++;
			}
		}
		for (int j=0; j<numSets; j++) {
			sb.append("Total number of loci in set " + locusSetNames[j] +  ": " + totalLociInSet[j] +"\n");
		}
		
		int universalLoci =0;
		for (int i=0; i<numLoci; i++) {
			boolean universal = true;
			for (int j=0; j<numSets; j++) {
				if (!locusInSet[i][j])
					universal=false;
			}
			if (universal)
				universalLoci++;
		}
		sb.append("\nNumber of universal loci " + universalLoci +"\n");

		for (int i=0; i<numLoci; i++) {
			for (int j=0; j<numSets; j++) {
				if (locusInSet[i][j]) {  //it's in this set, now see if it is unique
					boolean unique=true;
					for (int k=0; k<numSets; k++) {
						if (j!=k && locusInSet[i][k])
							unique=false;
					}
					if (unique)
						uniqueLociInSet[j]++;
				}
			}
		}
		for (int j=0; j<numSets; j++) {
			sb.append("Total number of unique loci in set " + locusSetNames[j] +  ": " + uniqueLociInSet[j] +"\n");
			sb.append("    Universal + unique: " +(universalLoci+uniqueLociInSet[j])+"\n");
		}
		sb.append("\n");


		
		int[][] vennBoth = new int[numSets][numSets];
		int[][] vennOnly = new int[numSets][numSets];
		for (int j=0; j<numSets; j++) {
			for (int k=0;k <numSets;k++) {
				vennBoth[j][k]=0;
				vennOnly[j][k]=0;
			}
		}
		for (int i=0; i<numLoci; i++) {
			for (int j=0; j<numSets; j++) {
				for (int k=0; k<numSets; k++) {
					if (locusInSet[i][j] && locusInSet[i][k]) {
						vennBoth[j][k]++;
						boolean unique=true;
						for (int m=0; m<numSets; m++) {  // see if present in any others
							if (j!=m && k!=m && locusInSet[i][m])
								unique=false;
						}
						if (unique)
							vennOnly[j][k]++;

					}
				}
			}
		}
		
		sb.append("Venn\n");

		for (int j=0; j<numSets; j++) {
			sb.append("\t"+locusSetNames[j]);
		}
		sb.append("\n");

		for (int j=0; j<numSets; j++) {
			sb.append(locusSetNames[j]);
			for (int k=0; k<numSets; k++) {
				sb.append("\t"+vennOnly[j][k]);
			}
			sb.append("\n");
		}


		return sb.toString();
	}

	/*.................................................................................................................*/
	public boolean processLocusListFile() {
		if (!StringUtil.blank(lociListPath)) {
			lociList = MesquiteFile.getFileContentsAsString(lociListPath);

			if (!StringUtil.blank(lociList)) {
				initializeValues();
				lociListParser = new Parser(lociList);
				String line = lociListParser.getRawNextDarkLine();
				Parser lineParser = new Parser();
				String locus;
				String locusSet;
				while (StringUtil.notEmpty(line)) {
					lineParser.setString(line);
					locus = lineParser.getFirstToken();
					locusSet = lineParser.getNextToken();
					processLocusLine(locus, locusSet);
					line = lociListParser.getRawNextDarkLine();
				}
				Debugg.println("\n");
				Debugg.println("\n");
				Debugg.println(getLocusTable());
				Debugg.println("\n");
				Debugg.println("\n");
				Debugg.println(getOverallStatistics());
				
				return true;
			}
		}	
		return false;

	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Tabulate loci in set.", null, commandName, "tabulate")) {

			MesquiteString locusListDir = new MesquiteString();
			MesquiteString locusListFile = new MesquiteString();
			String s = MesquiteFile.openFileDialog("Choose file containing loci in sets", locusListDir, locusListFile);
			if (!StringUtil.blank(s)) {
				lociListPath = s;
				lociListFile = locusListFile.getValue();
				processLocusListFile();
			}
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}


	@Override
	public String getName() {
		return "Tabulate Loci in Sets...";
	}

}
