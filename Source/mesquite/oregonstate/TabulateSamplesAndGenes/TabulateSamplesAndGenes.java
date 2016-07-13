/* Mesquite Chromaseq source code.  Copyright 2005-2011 David Maddison and Wayne Maddison.
Version 1.0   December 2011
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

/* initiated 
 * 8.vi.14 DRM based upon SegregateChromatograms
 */

/** this module is simple; it segregates files contained in a directory that have a sample code that matches those listed in a 
 * chosen file of sample codes.  It uses a ChromatogramFileNameParser to find the sample code within each file's name*/

package mesquite.oregonstate.TabulateSamplesAndGenes; 

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.chromaseq.lib.*;

/* ======================================================================== */
public class TabulateSamplesAndGenes extends UtilitiesAssistant{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;

	static String previousDirectory = null;
	int sequenceCount = 0;
	int duplicates = 0;
	//String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;
	String sampleCodeListPath = null;
	String sampleCodeListFile = null;
	String sampleCodeList = "";
	Parser sampleCodeListParser = null;

	static final int maxCodes = 5000;

	static final int gene28S=0;
	static final int gene18S=1;
	static final int geneCOIBC=2;
	static final int geneCOIPJ=3;
	static final int geneCAD2=4;
	static final int geneCAD3=5;
	static final int geneCAD4=6;
	static final int geneTopo=7;
	static final int geneMSP=8;
	static final int geneWg=9;
	static final int geneArgK=10;
	static final int numGenes = 11;

	String[]geneName = new String[]{"28S", "18S","COIBC", "COIPJ", "CAD2", "CAD3", "CAD4", "Topo", "MSP", "wg", "ArgK"};
	String[][] genes = new String[maxCodes][numGenes];

