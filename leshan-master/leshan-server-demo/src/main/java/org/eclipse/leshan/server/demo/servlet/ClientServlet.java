/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.json.ResponseSerializer;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.tools.javac.util.List;

import org.eclipse.leshan.server.demo.LeshanServerSQLite;
import org.eclipse.leshan.server.demo.manager;

/**
 * Service HTTP REST API calls.
 */
public class ClientServlet extends HttpServlet {

    private static final String FORMAT_PARAM = "format";

    private static final Logger LOG = LoggerFactory.getLogger(ClientServlet.class);

    private static final long TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;

    private final LwM2mServer server;
    public static LwM2mServer server_static;
    
    public String Vehicle_req_ID; // to intercept request to PI for validation
    

    private final Gson gson;

    public ClientServlet(LwM2mServer server) {
        this.server = server;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(server.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
        
        
        
        
        
        
        
        
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	//System.out.println("do GET.");
    	//System.out.println(req);
        // all registered clients
        if (req.getPathInfo() == null) {
            Collection<Registration> registrations = new ArrayList<>();
            for (Iterator<Registration> iterator = server.getRegistrationService().getAllRegistrations(); iterator
                    .hasNext();) {
                registrations.add(iterator.next());
            }

            String json = this.gson.toJson(registrations.toArray(new Registration[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String clientEndpoint = path[0];

        // /endPoint : get client
        if (path.length == 1) {
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                resp.setContentType("application/json");
                resp.getOutputStream().write(this.gson.toJson(registration).getBytes(StandardCharsets.UTF_8));
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
        if (path.length >= 3 && "discover".equals(path[path.length - 1])) {
            String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/discover");
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // create & process request
                    DiscoverRequest request = new DiscoverRequest(target);
                    DiscoverResponse cResponse = server.send(registration, request, TIMEOUT);
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M read request on a given client.
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ReadRequest request = new ReadRequest(contentFormat, target);
                ReadResponse cResponse = server.send(registration, request, TIMEOUT);
                System.out.println(request);
                System.out.println(contentFormat);
                System.out.println(target);
                System.out.println(cResponse.toString());
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException e) {
            handleException(e, resp);
        }
    }
    
    public static String getResource(Registration registration, int code) throws SQLException {
    	
    	// get Occupancy and CarID 
    	// If called with registration Code =1 , SQL is updated 
    	
    	
    	//Code = 1 : occupancy return
    	// code = 2 : carID return 
    	
    	String target1 = "/32700/0/32801";
    	String target2 = "/32700/0/32802";
    	long TIMEOUT = 5000; // ms
    	try {
    		
    	ReadRequest request = new ReadRequest(ContentFormat.fromName("JSON"), target1);
    	ReadResponse cResponse = server_static.send(registration, request, TIMEOUT);
    	String[] path = StringUtils.split(cResponse.getContent().toString(), ',');
    	String[] occupancy = StringUtils.split(path[1], '=');
//    	java.util.List<String> respList = Arrays.asList(str.split(/));
//        System.out.println("***************************");
//        System.out.println(request);
//        System.out.println(cResponse);
//        System.out.println(str);
//        for (int i=0;i<respList.size();i++)
//        { System.out.println(respList.get(i));
//        System.out.println("test");
//        }
//        System.out.println("***************************");
    	String carID="" ;
    	if(!occupancy[1].equals("free")) {
    	ReadRequest request2 = new ReadRequest(ContentFormat.fromName("JSON"), target2);
    	ReadResponse cResponse2 = server_static.send(registration, request2, TIMEOUT);
    	
    	String[] path2 = StringUtils.split(cResponse2.getContent().toString(), ',');
    	String[] carIDs = StringUtils.split(path2[1], '=');
    	carID = carIDs[1];
    	
    	if(code==2) {
			
			return carID;
			
		}
    	
    	}

//    	ReadRequest request3 = new ReadRequest(ContentFormat.fromName("JSON"), target2);
//    	ReadResponse cResponse3 = server_static.send(registration, request2, TIMEOUT);
//    	String spotID = cResponse3.getContent().toString();    	
    				
    	
    	System.out.println("ClientServlet->getResource-Registration : "+occupancy[1]);

    	
        LeshanServerSQLite.ToSQLDB("OVERVIEW",10,Instant.now().getEpochSecond(),"Registration",registration.getEndpoint(),occupancy[1],carID,0,null,null);
        
        return occupancy[1];
    	}
    	catch (RuntimeException | InterruptedException e) {
    		System.out.println(e);
    		return null;
    	}
    }
    
    public static void startObservation(Registration registration, String occupancy) {
    	
        
        String clientEndpoint = null;
        //System.out.println("Client Servlet : StartObservation");
        //System.out.println(occupancy);

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if(occupancy!=null)
            try {
                String target = "/32700/0/32801";
                
                if (registration != null) {
                    // get content format
                    
                    ContentFormat contentFormat = ContentFormat.fromName("JSON");
                            ;

                    // create & process request
                    ObserveRequest request = new ObserveRequest(contentFormat, target);
                   // System.out.println(target);
                   // System.out.println(contentFormat);
                    
                    ObserveResponse cResponse = server_static.send(registration, request, TIMEOUT);
                   // System.out.println(cResponse);
   
                } else {
                	System.out.println("no registered client with id ");
                }
            } catch (RuntimeException | InterruptedException e) {
            	System.out.println("Exception ClientServlet:startObservation");
            }
            return;
    }    
    	


    private void handleException(Exception e, HttpServletResponse resp) throws IOException {
        if (e instanceof InvalidRequestException || e instanceof CodecException
                || e instanceof ClientSleepingException) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append("Invalid request:").append(e.getMessage()).flush();
        } else if (e instanceof RequestRejectedException) {
            LOG.warn("Request rejected", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request rejected:").append(e.getMessage()).flush();
        } else if (e instanceof RequestCanceledException) {
            LOG.warn("Request cancelled", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request cancelled:").append(e.getMessage()).flush();
        } else if (e instanceof InvalidResponseException) {
            LOG.warn("Invalid response", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Invalid Response:").append(e.getMessage()).flush();
        } else if (e instanceof InterruptedException) {
            LOG.warn("Thread Interrupted", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Thread Interrupted:").append(e.getMessage()).flush();
        } else {
            LOG.warn("Unexpected exception", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Unexpected exception:").append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        
        
        if(path[0].equals("httpQuery")) { // Check for reservation slot in the requested time and return to the user
        	System.out.println("yes");
        	int StartTime = Integer.valueOf(path[1]);
        	int Endtime = Integer.valueOf(path[2]);   
        	String rate= LeshanServerSQLite.userToDB(1, StartTime, Endtime);
        	processDeviceResponse_user(req, resp, rate);
        	
        	return;
        }
        	
        if(path[0].equals("choice")) { // Add reservation data to the database
        	String StartTime = path[1];
        	String Endtime = path[2];       	
        	String ClietName = path[3];
        	String carNumber = path[4];

        	Long now = Instant.now().getEpochSecond();
        	String rate= "Reserved" ;
        	
			   String str = "INSERT INTO "+ ClietName  +" (TIME,RSTART,REND,RCAR) VALUES (" 
			   + Long.toString(now)
			   + ",'"+StartTime
			   +"','"+ Endtime
			   + "','"+carNumber
			   + "');" ; 
			   
			   //if(Integer.valueOf(StartTime) < now+30 && Integer.valueOf(StartTime) > now-30) {
			   if(path[5].equals("1")) {
				   //Write reservationd data to PI
				   markParkingSpotReserved(carNumber, ClietName  );
				   
				   
			   }
			   else {
				   //Add reservation start to Scheduler
				   manager.addToHash(Integer.valueOf(StartTime), ClietName, carNumber, "Start");
				   manager.addToHash(Integer.valueOf(Endtime), ClietName, null, "End");
				   
				   
			   }
				   
			   
			   try {
				   //System.out.println("To insert"+str);
				LeshanServerSQLite.insert(str);
				   rate=rate+",ReferenceID,"+Long.toString(now); 
				processDeviceResponse_user(req, resp, rate);	   
				   
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	return;
        	
        }
        
        String clientEndpoint = path[0];

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            System.out.println("Target in Put - "+target);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;
                        	
                // create & process request
                LwM2mNode node = extractLwM2mNode(target, req);
               
//                if(target.equals("/32700/0/32802/")) { // Check for Validity of Vehicle Registration
//                	 String[] value = StringUtils.split(Vehicle_req_ID, '"');
//                	System.out.println("test= " +value[5]);
//                	if(!LeshanServerSQLite.hit(value[5]))
//                		return;
//                }
                
                WriteRequest request = new WriteRequest(Mode.REPLACE, contentFormat, target, node);
                WriteResponse cResponse = server.send(registration, request, TIMEOUT);
                
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException e) {
            handleException(e, resp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];
        System.out.println("Post");
        System.out.println(req);

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if (path.length >= 3 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/observe");
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    ObserveRequest request = new ObserveRequest(contentFormat, target);
                    System.out.println(target);
                    System.out.println(contentFormat);
                    
                    ObserveResponse cResponse = server.send(registration, request, TIMEOUT);
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);

        // /clients/endPoint/LWRequest : do LightWeight M2M execute request on a given client.
        if (path.length == 4) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    ExecuteRequest request = new ExecuteRequest(target, IOUtils.toString(req.getInputStream()));
                    ExecuteResponse cResponse = server.send(registration, request, TIMEOUT);
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M create request on a given client.
        if (2 <= path.length && path.length <= 3) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    LwM2mNode node = extractLwM2mNode(target, req);
                    if (node instanceof LwM2mObjectInstance) {
                        CreateRequest request = new CreateRequest(contentFormat, target, (LwM2mObjectInstance) node);
                        CreateResponse cResponse = server.send(registration, request, TIMEOUT);
                        processDeviceResponse(req, resp, cResponse);
                    } else {
                        throw new IllegalArgumentException("payload must contain an object instance");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/LWRequest/observe : cancel observation for the given resource.
        if (path.length >= 3 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringsBetween(req.getPathInfo(), clientEndpoint, "/observe")[0];
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    server.getObservationService().cancelObservations(registration, target);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest/ : delete instance
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                DeleteRequest request = new DeleteRequest(target);
                DeleteResponse cResponse = server.send(registration, request, TIMEOUT);
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException e) {
            handleException(e, resp);
        }
    }

    private void processDeviceResponse(HttpServletRequest req, HttpServletResponse resp, LwM2mResponse cResponse)
            throws IOException {
        if (cResponse == null) {
            LOG.warn(String.format("Request %s%s timed out.", req.getServletPath(), req.getPathInfo()));
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request timeout").flush();
        } else {
            String response = this.gson.toJson(cResponse);
            resp.setContentType("application/json");
            resp.getOutputStream().write(response.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
            System.out.println(response);
        }
    }

    private void processDeviceResponse_user(HttpServletRequest req, HttpServletResponse resp, String response)
            throws IOException {
        if (response == null) {
            LOG.warn(String.format("Request %s%s timed out.", req.getServletPath(), req.getPathInfo()));
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            resp.getWriter().append("Request timeout").flush();
        } else {
          
            resp.setContentType("application/json");
            resp.getOutputStream().write(response.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
            System.out.println(response);
        }
    }    
    
    public void markParkingSpotReserved (String CarID, String ClientName) {
  	
    	String target ="/32700/0/32801";
    	String content = "{\"id\":32801,\"value\":\"reserved\"}";   	
    	serverWriteToParkingSpot(target,content,ClientName);
    	
    	target ="/32700/0/32802";
    	content = "{\"id\":32802,\"value\":"+CarID+"}";   	
    	serverWriteToParkingSpot(target,content,ClientName);
    	
    }
    
    public void unmarkParkingSpotReserved (String ClientName) {
 	
    	String target ="/32700/0/32801";
    	String content = "{\"id\":32801,\"value\":\"free\"}";   	
    	serverWriteToParkingSpot(target,content,ClientName);
    	
    }    
    
    private void serverWriteToParkingSpot(String target,  String content, String ClientName ) { //String Id, String Value, String target) {

        LwM2mNode node;
        try {
            node = gson.fromJson(content, LwM2mNode.class);
            System.out.println(node);
        } catch (JsonSyntaxException e) {
            throw new InvalidRequestException(e, "unable to parse json to tlv:%s", e.getMessage());
        }
        
        ContentFormat contentFormat = ContentFormat.fromName("JSON");
        
        WriteRequest request = new WriteRequest(Mode.REPLACE, contentFormat, target, node);
        Registration registration = server.getRegistrationService().getByEndpoint("LeshanClientDemo");
        try {
			WriteResponse cResponse = server.send(registration, request, TIMEOUT);
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| ClientSleepingException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
       // System.out.println(cResponse);
        
    }
    

    
    private LwM2mNode extractLwM2mNode(String target, HttpServletRequest req) throws IOException {
        String contentType = StringUtils.substringBefore(req.getContentType(), ";");
        if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            Vehicle_req_ID = content; 
            LwM2mNode node;
            try {
                node = gson.fromJson(content, LwM2mNode.class);
                System.out.println(node);
            } catch (JsonSyntaxException e) {
                throw new InvalidRequestException(e, "unable to parse json to tlv:%s", e.getMessage());
            }
            return node;
        } else if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            int rscId = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
            return LwM2mSingleResource.newStringResource(rscId, content);
        }
        throw new InvalidRequestException("content type %s not supported", req.getContentType());
    }
}
