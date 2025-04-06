package mesquite.oregonstate.InterpretFastaDNAByOTUIDMaddisonLab;

import mesquite.categ.lib.MolecularData;
import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.io.InterpretFastaDNA.InterpretFastaDNA;
import mesquite.lib.Associable;
import mesquite.lib.Parser;
import mesquite.lib.StringUtil;
import mesquite.lib.ui.*;
import mesquite.lib.taxa.*;
import mesquite.lib.misc.*;
import mesquite.lib.characters.CharacterData;

public class InterpretFastaDNAByOTUIDMaddisonLab extends InterpretFastaDNA {

	/*.................................................................................................................*
	public void setTaxonNameToIncoming(Taxon t, String incomingName) {  
		if (StringUtil.blank(t.getName()))
				t.setName(incomingName);
	}
	/*.................................................................................................................*/
	public void processFileName(String fileName, CharacterData data, Taxa taxa, int taxonNumber) {
		// e.g., &vDRMDNA0772_&g28S_&pPUB006_&aAF438104_&nOodes amaroides.fas
		
		if (StringUtil.notEmpty(fileName)  && data!=null) {
			Parser parser = new Parser(fileName);
			parser.setPunctuationString("_");
			String token = parser.getFirstToken();
			while (StringUtil.notEmpty(token)) {
				if (token.startsWith("&v")) {  // voucher token
					String code = token.substring(2);
					if (code.startsWith("DRMDNA")) {
						code = "DRMDNA" + code.substring(6);
					}
					String current = (String)taxa.getAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, taxonNumber);
					if (StringUtil.blank(current))
						taxa.setAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, taxonNumber, code);
					else {
						if (!StringUtil.containsIgnoreCase(current, code))  // if not there, add it
							taxa.setAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, taxonNumber, current+"/"+code);
					}
				} else if (token.startsWith("&a")) {  // accession number token
					String accession = token.substring(2);
					Taxon taxon = data.getTaxa().getTaxon(taxonNumber);
					Associable tInfo = data.getTaxaInfo(true);
					if (tInfo != null && taxon != null) {
						tInfo.setAssociatedObject(MolecularData.genBankNumberRef, taxonNumber, accession);
					}
				} else if (token.startsWith("&p")) {  // accession number token
					String pubCode = token.substring(2);
					if (!pubCode.equalsIgnoreCase("PUB000") && !pubCode.equalsIgnoreCase("PUB00"))
						data.setPublicationCode(taxonNumber, pubCode);
				} 
				token = parser.getNextToken();
			}
		}
		
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
		return "FASTA - Taxon Names Match Taxon ID Code [DRM Lab Custom]";
	}

}
