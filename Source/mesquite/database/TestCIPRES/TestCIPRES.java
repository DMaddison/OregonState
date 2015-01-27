package mesquite.database.TestCIPRES;

import java.io.*;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import mesquite.lib.*;
import mesquite.lib.duties.*;

import org.dom4j.Document;
import org.dom4j.Element;

public class TestCIPRES extends UtilitiesAssistant {

	String baseURL = "https://www.phylo.org/cipresrest/v1";
	boolean verbose = true;

	static String username = "DavidMaddison";
	static String password = ""; // we would need to retrieve this from somewhere
	String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		addMenuItem(null, "Send Job to CIPRES", makeCommand("sendJobToCIPRES", this));
		addMenuItem(null, "CIPRES Job List", makeCommand("listCIPRESJobs", this));
		addMenuItem(null, "CIPRES Tool List", makeCommand("listCIPRESTools", this));
		addMenuItem(null, "CIPRES Job Status...", makeCommand("checkJob", this));
		return true;
	}
	/*.................................................................................................................*/
	boolean checkUsernamePassword(){
		if (StringUtil.blank(username) || StringUtil.blank(password)){
			MesquiteBoolean answer = new MesquiteBoolean(false);
			MesquiteString usernameString = new MesquiteString();
			if (username!=null)
				usernameString.setValue(username);
			MesquiteString passwordString = new MesquiteString();
			if (password!=null)
				passwordString.setValue(password);
			new UserNamePasswordDialog(containerOfModule(), "Username and Password", "Username", "Password", answer, usernameString, passwordString, false);
			if (answer.getValue()){
				username=usernameString.getValue();
				password=passwordString.getValue();
			}

		}
		return StringUtil.notEmpty(username) && StringUtil.notEmpty(password);

	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Send Job to CIPRES", null, commandName, "sendJobToCIPRES")) {
			sendJobToCipres();
		}
		else if (checker.compare(this.getClass(), "CIPRES Job List", null, commandName, "listCIPRESJobs")) {
			listCipresJobs();
		}
		else if (checker.compare(this.getClass(), "CIPRES Tool List", null, commandName, "listCIPRESTools")) {
			listCipresTools();
		}
		else if (checker.compare(this.getClass(), "CIPRES Job Finished?", null, commandName, "checkJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Job String", "Job String", "");
			checkJobStatus(jobURL);
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public Document loadXMLFile(String rootTag, String xmlFile) {
		if (!StringUtil.blank(xmlFile)) {
			Document CipresDoc = XMLUtil.getDocumentFromString(rootTag, xmlFile);
			return CipresDoc;
		}
		return null;
	}

	/*.................................................................................................................*/
	public HttpClient getHttpClient(){
		// from http://www.artima.com/forums/flat.jsp?forum=121&thread=357685
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(AuthScope.ANY, credentials);
		return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
	}
	/*.................................................................................................................*/
	public Document  cipresQuery(HttpClient httpclient, String URL, String xmlRootTag){
		HttpGet httpget = new HttpGet(URL); 
		httpget.addHeader("cipres-appkey", CIPRESkey);
		try {
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity responseEntity = response.getEntity();
			InputStream instream = responseEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(instream));
			String line = "";
			StringBuffer sb = new StringBuffer();
			while((line = br.readLine()) != null) {
				sb.append(line+StringUtil.lineEnding());
			}
			Document cipresResponseDoc = loadXMLFile(xmlRootTag, sb.toString());
			if (cipresResponseDoc!=null && verbose) {
				Debugg.println(sb.toString());
			}
			EntityUtils.consume(response.getEntity());
			return cipresResponseDoc;
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
		return null;
	}

	/*.................................................................................................................*/

	public void processToolList(Document cipresResponseDoc) {
		String elementName = "tool";
		List tools = cipresResponseDoc.getRootElement().elements(elementName);
		int count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextTool = (Element) iter.next();
			count++;
		}
		String[] toolName = new String[count];
		count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextTool = (Element) iter.next();
			String name = nextTool.elementText("toolName");
			if (!StringUtil.blank(name)&& count<toolName.length) {
				//toolName[count] = name;
				Debugg.println(name);
			}
			count++;
		}

	}
	/*.................................................................................................................*/

	public String[] processJobsList(Document cipresResponseDoc) {
		String elementName = "jobstatus";
		Element jobs = cipresResponseDoc.getRootElement().element("jobs");
		if (jobs==null)
			return null;
		List tools = jobs.elements("jobstatus");
		int count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextTool = (Element) iter.next();
			count++;
		}
		String[] url = new String[count];
		count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextJob = (Element) iter.next();
			if (nextJob!=null) {
				Element selfUriElement= nextJob.element("selfUri");
				if (selfUriElement!=null) {
					String jobURL = selfUriElement.elementText("url");
					if (!StringUtil.blank(jobURL)&& count<url.length) {
						url[count] = jobURL;
					}
				}
				count++;
			}
		}
		return url;

	}
	/*.................................................................................................................*/
	public void  listTools(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, baseURL + "/tool", "tools");
		if (cipresResponseDoc!=null) {
			processToolList(cipresResponseDoc);
		}
	}
	/*.................................................................................................................*/
	public void  listJobs(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, baseURL + "/job/" + username, "joblist");
		if (cipresResponseDoc!=null) {
			String[] jobList = processJobsList(cipresResponseDoc);
			if (jobList!=null)
				for (int job=0; job<jobList.length; job++){
					Debugg.println("job " + job + ": " + jobList[job]);
					String status = reportJobStatus(jobList[job]);
					Debugg.println("   " + status);

				}
		}
	}

	/*.................................................................................................................*/

	public void processJobSubmissionResponse(Document cipresResponseDoc) {

		Element element = cipresResponseDoc.getRootElement().element("selfUri");
		Element subelement = null;
		if (element!=null)
			subelement=element.element("url");

		Debugg.println("url " + subelement.getText());

		element = cipresResponseDoc.getRootElement().element("metadata");
		List entries = element.elements("entry");
		String reportedJobID = "";
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Element nextEntry = (Element) iter.next();
			if (nextEntry.elementText("key")=="clientJobId")
				reportedJobID= nextEntry.elementText("value");
		}

		Debugg.println("reportedJobID " + reportedJobID);


	}

	/*.................................................................................................................*/
	public boolean postJob(HttpClient httpclient, String cipresTool, String filePath, String jobID){
		if (filePath==null)
			return false;
		String URL = baseURL + "/job/" + username;
		HttpPost httppost = new HttpPost(URL);
		httppost.addHeader("cipres-appkey", CIPRESkey); 

		//http://stackoverflow.com/questions/18964288/upload-a-file-through-an-http-form-via-multipartentitybuilder-with-a-progress
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();        
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		final File file = new File(filePath);
		FileBody fb = new FileBody(file);

		builder.addPart("input.infile_", fb);  
		builder.addTextBody("tool", cipresTool);
		if (StringUtil.notEmpty(jobID))
			builder.addTextBody("metadata.clientJobId", jobID);
		builder.addTextBody("metadata.statusEmail", "true");
		HttpEntity yourEntity = builder.build();

		httppost.setEntity(yourEntity);

		try {
			HttpResponse response = httpclient.execute(httppost);

			Debugg.println("-----------------RESPONSE-----------------------");
			Debugg.println(response.getStatusLine().toString());

			HttpEntity responseEntity = response.getEntity();
			InputStream instream = responseEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(instream));
			StringBuffer sb = new StringBuffer();
			String line = "";
			while((line = br.readLine()) != null) {
				sb.append(line+StringUtil.lineEnding());
			}
			Document cipresResponseDoc = loadXMLFile("jobstatus", sb.toString());
			if (cipresResponseDoc!=null) {
				processJobSubmissionResponse(cipresResponseDoc);
			} else {
				Debugg.println("********  no job status XML ********* ");
			}

			Debugg.println("----------------------------------------------------");
			Debugg.println(sb.toString());
			Debugg.println("----------------------------------------------------");
			EntityUtils.consume(response.getEntity());
			return true;
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean checkJob (HttpClient httpclient, String jobURL){
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		if (cipresResponseDoc!=null) {
			// process xml
		}
		return true;
	}
	
	/*.................................................................................................................*/

	public String jobStatusFromResponse(Document cipresResponseDoc) {

		String status = "Status not available";
		
		Element element = cipresResponseDoc.getRootElement().element("terminalStage");
		if (element!=null) {
			status = element.getText();
			if ("true".equalsIgnoreCase(status))
				return "COMPLETED";
		}
		element = cipresResponseDoc.getRootElement().element("messages");
		if (element==null)
			return status;

		List entries = element.elements("message");
		String reportedJobID = "";
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Element nextEntry = (Element) iter.next();
			if (nextEntry!=null)
				status= nextEntry.elementText("stage");
		}

		return status;

	}

	/*.................................................................................................................*/
	public String getJobStatus (HttpClient httpclient, String jobURL){
		verbose=false;
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		verbose=true;
		if (cipresResponseDoc!=null) {
			return jobStatusFromResponse(cipresResponseDoc);
		}
		return "Status not available";
	}


	/*.................................................................................................................*/
	public String reportJobStatus(String jobURL) {
		if (checkUsernamePassword()) {
			HttpClient httpclient = getHttpClient();
			return getJobStatus(httpclient, jobURL);
		}
		return "Status not available";

	}
	/*.................................................................................................................*/
	public boolean checkJobStatus(String jobURL) {
		if (checkUsernamePassword()) {
			HttpClient httpclient = getHttpClient();
			checkJob(httpclient, jobURL);
			return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	public void sendJobToCipres() {
		if (checkUsernamePassword()) {
			HttpClient httpclient = getHttpClient();
			postJob(httpclient, "RAXMLHPC2_TGB", "/ZephyrTest.fas", "ZEPHYR.0004");
		}
	}
	/*.................................................................................................................*/
	public void listCipresJobs() {
		if (checkUsernamePassword()) {
			HttpClient httpclient = getHttpClient();
			listJobs(httpclient);
		}
	}
	/*.................................................................................................................*/
	public void listCipresTools() {
		if (checkUsernamePassword()) {
			HttpClient httpclient = getHttpClient();
			listTools(httpclient);
		}
	}

	public String getName() {
		return "Test CIPRES connection";
	}

}
