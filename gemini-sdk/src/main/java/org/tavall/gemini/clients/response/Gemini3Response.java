package org.tavall.gemini.clients.response;


import org.tavall.gemini.clients.response.metadata.ClientResponseMetadata;


public class Gemini3Response<T> {

    private T response;
    private ClientResponseMetadata responseMetadata;

   public Gemini3Response(T response, ClientResponseMetadata responseMetadata) {
       this.response = response;
       this.responseMetadata = responseMetadata;
   }
    public Gemini3Response(T response) {
        this.response = response;

    }
   public T getResponse() {
       return response;
   }

   public void setResponse(T response) {
       this.response = response;
   }

   public ClientResponseMetadata getClientResponseMetadata() {
       return responseMetadata;
   }





}