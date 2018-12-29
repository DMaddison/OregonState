package mesquite.oregonstate.ExportForGenBankRevision;

import mesquite.categ.lib.MolecularData;
import mesquite.io.InterpretFastaDNA.InterpretFastaDNA;
import mesquite.lib.Associable;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.lib.characters.CharacterData;

public class ExportForGenBankRevision extends InterpretFastaDNA {
	
	
	protected String getTaxonName(Taxa taxa, int it, CharacterData data){
		if (data==null || taxa==null)
			return "";
		Taxon taxon = data.getTaxa().getTaxon(it);
		Associable tInfo = data.getTaxaInfo(false);
		if (tInfo != null && taxon != null) {
			return (String)tInfo.getAssociatedObject(MolecularData.genBankNumberRef, it);
		}
		return taxon.getName();
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Export with GenBank Accession as Taxon Name";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports a FASTA file with GenBank accession as taxon name." ;
	}

}
