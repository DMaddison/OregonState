package mesquite.database.EditBaseCallPerson;

import java.awt.Checkbox;

import mesquite.categ.lib.RequiresAnyDNAData;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;

/**
 * DataUtility to add a pair NameReference objects to sequences to indicate identity 
 * of person who made final base calls.  Primarily used to migrate information into 
 * database. 
 */
public class EditBaseCallPerson extends DataUtility {
	protected String finalBaseFirst;
	protected String finalBaseLast;
	public boolean writeOnlySelectedSeqs = false;
	public boolean purgeNames = false;

	/**
	 * @return true
	 */
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	/**
	 * Assigns final base call person identity to sequences in passed {@code data}.
	 * 
	 * @param CharacterData data
	 * 	The sequence data to add/edit information
	 * 
	 * @return {@code true} if information is changed, {@code false} if not.  Stores identity information in a pair of 
	 * {@link NameReference} objects: "finalBaseCallFirstRef" and "finalBaseCallLastRef". 
	 */
	public boolean operateOnData(CharacterData data) {
		if (data == null) {
			return false;
		}
		if (queryNames()) {
			Taxa taxa = data.getTaxa();
			boolean anyChanged = false;
			if (writeOnlySelectedSeqs && !taxa.anySelected()) {
				discreetAlert("You have chosen to only set details for selected sequences, but none are selected.");
				return false;
			} else {
				int numTaxa = data.getNumTaxa();
				for (int it = 0; it < numTaxa; it++) {
					if (!writeOnlySelectedSeqs || taxa.getSelected(it)) {
						NameReference firstRef = NameReference.getNameReference("finalBaseCallFirstRef");
						NameReference lastRef = NameReference.getNameReference("finalBaseCallLastRef");
						// The two strings (finalBaseFirst and finalBaseLast) will either have 
						// 1. Strings of non-zero length, as long as they were entered correctly and purgeNames is false
						// 2. Empty strings (""), set in queryNames() because purgeNames is true
						data.setAssociatedObject(firstRef, it, finalBaseFirst);
						data.setAssociatedObject(lastRef, it, finalBaseLast);
						anyChanged = true;
					}
				}
			}
			if (anyChanged) {
				data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));
				data.setDirty(true);
			}
			return true;
		} else {
			return false;
		}
	}
	/*.................................................................................................................*/
	/**
	 * Queries user for information to add/remove from sequences concerning the names of person responsible for 
	 * performing final base calls.
	 * 
	 * @return {@code true} if 'OK' button was pressed and valid parameter combinations entered in dialog; otherwise 
	 * 	returns {@code false}.
	 */
	private boolean queryNames() {
		boolean success = false;
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog namesDialog = new ExtensibleDialog(containerOfModule(), "Final base call person", buttonPressed);
		String helpString = "Enter the first and last names of the person responsible for making final base call(s) for ";
		helpString += "sequences.";
		helpString += "<br /><br />If <strong>Set only selected sequences</strong> is checked, information will only be added / removed ";
		helpString += "for those selected sequences.";
		helpString += "<br /><br /><strong>Purge final base call info</strong> will remove any existing information about ";
		helpString += "who made final base calls";
		namesDialog.appendToHelpString(helpString);

		namesDialog.addLabel("Final Base Calls Performed by:");
		SingleLineTextField finalBaseFirstField = namesDialog.addTextField("First name", "", 20, true);
		SingleLineTextField finalBaseLastField = namesDialog.addTextField("Last name", "", 20, true);

		namesDialog.addHorizontalLine(1);

		Checkbox writeSelectedBox = namesDialog.addCheckBox("Set only selected sequences", writeOnlySelectedSeqs);
		Checkbox purgeNamesBox = namesDialog.addCheckBox("Purge final base call info", purgeNames);
		
		namesDialog.completeAndShowDialog(true);
		//'OK' button pressed, so extract the values
		if(buttonPressed.getValue() == 0){
			finalBaseFirst = finalBaseFirstField.getText();
			finalBaseLast = finalBaseLastField.getText();
			writeOnlySelectedSeqs = writeSelectedBox.getState();
			purgeNames = purgeNamesBox.getState();

			if (purgeNames) {
				finalBaseFirst = "";
				finalBaseLast = "";
				success = true;
			} else { // if we are not purging, make sure names aren't empty
				if (finalBaseFirst.length() > 0 && finalBaseLast.length() > 0) {
					success = true;
				}
			}
		}
		
		return success;
	}
	/*.................................................................................................................*/
	/** if returns true, then requests to remain on even after operateData is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	/**To be sure the module deals with DNA data*/
	public CompatibilityTest getCompatibilityTest(){
		return new RequiresAnyDNAData();
	}
	/*.................................................................................................................*/
	/**@return	{@code true}, indicating the module is not part of a publically released package.*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return false;
	}
	/*.................................................................................................................*/
	/** A very short name for menus.
	 * @return	The name of the module*/
	public String getNameForMenuItem(){
		return "Add/Remove Information About Final Base Calls...";
	}
	/*.................................................................................................................*/
	/** @return	The name of the module*/
	public String getName() {
		return "Add or Remove Base Call Person";
	}
}
