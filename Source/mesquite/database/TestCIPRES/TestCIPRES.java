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
	Document CipresDoc = null;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		addMenuItem(null, "Send Job to CIPRES", makeCommand("sendJobToCIPRES", this));
		addMenuItem(null, "CIPRES Job List", makeCommand("listCIPRESJobs", this));
		addMenuItem(null, "CIPRES Tool List", makeCommand("listCIPRESTools", this));
		addMenuItem(null, "CIPRES Job Status...", makeCommand("checkJob", this));
		return true;
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

	String baseURL = "https://www.phylo.org/cipresrest/v1";

	String username = "DavidMaddison";
	String password = ""; // we would need to retrieve this from somewhere
	String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";

	/*.................................................................................................................*/
	public boolean loadXMLFile(String rootTag, String xmlFile) {
		if (!StringUtil.blank(xmlFile)) {
			CipresDoc = XMLUtil.getDocumentFromString(rootTag, xmlFile);
			return CipresDoc!=null;
		}
		return false;
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

	public void processToolList() {
		String elementName = "tool";
		List tools = CipresDoc.getRootElement().elements(elementName);
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
	public void  listTools(HttpClient httpclient){
		String URL = baseURL + "/tool";
		HttpGet httpget = new HttpGet(URL); // Currently returns 404, maybe because missing cipres-appkey header
		httpget.addHeader("cipres-appkey", CIPRESkey); //Need a value for this header
		try {
			HttpResponse response = httpclient.execute(httpget);
			Debugg.println("-----------------RESPONSE-----------------------");
			Debugg.println(response.getStatusLine().toString());

			HttpEntity responseEntity = response.getEntity();
			InputStream instream = responseEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(instream));
			String line = "";
			StringBuffer sb = new StringBuffer();
			while((line = br.readLine()) != null) {
				sb.append(line+StringUtil.lineEnding());
			}
			if (loadXMLFile("tools", sb.toString())) {
				processToolList();
			}
			Debugg.println("----------------------------------------------------");
			EntityUtils.consume(response.getEntity());
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
	}
	/*.................................................................................................................*/
	public void  listJobs(HttpClient httpclient){
		String URL = baseURL + "/job/" + username;
		HttpGet httpget = new HttpGet(URL); 
		httpget.addHeader("cipres-appkey", CIPRESkey);
		try {
			HttpResponse response = httpclient.execute(httpget);
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
			if (loadXMLFile("joblist", sb.toString())) {
				// do something with the joblist that is now in CipresDoc
			}

			Debugg.println("----------------------------------------------------");
			Debugg.println(sb.toString());
			Debugg.println("----------------------------------------------------");
			EntityUtils.consume(response.getEntity());
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
	}

	/*.................................................................................................................*/

	public void processJobSubmissionResponse() {

		Element element = CipresDoc.getRootElement().element("selfUri");
		Element subelement = null;
		if (element!=null)
			subelement=element.element("url");

		Debugg.println("url " + subelement.getText());

		element = CipresDoc.getRootElement().element("metadata");
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
			if (loadXMLFile("jobstatus", sb.toString())) {
				processJobSubmissionResponse();
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
		HttpGet httpget = new HttpGet(jobURL); 
		httpget.addHeader("cipres-appkey", CIPRESkey);
		try {
			HttpResponse response = httpclient.execute(httpget);
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
			if (loadXMLFile("jobstatus", sb.toString())) {
				// do something with the joblist that is now in CipresDoc
			}

			Debugg.println("----------------------------------------------------");
			Debugg.println(sb.toString());
			Debugg.println("----------------------------------------------------");
			EntityUtils.consume(response.getEntity());
		} catch (IOException e) {
			Debugg.printStackTrace(e);
			return false;
		}
		return true;
	}

	/*.................................................................................................................*/
	public boolean checkJobStatus(String jobURL) {
		HttpClient httpclient = getHttpClient();
		checkJob(httpclient, jobURL);
		return true;
	}
	/*.................................................................................................................*/
	public void sendJobToCipres() {
		HttpClient httpclient = getHttpClient();
		postJob(httpclient, "RAXMLHPC2_TGB", "/ZephyrTest.fas", "ZEPHYR.0002");
	}
	/*.................................................................................................................*/
	public void listCipresJobs() {
		HttpClient httpclient = getHttpClient();
		listJobs(httpclient);
	}
	/*.................................................................................................................*/
	public void listCipresTools() {
		HttpClient httpclient = getHttpClient();
		listTools(httpclient);
	}

	public String getName() {
		return "Test CIPRES connection";
	}

}
