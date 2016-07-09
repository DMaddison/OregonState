package mesquite.oregonstate.ExportSeparateSequenceFASTACustom;

import mesquite.chromaseq.ExportSeparateSequenceFASTA.*;
import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.lib.StringUtil;
import mesquite.lib.Taxa;
import mesquite.lib.characters.CharacterData;

public class ExportSeparateSequenceFASTACustom extends ExportSeparateSequenceFASTA {

	/*.................................................................................................................*/
	public String getFileName(Taxa taxa, int it, CharacterData data, int index, String voucherID) {
		String fileName = "&v" + StringUtil.cleanseStringOfFancyChars(voucherPrefix+voucherID,false,true);

		String s = ChromaseqUtil.getFragmentName(data, index);
		if (StringUtil.notEmpty(s)) 
			fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(s,false,true);
		else
			fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(data.getName(),false,true);

		
		fileName+="_&n"+StringUtil.cleanseStringOfFancyChars(taxa.getName(it),false,true);


		fileName += ".fas";

		return fileName;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Single-sequence FASTA Files [DRM Lab Custom]";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports each sequence as a separate FASTA file." ;
	}

}
