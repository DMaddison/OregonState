package mesquite.oregonstate.ExportSeparateSequenceFASTACustom;

import java.awt.Checkbox;

import mesquite.chromaseq.ExportSeparateSequenceFASTA.*;
import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.lib.ExporterDialog;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.RadioButtons;
import mesquite.lib.SingleLineTextField;
import mesquite.lib.StringUtil;
import mesquite.lib.Taxa;
import mesquite.lib.characters.CharacterData;

public class ExportSeparateSequenceFASTACustom extends ExportSeparateSequenceFASTA {
	String publication = "PUB";
	String COIFragmentName = "COIBC";
	String CADFragmentName = "CAD4";

	public boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExporterDialog exportDialog = new ExporterDialog(this,containerOfModule(), "Single-sequence FASTA export [DRM Lab]", buttonPressed);
		exportDialog.setSuppressLineEndQuery(true);
		exportDialog.setDefaultButton(null);
		exportDialog.addLabel("Saving each sequence in a separate FASTA file [DRM Lab]");

		SingleLineTextField publicationField= exportDialog.addTextField("publication", publication, 8);
		SingleLineTextField COIField= exportDialog.addTextField("COI Fragment", COIFragmentName, 8);
		SingleLineTextField CADField= exportDialog.addTextField("CAD Fragment", CADFragmentName, 8);

		exportDialog.completeAndShowDialog(dataSelected, taxaSelected);

		boolean ok = (exportDialog.query(dataSelected, taxaSelected)==0);

		//		convertAmbiguities = convertToMissing.getState();
		if (ok) {
			publication = publicationField.getText();
			COIFragmentName = COIField.getText();
			CADFragmentName = CADField.getText();
		}

		voucherPrefix="DRMDNA";
		buildFileName=true;
		exportDialog.dispose();
		return ok;
	}	
	/*.................................................................................................................*/
	public String getFileName(Taxa taxa, int it, CharacterData data, int index, String voucherID) {
		String fileName = "&v" + StringUtil.cleanseStringOfFancyChars(voucherPrefix+voucherID,false,true);

		String s = ChromaseqUtil.getFragmentName(data, index);
		if (StringUtil.notEmpty(s)) 
			fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(s,false,true);
		else {
			if ("CAD1".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD2";
			} else if ("CAD2".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD2";
			} else if ("CAD3".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD4";
			} else if ("CAD4".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD4";
			} else {
				fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(data.getName(),false,true);
				if ("COI".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(COIFragmentName))
					fileName += "_&f"+StringUtil.cleanseStringOfFancyChars(COIFragmentName,false,true);
				else if ("CAD".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(CADFragmentName))
					fileName += "_&f"+StringUtil.cleanseStringOfFancyChars(CADFragmentName,false,true);
			}

		}


		if (StringUtil.notEmpty(publication)) 
			fileName+="_&p"+publication;
		fileName+="_&n"+StringUtil.cleanseStringOfFancyChars(taxa.getName(it),false,true);


		fileName += ".fas";

		return fileName;
	}
	/*.................................................................................................................*/
	public String getSequenceName(Taxa taxa, int it, String voucherID) {
		return "DNA"+voucherID;
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
