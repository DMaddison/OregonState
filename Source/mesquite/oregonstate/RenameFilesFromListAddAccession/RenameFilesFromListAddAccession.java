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

package mesquite.oregonstate.RenameFilesFromListAddAccession; 

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;
import mesquite.lib.ui.*;
import mesquite.lib.taxa.*;
import mesquite.lib.misc.*;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.chromaseq.lib.*;

/* ======================================================================== */
public class RenameFilesFromListAddAccession extends UtilitiesAssistant{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;

	static String previousDirectory = null;
	ProgressIndicator progIndicator = null;
	int sequenceCount = 0;
	String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;

	String sampleCodeListPath = null;
	String sampleCodeListFile = null;
	String sampleCodeList = "";
	Parser sampleCodeListParser = null;


	boolean preferencesSet = false;
	boolean verbose=true;
	


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();

		addMenuItem(null, "Rename Files Add Accession [DRM Lab]...", makeCommand("rename", this));
		return true;
	}
	/*.................................................................................................................*
	boolean makeDirectoriesForMatch(String matchStringPath){

		File newDir = new File(matchStringPath);
		try { newDir.mkdir();    //make folder for this match			
		}
		catch (SecurityException e) { 
			logln("Couldn't make directory.");
			return false;
		}
		return true;
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
	public String getNewFileName(String originalFileName, String matchLine) {
		MesquiteInteger pos = new MesquiteInteger(0);
		String token = StringUtil.getNextTabbedToken(matchLine, pos);
		String geneName = StringUtil.getNextTabbedToken(matchLine, pos);
		String fragmentName = StringUtil.getNextTabbedToken(matchLine, pos); 
		String accessionNumber = StringUtil.getNextTabbedToken(matchLine, pos); 
		
		StringBuffer newName = new StringBuffer();
		parser.setString(originalFileName);
		parser.setPunctuationString("_.");
		token = parser.getFirstToken();
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&v")){
				newName.append(token+"_");
			}
			else if (token.startsWith("&a")){
			}
			else if (!token.equalsIgnoreCase("fas") && !token.equalsIgnoreCase("_")&& !token.equalsIgnoreCase(".")) {
				newName.append(token+"_");
			}
			token = parser.getNextToken();
		}

		if (StringUtil.notEmpty(accessionNumber))
			newName.append("&a"+accessionNumber+"");
		
		newName.append(".fas");

		return newName.toString();
	}
	/** This scans the file listing the accessions, and sees if a particular entry is in the list.  The file format
	 * should be as follows:  Tab-delimited file, four columns.  
	 * First column is sample code.
	 * Second column is gene name
	 * Third column is fragment name
	 * Fourth column is GenBank accession number. 
	 * */

	public String codeIsInCodeListFile(String codeToMatch, String geneToMatch, String fragmentToMatch) {
		if (sampleCodeListParser==null)
			return null;
		if (StringUtil.blank(codeToMatch)||StringUtil.blank(geneToMatch))
			return null;
		sampleCodeListParser.setPosition(0);
		Parser subParser = new Parser();
		String line = sampleCodeListParser.getRawNextDarkLine();
		while (StringUtil.notEmpty(line)) {
			subParser.setString(line);
			MesquiteInteger pos = new MesquiteInteger(0);
			String code = StringUtil.getNextTabbedToken(line, pos);
			String gene = StringUtil.getNextTabbedToken(line, pos);
			String fragment = StringUtil.getNextTabbedToken(line, pos); 
			fragment = StringUtil.stripBoundingWhitespace(fragment);
			if (StringUtil.notEmpty(fragmentToMatch)) {
				if (codeToMatch.equalsIgnoreCase(code) && geneToMatch.equalsIgnoreCase(gene)&& fragmentToMatch.equalsIgnoreCase(fragment)) {
					return line;
				}
			}
			else if (codeToMatch.equalsIgnoreCase(code) && geneToMatch.equalsIgnoreCase(gene)) {
				return line;
			}
			line = sampleCodeListParser.getRawNextDarkLine();
		}
		return null;
	}

	/*.................................................................................................................*/
	public boolean renameFiles(String directoryPath, File directory){

		logBuffer.setLength(0);
		String[] files = directory.list();
		progIndicator = new ProgressIndicator(getProject(),"Renaming Files", files.length);
		progIndicator.setStopButtonName("Stop");
		progIndicator.start();
		boolean abort = false;
		String cPath;
		sequenceCount = 0;


		loglnEchoToStringBuffer(" Renaming files that match the specified criteria. ", logBuffer);
		loglnEchoToStringBuffer("  "+directoryPath+"\n", logBuffer);

		int numPrepared = 0;

		for (int i=0; i<files.length; i++) {
			if (progIndicator!=null){
				progIndicator.setCurrentValue(i);
				progIndicator.setText("Number of files renamed: " + numPrepared);
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

					String originalFileName = cFile.getName();
					if (StringUtil.blank(originalFileName)) {
						loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
						// remove "running"
						if (progIndicator!=null) progIndicator.goAway();
						return false;
					}

					String codeToMatch = "";
					String geneToMatch = "";
					String fragmentToMatch = "";
					MesquiteString startTokenResult = new MesquiteString();
					//here's where the names parser processes the name

					Parser parser = new Parser(originalFileName);
					parser.setPunctuationString("_.");
					String token = parser.getFirstToken();
					while (StringUtil.notEmpty(token)) {
						if (token.startsWith("&v")) {  //get sample code:   NOTE THIS PRESUMED proceded by "DRMDNA"
							codeToMatch=token.substring(8);
						} else if (token.startsWith("&g")) {  //get gene
							geneToMatch=token.substring(2);
						} else if (token.startsWith("&f")) {  //get fragment
						fragmentToMatch=token.substring(2);
					}
						token = parser.getNextToken();
					}

					// match and name new name

			//		if (!nameParserManager.parseFileName(chromFileName, sampleCode, sampleCodeSuffix, primerName, logBuffer, startTokenResult, null))
			//			continue;

					if (startTokenResult.getValue() == null)
						startTokenResult.setValue("");

					if (codeToMatch.equalsIgnoreCase("2560")) {
						Debugg.println(originalFileName);
					}
					String matchLine = codeIsInCodeListFile(codeToMatch, geneToMatch, fragmentToMatch);



					if (StringUtil.notEmpty(matchLine)) {
						if (verbose)
							loglnEchoToStringBuffer(originalFileName, logBuffer);
						numPrepared++;
						try {
							String newFileName = getNewFileName(originalFileName, matchLine);
							if (verbose)
								loglnEchoToStringBuffer("   ->"+newFileName, logBuffer);
							String newFilePath = directoryPath + MesquiteFile.fileSeparator + newFileName;					
							File newFile = new File(newFilePath); //
							cFile.renameTo(newFile); 
						}
						catch (SecurityException e) {
							logln( "Can't rename: " + originalFileName);
						}

					} 


				}
			}
		}

		loglnEchoToStringBuffer("Number of files examined: " + files.length, logBuffer);
		loglnEchoToStringBuffer("Number of files renamed: " + numPrepared, logBuffer);



		if (!abort) {

			progIndicator.spin();
		}


		if (progIndicator!=null)
			progIndicator.goAway();

		return true;

	}
	/*.................................................................................................................*/
	public boolean renameFiles(String directoryPath){
		if ( logBuffer==null)
			logBuffer = new StringBuffer();

		loglnEchoToStringBuffer("Renaming files based on information in file: " + sampleCodeListFile, logBuffer);


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
		importedDirectoryPath = directoryPath + MesquiteFile.fileSeparator;
		importedDirectoryName = directory.getName();
		previousDirectory = directory.getParent();
		storePreferences();
		if (directory.exists() && directory.isDirectory()) {
			return renameFiles(directoryPath, directory);
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
		if (checker.compare(this.getClass(), "Renames files using information in a text file.", null, commandName, "rename")) {

			MesquiteString dnaNumberListDir = new MesquiteString();
			MesquiteString dnaNumberListFile = new MesquiteString();
			String s = MesquiteFile.openFileDialog("Choose file containing sample codes", dnaNumberListDir, dnaNumberListFile);
			if (!StringUtil.blank(s)) {
				sampleCodeListPath = s;
				sampleCodeListFile = dnaNumberListFile.getValue();
				processCodesFile();
				renameFiles(null);


			}
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Rename Files  Add Accession [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Renames files, adding accession numbers from a list.";
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





