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

package mesquite.oregonstate.SegregateChromMatchingFBCs; 

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
public class SegregateChromMatchingFBCs extends UtilitiesAssistant implements ActionListener{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;
	ChromatogramFileNameParser nameParserManager;

	static String previousDirectory = null;
	ProgressIndicator progIndicator = null;
	int sequenceCount = 0;
	String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;

	PrimerInfoSource primerInfoTask = null;

	boolean preferencesSet = false;
	boolean verbose=true;
	boolean stripSixFromVoucher = true;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e2 = registerEmployeeNeed(PrimerInfoSource.class, "Chromatogram processing requires a source of information about primers, including their names, direction, and sequence, as well as the gene fragments to which they correspond.", "This is activated automatically.");
		EmployeeNeed e3 = registerEmployeeNeed(ChromatogramFileNameParser.class, "Chromatogram processing requires a means to determine the sample code and primer name.", "This is activated automatically.");
	}

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();

		addMenuItem(null, "Segregate Chromatograms by FBC files [DRM Lab]...", makeCommand("segregate", this));
		return true;
	}
	/*.................................................................................................................*/
	public boolean hireRequired(){

		if (nameParserManager == null)
			nameParserManager= (ChromatogramFileNameParser)MesquiteTrunk.mesquiteTrunk.hireEmployee(ChromatogramFileNameParser.class, "Supplier of sample code and primer name from the chromatogram file name.");
		if (nameParserManager == null) {
			return false;
		} else if (!nameParserManager.queryOptions())
				return false;

		primerInfoTask = (PrimerInfoSource)hireCompatibleEmployee(PrimerInfoSource.class,  new MesquiteString("alwaysAsk"), "Supplier of information about primers and gene fragments");
		if (primerInfoTask==null) 
			return false;


		return true;
	}
	/*.................................................................................................................*/
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
	/*.................................................................................................................*
	public String getNewFileName(String originalFileName, String matchLine) {
		Parser parser = new Parser(matchLine);
		String token = parser.getFirstToken();// should be GenBank accession number
		String geneName = parser.getNextToken();
		String sampleCode = parser.getNextToken();
		
		StringBuffer newName = new StringBuffer();
		if (StringUtil.blank(sampleCode))
			newName.append("&vDRMDNA0000_");
		else
			newName.append("&vDRMDNA"+sampleCode+"_");
		parser.setString(originalFileName);
		parser.setPunctuationString("_.");
		token = parser.getFirstToken();
		boolean alreadyHasGeneName=false;
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&g")) {
				alreadyHasGeneName=true;
				break;
			}
			token = parser.getNextToken();
		}
		if (!alreadyHasGeneName)
			newName.append("&g"+geneName+"_");
		newName.append(originalFileName);

		return newName.toString();
	}
	/*.................................................................................................................*/

	JLabel nameParserLabel = null;
	JLabel sequenceNameTaskLabel = null;
	JLabel primerInfoTaskLabel = null;
	
	MesquiteTextCanvas nameParserTextCanvas = null;
	MesquiteTextCanvas primerInfoTaskTextCanvas = null;
	Button sequenceNameTaskButton = null;
	Button nameParserButton = null;
	Button primerInfoTaskButton = null;

	
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(ExtensibleDialog.defaultCANCEL);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Segregate Chromatograms Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()

		TextCanvasWithButtons textCanvasWithButtons;

		//section for name parser
				
				dialog.addHorizontalLine(1);
				dialog.addLabel("Chromatogram File Name Parser");
				dialog.forceNewPanel();
				String s = getModuleText(nameParserManager);
				if (MesquiteTrunk.mesquiteTrunk.numModulesAvailable(ChromatogramFileNameParser.class)>1){
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s,"File Name Parser...", "nameParserReplace", "Options...", "nameParserButton",this);
					nameParserButton = textCanvasWithButtons.getButton2();
				}
				else {
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s, "Options...", "nameParserButton",this);
					nameParserButton = textCanvasWithButtons.getButton();
				}
				nameParserButton.setEnabled (nameParserManager.hasOptions());
				nameParserTextCanvas = textCanvasWithButtons.getTextCanvas();
				

		//section for PrimerInfoSource
				dialog.addHorizontalLine(1);
				dialog.addLabel("Source of Primer Information");
				dialog.forceNewPanel();
				s = getModuleText(primerInfoTask);
				if (MesquiteTrunk.mesquiteTrunk.numModulesAvailable(PrimerInfoSource.class)>1){
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s,"Primer Info Source...", "primerInfoTaskReplace", "Options...", "primerInfoTaskButton",this);
					primerInfoTaskButton = textCanvasWithButtons.getButton2();
				}
				else {
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s, "Options...", "primerInfoTaskButton",this);
					primerInfoTaskButton = textCanvasWithButtons.getButton();
				}
				nameParserButton.setEnabled (primerInfoTask.hasOptions());
				primerInfoTaskTextCanvas = textCanvasWithButtons.getTextCanvas();
				Checkbox stripSixFromVoucherBox = dialog.addCheckBox("strip 6 characters from start of FBC voucher code ", stripSixFromVoucher);
		

				dialog.setDefaultButton("Segregate");



		s = "This will move all chromatograms whose long sequence names and gene fragment names contain the specified text into their own folder.\n";
		s+="Mesquite extracts from within the name of each chromatogram file for both a code indicating the sample (e.g., a voucher number) and the primer name. ";
		s+= "To allow this, you must first define an rule that defines how the chromatogram file names are structured.\n\n";
		s+= "If you so choose, Mesquite will search for the sample code within a sample names file you select, on each line of which is:\n";
		s+= "   <code><tab><short sequence name><tab><long sequence name>;\n";
		s+= "where the code, short sequence name, and long sequence name are all single tokens (you can force a multi-word name to be a single token by surrounding the name with single quotes). ";
		s+= "For Segregating Chromosomes, only the long sequence name is used.   If you wish, it can contain many taxa names, e.g. \"Insecta Coleoptera Carabidae Trechinae Bembidiini Bembidion (Pseudoperyphus) louisella\".\n\n";
		dialog.appendToHelpString(s);

		dialog.completeAndShowDialog(true);
		boolean success=(buttonPressed.getValue()== dialog.defaultOK);
		if (success)  {
			stripSixFromVoucher = stripSixFromVoucherBox.getState();
		}
		storePreferences();  // do this here even if Cancel pressed as the File Locations subdialog box might have been used
		dialog.dispose();
		return success;
	}

	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		 if (e.getActionCommand().equalsIgnoreCase("nameParserButton")) {
			if (nameParserManager!=null) {
				if (nameParserManager.queryOptions() && nameParserTextCanvas!=null)
					nameParserTextCanvas.setText(getModuleText(nameParserManager));
			}
		}
		else if (e.getActionCommand().equalsIgnoreCase("primerInfoTaskButton")) {
			if (primerInfoTask!=null) {
				if (primerInfoTask.queryOptions() && primerInfoTaskTextCanvas!=null)
					primerInfoTaskTextCanvas.setText(getModuleText(primerInfoTask));
			}
		}
	}

	/*.................................................................................................................*/

	public String getFBCSampleCode(String FBCFileName) {
		if (StringUtil.blank(FBCFileName))
			return null;
		Parser FBCParser = new Parser (FBCFileName);
		FBCParser.setPunctuationString("_.");
		String token = FBCParser.getFirstToken();
		String sampleCode = "";
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&v")){
				if (stripSixFromVoucher && token.length()>8)
					sampleCode=token.substring(8);
				else
					sampleCode=token.substring(2);
				return sampleCode;
			} else
			token = FBCParser.getNextToken();
		}
		return null;
	}
	/*.................................................................................................................*/

	public String getFBCGene(String FBCFileName) {
		if (StringUtil.blank(FBCFileName))
			return null;
		Parser FBCParser = new Parser (FBCFileName);
		FBCParser.setPunctuationString("_.");
		String token = FBCParser.getFirstToken();
		String geneName = "";
		String fragmentName ="";
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&g")){
				geneName=token.substring(2);
			} else if (token.startsWith("&f")){
				fragmentName=token.substring(2);	
			}
			token = FBCParser.getNextToken();
		}
		if (StringUtil.notEmpty(fragmentName))
			return fragmentName;
		return geneName;
	}
	/*.................................................................................................................*

	public boolean chromatogramMatches(String sampleCode, String gene, String chromatogramFileName) {
		if (StringUtil.blank(sampleCode) || StringUtil.blank(gene) || StringUtil.blank(chromatogramFileName))
			return false;
		Parser FBCParser = new Parser (FBCFileName);
		Parser chromatogramFileParser = new Parser (chromatogramFileName);
		FBCParser.setPunctuationString("_.");
		chromatogramFileParser.setPunctuationString("_.");
		String token = FBCParser.getFirstToken();
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
		return false;
	}

	/*.................................................................................................................*/

	public boolean chromatogramMatches(MesquiteString sampleCode, String fragName, String FBCSampleCode, String FBCGeneName) {

		boolean match = false;
		if (!sampleCode.isBlank() && StringUtil.notEmpty(fragName)) {
			match = sampleCode.toString().equalsIgnoreCase(FBCSampleCode);
			if (match) {  // sample code matches, good
				match = fragName.equalsIgnoreCase(FBCGeneName);
				if (!match) {
					match = (FBCGeneName.equals("COIAll") && (fragName.equalsIgnoreCase("COIPJ")||fragName.equalsIgnoreCase("COIBC"))) || (FBCGeneName.equals("CAD234") && fragName.startsWith("CAD"));
				}
			}
		}
		return match;
	}
	/*.................................................................................................................*/
	public boolean segregateFiles(String chromatoDirectoryPath, File chromatoDirectory, String FBCDirectoryPath, File FBCDirectory){

		logBuffer.setLength(0);
		String[] FBCfiles = FBCDirectory.list();
		String chromatoFilePath="";
		String processedDirPath = chromatoDirectoryPath + MesquiteFile.fileSeparator + "Represented in FBCs";
		progIndicator = new ProgressIndicator(getProject(),"Segregating Files", FBCfiles.length);
		progIndicator.setStopButtonName("Stop");
		progIndicator.start();
		boolean abort = false;
		String cPath;
		sequenceCount = 0;

		if (primerInfoTask!=null)
			primerInfoTask.echoParametersToFile(logBuffer);

		int numPrepared = 0;
		if (nameParserManager!=null) {
			nameParserManager.setWarnIfCantExtract(false);
		}

		for (int i=0; i<FBCfiles.length; i++) {
			if (progIndicator!=null){
				progIndicator.setCurrentValue(i);
				progIndicator.setText("Number of files segregated: " + numPrepared);
				if (progIndicator.isAborted())
					abort = true;
			}
			if (abort)
				break;
			if (FBCfiles[i]==null )
				;
			else {
				cPath = FBCDirectoryPath + MesquiteFile.fileSeparator + FBCfiles[i];
				File FBCFile = new File(cPath);
				if (FBCFile.exists() && !FBCFile.isDirectory() && (!FBCfiles[i].startsWith("."))) {

					String FBCFileName = FBCFile.getName();
					if (StringUtil.blank(FBCFileName)) {
						loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
						// remove "running"
						if (progIndicator!=null) progIndicator.goAway();
						if (nameParserManager!=null) {
							nameParserManager.setWarnIfCantExtract(true);
						}
					return false;
					}

					String FBCSampleCode = getFBCSampleCode(FBCFileName);
					String FBCGeneName = getFBCGene(FBCFileName);

					MesquiteString sampleCodeSuffix = new MesquiteString();
					MesquiteString sampleCode = new MesquiteString();
					MesquiteString primerName = new MesquiteString();
					MesquiteString startTokenResult = new MesquiteString();
					//here's where the names parser processes the name

					String[] chromatogramFiles = chromatoDirectory.list();
					for (int j=0; j<chromatogramFiles.length; j++) {  // now let's go through the chromatogram files to see if 
						if (chromatogramFiles[j]==null )
							;
						else {
							chromatoFilePath = chromatoDirectoryPath + MesquiteFile.fileSeparator + chromatogramFiles[j];
							File chromatogramFile = new File(chromatoFilePath);
							if (chromatogramFile.exists() && !chromatogramFile.isDirectory() && (!chromatogramFiles[j].startsWith("."))) {

								String chromatogramFileName = chromatogramFile.getName();
								if (StringUtil.blank(chromatogramFileName)) {
									loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
									// remove "running"
									if (progIndicator!=null) progIndicator.goAway();
									if (nameParserManager!=null) {
										nameParserManager.setWarnIfCantExtract(true);
									}
									return false;
								}		
								
								if (nameParserManager!=null) {
									if (!nameParserManager.parseFileName(chromatogramFileName, sampleCode, sampleCodeSuffix, primerName, logBuffer, startTokenResult, null))
										continue;
								}
								else {
									loglnEchoToStringBuffer("Naming parsing rule is absent.", logBuffer);
									if (progIndicator!=null)
										progIndicator.goAway();
									if (nameParserManager!=null) {
										nameParserManager.setWarnIfCantExtract(true);
									}
									return false;
								}

								String fragName = "";
								if (primerInfoTask != null){
									fragName = primerInfoTask.getGeneFragmentName(primerName.getValue());
								}


								boolean match = chromatogramMatches(sampleCode, fragName, FBCSampleCode,FBCGeneName);

								if (match) {
									if (verbose)
										loglnEchoToStringBuffer(chromatogramFileName, logBuffer);
									numPrepared++;
									if (!makeDirectoriesForMatch(processedDirPath)){   //make directories for this in case it doesn't already exist
										if (progIndicator!=null) progIndicator.goAway();
										return false;
									}
									try {
										String newFileName = chromatogramFileName;
										String newFilePath = processedDirPath + MesquiteFile.fileSeparator + chromatogramFileName;					
										File newFile = new File(newFilePath); //
										int count=1;
										while (newFile.exists()) {
											newFileName = ""+count + "." + chromatogramFileName;
											newFilePath = processedDirPath + MesquiteFile.fileSeparator + newFileName;
											newFile = new File(newFilePath);
											count++;
										}
										chromatogramFile.renameTo(newFile); 
									}
									catch (SecurityException e) {
										logln( "Can't rename: " + chromatogramFileName);
									}
									
								} 
							}
						}
					}
				}
			}
		}

		loglnEchoToStringBuffer("Number of files examined: " + FBCfiles.length, logBuffer);
		loglnEchoToStringBuffer("Number of files segregated: " + numPrepared, logBuffer);



		if (!abort) {

			progIndicator.spin();
		}


		if (nameParserManager!=null) {
			nameParserManager.setWarnIfCantExtract(true);
		}
		if (progIndicator!=null)
			progIndicator.goAway();

		return true;

	}
	/*.................................................................................................................*/
	public boolean segregateFiles(){
		if ( logBuffer==null)
			logBuffer = new StringBuffer();



		MesquiteBoolean pleaseStorePrefs = new MesquiteBoolean(false);

		if (pleaseStorePrefs.getValue())
			storePreferences();

		String directoryToSegregatePath="";
		String directoryWithFBCsPath="";

		if (StringUtil.blank(directoryToSegregatePath)) {
			directoryToSegregatePath = MesquiteFile.chooseDirectory("Choose directory containing files to segregate:", previousDirectory); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		}
		if (StringUtil.blank(directoryToSegregatePath))
			return false;
		
		if (StringUtil.blank(directoryWithFBCsPath)) {
			directoryWithFBCsPath = MesquiteFile.chooseDirectory("Choose directory containing FBC files:", previousDirectory); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		}
		if (StringUtil.blank(directoryWithFBCsPath))
			return false;

		loglnEchoToStringBuffer("Segregating files in folder: " + directoryToSegregatePath, logBuffer);
		loglnEchoToStringBuffer("Using FBC files in folder: " + directoryWithFBCsPath, logBuffer);


		File directory = new File(directoryToSegregatePath);
		File FBCDirectory = new File(directoryWithFBCsPath);
		importedDirectoryPath = directoryToSegregatePath + MesquiteFile.fileSeparator;
		importedDirectoryName = directory.getName();
		previousDirectory = directory.getParent();
		storePreferences();
		if (directory.exists() && directory.isDirectory() && FBCDirectory.exists() && FBCDirectory.isDirectory()) {
			return segregateFiles(directoryToSegregatePath, directory, directoryWithFBCsPath, FBCDirectory);
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
	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String content) {
		if ("requiresExtension".equalsIgnoreCase(tag))
			requiresExtension = MesquiteBoolean.fromTrueFalseString(content);
		else if ("previousDirectory".equalsIgnoreCase(tag))
			previousDirectory = StringUtil.cleanXMLEscapeCharacters(content);
		preferencesSet = true;
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "requiresExtension", requiresExtension);  
		StringUtil.appendXMLTag(buffer, 2, "previousDirectory", previousDirectory);  
		preferencesSet = true;
		return buffer.toString();
	}


	JLabel nameParserLabel = null;

	MesquiteTextCanvas nameParserTextCanvas = null;
	Button nameParserButton = null;


	/*.................................................................................................................*/
	private String getModuleText(MesquiteModule mod) {
		return mod.getName() + "\n" + mod.getParameters();
	}
	/*.................................................................................................................*
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(ExtensibleDialog.defaultCANCEL);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Rename File Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()

		TextCanvasWithButtons textCanvasWithButtons;

		//section for name parser

				dialog.addHorizontalLine(1);
				dialog.forceNewPanel();
				String s = getModuleText(nameParserManager);
				if (MesquiteTrunk.mesquiteTrunk.numModulesAvailable(ChromatogramFileNameParser.class)>1){
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s,"File Name Parser...", "nameParserReplace", "Options...", "nameParserButton",this);
					nameParserButton = textCanvasWithButtons.getButton2();
				}
				else {
					textCanvasWithButtons = dialog.addATextCanvasWithButtons(s, "Options...", "nameParserButton",this);
					nameParserButton = textCanvasWithButtons.getButton();
				}
				nameParserButton.setEnabled (nameParserManager.hasOptions());
				nameParserTextCanvas = textCanvasWithButtons.getTextCanvas();


				dialog.setDefaultButton("Segregate");

		Checkbox requiresExtensionBox = dialog.addCheckBox("only process files with standard extensions (ab1,abi,ab,CRO,scf)", requiresExtension);
		dialog.addHorizontalLine(2);


		s = "This will move all chromatograms whose sample codes are listed in a chosen file into their own folder.\n";
		s+="Mesquite extracts from within the name of each chromatogram file a code indicating the sample (e.g., a voucher number). ";
		s+= "It then looks at the first entry on each line of a tab-delimited text file, and sees if it can find in that sample codes file ";
		s+= "the sample code in the chromatogram's file name.  If so, it will move the file into a folder; if not, it will ignore the chromatogram file.";
		dialog.appendToHelpString(s);

		dialog.completeAndShowDialog(true);
		boolean success=(buttonPressed.getValue()== dialog.defaultOK);
		if (success)  {
			requiresExtension = requiresExtensionBox.getState();
		}
		storePreferences();  // do this here even if Cancel pressed as the File Locations subdialog box might have been used
		dialog.dispose();
		return success;
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Segregates chromatograms that are accounted for in a directory of FBC files.", null, commandName, "segregate")) {

			if (!hireRequired())
				return null;
			
			if (queryOptions()) 
				segregateFiles();
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
		return "Segregate Chromatograms that Match FBCs [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Segregates a files in a folder that correspond to files in FBCs in another directory.";
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





