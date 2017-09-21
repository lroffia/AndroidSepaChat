/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.android.api;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.android.util.LogManager;
import it.unibo.arces.wot.sepa.android.util.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.android.api.SPARQL11SEProperties.SPARQL11SEPrimitive;
import it.unibo.arces.wot.sepa.android.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.android.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.android.commons.protocol.SPARQL11Properties.QueryResultsFormat;
import it.unibo.arces.wot.sepa.android.commons.protocol.SPARQL11Properties.SPARQLPrimitive;

import it.unibo.arces.wot.sepa.android.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.android.commons.request.RegistrationRequest;
import it.unibo.arces.wot.sepa.android.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.android.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.android.commons.request.UpdateRequest;

import it.unibo.arces.wot.sepa.android.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.android.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.android.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.android.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.android.commons.response.Response;
import it.unibo.arces.wot.sepa.android.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.android.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.android.commons.response.UpdateResponse;

public class SPARQL11SEProtocol extends SPARQL11Protocol {
	private static final Logger logger = LogManager.getLogger("SPARQL11SEProtocol");

	private Websocket wsClient;
	private Websocket wssClient;

	private SSLSecurityManager sm = new SSLSecurityManager("TLSv1","sepa.jks","sepa2017","sepa2017");
	
	protected SPARQL11SEProperties properties = null;

