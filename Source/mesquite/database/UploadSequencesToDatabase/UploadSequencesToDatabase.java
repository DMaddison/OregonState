package mesquite.database.UploadSequencesToDatabase;

import java.io.*;
import java.util.*;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.apache.http.*;

import mesquite.categ.lib.*;
import mesquite.database.lib.AuthenticationElement;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;

import mesquite.database.lib.*;

/**For directly submitting sequences to local database from within Mesquite.  Most of the 
 * XML formatting performed by {@link XMLForCastor}*/
public class UploadSequencesToDatabase extends DataUtility {
	String serverLoc = "http://128.193.224.50/castor/xml-upload.php";
	
	public boolean startJob(String arguments, Object condition,	boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	/**Takes passed {@code data} and calls {@link XMLForCastor#getXMLDocument(CharacterData, Object) 
	 * XMLForCastor.getXMLDocument} to format the data as an XML-formatted document.  Then POSTs 
	 * the XML to the local server. After the POST, calls {@link #processResponse(HttpResponse) 
	 * processResponse} to process the response from the server.
	 * @param	data	A {@link CharacterData} object to be formatted.  Must be {@link DNAData}.
	 * @return	{@code true} if data formatted, post is successful, and response indicates a 
	 * successful upload; {@code false} if any necessary objects are null, the post fails, or 
	 * the server indicates an unsuccessful upload of sequences.*/
	public boolean operateOnData(CharacterData data) {
		boolean success = false;
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

		if(urlChecked()){//Just provides a means to enter URL
		
			//Going to be posting this, so we need authentication credentials
			String username = "";
			String password = "";
	
			boolean authChecked = false;
			while(!authChecked){
				//Authentication element - a lightweight file element that just stores the two strings.
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
	
			//Authentication credentials are set, so prepare the Document
			//A class for formatting the sequence data in XML format
			XMLForCastor formatter = new XMLForCastor();
			//Call XMLCastor.getXMLDocument to get the sequences in XML 
			Document sequencesAsXML = formatter.getXMLDocument(data, containerOfModule());
	
			//Need to attach authentication details, so get the root element ("sequenceset" see XMLForCastor)
			if (sequencesAsXML != null) {
				Element rootElement = sequencesAsXML.getRootElement();
				rootElement.addAttribute("username", username);
				rootElement.addAttribute("password", password);
				
				//Change xml object to string so we can pass it to server
				String docAsString = XMLUtil.getDocumentAsXMLString(sequencesAsXML, false);
		
				//To print the outgoing xml to the log (for debugging purposes), just uncomment the next 2 lines (and comment out the succeeding try/catch)
				//logln(docAsString);
				//return true;
/**/
			
				//Document is now prepared.  Post it to the server.  Various exceptions are possible...
				try{
					StringEntity docAsStringEntity = new StringEntity(docAsString);
					docAsStringEntity.setContentType("text/xml");
					DefaultHttpClient httpClient = new DefaultHttpClient();
					HttpPost httpPost = new HttpPost(serverLoc);
					httpPost.setEntity(docAsStringEntity);
		
					//Post attempt and receive the response
					HttpResponse response = httpClient.execute(httpPost);
		
					try{
						//Process the response from the server, which should have information about upload success/failure
						if(!processResponse(response)){
							if(!MesquiteThread.isScripting()){
								alert("The upload failed.  See Mesquite log for details.");
							}
							return false;
						} else {
							success = true;
						}
					} catch (Exception e){//Catch IOEException and DocumentException from processResponse
						e.printStackTrace();
						return false;
					}
				} catch (Exception e){//Catch the UnsupportedEncodingException from StringEntity declaration
					e.printStackTrace();
					return false;
				}
/**/
			}
		} else { // urlChecked returned false
			return false;
		}
		return success;
	}
	/*.................................................................................................................*/
	/**Processes an {@link HttpResponse} from a server.  Reads incoming XML and presents any messages contained in the
	 * XML response.  Messages will be stored in 'result' elements, which are children of 'sequence' elements (which
	 * themselves are children of the root element).
	 * @param	response	an {@code HttpResponse} object returned by server.
	 * @return	{@code true} if response is not-null, some sequences have been uploaded, and results have been
	 * successfully read; {@code false} if a server error occurs, response is null, or all sequences failed to upload*/
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
				//Server is indicating a complete failure to upload sequences to database.  Get 'result' elements to find out why.
				if(rootName == "uploadfail"){
					logln("The sequences were not uploaded to database for the following reason(s)");
					Iterator<Element> failResult = rootElement.elementIterator("result");
					while(failResult.hasNext()){
						Element resultElement = failResult.next();
						String resultString = resultElement.getTextTrim();
						logln("\t" + resultString);
						Attribute login = resultElement.attribute("login");
						if(login != null){
							if(login.getValue().equalsIgnoreCase("fail")){
								logln("\tYou can re-enter your username and password via File > Defaults > Set Authentication Credentials...\n");
							}
						}
					}
					fail = true;
				} else if(rootName == "uploadresponse"){//Server indicates at least some success
					//First check to see if all sequences were uploaded correctly.
					Attribute fullSuccessAttribute = rootElement.attribute("full_success");
					String fullSuccessString = fullSuccessAttribute.getValue();
					boolean fullSuccess = Boolean.parseBoolean(fullSuccessString);

					if(fullSuccess){//All sequences made it, just report full success
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
								}//end conditional for non-null nameElement
							}//end conditional for non-null sequenceSuccessElement
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
	/**Called for user to enter authentication credentials.  Stores the two strings as a {@link AuthenticationElement} 
	 * attached to the project.
	 * @return	{@code true} if user pressed 'OK' button; {@code false} if user pressed 'cancel' button.*/
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
			//First remove any existing AuthenticationElement(s) for this project
			Listable authElementsToRemove[] = getProject().getFileElements(AuthenticationElement.class);
			int numElements = authElementsToRemove.length;
			if(numElements > 0){//at least one AuthenticationElement.  Get it and remove it
				for(int ie = 0; ie < numElements; ie++){
					AuthenticationElement toRemove = (AuthenticationElement)authElementsToRemove[ie];
					getProject().removeFileElement(toRemove);
				}
			}
			//Now that any previous AuthenticationElements have been removed, create and add this new one
			AuthenticationElement authElement = new AuthenticationElement(username,password);
			getProject().addFileElement(authElement);
		}
		return (buttonPressed.getValue() == 0);
	}
	/*.................................................................................................................*/
	protected boolean urlChecked(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog urlDialog = new ExtensibleDialog(containerOfModule(), "Server location", buttonPressed);
		urlDialog.addLabel("Location for upload (URL):");
		SingleLineTextField urlField = urlDialog.addTextField(serverLoc);
		
		urlDialog.completeAndShowDialog(true);
		
		if(buttonPressed.getValue() == 0){
			String tempServerLoc = urlField.getText();
			//TODO: could add more checks here to make sure tempServerLoc is a valid URL
			if(tempServerLoc != null){
				serverLoc = tempServerLoc;
			}
		}
		return(buttonPressed.getValue() == 0);
	}
	/*.................................................................................................................*/
	/** if returns true, then requests to remain on even after operateData is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	/**To be sure the module deals with DNA data?*/
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
		return "Upload Sequences to Database...";
	}
	/*.................................................................................................................*/
	/** @return	The name of the module*/
	public String getName() {
		return "Upload sequences to database";
	}
	/*.................................................................................................................*/
	/** Returns an explanation of what the module does for (?) button in dialogs.
	 * @return	A short String explaining the module*/
	public String getExplanation(){
		return "Uploads sequences to local database and provides server response to upload attempt.";
	}
}
