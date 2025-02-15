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

package mesquite.oregonstate.AnnotateGenBankPublicationData; 

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

import mesquite.lib.*;
import mesquite.lib.ui.*;
import mesquite.lib.taxa.*;
import mesquite.lib.misc.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.categ.lib.MolecularData;
import mesquite.chromaseq.lib.*;

/* ======================================================================== */
public class AnnotateGenBankPublicationData extends TaxonUtility{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;

	static String previousDirectory = null;
	int sequenceCount = 0;
	int duplicates = 0;
	//String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;
	int numProcessed = 0;


	boolean preferencesSet = false;
	boolean verbose=true;
	//		 Associable as = data.getTaxaInfo(true);

	/*...................................................................................................................*/
	void cleanUpGenBankAssociatedObject (Associable as, int whichTaxon, String genBankNote){
	}

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();
		addMenuItem(null, "Annotate GenBank & Publications [DRM Lab]...", makeCommand("tabulate", this));
		return true;
	}
	/*.................................................................................................................*/
	void recordValues(String matrixName, String sampleCode, String accession, String publication) {
		Listable[] matrices = proj.getFileElements(CharacterData.class);	
		CharacterData matrixData=null;
		for (int im = 0; im < matrices.length; im++){
			CharacterData data = (CharacterData)matrices[im];
			if (data.getName().equalsIgnoreCase(matrixName)) {
				matrixData = data;
				break;
			}
		}
		if (matrixData!=null) {
			Associable as = matrixData.getTaxaInfo(true);
			int it=0;
			boolean found=false;
			Taxa taxa = matrixData.getTaxa();
			if (taxa!=null) {
				for (it=0; it<taxa.getNumTaxa(); it++){
					String s = (String)taxa.getAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, it);
					if (sampleCode.equalsIgnoreCase(s)) {
						found=true;
						break;
					}
				}
			}
			if (found) {
				String previousGenBankValue = (String)taxa.getAssociatedObject(MolecularData.genBankNumberRef, it);
				String previousPUBValue = (String)taxa.getAssociatedObject(MolecularData.publicationCodeNameRef, it);
				if (StringUtil.notEmpty(previousGenBankValue) || StringUtil.notEmpty(previousPUBValue)){
					String message = "WARNING: duplicate values: " + matrixData.getName()+", taxon " +taxa.getTaxonName(it);
					if (StringUtil.notEmpty(previousGenBankValue)) 
						message+=", previousGenBank: " + previousGenBankValue;
					if (StringUtil.notEmpty(previousPUBValue)) 
						message+=", previousPUB: " + previousPUBValue;
					message+= ", newGenBank: " + accession + ", newPUB: " + publication;
					logln(message);
				}
					
				if (StringUtil.notEmpty(accession)) {
					as.setAssociatedObject(MolecularData.genBankNumberRef, it, accession);
					numProcessed++;
				}
				else if (StringUtil.notEmpty(publication)){
					as.setAssociatedObject(CharacterData.publicationCodeNameRef, it, publication);
					numProcessed++;
				}
				
			} else if (verbose) {
				String message = "WARNING: matrix found (" + matrixData.getName()+"), but not taxon; sample code: " +sampleCode;
				logln(message);
				
			}
				
		} else if (verbose) {
			String message = "WARNING: matrix not found: " + matrixName +", sample code " +sampleCode;
			logln(message);
		}

	}
	/*.................................................................................................................*
	void setup(){
		Listable[] matrices = proj.getFileElements(CharacterData.class);	
		if (matrices == null)
			return ;
		matrixNames=new String[matrices.length];
		for (int im = 0; im < matrices.length; im++){
			CharacterData data = (CharacterData)matrices[im];
			matrixNames[im]=data.getName();
		}
	}
	/*.................................................................................................................*/
	public void processFile(String originalFileName) {
		Parser parser = new Parser(originalFileName);
		parser.setPunctuationString("_.");
		String fullSampleCode ="";
		String DRMCode ="";
		String DNACode ="";
		String code ="";
		String gene ="";
		String fragment ="";
		String accession ="";
		String publication ="";

		String token = parser.getFirstToken();
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&v")) {
				fullSampleCode=token.substring(2);
				if (token.length()>7)
					DRMCode=token.substring(8);
				if (token.length()>4)
					DNACode=token.substring(5);
				code = DNACode;  //use this to decide
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
		if ("COI".equalsIgnoreCase(gene)){
			if ("COIBC".equalsIgnoreCase(fragment))
				recordValues("COIBC", code, accession, publication);
			else if ("COIPJ".equalsIgnoreCase(fragment))
				recordValues("COIPJ", code, accession, publication);
			else if ("COIAll".equalsIgnoreCase(fragment)){
				recordValues("COIBC", code, accession, publication);
				recordValues("COIPJ", code, accession, publication);
			}
		}
		else if ("CAD".equalsIgnoreCase(gene)){
			if ("CAD2".equalsIgnoreCase(fragment))
				recordValues("CAD2", code, accession, publication);
			else if ("CAD3".equalsIgnoreCase(fragment))
				recordValues("CAD3", code, accession, publication);
			else if ("CAD4".equalsIgnoreCase(fragment))
				recordValues("CAD4", code, accession, publication);
			else if ("CAD234".equalsIgnoreCase(fragment)){
				recordValues("CAD2", code, accession, publication);
				recordValues("CAD3", code, accession, publication);
				recordValues("CAD4", code, accession, publication);
			}
		} else
			recordValues(gene, code, accession, publication);


	}
	/*.................................................................................................................*/
	public boolean tabulateFiles(String directoryPath, File directory){
		proj = getProject();
		
		logBuffer.setLength(0);
		String[] files = directory.list();
		String cPath;
		sequenceCount = 0;
		numProcessed = 0;



		logln(" Annotation. ");
		logln("  "+directoryPath+"\n");

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

		logln("Number of files examined: " + files.length);
		logln("Number of annotations incorporated: " + numProcessed);

		return true;

	}
	/*.................................................................................................................*/
	public boolean tabulate(String directoryPath){
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

			tabulate(null);
			parametersChanged();
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
		return "Annotate GenBank & Publications [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Annotate GenBank & Publications.";
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

	public boolean operateOnTaxa(Taxa taxa) {
		tabulate(null);
		return true;
	}

}





