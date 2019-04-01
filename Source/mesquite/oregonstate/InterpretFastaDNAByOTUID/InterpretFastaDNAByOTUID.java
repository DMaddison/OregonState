package mesquite.oregonstate.InterpretFastaDNAByOTUID;

import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.io.InterpretFastaDNA.InterpretFastaDNA;
import mesquite.lib.Parser;
import mesquite.lib.StringUtil;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.lib.VoucherInfoFromOTUIDDB;

public class InterpretFastaDNAByOTUID extends InterpretFastaDNA {

	/*.................................................................................................................*/
	public void setTaxonNameToIncoming(Taxon t, String incomingName) {  
		if (StringUtil.blank(t.getName()))
				t.setName(incomingName);
	}

	/*.................................................................................................................*/
	public boolean codesMatch(String OTUIDCode, String taxonName) {
		if (StringUtil.blank(OTUIDCode))
			return false;
		if (!OTUIDCode.contains("/"))  // doesn't contain multiple entries
			return OTUIDCode.equalsIgnoreCase(taxonName);
		Parser parser = new Parser(OTUIDCode);
		parser.setPunctuationString("/");
		String code = parser.getFirstToken();
		code = StringUtil.stripBoundingWhitespace(code);
		while (StringUtil.notEmpty(code)) {
			if (code.equalsIgnoreCase(taxonName))
				return true;
			code = parser.getNextToken();
			code = StringUtil.stripBoundingWhitespace(code);
		}
		return false;

	}

	/*.................................................................................................................*/
	public int getTaxonNumber(Taxa taxa, String token) {
		if (StringUtil.blank(token))
			return -1;
		String code = "";
		for (int it=0; it<taxa.getNumTaxa(); it++) {
			code = ChromaseqUtil.getStringAssociated(taxa, VoucherInfoFromOTUIDDB.voucherCodeRef, it);
			if (codesMatch(code, token))
						return it;
		}
		return -1;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "FASTA - Taxon Names Match OTU ID Code [DRM Lab Custom]";
	}

}
