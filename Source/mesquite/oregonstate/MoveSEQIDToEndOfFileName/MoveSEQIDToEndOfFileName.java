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

package mesquite.oregonstate.MoveSEQIDToEndOfFileName; 

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
public class MoveSEQIDToEndOfFileName extends UtilitiesAssistant{ 
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

		addMenuItem(null, "Move SEQID To End of File Name [DRM Lab]...", makeCommand("rename", this));
		return true;
	}
		/*.................................................................................................................*/
	public String getNewFileName(String originalFileName) {
		int posOfSEQID = originalFileName.indexOf("&iSEQID");
		int posEndOfSEQID = originalFileName.indexOf("_");
		int posFAS =  originalFileName.indexOf(".fas");
		int extensionStart = originalFileName.lastIndexOf(".");
		
		String seqID = originalFileName.substring(0, posEndOfSEQID);
		String middle = originalFileName.substring(posEndOfSEQID+1, extensionStart);
		String extension = originalFileName.substring(extensionStart);
		String newName = middle+"_"+seqID+extension;


		return newName;
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


		loglnEchoToStringBuffer(" Renaming files by moving SEQID to end. ", logBuffer);
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


					if (originalFileName.indexOf("&iSEQID")==0) {
						if (verbose)
							loglnEchoToStringBuffer(originalFileName, logBuffer);
						numPrepared++;
						try {
							String newFileName = getNewFileName(originalFileName);
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
		if (checker.compare(this.getClass(), "Renames files by moving SEQID.", null, commandName, "rename")) {
				renameFiles(null);
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Rename Files by Moving SEQID To End [DRM Lab]";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "Renames files.";
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