	public SPARQL11SEProtocol(SPARQL11SEProperties properties)
			throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		this(properties,null, "sepa.jks", "sepa2017");
	}
	
	public SPARQL11SEProtocol(SPARQL11SEProperties properties,INotificationHandler handler)
			throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		this(properties,handler, "sepa.jks", "sepa2017");
	}

	public SPARQL11SEProtocol(SPARQL11SEProperties properties, INotificationHandler handler,String jksName, String jksPassword)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(properties);

		if (properties == null) {
			logger.fatal("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}

		this.properties = properties;

		// Create secure HTTP client
		httpclient = sm.getSSLHttpClient();

		// Create WebSocket clients (secure and not)
		wsClient = new Websocket(
				"ws://" + properties.getHost() + ":" + properties.getWsPort() + properties.getSubscribePath(), false,handler);
		wssClient = new Websocket("wss://" + properties.getHost() + ":" + properties.getWssPort()
				+ properties.getSecurePath() + properties.getSubscribePath(), true,handler);

		// HTTP response handler
		responseHandler = new ResponseHandler<String>() {
			@Override
			public String handleResponse(final HttpResponse response) {
				String body = null;

				HttpEntity entity = response.getEntity();

				try {
					body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
				} catch (ParseException e) {
					body = e.getMessage();
				} catch (IOException e) {
					body = e.getMessage();
				}
				return body;
			}
		};
	}

	// SPARQL 1.1 Update Primitive
	public Response update(UpdateRequest request) {
		logger.debug(request.toString());
		return super.update(request);
	}

	// SPARQL 1.1 Query Primitive
	public Response query(QueryRequest request) {
		logger.debug(request.toString());
		return super.query(request);
	}

	// SPARQL 1.1 SE Subscribe Primitive
	public Response subscribe(SubscribeRequest request)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug(request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SUBSCRIBE, request);
	}

	// SPARQL 1.1 SE Unsubscribe Primitive
	public Response unsubscribe(UnsubscribeRequest request) throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug(request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.UNSUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Subscribe Primitive
	public Response secureSubscribe(SubscribeRequest request)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECURESUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Unsubscribe Primitive
	public Response secureUnsubscribe(UnsubscribeRequest request)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUNSUBSCRIBE, request);
	}

	// SPARQL 1.1 SE SECURE Update Primitive
	public Response secureUpdate(UpdateRequest request) throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREUPDATE, request);
	}

	// SPARQL 1.1 SE SECURE Query Primitive
	public Response secureQuery(QueryRequest request) throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("SECURE " + request.toString());
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.SECUREQUERY, request);
	}

	// Registration to the Authorization Server (AS)
	public Response register(String identity) throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		logger.debug("REGISTER " + identity);
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REGISTER, identity);
	}

	// Token request to the Authorization Server (AS)
	public Response requestToken() throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		return executeSPARQL11SEPrimitive(SPARQL11SEPrimitive.REQUESTTOKEN);
	}

	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		return executeSPARQL11SEPrimitive(op, null);
	}

	protected Response executeSPARQL11SEPrimitive(SPARQL11SEPrimitive op, Object request)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		// Create the HTTPS request
		URI uri;
		String path = null;
		int port = 0;

		// Headers and body
		String contentType = null;
		ByteArrayEntity body = null;
		String accept = null;
		String authorization = null;

		switch (op) {
		case SUBSCRIBE:
			SubscribeRequest subscribe = (SubscribeRequest) request;
			wsClient.subscribe(subscribe.getSPARQL(), subscribe.getAlias(), null);

			return new SubscribeResponse();
		case UNSUBSCRIBE:
			UnsubscribeRequest unsubscribe = (UnsubscribeRequest) request;
			wsClient.unsubscribe(unsubscribe.getSubscribeUUID(), null);

			return new UnsubscribeResponse();
		case SECURESUBSCRIBE:
			SubscribeRequest securesubscribe = (SubscribeRequest) request;
			wssClient.subscribe(securesubscribe.getSPARQL(), securesubscribe.getAlias(), properties.getAccessToken());

			return new SubscribeResponse();
		case SECUREUNSUBSCRIBE:
			UnsubscribeRequest secureunsubscribe = (UnsubscribeRequest) request;
			wssClient.unsubscribe(secureunsubscribe.getSubscribeUUID(), properties.getAccessToken());

			return new SubscribeResponse();
		default:
			break;
		}

		switch (op) {
		case REGISTER:
			path = properties.getRegisterPath();
			port = properties.getHttpsPort();

			accept = "application/json";
			contentType = "application/json";
			String identity = (String) request;

			body = new ByteArrayEntity(new RegistrationRequest(identity).toString().getBytes("UTF-8"));
			break;
		case REQUESTTOKEN:
			String basic = properties.getBasicAuthorization();
			if (basic == null)
				return new ErrorResponse(0, 401, "Basic authorization in null. Register first");

			path = properties.getTokenRequestPath();
			port = properties.getHttpsPort();

			authorization = "Basic " + properties.getBasicAuthorization();
			contentType = "application/json";
			accept = "application/json";
			break;
		case SECUREUPDATE:
			path = properties.getSecurePath() + properties.getUpdatePath();
			port = properties.getHttpsPort();

			accept = "text/plain";
			contentType = "application/x-www-form-urlencoded";
			authorization = "Bearer " + properties.getAccessToken();

			String encodedContent = URLEncoder.encode(((UpdateRequest) request).getSPARQL(), "UTF-8");
			body = new ByteArrayEntity(("update=" + encodedContent).getBytes());
			body.setContentType(contentType);
			break;
		case SECUREQUERY:
			path = properties.getSecurePath() + properties.getQueryPath();
			port = properties.getHttpsPort();

			accept = "application/sparql-results+json";
			contentType = "application/sparql-query";
			authorization = "Bearer " + properties.getAccessToken();

			body = new ByteArrayEntity(((QueryRequest) request).getSPARQL().getBytes("UTF-8"));
			break;
		default:
			break;
		}

		// POST request
		uri = new URI("https", null, properties.getHost(), port, path, null, null);

		HttpUriRequest httpRequest = new HttpPost(uri);

		if (contentType != null)
			httpRequest.setHeader("Content-Type", contentType);
		if (accept != null)
			httpRequest.setHeader("Accept", accept);
		if (authorization != null)
			httpRequest.setHeader("Authorization", authorization);
		if (body != null)
			((HttpPost) httpRequest).setEntity(body);

		logger.debug("Request: " + httpRequest);

		// HTTP request execution
		String response = null;

		long timing = System.nanoTime();

		response = httpclient.execute(httpRequest, responseHandler);

		timing = System.nanoTime() - timing;

		if (op.equals(SPARQL11SEPrimitive.REGISTER))
			logger.info("REGISTER " + timing / 1000000 + " ms");
		else if (op.equals(SPARQL11SEPrimitive.REQUESTTOKEN))
			logger.info("TOKEN " + timing / 1000000 + " ms");
		else if (op.equals(SPARQL11SEPrimitive.SECUREQUERY))
			logger.info("SECURE_QUERY " + timing / 1000000 + " ms");
		else if (op.equals(SPARQL11SEPrimitive.SECUREUPDATE))
			logger.info("SECURE_UPDATE " + timing / 1000000 + " ms");

		logger.debug(response);

		// Parsing the response
		return parseSPARQL11SEResponse(response, op);
	}

	/**
	 * Parse SPARQL 1.1 Query and Update
	 * 
	 * Update and error responses are serialized as JSON objects:
	 * 
	 * {"code": HTTP Return Code, "body": "Message body"}
	 */

	@Override
	protected Response parseEndpointResponse(int token, String jsonResponse, SPARQLPrimitive op,
			QueryResultsFormat format) {
		if (token != -1)
			logger.debug("Parse endpoint response #" + token + " " + jsonResponse);
		else
			logger.debug("Parse endpoint response " + jsonResponse);

		JsonObject json = null;
		try {
			json = new JsonParser().parse(jsonResponse).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			// An update response is not forced to be JSON
			if (op.equals(SPARQLPrimitive.UPDATE))
				return new UpdateResponse(token, jsonResponse);

			logger.error(e.getMessage());
			return new ErrorResponse(token, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		if (json.get("code") != null) {
			if (json.get("code").getAsInt() >= 400)
				return new ErrorResponse(token, json.get("code").getAsInt(), json.get("body").getAsString());
		}

		if (op.equals(SPARQLPrimitive.UPDATE))
			return new UpdateResponse(token, json.get("body").getAsString());
		if (op.equals(SPARQLPrimitive.QUERY))
			return new QueryResponse(token, json);

		return new ErrorResponse(token, HttpStatus.SC_INTERNAL_SERVER_ERROR, jsonResponse);
	}

	protected Response parseSPARQL11SEResponse(String response, SPARQL11SEPrimitive op) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (response == null)
			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response is null");

		JsonObject json = null;
		try {
			json = new JsonParser().parse(response).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown response: " + response);
		}

		// Error response
		if (json.get("code") != null)
			if (json.get("code").getAsInt() >= 400)
				return new ErrorResponse(0, json.get("code").getAsInt(), json.get("body").getAsString());

		if (op == SPARQL11SEPrimitive.SECUREQUERY)
			return new QueryResponse(json);
		if (op == SPARQL11SEPrimitive.SECUREUPDATE)
			return new UpdateResponse(response);

		if (op == SPARQL11SEPrimitive.REGISTER) {
			if (json.get("client_id") != null && json.get("client_secret") != null) {
				try {
					properties.setCredentials(json.get("client_id").getAsString(),
							json.get("client_secret").getAsString());
				} catch (IOException e) {
					return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to save credentials");
				}

				return new RegistrationResponse(json.get("client_id").getAsString(),
						json.get("client_secret").getAsString(), json.get("signature"));
			}
			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR,
					"Credentials not found in registration response");
		}

		if (op == SPARQL11SEPrimitive.REQUESTTOKEN) {
			if (json.get("access_token") != null && json.get("expires_in") != null && json.get("token_type") != null) {
				int seconds = json.get("expires_in").getAsInt();
				Date expires = new Date();
				expires.setTime(expires.getTime() + (1000 * seconds));
				try {
					properties.setJWT(json.get("access_token").getAsString(), expires,
							json.get("token_type").getAsString());
				} catch (IOException e) {
					return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to save JWT");
				}
				return new JWTResponse(json.get("access_token").getAsString(), json.get("token_type").getAsString(),
						json.get("expires_in").getAsLong());
			} else if (json.get("code") != null && json.get("body") != null)
				return new ErrorResponse(0, json.get("code").getAsInt(), json.get("body").getAsString());
			else if (json.get("code") != null)
				return new ErrorResponse(0, json.get("code").getAsInt(), "");

			return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR,
					"Response not recognized: " + json.toString());
		}

		return new ErrorResponse(0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response unknown: " + response);
	}

	
	public void setNotificationHandler(INotificationHandler handler) {
		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new IllegalArgumentException("Notificaton handler is null");
		}
		
		wsClient.setNotificationHandler(handler);
		wssClient.setNotificationHandler(handler);
	}
}
