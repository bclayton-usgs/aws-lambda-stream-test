package com.example.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StreamTest implements RequestStreamHandler {

  static final Gson GSON;
  
  static {
    GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
    LambdaLogger logger = context.getLogger();
    
    JsonObject responseJson = new JsonObject();
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));

      JsonParser parser = new JsonParser();
      JsonObject requestJson = parser.parse(reader).getAsJsonObject();
      
      Request request = processRequest(requestJson); 
      
      responseJson.addProperty("statusCode", "200");

      Response response = new Response(request);
      responseJson.addProperty("body", GSON.toJson(response, Response.class));
    } catch (Exception e) {
      responseJson.addProperty("statusCode", "400");
      responseJson.addProperty("body", GSON.toJson(new Error(e), Error.class));
    }

    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
    logger.log("\n\n" + responseJson.toString() + "\n\n");
    writer.write(responseJson.toString());
    writer.close();
  }
  
  static Request processRequest(JsonObject requestJson) {
    JsonObject params;
    
    if (requestJson.get("queryStringParameters") != null) {
      params = requestJson.get("queryStringParameters").getAsJsonObject();
    } else if (requestJson.get("body") != null) {
      params = requestJson.get("body").getAsJsonObject();
    } else {
      throw new RuntimeException("No query parameters or body found");
    }
   
    if (params.get("firstName") == null || params.get("lastName") == null) {
      throw new RuntimeException("firstName or lastName not found");
    }
    
    return GSON.fromJson(params, Request.class);
  }
  
  static class Request {
    String firstName;
    String lastName;

    String name() {
      return firstName + " " + lastName;
    }
  }

  static class Response {
    String status = "Success";
    Request request;
    String firstName;
    String lastName;
    String name;

    Response(Request request) {
      this.request = request;
      firstName = request.firstName;
      lastName = request.lastName;
      name = request.name();
    }
  }

  static class Error {
    String status = "Error";
    String message;
    String stackTrace;
    
    Error(Throwable e) {
      message = e.getMessage();
      stackTrace = Throwables.getStackTraceAsString(e);
    }
  }

}
