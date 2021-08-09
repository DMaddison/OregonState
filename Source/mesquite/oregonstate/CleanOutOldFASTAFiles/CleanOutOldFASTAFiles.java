/*
 *  Mesquite Chromaseq source code.  Copyright 2005-2011 David Maddison and Wayne Maddison.
Version 1.0   December 2011
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquites web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

/* initiated 
 * 8.vi.14 DRM based upon SegregateChromatograms
 */

/** this module is simple; it segregates files contained in a directory that have a sample code that matches those listed in a 
 * chosen file of sample codes.  It uses a ChromatogramFileNameParser to find the sample code within each file's name*/

package mesquite.oregonstate.CleanOutOldFASTAFiles; 

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
public class CleanOutOldFASTAFiles extends UtilitiesAssistant{ 
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



	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();

		addMenuItem(null, "Remove older versions of FASTA files  [DRM Lab]...", makeCommand("removeOldFasta", this));
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
	public String getSeqID(String fileName) {
		String seqID = getFileNameElement(fileName,"&i");
		if (StringUtil.notEmpty(seqID))
			seqID=seqID.replace("SEQID", "0");
		return seqID;
	}
	/*.................................................................................................................*/
	public boolean getMultiFragmentAll(String gene, String geneFragment) {
		if (StringUtil.blank(geneFragment))
			return false;
		if (gene.equalsIgnoreCase("COI")&& geneFragment.equalsIgnoreCase("COIAll"))
			return true;
		if (gene.equalsIgnoreCase("CAD")&& geneFragment.equalsIgnoreCase("CAD234"))
			return true;
		if (gene.equalsIgnoreCase("CAD")&& geneFragment.equalsIgnoreCase("CADAll"))
			return true;
		return false;
	}
	/*.................................................................................................................*/
	public boolean getMultiFragmentStart(String gene, String geneFragment) {
		if (StringUtil.blank(geneFragment))
			return false;
		if (gene.equalsIgnoreCase("COI")&& geneFragment.equalsIgnoreCase("COIBC"))
			return true;
		if (gene.equalsIgnoreCase("CAD")&& geneFragment.equalsIgnoreCase("CAD2"))
			return true;
		return false;
	}
	/*.................................................................................................................*/
	public boolean getMultiFragmentEnd(String gene, String geneFragment) {
		if (StringUtil.blank(geneFragment))
			return false;
		if (gene.equalsIgnoreCase("COI")&& geneFragment.equalsIgnoreCase("COIPJ"))
			return true;
		if (gene.equalsIgnoreCase("CAD")&& geneFragment.equalsIgnoreCase("CAD4"))
			return true;
		return false;
	}
	/*.................................................................................................................*/
	public boolean correctFragment(String gene, String geneFragment, String otherGeneFragment) {
		if (StringUtil.blank(geneFragment) || StringUtil.blank(otherGeneFragment))  // no fragments specified for at least one
			return true;
		if (geneFragment.equalsIgnoreCase(otherGeneFragment))
			return true;
		if (gene.equalsIgnoreCase("COI")) {
			if (geneFragment.equalsIgnoreCase("COIAll") || otherGeneFragment.equalsIgnoreCase("COIAll"))
				return true;
			return false;
		}
		if (gene.equalsIgnoreCase("CAD")) {
			if (geneFragment.equalsIgnoreCase("CADAll") || otherGeneFragment.equalsIgnoreCase("CADAll"))
				return true;
			if (geneFragment.equalsIgnoreCase("CAD234") || otherGeneFragment.equalsIgnoreCase("CAD234"))
				return true;
			return false;
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean sameFragment(String gene, String geneFragment, String otherGeneFragment) {
		if (gene.equalsIgnoreCase("COI") || gene.equalsIgnoreCase("CAD")) {  // these ones need to have the gene fragment specified
			if (StringUtil.notEmpty(geneFragment) && StringUtil.notEmpty(otherGeneFragment))  //  fragments specified for both
				if (geneFragment.equalsIgnoreCase(otherGeneFragment))  // same one
					return true;
			return false;
		} 
		return true;
	}
	/*.................................................................................................................*/
	public boolean fragmentNameMissing(String gene, String geneFragment) {
		if (gene.equalsIgnoreCase("COI")) {
			if (StringUtil.blank(geneFragment))  // no fragments specified for at least one
				return true;
		}
		if (gene.equalsIgnoreCase("CAD")) {
			if (StringUtil.blank(geneFragment))  // no fragments specified for at least one
				return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	public String getAccessionNumber(String fileName) {
		return getFileNameElement(fileName,"&a");
	}
	static String GENBANK = "GENBANK";
	static String MULTIFRAGMENT = "MULTIFRAGMENT";
	static String REPLACED = "REPLACED";
	/*.................................................................................................................*/
	private void markYounger(String directoryPath, String[] files, String cPath, File cFile, String fileName) {
		String sampleCode = getOTUIDCode(fileName);
		String gene = getGene(fileName);
		if (StringUtil.blank(sampleCode)) {
			logln("Note: Without a DRMDNA sample code. "+ fileName);
			return;
		}
		if (StringUtil.blank(gene)) {
			logln("WARNING: Gene is missing. "+ fileName);
			return;
		}
		String geneFragment = getGeneFragment(fileName);
		String accessionNumber = getAccessionNumber(fileName);
		boolean inGenBank = StringUtil.notEmpty(accessionNumber);
		String seqID = getSeqID(fileName);
		int seqNumber = MesquiteInteger.fromString(seqID);
		int maxSeqNumber = seqNumber;
		if (!MesquiteInteger.isCombinable(maxSeqNumber))
			maxSeqNumber=-1;
		Vector fileVector = new Vector();
		if (!inGenBank)
			fileVector.addElement(fileName);  // only add ones not in GenBank
		int maxVectorElement = 0;
		boolean atLeastOneInGenBank = inGenBank;

		boolean multiFragmentAll = getMultiFragmentAll(gene, geneFragment);
		boolean multiFragmentStart = getMultiFragmentStart(gene, geneFragment);
		boolean multiFragmentEnd = getMultiFragmentEnd(gene, geneFragment);
		boolean fragmentNameMissing = fragmentNameMissing(gene, geneFragment);
		boolean allSameFragment = true;


		if (StringUtil.blank(sampleCode) && StringUtil.blank(gene) && StringUtil.blank(seqID)) {
			return;
		}
		/*		Debugg.println("\n\n" + fileName);
		Debugg.println("fileVector.size(): " + fileVector.size());
		Debugg.println("sample: " + sampleCode + ", gene: " + gene + ", seqID: " + seqID+ ", seqNumber: " + seqNumber);
		 */
		for (int i=0; i<files.length; i++) {
			if (files[i]!=null) {
				String otherPath;
				otherPath = directoryPath + MesquiteFile.fileSeparator + files[i];
				File otherFile = new File(otherPath);
				if (otherFile.exists() && !otherFile.isDirectory() && (!files[i].startsWith("."))) {

					String otherFileName = otherFile.getName();
					if (StringUtil.notEmpty(fileName) && StringUtil.notEmpty(otherFileName) && !fileName.equalsIgnoreCase(otherFileName)) {
						String otherGene = getGene(otherFileName);
						if (!fileName.contains(MULTIFRAGMENT) && !fileName.contains(REPLACED) && StringUtil.notEmpty(otherGene)) {
							String otherSampleCode = getOTUIDCode(otherFileName);
							String otherGeneFragment = getGeneFragment(otherFileName);
							String otherAccessionNumber = getAccessionNumber(otherFileName);
							boolean otherInGenBank = StringUtil.notEmpty(otherAccessionNumber);
							String otherSeqID = getSeqID(otherFileName);
							int otherSeqNumber = MesquiteInteger.fromString(otherSeqID);
							if (!MesquiteInteger.isCombinable(otherSeqNumber))
								otherSeqNumber=-1;

							if (sampleCode.equalsIgnoreCase(otherSampleCode) && gene.equalsIgnoreCase(otherGene) && (seqNumber!=otherSeqNumber || !MesquiteInteger.isCombinable(seqNumber))) { // same sample, same gene
								if (getMultiFragmentAll(otherGene, otherGeneFragment))
									multiFragmentAll=true;
								if (getMultiFragmentStart(otherGene, otherGeneFragment))
									multiFragmentStart=true;
								if (getMultiFragmentEnd(otherGene, otherGeneFragment))
									multiFragmentEnd=true;
								boolean otherFragmentNameMissing = fragmentNameMissing(gene, otherGeneFragment);
								if (!sameFragment(gene, geneFragment, otherGeneFragment))
									allSameFragment = false;

								//Debugg.println("   other: sample: " + otherSampleCode + ", gene: " + otherGene + ", seqID: " + otherSeqID+ ", seqNumber: " + otherSeqNumber);
								if (maxSeqNumber< otherSeqNumber) {  // found a new maximum
									maxSeqNumber = otherSeqNumber;
									maxVectorElement = fileVector.size();
								}
								//if (!otherInGenBank || !allSameFragment)
								fileVector.addElement(otherFileName);
								if (otherInGenBank)
									atLeastOneInGenBank=true;
							}
						}
					}
				}
			}
		}

		/*
		Debugg.println("maxSeqNumber: " + maxSeqNumber);
		Debugg.println("maxVectorElement: " + maxVectorElement);
		Debugg.println("fileVector.size(): " + fileVector.size());
		 */

		if (atLeastOneInGenBank) {  
			for (int i=0; i<fileVector.size(); i++) {
				String originalPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i);
				String modifiedPath="";
				boolean hasAccessionNumber = StringUtil.notEmpty(getAccessionNumber((String)fileVector.get(i)));  // double-check to see if this one has an accession number
				if (allSameFragment && !hasAccessionNumber) { // if they are all the same fragment, we replace only  the non-GenBank ones 
					modifiedPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i)+"."+REPLACED;
					MesquiteFile.rename(originalPath, modifiedPath);
				} else if (!allSameFragment)  { // if they aren't all the same fragment, we move everything over. 
					modifiedPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i)+"."+GENBANK;
					MesquiteFile.rename(originalPath, modifiedPath);
				}
			}
		} else if (allSameFragment) {  
			for (int i=0; i<fileVector.size(); i++) {
				if (maxVectorElement!=i) {
					String originalPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i);
					String modifiedPath="";
					modifiedPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i)+"."+REPLACED;
					MesquiteFile.rename(originalPath, modifiedPath);

				}
			}
		} else
			for (int i=0; i<fileVector.size(); i++) {
				String originalPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i);
				String modifiedPath="";
				modifiedPath = directoryPath + MesquiteFile.fileSeparator + fileVector.get(i)+"."+MULTIFRAGMENT;
				MesquiteFile.rename(originalPath, modifiedPath);
			}

	}

	/*.................................................................................................................*/
	public boolean scanFilesForDuplicates(String directoryPath, File directory){

		logBuffer.setLength(0);
		String[] files = directory.list();
		progIndicator = new ProgressIndicator(getProject(),"Scanning files", files.length);
		progIndicator.setStopButtonName("Stop");
		progIndicator.start();
		boolean abort = false;
		String cPath;
		sequenceCount = 0;


		loglnEchoToStringBuffer(" Removing older versions. ", logBuffer);
		loglnEchoToStringBuffer("  "+directoryPath+"\n", logBuffer);

		cPath= directoryPath + MesquiteFile.fileSeparator +MULTIFRAGMENT;
		if (!MesquiteFile.fileOrDirectoryExists(cPath))
			MesquiteFile.createDirectory(cPath);
		cPath= directoryPath + MesquiteFile.fileSeparator +GENBANK;
		if (!MesquiteFile.fileOrDirectoryExists(cPath))
			MesquiteFile.createDirectory(cPath);
		cPath= directoryPath + MesquiteFile.fileSeparator +REPLACED;
		if (!MesquiteFile.fileOrDirectoryExists(cPath))
			MesquiteFile.createDirectory(cPath);

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
						if (progIndicator!=null) progIndicator.goAway();
						return false;
					}		
					if (!fileName.contains(MULTIFRAGMENT) && !fileName.contains(REPLACED)&& !fileName.contains(GENBANK)) 
						markYounger(directoryPath, files, cPath, cFile, fileName);
				}
			}
		}

		files = directory.list();
		int replaceCount =0;
		loglnEchoToStringBuffer("  Sequestering the following files to REPLACED directory:", logBuffer);
		String originalPath ="";
		String modifiedPath = "";
		for (int i=files.length-1; i>=0; i--) {
			cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
			File cFile = new File(cPath);
			if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {
				if (files[i]!=null && files[i].contains(REPLACED)) {
					originalPath= directoryPath + MesquiteFile.fileSeparator+ files[i];
					modifiedPath= directoryPath + MesquiteFile.fileSeparator +REPLACED + MesquiteFile.fileSeparator+ files[i];
					MesquiteFile.rename(originalPath, modifiedPath);
					loglnEchoToStringBuffer("  " + files[i], logBuffer);
					replaceCount++;
				}
			}
		}

		files = directory.list();
		int multiFragmentCount =0;
		loglnEchoToStringBuffer("  Sequestering the following files to MULTIFRAGMENT directory:", logBuffer);
		for (int i=files.length-1; i>=0; i--) {
			cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
			File cFile = new File(cPath);
			if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {
				if (files[i]!=null && files[i].contains(MULTIFRAGMENT)) {
					originalPath= directoryPath + MesquiteFile.fileSeparator+ files[i];
					modifiedPath= directoryPath + MesquiteFile.fileSeparator +MULTIFRAGMENT + MesquiteFile.fileSeparator+ files[i];
					MesquiteFile.rename(originalPath, modifiedPath);
					loglnEchoToStringBuffer("  " + files[i], logBuffer);
					multiFragmentCount++;
				}
			}
		}

		files = directory.list();
		int genBankCount =0;
		loglnEchoToStringBuffer("  Sequestering the following files to GenBank directory:", logBuffer);
		for (int i=files.length-1; i>=0; i--) {
			cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
			File cFile = new File(cPath);
			if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {
				if (files[i]!=null && files[i].contains(GENBANK)) {
					originalPath= directoryPath + MesquiteFile.fileSeparator+ files[i];
					modifiedPath= directoryPath + MesquiteFile.fileSeparator +GENBANK + MesquiteFile.fileSeparator+ files[i];
					MesquiteFile.rename(originalPath, modifiedPath);
					loglnEchoToStringBuffer("  " + files[i], logBuffer);
					genBankCount++;
				}
			}
		}

		if (!abort) {

			progIndicator.spin();
		}


		if (progIndicator!=null)
			progIndicator.goAway();

		loglnEchoToStringBuffer("Number of files examined: " + files.length, logBuffer);
		loglnEchoToStringBuffer("Number of files sequestered to "+ REPLACED +" directory: " + replaceCount, logBuffer);
		loglnEchoToStringBuffer("Number of files sequestered to "+ MULTIFRAGMENT + " directory: " + multiFragmentCount, logBuffer);
		loglnEchoToStringBuffer("Number of files sequestered to "+ GENBANK + " directory: " + genBankCount, logBuffer);



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
			return scanFilesForDuplicates(directoryPath, directory);
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
		if (checker.compare(this.getClass(), "Remove old FASTA files.", null, commandName, "removeOldFasta")) {
			scanDirectory(null);
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Remove Older FASTA files [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Removes older versions of FASTA files names with DRM Lab &v &i &a naming conventions.";
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









