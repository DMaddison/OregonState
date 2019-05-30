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

package mesquite.oregonstate.ListCompleteICOBSTaxa; 

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.Sequencer;
import javax.swing.JLabel;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import java.util.*;
import mesquite.chromaseq.lib.*;

/* ======================================================================== */
public class ListCompleteICOBSTaxa extends UtilitiesAssistant{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;

	static String previousDirectory = null;
	ProgressIndicator progIndicator = null;
	int sequenceCount = 0;
	String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;


	boolean preferencesSet = false;
	boolean verbose=true;

	Vector vector = new Vector();


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();

		addMenuItem(null, "List Complete ICOBS Taxa...", makeCommand("listTaxa", this));
		return true;
	}
	/*.................................................................................................................*/
	void addTaxon(String tag) {

	}
	/*.................................................................................................................*/
	public String getFileNameElement(String fileName, String tag) {
		if (StringUtil.blank(fileName) || StringUtil.blank(tag))
			return null;
		int posOfTag = fileName.indexOf(tag);
		if (posOfTag<0)
			return null;
		String part = fileName.substring(posOfTag+tag.length(), fileName.length());
		int endOfElement = part.indexOf("_");
		if (endOfElement<0)
			endOfElement= part.indexOf(".");
		if (endOfElement<0)
			endOfElement= fileName.length();

		String element = part.substring(0, endOfElement);
		return element;
	}
	/*.................................................................................................................*/
	public String getOTUIDCode(String fileName) {
		return getFileNameElement(fileName,"&vDRMDNA");
	}
	/*.................................................................................................................*/
	public String getGene(String fileName) {
		return getFileNameElement(fileName,"&g");
	}
	/*.................................................................................................................*/
	public String getGeneFragment(String fileName) {
		return getFileNameElement(fileName,"&f");
	}

	/*.................................................................................................................*/
	public TaxonCompleteness findTaxonCompleteness(String code) {
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMatches(code))
				return tc;
		}
		return null;
	}

	/*.................................................................................................................*/
	public boolean scanFilesForCompleteness(String directoryPath, File directory){

		logBuffer.setLength(0);
		String[] files = directory.list();
		progIndicator = new ProgressIndicator(getProject(),"Scanning files", files.length);
		progIndicator.setStopButtonName("Stop");
		progIndicator.start();
		boolean abort = false;
		String cPath;
		sequenceCount = 0;


		loglnEchoToStringBuffer(" Scanning files to check for ICOBS completeness. ", logBuffer);
		loglnEchoToStringBuffer("  "+directoryPath+"\n", logBuffer);


		for (int i=0; i<files.length; i++) {
			if (progIndicator!=null){
				progIndicator.setCurrentValue(i);
				progIndicator.setText("Number of files examined: " + i);
				if (progIndicator.isAborted())
					abort = true;
			}
			if (abort)
				break;
			if (files[i]==null )
				;
			else {
				cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
				File cFile = new File(cPath);
				if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {

					String fileName = cFile.getName();
					if (StringUtil.blank(fileName)) {
						loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
						// remove "running"
						if (progIndicator!=null) progIndicator.goAway();
						return false;
					}


						String code = getOTUIDCode(fileName);
						String gene = getGene(fileName);
						String geneFragment = getGeneFragment(fileName);

						TaxonCompleteness tc = findTaxonCompleteness(code);
						if (tc==null) {
							tc = new TaxonCompleteness(this, code);
							vector.addElement(tc);
						}
						tc.addGene(gene, geneFragment);


				}
			}
		}

		loglnEchoToStringBuffer("Number of files examined: " + files.length, logBuffer);



		if (!abort) {

			progIndicator.spin();
		}


		if (progIndicator!=null)
			progIndicator.goAway();

		int count=0;
		loglnEchoToStringBuffer("\nTaxa that are complete:", logBuffer);

		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonComplete()) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number of complete taxa: " + count + "]", logBuffer);
		count=0;
		
		
		loglnEchoToStringBuffer("\nTaxa that are complete in 28S CAD4 wg:", logBuffer);

		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxon28SCAD4wgComplete()) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number of taxa complete in 28S CAD4 wg: " + count + "]", logBuffer);
		count=0;
		


