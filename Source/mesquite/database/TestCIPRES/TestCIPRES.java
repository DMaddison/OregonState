package mesquite.database.TestCIPRES;

import java.io.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import mesquite.lib.CommandChecker;
import mesquite.lib.Debugg;
import mesquite.lib.duties.*;

public class TestCIPRES extends UtilitiesAssistant {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		addMenuItem(null, "Test CIPRES...", makeCommand("testCIPRES", this));
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Test CIPRES", null, commandName, "testCIPRES")) {
			testCIPRes();
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}

	public void testCIPRes() {
		String baseURL = "https://www.phylo.org/cipresrest/v1";
		
		String username = "DavidMaddison";
		String password = ""; // we would need to retrieve this from somewhere
		String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";
		
		// URL for a list of jobs
		String listURL = baseURL + "/job/" + username;

		// URL for list of tools (no authentication necessary)
		String toolURL = baseURL + "/tool";

		// from http://www.artima.com/forums/flat.jsp?forum=121&thread=357685
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(AuthScope.ANY, credentials);
		HttpClient httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

    	HttpGet httpget = new HttpGet(listURL); // Currently returns 404, maybe because missing cipres-appkey header
        httpget.addHeader("cipres-appkey", CIPRESkey); //Need a value for this header
        try {
        	HttpResponse response = httpclient.execute(httpget);
        	Debugg.println("-----------------RESPONSE-----------------------");
        	// Would do something with response here...
        	Debugg.println(response.getStatusLine().toString());
        	
        	// Just printing out the response entity.
        	// To do actually something, would want to use XML / DOM tools
        	HttpEntity responseEntity = response.getEntity();
			InputStream instream = responseEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(instream));
			String line = "";
			while((line = br.readLine()) != null) {
				Debugg.println(line);
			}

        	Debugg.println("----------------------------------------------------");
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
        	Debugg.printStackTrace(e);
        }
	}

	public String getName() {
		return "Test CIPRES connection";
	}

}
