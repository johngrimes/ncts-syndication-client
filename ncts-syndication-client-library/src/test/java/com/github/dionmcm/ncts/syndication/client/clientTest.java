package com.github.dionmcm.ncts.syndication.client;


import org.testng.annotations.Test;

import com.google.common.net.MediaType;

import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.Rule;
import org.mockserver.client.*;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;



public class clientTest {
	
	SyndicationClient testClient;
	String feedURL = "http://localhost:1080/syndication.xml";
	String tokenURL = "http://localhost:1080/mockToken";
	String clientID = "test";
	String secret = "test";
	File outDir = new File("src/test/java/com/github/dionmcm/ncts/syndication/client/Test_Client");

	private ClientAndServer mockServer;
	private MockServerClient mockServerClient;
	String [] serverFileList = { //That are contained in the "Test_Server" folder
			"blue1.r2",
			"blue2.r2",
			"red1.r2",
			"purple1.r2",
			"purple2.r2",
			"green1.r2"
	};
	String serverDir = "src/test/java/com/github/dionmcm/ncts/syndication/client/Test_Server/";

	
    @Test(priority = 1, groups = "downloading", enabled = true)
    public void downloadsAllFilesInCategory() throws URISyntaxException, IOException, NoSuchAlgorithmException, JDOMException, HashValidationFailureException {
    	testClient = new SyndicationClient(feedURL,tokenURL, outDir, clientID, secret);
    	Map<String, List<DownloadResult>> result = testClient.download(false, "SCT_RF2_BLUE");
    	assertTrue(
    		Files.list(outDir.toPath())
    			.anyMatch(fileName -> fileName.getFileName().toString().equals("blue1.r2")) &&
    		Files.list(outDir.toPath())
    			.anyMatch(fileName -> fileName.getFileName().toString().equals("blue2.r2"))
    	);
    }
    
    @Test(priority = 2, groups = "downloading", enabled = true )
    public void downloadsLatestInCategory() throws IOException, URISyntaxException, NoSuchAlgorithmException, JDOMException, HashValidationFailureException {
    	testClient = new SyndicationClient(feedURL,tokenURL, outDir, clientID, secret);
    	DownloadResult result = testClient.downloadLatest("SCT_RF2_PURPLE");
    	assertTrue(
    		Files.list(outDir.toPath())
    			.anyMatch(fileName -> fileName.getFileName().toString().equals("purple2.r2"))
    	);
    	assertFalse(
        		Files.list(outDir.toPath())
        			.anyMatch(fileName -> fileName.getFileName().toString().equals("purple1.r2"))
        	);
    }
    
    @Test(priority = 3, groups = "downloading", enabled = true, expectedExceptions = HashValidationFailureException.class)
    public void hashMismatchInSyndicationThrowsException() throws IOException, URISyntaxException, NoSuchAlgorithmException, JDOMException, HashValidationFailureException{
    	testClient = new SyndicationClient(feedURL,tokenURL, outDir, clientID, secret);
    	DownloadResult result = testClient.downloadLatest("SCT_RF2_GREEN");
    }

    
    @BeforeClass
    public void setUpMockServer() throws IOException
    {
    	//Begin mock server and request handler
    	mockServer = startClientAndServer(1080);
    	mockServerClient = new MockServerClient("localhost", 1080);

    	//Put provided server files in memory
    	HashMap<String, byte[]> serverFileBins = new HashMap<String, byte[]>();
    	for(String aFile : serverFileList){
    		Path filePath = new File(serverDir + aFile).toPath();
    		serverFileBins.put(aFile, Files.readAllBytes(filePath));
    	}
    	
    	//Set up handlers for individual files
    	for(String aFile : serverFileList) {
        	mockServerClient.when(HttpRequest.request().withPath("/" + aFile))
			.respond(
					HttpResponse.response()
					.withBody(serverFileBins.get(aFile))
					);
    	}
    
    	//Handle request for syndication file
    	File syndFilePath = new File("src/test/java/com/github/dionmcm/ncts/syndication/client/Test_Server/syndication.xml");
        byte[] syndFile = Files.readAllBytes(syndFilePath.toPath());
    	mockServerClient.when(HttpRequest.request().withPath("/syndication.xml"))
    					.respond(
    							HttpResponse.response()
    							.withBody(syndFile)
    							);
    	    	
    	//Handle request for token (return meaningless Access token, mock server doesn't need token)
    	mockServerClient.when(HttpRequest.request().withPath("/mockToken"))
		.respond(
				HttpResponse.response()
				.withBody("{ \"Access-token\":\"123\"}")
				);
    }
    
    @AfterMethod(alwaysRun=true)
    public void deleteAllFilesInClientFolder() throws IOException {
    	FileUtils.cleanDirectory(outDir); 
    }
    
    @AfterClass
    public void tearDownMockServer() {
        mockServer.stop();
    }
 
    
    public static void main(String[] theArgs) throws Exception {

    }

}