	boolean preferencesSet = false;
	boolean verbose=true;

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();
		initializeValues();
		addMenuItem(null, "Tabulate Samples & Genes [DRM Lab]...", makeCommand("tabulate", this));
		return true;
	}
	/*.................................................................................................................*/
	public void initializeValues(){
		for (int i=0; i<maxCodes; i++)
			for (int j=0; j<numGenes; j++)
				genes[i][j]="";
		duplicates=0;
	}
	/*.................................................................................................................*/
	public boolean processCodesFile() {
		if (!StringUtil.blank(sampleCodeListPath)) {
			sampleCodeList = MesquiteFile.getFileContentsAsString(sampleCodeListPath);

			if (!StringUtil.blank(sampleCodeList)) {
				sampleCodeListParser = new Parser(sampleCodeList);
				return true;
			}
		}	
		return false;

	}

	/*.................................................................................................................*/
	void recordValues(int geneNumber, int sampleCode, String accession, String publication) {
		if (sampleCode>=0 && sampleCode<maxCodes) {
			String newString = "";
			String current ="";
			boolean warned=false;
			if (StringUtil.notEmpty(genes[sampleCode][geneNumber])){
				loglnEchoToStringBuffer("WARNING: sample " + sampleCode + " for gene " + geneName[geneNumber] + " already contains a record! (publication: "+publication+")", logBuffer);
				warned=true;
				current = genes[sampleCode][geneNumber]+", ";
				duplicates++;
			}
			if (StringUtil.notEmpty(accession)) {
				if (warned) 
					loglnEchoToStringBuffer("    Previous entry " + genes[sampleCode][geneNumber] + ", new entry " + accession, logBuffer);
				genes[sampleCode][geneNumber]=current+accession;
			}
			else if (StringUtil.notEmpty(publication)) {
				if (warned) 
					loglnEchoToStringBuffer("    Previous entry " + genes[sampleCode][geneNumber] + ", new entry " + publication, logBuffer);
				genes[sampleCode][geneNumber]=current + publication;
			}
		}

	}
	/*.................................................................................................................*/
	public void processFile(String originalFileName) {
		Parser parser = new Parser(originalFileName);
		parser.setPunctuationString("_.");
		String fullSampleCode ="";
		String DRMCode ="";
		String gene ="";
		String fragment ="";
		String accession ="";
		String publication ="";
		int code =-1;

		String token = parser.getFirstToken();
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&v")) {
				fullSampleCode=token.substring(2);
				DRMCode=token.substring(8);
				code = MesquiteInteger.fromString(DRMCode);
				if (!MesquiteInteger.isCombinable(code) || code<0 || code>=maxCodes)
					code=-1;
			} else if (token.startsWith("&g")) {
				gene=token.substring(2);
			}  else if (token.startsWith("&a")) {
				accession=token.substring(2);
			} else if (token.startsWith("&f")) {
				fragment=token.substring(2);
			} else if (token.startsWith("&p")) {
				publication=token.substring(2);
			}
			token = parser.getNextToken();
		}

		if ("28S".equalsIgnoreCase(gene)) 
			recordValues(gene28S, code, accession, publication);
		else if ("18S".equalsIgnoreCase(gene)) 
			recordValues(gene18S, code, accession, publication);
		else if ("COI".equalsIgnoreCase(gene)){
			if ("COIBC".equalsIgnoreCase(fragment))
				recordValues(geneCOIBC, code, accession, publication);
			else if ("COIPJ".equalsIgnoreCase(fragment))
				recordValues(geneCOIPJ, code, accession, publication);
			else if ("COIAll".equalsIgnoreCase(fragment)){
				recordValues(geneCOIBC, code, accession, publication);
				recordValues(geneCOIPJ, code, accession, publication);
			}
		}
		else if ("CAD".equalsIgnoreCase(gene)){
			if ("CAD2".equalsIgnoreCase(fragment))
				recordValues(geneCAD2, code, accession, publication);
			else if ("CAD3".equalsIgnoreCase(fragment))
				recordValues(geneCAD3, code, accession, publication);
			else if ("CAD4".equalsIgnoreCase(fragment))
				recordValues(geneCAD4, code, accession, publication);
			else if ("CAD234".equalsIgnoreCase(fragment)){
				recordValues(geneCAD2, code, accession, publication);
				recordValues(geneCAD3, code, accession, publication);
				recordValues(geneCAD4, code, accession, publication);
			}
		}
		else if ("Topo".equalsIgnoreCase(gene)) 
			recordValues(geneTopo, code, accession, publication);
		else if ("MSP".equalsIgnoreCase(gene)) 
			recordValues(geneMSP, code, accession, publication);
		else if ("wg".equalsIgnoreCase(gene)) 
			recordValues(geneWg, code, accession, publication);
		else if ("ArgK".equalsIgnoreCase(gene)) 
			recordValues(geneArgK, code, accession, publication);


	}
	/*.................................................................................................................*/

	public String infoFromTabbedDelimitedFile(String codeToMatch) {
		if (sampleCodeListParser==null)
			return null;
		if (StringUtil.blank(codeToMatch))
			return null;
		sampleCodeListParser.setPosition(0);
		Parser parser = new Parser();
		String info = "";
		String line = sampleCodeListParser.getRawNextDarkLine();
		while (StringUtil.notEmpty(line)) {
			parser.setString(line);
			parser.setWhitespaceString("\t");
			parser.setPunctuationString("");
			String code = parser.getFirstToken();
			String token = parser.getNextToken();
			if (codeToMatch.equalsIgnoreCase(code)) {
				return line.substring(code.length());
			}
			line = sampleCodeListParser.getRawNextDarkLine();
		}
		return null;
	}
	/*.................................................................................................................*/

	public String titlesFromTabbedDelimitedFile() {
		if (sampleCodeListParser==null)
			return null;
		sampleCodeListParser.setPosition(0);
		Parser parser = new Parser();
		String info = "";
		String line = sampleCodeListParser.getRawNextDarkLine();
		if (StringUtil.notEmpty(line)) {
			parser.setString(line);
			parser.setWhitespaceString("\t");
			parser.setPunctuationString("");
			String token = parser.getFirstToken();
			token = parser.getNextToken();
			while (token!=null){
				info+="\t"+token;
				token = parser.getNextToken();
			}
			return info;
		}
		return null;
	}

	/*.................................................................................................................*/
	public boolean tabulateFiles(String directoryPath, File directory){

		logBuffer.setLength(0);
		String[] files = directory.list();
		String cPath;
		sequenceCount = 0;
		initializeValues();


		loglnEchoToStringBuffer(" Tabulating files. ", logBuffer);
		loglnEchoToStringBuffer("  "+directoryPath+"\n", logBuffer);

		int numPrepared = 0;

		for (int i=0; i<files.length; i++) {
			if (files[i]==null )
				;
			else {
				cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
				File cFile = new File(cPath);
				if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {

					String originalFileName = cFile.getName();
					if (StringUtil.blank(originalFileName)) {
						loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
						// remove "running"
						return false;
					}
					processFile(originalFileName);

				}
			}
		}

		loglnEchoToStringBuffer("Number of files examined: " + files.length, logBuffer);

		StringBuffer table = new StringBuffer();
		table.append("code");
		String titles = titlesFromTabbedDelimitedFile();
		if (StringUtil.notEmpty(titles))
			table.append(titles);
		for (int gene=0; gene<numGenes && gene<geneName.length; gene++) {
			table.append("\t"+geneName[gene]);
		}
		table.append("\n");
		String sampleCode ="";
		for (int code=0; code<maxCodes; code++){
			boolean hasEntry=false;
			for (int gene=0; gene<numGenes; gene++)   // check to see if there is anything in this row
				if (StringUtil.notEmpty(genes[code][gene])) {  
					hasEntry=true;
					break;
				}
			if (hasEntry) {
				table.append("DNA");
				if (code<10)
					sampleCode="000"+code;
				else if (code<100)
					sampleCode="00"+code;
				else if (code<1000)
					sampleCode="0"+code;
				else
					sampleCode =""+code;
				table.append(sampleCode);
				String info = infoFromTabbedDelimitedFile(sampleCode);
				if (StringUtil.notEmpty(info))
					table.append(info);

				for (int gene=0; gene<numGenes; gene++) {
					table.append("\t"+genes[code][gene]);
				}
				table.append("\n");
			}
		}
		logln("Number of cells in table with multiple entries: " + duplicates);
		logln("Saving results");
		String tableDirectory = MesquiteFile.putFileContentsQueryReturnDirectory("Save table of gene information", table.toString());
		MesquiteFile.putFileContents(tableDirectory+"tabulateLog.txt", logBuffer.toString(), false);

		logln("Completed tabulation");

		return true;

	}
	/*.................................................................................................................*/
	public boolean renameFiles(String directoryPath){
		if ( logBuffer==null)
			logBuffer = new StringBuffer();

		MesquiteBoolean pleaseStorePrefs = new MesquiteBoolean(false);

		if (pleaseStorePrefs.getValue())
			storePreferences();

		// if not passed-in, then ask
		if (StringUtil.blank(directoryPath)) {
			directoryPath = MesquiteFile.chooseDirectory("Choose directory containing files:", previousDirectory); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		}

		if (StringUtil.blank(directoryPath))
			return false;


		File directory = new File(directoryPath);

		previousDirectory = directory.getParent();
		storePreferences();
		if (directory.exists() && directory.isDirectory()) {
			return tabulateFiles(directoryPath, directory);
		}
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return false;
	}

	/*.................................................................................................................*/
	private String getModuleText(MesquiteModule mod) {
		return mod.getName() + "\n" + mod.getParameters();
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Tabulate samples and genes.", null, commandName, "tabulate")) {

			MesquiteString dnaNumberListDir = new MesquiteString();
			MesquiteString dnaNumberListFile = new MesquiteString();
			String s = MesquiteFile.openFileDialog("Choose file containing sample codes", dnaNumberListDir, dnaNumberListFile);
			if (!StringUtil.blank(s)) {
				sampleCodeListPath = s;
				sampleCodeListFile = dnaNumberListFile.getValue();
				processCodesFile();
			}
			renameFiles(null);
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*
	public  void actionPerformed(ActionEvent e) {
		 if (e.getActionCommand().equalsIgnoreCase("nameParserButton")) {
			if (nameParserManager!=null) {
				if (nameParserManager.queryOptions() && nameParserTextCanvas!=null)
					nameParserTextCanvas.setText(getModuleText(nameParserManager));
			}
		}
	}


	/*.................................................................................................................*/
	public String getName() {
		return "Tabulate Files [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Tabulates files.";
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

}





