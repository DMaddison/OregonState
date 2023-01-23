/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.oregonstate.RemovePUBMetadata;/*~~  */import mesquite.lists.lib.*;

import java.io.IOException;import java.net.URI;import java.net.URISyntaxException;import java.util.*;import java.awt.*;import java.awt.event.*;

import mesquite.lib.*;import mesquite.lib.characters.CharacterData;
import mesquite.categ.lib.*;
import mesquite.lib.duties.*;import mesquite.lib.table.*;
import mesquite.oregonstate.lib.*;
/* ======================================================================== */public class RemovePUBMetadata extends TaxaListAssistantI  {	Taxa taxa;	MesquiteTable table;
	public String getName() {
		return "Remove PUB Metadata";
	}
	public String getNameForMenuItem() {
		return "Remove PUB Metadata";
	}
	public String getExplanation() {		return "Removes PUB Metadata associated with sequences.";	}	/*.................................................................................................................*/	public int getVersionOfFirstRelease(){		return -NEXTRELEASE;  	}	/*.................................................................................................................*/	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		addMenuItem("Remove PUB Metadata", new MesquiteCommand("removePUBMetadata", this));		return true;	}

	/*.................................................................................................................*/	/** A request for the MesquiteModule to perform a command.  It is passed two strings, the name of the command and the arguments.	This should be overridden by any module that wants to respond to a command.*/	public Object doCommand(String commandName, String arguments, CommandChecker checker) { 		if (checker.compare(MesquiteModule.class, null, null, commandName, "removePUBMetadata")) {
			if (taxa == null)
				return null;
			int numMatrices = getProject().getNumberCharMatrices(taxa);
			if (numMatrices<1)
				return null;
			Vector datas = new Vector();
			for (int i = 0; i<numMatrices; i++){
				CharacterData data = getProject().getCharacterMatrix(taxa, i);
				if (data.isUserVisible())
					datas.addElement(data);
			}			if (getEmployer() instanceof ListModule){
				ListModule listModule = (ListModule)getEmployer();
				Puppeteer puppeteer = new Puppeteer(this);
				CommandRecord prevR = MesquiteThread.getCurrentCommandRecord();
				CommandRecord cRecord = new CommandRecord(true);
				MesquiteThread.setCurrentCommandRecord(cRecord);
				//at this point the vector should include only the ones not being shown.
				boolean anySelected = table.anyCellSelectedAnyWay();
				for (int i = 0; i<datas.size(); i++) {
					if (datas.elementAt(i) instanceof MolecularData) {
						MolecularData sequenceData =  (MolecularData)datas.elementAt(i);
						for (int it=0; it<taxa.getNumTaxa(); it++) {
							if (!anySelected || table.isRowSelected(it)) {
								String publicationCode = (String)taxa.getAssociatedObject(CharacterData.publicationCodeNameRef, it);
								sequenceData.setPublicationCode(it, "");
							}
						}
					}

				}

				MesquiteThread.setCurrentCommandRecord(prevR);
			}		}		else			return  super.doCommand(commandName, arguments, checker);		return null;	}	/*.................................................................................................................*/	public boolean isSubstantive(){		return false;	}	/*.................................................................................................................*/	public void setTableAndTaxa(MesquiteTable table, Taxa taxa){		this.table = table;		this.taxa = taxa;	}}