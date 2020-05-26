/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bank.paymentmgmt.facade;

import com.bank.paymentmgmt.domain.Payment;
import java.io.StringReader;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author theo
 */
@Path("payment")
@RequestScoped
public class PaymentResource {

    @EJB
    private BankingServiceRemote bankingService;    // Use @EJB for inject remote view because @Inject link to CDI spec do not allow remoting.

    /**
     * Creates a new instance of PaymentResource
     */
    public PaymentResource() {}

    @POST   // On ne connait pas l’URI du paiement à créer on préfère invoquer le service Web au travers d’une requête POST 
    @Consumes(MediaType.APPLICATION_JSON)   //données du corps de la requête POST
    public Response pay(String content) {
        //affichage du corps de la requête POST.
        // System.out.println(content);
        StringReader reader = new StringReader(content);
        String ccNumber;
        Double amount;
        
        try (JsonReader jreader = Json.createReader(reader)) {
            JsonObject paymentInfo = jreader.readObject();
            ccNumber = paymentInfo.getString("ccNumber");
            amount = paymentInfo.getJsonNumber("amount").doubleValue();
        }
        
        // méthode createPayment du Session Bean en lui passant en paramètre le numéro de carte bleue et le montant 
        Boolean isValid = !!bankingService.createPayment(ccNumber, amount);
        
        //retour d'une réponse sans corps indiquant un statut 202 : la requête a été acceptée mais le processus n'est pas terminé
        //return Response.accepted().build();
        Response resp = isValid ? 
                Response.accepted().build() : 
                Response.status(400).entity("n° CB invalide").build();
        /*
        if(isValid){
            resp = Response.accepted().build();
        }else{
            resp = Response.status(400).entity("n° CB invalide").build();
        }
        */
        return resp;
    }
    
    @Path("payments")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getStoredPayments(){
        //récupération de tous les ordres de paiement stockés
        List<Payment> storedPayments = bankingService.lookupAllStoredPayments();
        
        //création d'une entité générique pour pouvoir mapper un type paramétré  //(List<Payment>) avec  un corps de réponse
        GenericEntity<List<Payment>> genericList = new GenericEntity<List<Payment>>(storedPayments){};
        
        //construction de la réponse embarquant dans son corps les ordres de paiements
        Response resp = Response.ok(genericList).build();
        return resp;
    }

    @Path("{id}")   //utilisation d'un template parameter dans le pattern d'URI
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getStoredPayment(@PathParam("id") Long paymentId){ //@PathParam permet d'extraire la valeur du template parameter 

        Payment storedPayment = bankingService.lookupStoredPayment(paymentId);
       
       //si aucun ordre de paiment correspondant à l'id n'est stocké
       if(storedPayment == null){
           //exception mappée avec un code d'erreur 404
            throw new NotFoundException();
       }
       return Response.ok(storedPayment).build();
    }
    
    @Path("{id}")
    @DELETE
    public Response cancelStoredPayment(@PathParam("id") Long paymentId){
        Payment cancelledPayment = bankingService.deleteStoredPayment(paymentId);
        
        if(cancelledPayment == null){           //si aucun ordre de paiment correspondant à l'id n'est stocké
            throw new NotFoundException();      //exception mappée avec un code d'erreur 404
        }
        
        return Response.noContent().build();    //réponse vide indiquant un succès de l'opération
    }
}
