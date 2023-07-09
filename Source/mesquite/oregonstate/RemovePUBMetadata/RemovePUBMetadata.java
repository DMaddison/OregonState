/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


import java.io.IOException;

import mesquite.lib.*;
import mesquite.categ.lib.*;
import mesquite.lib.duties.*;
import mesquite.oregonstate.lib.*;


		return "Remove PUB Metadata";
	}
	public String getNameForMenuItem() {
		return "Remove PUB Metadata";
	}
	public String getExplanation() {
		addMenuItem("Remove PUB Metadata", new MesquiteCommand("removePUBMetadata", this));

	/*.................................................................................................................*/
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
			}
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
			}