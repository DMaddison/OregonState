package mesquite.oregonstate.RecordChromatogramsMissingFromList;


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
public class RecordChromatogramsMissingFromList extends UtilitiesAssistant implements ActionListener{ 
	//for importing sequences
	MesquiteProject proj = null;
	FileCoordinator coord = null;
	MesquiteFile file = null;
	ChromatogramFileNameParser nameParserManager;

	boolean requiresExtension=true;

	static String previousDirectory = null;
	ProgressIndicator progIndicator = null;
	int sequenceCount = 0;
	String importedDirectoryPath, importedDirectoryName;
	StringBuffer logBuffer;

	String sampleCodeListPath = null;
	String sampleCodeListFile = null;
	String sampleCodeList = "";
	Parser sampleCodeListParser = null;
	String[] sampleCodes = null;
	boolean[] haveSampleCode = null;
	

	boolean preferencesSet = false;
	boolean verbose=true;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e1 = registerEmployeeNeed(ChromatogramFileNameParser.class, "Chromatogram processing requires a means to determine the sample code.", "This is activated automatically.");
	}

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();

		addMenuItem(null, "Report Missing Chromatograms [DRM Lab]...", makeCommand("examineMissing", this));
		return true;
	}
	/*.................................................................................................................*/
	public boolean hireRequired(){

		if (nameParserManager == null)
			nameParserManager= (ChromatogramFileNameParser)MesquiteTrunk.mesquiteTrunk.hireEmployee(ChromatogramFileNameParser.class, "Supplier of sample code from the chromatogram file name.");
		if (nameParserManager == null) {
			return false;
		} else if (!nameParserManager.queryOptions())
			return false;

		return true;
	}
	int numSampleCodes = 0;
	/*.................................................................................................................*/
	public boolean processCodesFile() {
		if (!StringUtil.blank(sampleCodeListPath)) {
			sampleCodeList = MesquiteFile.getFileContentsAsString(sampleCodeListPath);

			if (!StringUtil.blank(sampleCodeList)) {
				sampleCodeListParser = new Parser(sampleCodeList);
				
				numSampleCodes = 0;
				sampleCodeListParser.setPosition(0);
				Parser subParser = new Parser();
				String line = sampleCodeListParser.getRawNextDarkLine();
				while (StringUtil.notEmpty(line)) {
					subParser.setString(line);
					subParser.setWhitespaceString("\t");
					subParser.setPunctuationString("");
					String code = subParser.getFirstToken();
					if (StringUtil.notEmpty(code))
						numSampleCodes++;
					line = sampleCodeListParser.getRawNextDarkLine();
				}
				
				if (numSampleCodes>0) {
					sampleCodes = new String[numSampleCodes];
					haveSampleCode = new boolean[numSampleCodes];
					sampleCodeListParser.setPosition(0);
					line = sampleCodeListParser.getRawNextDarkLine();
					int count = 0;
					while (StringUtil.notEmpty(line)) {
						subParser.setString(line);
						subParser.setWhitespaceString("\t");
						subParser.setPunctuationString("");
						String code = subParser.getFirstToken();
						if (StringUtil.notEmpty(code)) {
							sampleCodes[count]=code;
							haveSampleCode[count]=false;
							count++;
						}
						line = sampleCodeListParser.getRawNextDarkLine();
					}
					return true;
				}
			}
		}	
		return false;

	}
	/*.................................................................................................................*/

	public boolean sampleCodeIsInCodesFile(MesquiteString sampleCode) {
		if (sampleCode==null || sampleCode.isBlank()||sampleCodes==null)
			return false;
		String sampleCodeString  = sampleCode.getValue();
	
		for (int i=0; i<sampleCodes.length; i++) {
			if (sampleCodeString.equalsIgnoreCase(sampleCodes[i])) {
				haveSampleCode[i] = true;
				return true;
			}
		}
		return false;
	}

	/*.................................................................................................................*/
	public boolean examineChromatograms(String directoryPath, File directory){

		logBuffer.setLength(0);
		String[] files = directory.list();
		progIndicator = new ProgressIndicator(getProject(),"Segregating Chromatograms", files.length);
		progIndicator.setStopButtonName("Stop");
		progIndicator.start();
		boolean abort = false;
		String cPath;
		String seqName;
		String fullSeqName;
		String fragName = "";
		sequenceCount = 0;


		int loc = 0;



		loglnEchoToStringBuffer(" Searching for chromatograms that match the specified criteria. ", logBuffer);
		loglnEchoToStringBuffer(" Processing directory: ", logBuffer);
		loglnEchoToStringBuffer("  "+directoryPath+"\n", logBuffer);

		int numPrepared = 0;


		for (int i=0; i<files.length; i++) {
			if (progIndicator!=null){
				progIndicator.setCurrentValue(i);
				progIndicator.setText("Number of files segregated: " + numPrepared);
				if (progIndicator.isAborted())
					abort = true;
			}
			if (abort)
				break;
			fragName = "";
			if (files[i]==null )
				;
			else {
				cPath = directoryPath + MesquiteFile.fileSeparator + files[i];
				File cFile = new File(cPath);
				if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith(".")) && (!requiresExtension || (files[i].endsWith("ab1") ||  files[i].endsWith(".abi")  || files[i].endsWith(".ab")  ||  files[i].endsWith(".CRO") || files[i].endsWith(".scf")))) {

					String chromFileName = cFile.getName();
					if (StringUtil.blank(chromFileName)) {
						loglnEchoToStringBuffer("Bad file name; it is blank.", logBuffer);
						// remove "running"
						if (progIndicator!=null) progIndicator.goAway();
						return false;
					}

					MesquiteString sampleCodeSuffix = new MesquiteString();
					MesquiteString sampleCode = new MesquiteString();
					MesquiteString primerName = new MesquiteString();
					MesquiteString startTokenResult = new MesquiteString();
					//here's where the names parser processes the name

					if (nameParserManager!=null) {
						if (!nameParserManager.parseFileName(chromFileName, sampleCode, sampleCodeSuffix, primerName, logBuffer, startTokenResult, null))
							continue;
					}
					else {
						loglnEchoToStringBuffer("Naming parsing rule is absent.", logBuffer);
						return false;
					}
					if (startTokenResult.getValue() == null)
						startTokenResult.setValue("");

					boolean match = sampleCodeIsInCodesFile(sampleCode);



					if (match) {numPrepared++;} 


				}
			}
		}

		loglnEchoToStringBuffer("Number of files examined: " + files.length, logBuffer);
		loglnEchoToStringBuffer("Number of files matched in list: " + numPrepared, logBuffer);

		loglnEchoToStringBuffer("Missing sample codes:", logBuffer);
		for (int i=0; i<sampleCodes.length; i++) {
			if (!haveSampleCode[i]) {
				loglnEchoToStringBuffer(""+sampleCodes[i], logBuffer);
			}
		}


		if (!abort) {

			progIndicator.spin();
		}


		if (progIndicator!=null)
			progIndicator.goAway();

		return true;

	}
	/*.................................................................................................................*/
	public boolean examineChromatograms(String directoryPath){
		if ( logBuffer==null)
			logBuffer = new StringBuffer();

		loglnEchoToStringBuffer("Segregating chromatograms with codes present in text file: " + sampleCodeListFile, logBuffer);


		MesquiteBoolean pleaseStorePrefs = new MesquiteBoolean(false);

		if (pleaseStorePrefs.getValue())
			storePreferences();

		// if not passed-in, then ask
		if (StringUtil.blank(directoryPath)) {
			directoryPath = MesquiteFile.chooseDirectory("Choose directory containing chromatograms:", previousDirectory); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		}

		if (StringUtil.blank(directoryPath))
			return false;


		File directory = new File(directoryPath);
		importedDirectoryPath = directoryPath + MesquiteFile.fileSeparator;
		importedDirectoryName = directory.getName();
		previousDirectory = directory.getParent();
		storePreferences();
		if (directory.exists() && directory.isDirectory()) {
			return examineChromatograms(directoryPath, directory);
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
	public void processSingleXMLPreference (String tag, String content) {
		if ("requiresExtension".equalsIgnoreCase(tag))
			requiresExtension = MesquiteBoolean.fromTrueFalseString(content);
		else if ("previousDirectory".equalsIgnoreCase(tag))
			previousDirectory = StringUtil.cleanXMLEscapeCharacters(content);
		preferencesSet = true;
	}
	/*.................................................................................................................*/
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
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(ExtensibleDialog.defaultCANCEL);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Examine Chromatograms Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()

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


		dialog.setDefaultButton("Search");

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
		if (checker.compare(this.getClass(), "Examines a list of sample codes in a specified file, and records all sample codes not represented in a directory of chromatograms.", null, commandName, "examineMissing")) {

			if (!hireRequired())
				return null;

			if (queryOptions()) {
				MesquiteString dnaNumberListDir = new MesquiteString();
				MesquiteString dnaNumberListFile = new MesquiteString();
				String s = MesquiteFile.openFileDialog("Choose file containing sample codes", dnaNumberListDir, dnaNumberListFile);
				if (!StringUtil.blank(s)) {
					sampleCodeListPath = s;
					sampleCodeListFile = dnaNumberListFile.getValue();
					processCodesFile();
					examineChromatograms(null);

				}
			}
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
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
		return "Record Chromatograms Missing from List";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Examines a list of sample codes in a specified file, and records all sample codes not represented in a directory of chromatograms.";
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
		return -1110;  
	}

}