/*
		loglnEchoToStringBuffer("\nTaxa that are missing only COI and Topo:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonCompleteExceptForCOIandTopo()) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing only COI and Topo: " + count + "]", logBuffer);
		count=0;

		loglnEchoToStringBuffer("\nTaxa that are missing only COI:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonCompleteExceptForCOI()) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing only COI: " + count + "]", logBuffer);
		count=0;

		loglnEchoToStringBuffer("\nTaxa that are missing only Topo:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonCompleteExceptForTopo()) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing only Topo: " + count + "]", logBuffer);
		count=0;

*/

		loglnEchoToStringBuffer("\n============================", logBuffer);

		count=0;
		loglnEchoToStringBuffer("\nTaxa that are missing 28S:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMissingGene(TaxonCompleteness.GENE28S)) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing 28S: " + count + "]", logBuffer);

		count=0;
		loglnEchoToStringBuffer("\nTaxa that are missing CAD4:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMissingGene(TaxonCompleteness.GENECAD4)) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing CAD4: " + count + "]", logBuffer);

		count=0;
		loglnEchoToStringBuffer("\nTaxa that are missing wg:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMissingGene(TaxonCompleteness.GENEwg)) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing wg: " + count + "]", logBuffer);

		count=0;
		loglnEchoToStringBuffer("\nTaxa that are missing COI:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMissingGene(TaxonCompleteness.GENECOIBC)) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing COI: " + count + "]", logBuffer);

		count=0;
		loglnEchoToStringBuffer("\nTaxa that are missing Topo:", logBuffer);
		for (int i=0; i<vector.size(); i++) {
			TaxonCompleteness tc = (TaxonCompleteness)vector.elementAt(i);
			if (tc.taxonMissingGene(TaxonCompleteness.GENETopo)) {
				loglnEchoToStringBuffer(tc.getCode(), logBuffer);
				count++;
			}
		}
		loglnEchoToStringBuffer("[Number missing Topo: " + count + "]", logBuffer);


		return true;

	}
	/*.................................................................................................................*/
	public boolean scanDirectory(String directoryPath){
		if ( logBuffer==null)
			logBuffer = new StringBuffer();


		// if not passed-in, then ask
		if (StringUtil.blank(directoryPath)) {
			directoryPath = MesquiteFile.chooseDirectory("Choose directory containing files:", previousDirectory); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		}

		if (StringUtil.blank(directoryPath))
			return false;


		File directory = new File(directoryPath);
		importedDirectoryPath = directoryPath + MesquiteFile.fileSeparator;
		importedDirectoryName = directory.getName();
		previousDirectory = directory.getParent();
		storePreferences();
		if (directory.exists() && directory.isDirectory()) {
			return scanFilesForCompleteness(directoryPath, directory);
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
		if (checker.compare(this.getClass(), "Scan files for ICOBS Completeness.", null, commandName, "listTaxa")) {
			scanDirectory(null);
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "List Complete ICOBS Taxa [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Lists OTU ID codes.";
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

class TaxonCompleteness {
	String code;
	MesquiteModule ownerModule;

	public static int NUMREQUIREDGENES = 8;
	public static int GENE28S = 0;
	public static int GENECAD4 = 1;
	public static int GENEwg = 2;
	public static int GENECOIBC = 3;
	public static int GENETopo = 4;
	public static int GENEMSP = 5;
	public static int GENE18S = 6;
	public static int GENEArgK = 7;

	boolean[] genesPresent = new boolean[NUMREQUIREDGENES];

	public TaxonCompleteness(MesquiteModule ownerModule, String code){
		this.ownerModule = ownerModule;
		this.code = code;
		for (int i=0; i<NUMREQUIREDGENES; i++)
			genesPresent[i]=false;
	}

	public String getCode(){
		return code;
	}

	public boolean taxonMatches(String code){
		if (StringUtil.blank(code))
			return false;
		return code.equalsIgnoreCase(this.code);
	}
	
	public void add(int geneNumber, String geneName, String geneFragment){
		if (genesPresent[geneNumber])
			ownerModule.logln("DUPLICATE: code " + code + ", gene: " + geneName);
		genesPresent[geneNumber]=true;
	}


	public void addGene(String gene, String geneFragment) {
		if (StringUtil.blank(gene))
			return;
		if (gene.equalsIgnoreCase("28S")) {
			add(GENE28S, gene, geneFragment);
		}
		else if (gene.equalsIgnoreCase("wg")||gene.equalsIgnoreCase("wingless"))
			add(GENEwg, gene, geneFragment);
		else if (gene.equalsIgnoreCase("Topo")||gene.equalsIgnoreCase("Topoisomerase"))
			add(GENETopo, gene, geneFragment);
		else if (gene.equalsIgnoreCase("MSP"))
			add(GENEMSP, gene, geneFragment);
		else if (gene.equalsIgnoreCase("18S"))
			add(GENE18S, gene, geneFragment);
		else if (gene.equalsIgnoreCase("ArgK")||gene.equalsIgnoreCase("Arginine"))
			add(GENEArgK, gene, geneFragment);
		else if (gene.equalsIgnoreCase("COIBC"))
			add(GENECOIBC, gene, geneFragment);
		else if (gene.equalsIgnoreCase("CAD4"))
			add(GENECAD4, gene, geneFragment);
		else if (gene.equalsIgnoreCase("COI")) {
			if (StringUtil.blank(geneFragment) || geneFragment.equalsIgnoreCase("COIBC")|| geneFragment.equalsIgnoreCase("COIAll"))
				add(GENECOIBC, gene, geneFragment);				
		}
		else if (gene.equalsIgnoreCase("CAD")) {
			if (StringUtil.blank(geneFragment) || geneFragment.equalsIgnoreCase("CAD4")|| geneFragment.equalsIgnoreCase("CAD234"))
				add(GENECAD4, gene, geneFragment);				
		}
	}

	public boolean taxonComplete(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& genesPresent[GENECOIBC] 
										&& genesPresent[GENETopo];
	}
	public boolean taxon28SCAD4wgComplete(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4];
	}
	public boolean taxonCompleteExceptForTopo(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& genesPresent[GENECOIBC] 
										&& !genesPresent[GENETopo];
	}
	public boolean taxonCompleteExceptForCOI(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& !genesPresent[GENECOIBC] 
										&& genesPresent[GENETopo];
	}
	public boolean taxonCompleteExceptForCOIandTopo(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& !genesPresent[GENECOIBC] 
										&& !genesPresent[GENETopo];
	}
	public boolean taxonCompleteExceptForWg(){
		return genesPresent[GENE28S] 
				&& !genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& genesPresent[GENECOIBC] 
										&& genesPresent[GENETopo];
	}
	public boolean taxonCompleteExceptForCAD(){
		return genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& !genesPresent[GENECAD4] 
								&& genesPresent[GENECOIBC] 
										&& genesPresent[GENETopo];
	}
	public boolean taxonCompleteExceptFor28S(){
		return !genesPresent[GENE28S] 
				&& genesPresent[GENEwg] 
						&& genesPresent[GENECAD4] 
								&& genesPresent[GENECOIBC] 
										&& genesPresent[GENETopo];
	}

	public boolean taxonMissingGene(int geneNumber){
		return !genesPresent[geneNumber]; 
	}


}







