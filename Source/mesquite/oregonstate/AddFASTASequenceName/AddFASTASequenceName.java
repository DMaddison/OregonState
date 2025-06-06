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

package mesquite.oregonstate.AddFASTASequenceName; 

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

import mesquite.lib.*;
import mesquite.lib.ui.*;
import mesquite.lib.duties.*;
import mesquite.chromaseq.lib.*;

/* ======================================================================== */
public class AddFASTASequenceName extends UtilitiesAssistant{ 
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

		addMenuItem(null, "Add FASTA sequence name as Voucher Code [DRM Lab]...", makeCommand("rename", this));
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
	public String getFASTASequenceName(String fileName) {
		Parser parser = new Parser(fileName);
		parser.setPunctuationString("_.");
		String token = parser.getFirstToken();
		while (StringUtil.notEmpty(token)) {
			if (token.startsWith("&v")) {
				return token.substring(5);
			}
			token = parser.getNextToken();
		}

		return "";
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

					String sequenceName=getFASTASequenceName(originalFileName);
					if (StringUtil.notEmpty(sequenceName)) {

						String contents = MesquiteFile.getFileContentsAsString(cPath);
						StringBuffer newContents = new StringBuffer();
						newContents.append(">"+ sequenceName+"\n");
						Parser parser = new Parser(contents);
						String line = parser.getRawNextDarkLine();  //skip over current > line
						line = parser.getRawNextDarkLine();
						while (StringUtil.notEmpty(line)) {
							newContents.append(line+"\n");
							line = parser.getRawNextDarkLine();
						}
						numPrepared++;
						MesquiteFile.putFileContents(cPath, newContents.toString(), true);

					} else
						logln("\n****Sequence name empty: " + originalFileName);
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
		if (checker.compare(this.getClass(), "Renames files using information in a text file.", null, commandName, "rename")) {

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
		return "Add FASTA Sequence Name [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Adds sequence name to FASTA files.";
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





