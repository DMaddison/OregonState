package mesquite.database.UploadSequencesToDatabase;

import java.awt.*;
import java.io.*;
import java.util.*;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;

import mesquite.categ.lib.*;
import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.database.lib.AuthenticationElement;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.lib.ui.*;
import mesquite.lib.taxa.*;
import mesquite.lib.misc.*;

import mesquite.database.lib.*;

public class UploadSequencesToDatabaseOLD extends DataUtility{
	final static String OUTERTAG = "sequenceset";
	final static String SEQTAG = "sequence";
	final static String CHROMTAG = "chromfile";
	final static String SEQSTRINGTAG = "sequencestring";
	final static String NAMETAG = "name";
	final static String STATUSTAG = "status";
	final static String STATUS_ROUGH = "rough";
	final static String STATUS_INTERMEDIATE = "intermediate";
	final static String STATUS_FINAL = "final";
	final static String STATUS_BAD = "bad sequence";
	final static String GENBANKTAG = "genbank";
	final static String CONTAMINATEDTAG = "contaminated";
	final static String CONTAMNOTESTAG = "contamination_notes";
	public boolean writeOnlySelectedTaxa = false;
	public boolean writeOnlySelectedData = false;
	protected boolean includeGaps = false;
	protected boolean contaminated = false;
	protected String contaminationNotes;
	protected String status = STATUS_ROUGH;
//	protected String username;
//	private String password;
	protected String sampleCodePrefix = "DNA";
	protected String primerStartToken = "_";

	public boolean startJob(String arguments, Object condition,	boolean hiredByName) {
		return true;
	}

