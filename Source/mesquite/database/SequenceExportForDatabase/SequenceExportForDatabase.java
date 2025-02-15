package mesquite.database.SequenceExportForDatabase;

import org.dom4j.*;

import mesquite.categ.lib.*;
import mesquite.database.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.lib.ui.*;
import mesquite.lib.taxa.*;
import mesquite.lib.misc.*;

/**A class to export xml-formatted sequences for upload to local database*/
public class SequenceExportForDatabase extends FileInterpreterI {
	Class[] acceptedClasses;
	public boolean startJob(String arguments, Object condition,	boolean hiredByName) {
		acceptedClasses = new Class[] {DNAState.class};
		return true;
	}
	/*.................................................................................................................*/
	public void readFile(MesquiteProject mf, MesquiteFile mNF, String arguments) {
		//NOT designed to read in xml-files...but readFile method is required of FileInterpreter subclasses
	}
	/*.................................................................................................................*/
	/**Checks to see if the current project has DNA matrices and reports whether it can export any matrices in the project.
	 * @param	project	The current {@code MesquiteProject}
	 * @return	{@code true} if {@code project} has at least one matrix of type {@code DNAState.class}; {@code false} if
	 * there are no DNA matrices in the current project.*/
	public boolean canExportProject(MesquiteProject project) {  
		return project.getNumberCharMatrices(acceptedClasses) > 0;
	}
	/*.................................................................................................................*/
	/**@return	{@code true}*/
	public boolean canExportEver() {  
		return true;
	}
	/*.................................................................................................................*/
	/**Determines if this module can export data of the Class being sent in {@code dataClass}.<p>
	 * @param	dataClass	A Class of data; e.g. {@code DNAState.class, ProteinState.class}
	 * @return	{@code true} if {@code dataClass == DNAState.class}; {@code false} if the passed
	 * {@code dataClass != DNAState.class}*/
	public boolean canExportData(Class dataClass) {  
		for (int i = 0; i<acceptedClasses.length; i++){
			if (dataClass==acceptedClasses[i]){
				return true;
			}
		}
		return false; 
	}
	/*.................................................................................................................*/
	/**Calls {@link XMLForCastor#getXMLDocument(CharacterData, Object) XMLCastor.getXMLDocument} 
	 * to format sequence.
	 * @param	file	the {@code MesquiteFile} to which the data belong
	 * @param	data	the data to format; must be {@link DNAState} data
	 * @return	a {@code StringBuffer} of XML-formatted sequence; if problems are encountered, returns {@code null}*/
	protected MesquiteStringBuffer getDataAsEntity(MesquiteFile file, CharacterData data){
		if(data == null || file == null){
			return null;
		} else {
			//A class for formatting the sequence data in XML format
			XMLForCastor formatter = new XMLForCastor();
			//Call XMLCastor.getXMLDocument to get the sequences in XML 
			Document sequencesAsXML = formatter.getXMLDocument(data, containerOfModule());
			//As long as Document is not null, send it as a string back to exportFile
			if(sequencesAsXML != null){
				String docAsString = XMLUtil.getDocumentAsXMLString(sequencesAsXML, false);
				MesquiteStringBuffer toReturn = new MesquiteStringBuffer(docAsString);
				return toReturn;
			} else return null;
		}
	}
	/*.................................................................................................................*/
	/**Performs necessary calls for formatting ({@link #getDataAsEntity(MesquiteFile, CharacterData) getDataAsEntity}) and
	 * calls {@link #saveExportedFileWithExtension(StringBuffer, String, String) saveExportedFileWithExtension} to write 
	 * the file.
	 * @param	file	the file containing data to export
	 * @param	arguments	a {@code String} of arguments; unused
	 * @return	{@code true} if data were found and formatting was successful; {@code false} if not.*/
	public boolean exportFile(MesquiteFile file, String arguments) {
		CharacterData data = findDataToExport(file, arguments);
		if (data == null) {
			showLogWindow(true);
			logln("WARNING: No suitable data available for export to a file of format \"" + getName() + "\".  The file will not be written.\n");
			return false;
		}

		MesquiteStringBuffer outputBuffer = getDataAsEntity(file, data);

		if (outputBuffer != null) {
			saveExportedFileWithExtension(outputBuffer, arguments, "xml");
			return true;
		} else {
			logln("WARNING: Export unsuccessful.  See log for details");
		}
		return false;
	}
	/*.................................................................................................................*/
	/**Selects the data (matrix) to export.  If more than one matrix is available in the project, prompts user to choose one.
	 * @param	file	the current {@code MesquiteFile}
	 * @param	arguments	String (currently unused)
	 * @return	a {@code CharacterData} object that will be exported*/
	public CharacterData findDataToExport(MesquiteFile file, String arguments) { 
		return getProject().chooseData(containerOfModule(), file, null, DNAState.class, "Select data to export");
	}
	/*.................................................................................................................*/
	/** A very short name for menus.
	 * @return	The name of the module*/
	public String getName() {
		return "XML for uploading to CASTOR database";
	}
	/*.................................................................................................................*/
	/** Returns an explanation of what the module does for (?) button in dialogs.
	 * @return	A short String explaining the module*/
	public String getExplanation(){
		return "Exports a matrix in xml-format.  For uploading sequences to local database";
	}
	/*.................................................................................................................*/
	/**@return	whether or not this module is pre-release*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	/**@return	whether or not this module is substative*/
	public boolean isSubstantive(){
		return false;
	}
}
