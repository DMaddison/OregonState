/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


import java.io.IOException;

import mesquite.lib.*;
import mesquite.categ.lib.*;
import mesquite.lib.duties.*;
import mesquite.oregonstate.lib.*;

	CodesGenBankFileProcessor codeFileWithGenBankNumbers;

		return "Get GenBank Numbers from File";
	}
	public String getNameForMenuItem() {
		return "Get GenBank Numbers from File...";
	}
	public String getExplanation() {
		addMenuItem("Get GenBank Numbers from File", new MesquiteCommand("getGenBankNumbers", this));
	/*.................................................................................................................*/
	public String getGeneName(CharacterData data) {
		String geneName = data.getName();
		if ("CAD1".equalsIgnoreCase(data.getName())) {
			geneName = "CAD";
		} else if ("CAD2".equalsIgnoreCase(data.getName())) {
			geneName = "CAD";
		} else if ("CAD3".equalsIgnoreCase(data.getName())) {
			geneName = "CAD";
		} else if ("CAD4".equalsIgnoreCase(data.getName())) {
			geneName = "CAD";
		} else if ("COIBC".equalsIgnoreCase(data.getName())) {
			geneName = "COI";
		} else if ("COIPJ".equalsIgnoreCase(data.getName())) {
			geneName = "COI";
		} else if ("COIALL".equalsIgnoreCase(data.getName())) {
			geneName = "COI";
		}
		return geneName;
	}
	/*.................................................................................................................*/
	public String getFragmentName(CharacterData data) {
		if ("CAD1".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("CAD2".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("CAD3".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("CAD4".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("COIBC".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("COIPJ".equalsIgnoreCase(data.getName())) {
			return data.getName();
		} else if ("COIALL".equalsIgnoreCase(data.getName())) {
			return data.getName();
		}
		return "";
	}

	/*.................................................................................................................*/
			if (taxa == null)
				return null;
			codeFileWithGenBankNumbers = new CodesGenBankFileProcessor();
			if (!codeFileWithGenBankNumbers.chooseCodeFile())
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
				/*	Vector v = listModule.getAssistants();
				for (int k = 0; k< v.size(); k++){
					ListAssistant a = (ListAssistant)v.elementAt(k);
					if (a instanceof mesquite.molec.TaxaListHasData.TaxaListHasData){
						mesquite.molec.TaxaListHasData.TaxaListHasData tLHD = (mesquite.molec.TaxaListHasData.TaxaListHasData)a;
						CharacterData data = tLHD.getCharacterData();
						if (datas.indexOf(data)>=0)
							datas.removeElement(data);
					}
				}
				 */
				Puppeteer puppeteer = new Puppeteer(this);
				CommandRecord prevR = MesquiteThread.getCurrentCommandRecord();
				CommandRecord cRecord = new CommandRecord(true);
				MesquiteThread.setCurrentCommandRecord(cRecord);
				//at this point the vector should include only the ones not being shown.
				for (int i = 0; i<datas.size(); i++) {
					if (datas.elementAt(i) instanceof MolecularData) {
						MolecularData sequenceData =  (MolecularData)datas.elementAt(i);
						Debugg.println("data: " + sequenceData.getName());
						for (int it=0; it<taxa.getNumTaxa(); it++) {
							String voucherCode = (String)taxa.getAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, it);

							String line = codeFileWithGenBankNumbers.codeIsInCodeListFile(voucherCode, getGeneName(sequenceData), getFragmentName(sequenceData));
							if (StringUtil.notEmpty(line)) {
								String genBankNumber = codeFileWithGenBankNumbers.getGenBankNumberFromCodeFileLine(line);
								if (StringUtil.notEmpty(genBankNumber)) {
									sequenceData.setGenBankNumber(it, genBankNumber);
								}
							}
						}
					}

				}

				MesquiteThread.setCurrentCommandRecord(prevR);
			}