	/*.................................................................................................................*/
	public boolean operateOnData(CharacterData data) {
		if(data == null){
			return false;
		}
		if(data.getClass() != DNAData.class){
			alert(getName() + " can only upload DNA data.");
			return false;
		}
		Taxa taxa = data.getTaxa();
		if(taxa == null){
			return false;
		}

		if(getUploadOptions()){

			if(writeOnlySelectedTaxa && !taxa.anySelected()){
				alert("You have chosen to only upload selected taxa, but none are selected.  Upload will not proceed.");
				return false;
			} else {
				int numTaxa = taxa.getNumTaxa();
				int numChars = data.getNumChars();

				String username = "";
				String password = "";

				boolean authChecked = false;
				while(!authChecked){
					AuthenticationElement creds = (AuthenticationElement)getProject().getFileElement(AuthenticationElement.class,0);
					if(creds != null){//already AuthenticationElement established, just get info from it
						username = creds.getUserName();
						password = creds.getPassword();
						authChecked = true;
					} else {//no AuthenticationElement with this project; try to create one via getCredentials()
						boolean authenticated = authenticationEstablished();
						if(!authenticated){
							alert("Sorry, uploads will require authentication credentials.");
							return false;
						}
					}
				}

				if(username.length() == 0 || password.length() == 0){
					alert("Upload requires non-empty values for username and password");
					return false;
				}
				Document sequencesAsXML = DocumentHelper.createDocument();
/*
				Here's where we can grab the root element and add username & password to it.
				Element rootElement = sequencesAsXML.getRootElement();
				rootElement.add(attribute)
*/						
				Element sequenceSetElement = sequencesAsXML.addElement(OUTERTAG);
				sequenceSetElement.addAttribute("matrix", data.getName());
				sequenceSetElement.addAttribute("username", username);
				sequenceSetElement.addAttribute("password", password);
				sequenceSetElement.addAttribute("sample_code_prefix", sampleCodePrefix);
				sequenceSetElement.addAttribute("delimiter", primerStartToken);
				
				password = "";//what does this accomplish?

				for(int it = 0; it < numTaxa; it++){
					if((!writeOnlySelectedTaxa || (taxa.getSelected(it))) && taxonHasData(data, it)){
						Element sequenceElement = sequenceSetElement.addElement(SEQTAG);
						int buildInt = ChromaseqUtil.getChromaseqRegistrationBuildOfMatrix((DNAData)data);
						if(buildInt > 0){
							sequenceElement.addAttribute("software", "chromaseq");
							String build = String.valueOf(buildInt);
							sequenceElement.addAttribute("build", build);

						}
						//Some handy utilities in XMLUtil that cut down on code:
						//Instead of:
						//	Element nameElement = sequenceElement.addElement(NAMETAG);
						//	nameElement.addText(getTaxonName(taxa,it));
						//We can just write:
						XMLUtil.addFilledElement(sequenceElement, NAMETAG, getTaxonName(taxa, it));

						//Do the same for status tag; although in this implementation, a single value is assigned to all sequences in the sequence set,
						//this should remain a property of each individual sequence, NOT the sequence set as a whole
						XMLUtil.addFilledElement(sequenceElement, STATUSTAG, status);

						//Do the same for contaminated tag; although in this implementation, a single value is assigned to all sequences in the sequence set,
						//this should remain a property of each individual sequence, NOT the sequence set as a whole
						String contaminatedString;
						contaminatedString = contaminated ? "1" : "0"; 
						XMLUtil.addFilledElement(sequenceElement, CONTAMINATEDTAG, contaminatedString);
						
						if(contaminationNotes != null){
							XMLUtil.addFilledElement(sequenceElement, CONTAMNOTESTAG, contaminationNotes);
						}
						
						//Now need to fill in sequence
						MesquiteStringBuffer sequenceBuffer = new MesquiteStringBuffer(20 + numChars);
						for (int ic = 0; ic<numChars; ic++) {
							if (!writeOnlySelectedData || (data.getSelected(ic))){
								long currentSize = sequenceBuffer.length();
								boolean wroteMoreThanOneSymbol = false;
								if (data.isUnassigned(ic, it)){
									sequenceBuffer.append(getUnassignedSymbol());
								}
								else if (includeGaps || (!data.isInapplicable(ic,it))) {
									data.statesIntoStringBuffer(ic, it, sequenceBuffer, false);
									wroteMoreThanOneSymbol = sequenceBuffer.length()-currentSize>1;
								}
								if (wroteMoreThanOneSymbol) {
									alert("Sorry, this data matrix can't be uploaded to the database (some character states aren't represented by a single symbol [char. " + CharacterStates.toExternal(ic) + ", taxon " + Taxon.toExternal(it) + "])");
									return false;
								}
							}
						}
						XMLUtil.addFilledElement(sequenceElement, SEQSTRINGTAG, sequenceBuffer.toString());

						//Now add chromatogram data, but only the original file name
						Associable taxaInfo = data.getTaxaInfo(false);
						String[] chromatFileNames = ChromaseqUtil.getStringsAssociated(taxaInfo, ChromaseqUtil.origReadFileNamesRef, it);
						for (int ic = 0; ic < chromatFileNames.length; ic++){
							if(ic % 2 != 0){//an odd index, so it is original file name (e.g. E07_DNA3362_MSP1R_763741_055.ab1) not a renamed file name (DNA3362.g.MSP1R.ab1) see lines 486-487 in Contig.java
								XMLUtil.addFilledElement(sequenceElement, CHROMTAG, chromatFileNames[ic]);
							}
						}
					}// end conditional for !writeOnlySelectedTaxa or taxon is selected & has data
				}// end looping over taxa

				//Change xml object to string so we can pass it to server
				String docAsString = XMLUtil.getDocumentAsXMLString(sequencesAsXML, false);

				//To print the outgoing xml to the log (for debugging purposes), just uncomment the next 2 lines (and comment out the succeeding try/catch)
				//logln(docAsString);
				//return true;
/**/
				try{
					StringEntity docAsStringEntity = new StringEntity(docAsString);
					docAsStringEntity.setContentType("text/xml");
					DefaultHttpClient httpClient = new DefaultHttpClient();
					HttpPost httpPost = new HttpPost("http://localhost/castor/castor/main/xml_upload.php");
					httpPost.setEntity(docAsStringEntity);

					HttpResponse response = httpClient.execute(httpPost);

					try{
						if(!processResponse(response)){
							if(!MesquiteThread.isScripting()){
								alert("The upload failed.  See Mesquite log for details.");
							}
							return false;
						} else return true;
					} catch (Exception e){//Catch IOEException and DocumentException from processResponse
						e.printStackTrace();
						return false;
					}
				} catch (Exception e){//Catch the UnsupportedEncodingException from StringEntity declaration
					e.printStackTrace();
					return false;
				}
/**/
			}//end else for at least some taxa are selected if writeOnlySelectedTaxa is true
		} else return false;
	}
	/*.................................................................................................................*/
	@SuppressWarnings("unchecked")
	protected boolean processResponse(HttpResponse response) throws IOException, DocumentException {
		if(response.getStatusLine().getStatusCode() == 200){
			HttpEntity entity = response.getEntity();
			if(entity != null){
				boolean fail = false;
				//To print the incoming xml to the log (for debugging purposes), just uncomment the next 8 lines
				/*
				InputStream instream = entity.getContent();
				BufferedReader br = new BufferedReader(new InputStreamReader(instream));
				String line = "";
				int lineCounter = 0;
				while((line = br.readLine()) != null) {
					logln(lineCounter + ": " + line);
					lineCounter++;
				}
				*/

				ByteArrayInputStream is = new ByteArrayInputStream(EntityUtils.toByteArray(entity));
				SAXReader saxReader = new SAXReader();
				Document responseDocument = null;
				responseDocument = saxReader.read(is);
				Element rootElement = responseDocument.getRootElement();
				
				//If root element is named uploadfail, none were uploaded and there will be either attributes or elements describing why...
				String rootName = rootElement.getName();
				if(rootName == "uploadfail"){
					logln("The sequences were not uploaded to database for the following reason(s)");
					Iterator<Element> failResult = rootElement.elementIterator("result");
					while(failResult.hasNext()){
						Element resultElement = failResult.next();
						String resultString = resultElement.getTextTrim();
						logln("\t" + resultString);
						Attribute login = resultElement.attribute("login");
						if(login != null){
							//String loginString = login.getValue();
							//logln("login text: " + login.getText());
							//logln("login value: " + login.getValue());
							if(login.getValue().equalsIgnoreCase("fail")){
								logln("\tYou can re-enter your username and password via File > Defaults > Set Authentication Credentials...\n");
							}
						}
					}
					fail = true;
				} else if(rootName == "uploadresponse"){
				
					//First check to see if all sequences were uploaded correctly.  Can just stop, if so?
					Attribute fullSuccessAttribute = rootElement.attribute("full_success");
					String fullSuccessString = fullSuccessAttribute.getValue();
					boolean fullSuccess = Boolean.parseBoolean(fullSuccessString);

					if(fullSuccess){
						logln("All sequences uploaded successfully");
					} else {//Not all sequences made it.  Iterate over them and check (1) for success attribute of sequence element and if false (2) result element(s) of that sequence element
						Iterator<Element> sequenceIterator = rootElement.elementIterator("sequence");// just grab the child sequence elements
						while(sequenceIterator.hasNext()){//iterate through the sequence elements
							Element sequenceElement = sequenceIterator.next();
							Attribute sequenceSuccessAttribute = sequenceElement.attribute("success");//grab the success attribute
							if(sequenceSuccessAttribute != null){
								String sequenceSuccessString = sequenceSuccessAttribute.getValue();
								boolean sequenceSuccess = Boolean.parseBoolean(sequenceSuccessString);//value is stored as string ("true" or "false") that needs to be parsed
	
								Element nameElement = sequenceElement.element("name");//grab the name element
								if(nameElement != null){
									String name = nameElement.getTextTrim();
		
									if(sequenceSuccess){//if it was uploaded OK, just report that
										logln(name + " successfully added to database.");
									} else {//Sequence not uploaded, need to look at result element(s)
										Iterator<Element> resultIterator = sequenceElement.elementIterator("result");
										boolean foundResult = false;
										String resultOut = "";
										while(resultIterator.hasNext()){//iterate over result element(s) and append the messages to resultOut String
											Element result = resultIterator.next();
											String resultString = result.getTextTrim();
											resultOut += "\t" + resultString + "\n";
											if(!foundResult){
												foundResult = true;
											}
										}
										if(foundResult){//found result elements, so provide them in log
											logln(name + " could not be uploaded to the database for the following reason(s):");
											log(resultOut);
										} else {//shouldn't happen...
											logln(name + " could not be uploaded to the database, but no reason was given by server.");
										}
									}//end else for failed upload
								}
							}
						}//end iteration over sequence element
						if(!MesquiteThread.isScripting()){
							alert("Problems encountered during upload process; some sequences were not added to the database.  See Mesquite log for details");
						}
					}//end else for !full_success
				}//end else for uploadresponse

				EntityUtils.consume(entity);//nom nom
				return !fail;
			} else {
				logln("The server returned a null response; upload status unknown");
				return false;
			}
		} else {//Status code other than 200 returned
			logln("A server error occurred and sequence upload could not be completed.  From server: " + response.getStatusLine());
			return false;
		}
	}
	/*.................................................................................................................*/
	protected boolean getUploadOptions(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog uploadDialog = new ExtensibleDialog(containerOfModule(), "Upload sequences to database", buttonPressed);
//		int dialogWidth = uploadDialog.getDialogWidth();
//		int fieldLength = dialogWidth-12*2;//See ExtensibleDialog.addSingleLineTextArea
		uploadDialog.appendToHelpString("Choose the options for uploading sequences to local database.");
		uploadDialog.addLabel("Upload options:");
		Checkbox includeGapsCheckBox = uploadDialog.addCheckBox("include gaps", includeGaps);
		Checkbox contaminatedCheckBox = uploadDialog.addCheckBox("contaminated sequence", contaminated);
		Checkbox writeSelectedTaxaBox = uploadDialog.addCheckBox("Upload only selected taxa", writeOnlySelectedTaxa);
		//a drop-down menu for selecting the status value
		String[] statusList = {STATUS_ROUGH, STATUS_INTERMEDIATE, STATUS_FINAL, STATUS_BAD};
		Choice statusChoice = uploadDialog.addPopUpMenu("Status:", statusList, 0);
		
		uploadDialog.addHorizontalLine(1);
		
		uploadDialog.addLabel("Chromatogram file parsing rules:");
		SingleLineTextField sampleCodePrefixField = uploadDialog.addTextField("Sample code prefix", sampleCodePrefix, 4, true);
		SingleLineTextField primerStartTokenField = uploadDialog.addTextField("Primer start token", primerStartToken, 4, true);//just called "delimiter" on server
		
		uploadDialog.addHorizontalLine(1);
/*
		uploadDialog.addLabel("Database authorization:");
		SingleLineTextField userNameField = uploadDialog.addTextField("username (email)", "",30);
		SingleLineTextField passwordField = uploadDialog.addTextField("password", "", 30);
		passwordField.setEchoChar('*');
*/
		uploadDialog.completeAndShowDialog(true);
		if(buttonPressed.getValue() == 0){
			status = statusList[statusChoice.getSelectedIndex()];
			includeGaps = includeGapsCheckBox.getState();
			contaminated = contaminatedCheckBox.getState();
			writeOnlySelectedTaxa = writeSelectedTaxaBox.getState();
			sampleCodePrefix = sampleCodePrefixField.getText();
			primerStartToken = primerStartTokenField.getText();
//			username = userNameField.getText();
//			password = passwordField.getText();
//			passwordField.setText("");
			
		}

		if(contaminated){
			MesquiteInteger contaminationButton = new MesquiteInteger(1);
			ExtensibleDialog contaminationDialog = new ExtensibleDialog(containerOfModule(), "Contamination notes", contaminationButton);
			contaminationDialog.appendToHelpString("Enter any notes about the contamination.");
			contaminationDialog.addLabel("Contamination notes:");
			TextArea contaminationNotesArea = contaminationDialog.addTextArea("", 4);
			contaminationDialog.completeAndShowDialog(true);
			if(contaminationButton.getValue() == 0){
				String notes = contaminationNotesArea.getText();
				if(notes.length() > 0){
					contaminationNotes = new String(notes);
				}
			}
		}
		
		//*NOT* a security measure; just checks for empty or null username & password
/*		boolean userInfoEntered = false;
		if(username != null && password != null){
			if(username.length() > 0 && password.length() > 0){
				userInfoEntered = true;
			}
		}
		*/

		uploadDialog.dispose();
		return (buttonPressed.getValue() == 0);// && userInfoEntered;
	}
	/*.................................................................................................................*/
	protected boolean authenticationEstablished(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog credentialsDialog = new ExtensibleDialog(containerOfModule(), "Authorization details", buttonPressed);
		credentialsDialog.addLabel("Database authorization:");
		SingleLineTextField userNameField = credentialsDialog.addTextField("username (email)", "",30);
		SingleLineTextField passwordField = credentialsDialog.addTextField("password", "", 30);
		passwordField.setEchoChar('*');
		credentialsDialog.completeAndShowDialog(true);
		if(buttonPressed.getValue() == 0){
			String username = userNameField.getText();
			String password = passwordField.getText();
			AuthenticationElement authElement = new AuthenticationElement(username,password);
			getProject().addFileElement(authElement);
		}
		return (buttonPressed.getValue() == 0);
	}
	/*.................................................................................................................*/
	protected boolean taxonHasData(CharacterData data, int it){
		for (int ic = 0; ic<data.getNumChars(); ic++) {
			if (!writeOnlySelectedData || (data.getSelected(ic))){
				if (!data.isUnassigned(ic, it) && !data.isInapplicable(ic, it))
					return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/
	/**Returns the name of taxa {@code it} in the block {@code taxa}
	 * @return	the name of the <i>it<sup>th</sup></i> taxa as a {@code String}*/
	protected String getTaxonName(Taxa taxa, int it){//TODO: can add more here, if we want to try simplifying or parsing names to some rule or another
		return taxa.getTaxonName(it);//could add suffixes et al.  See InterpretFasta.getExportOptions
	}
	/*.................................................................................................................*/
	/**@return N*/
	protected String getUnassignedSymbol(){
		return "N";
	}
	/*.................................................................................................................*/
	/** if returns true, then requests to remain on even after operateData is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	public CompatibilityTest getCompatibilityTest(){
		return new RequiresAnyDNAData();
	}
	/*.................................................................................................................*/
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
		return "OLD Upload sequences to database...";
	}
	/*.................................................................................................................*/
	/** @return	The name of the module*/
	public String getName() {
		return "OLD Upload sequences to database";
	}
	/*.................................................................................................................*/
	/** Returns an explanation of what the module does for (?) button in dialogs.
	 * @return	A short String explaining the module*/
	public String getExplanation(){
		return "Uploads sequences to local database and provides server response to upload attempt.";
	}
}