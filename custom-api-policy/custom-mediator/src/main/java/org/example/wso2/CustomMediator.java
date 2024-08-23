package org.example.wso2;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class CustomMediator extends AbstractMediator {

    @Override
    public boolean mediate(MessageContext context) {
        // Your custom mediation logic goes here
        System.out.println("Custom Mediator executed!");

        // You can modify the message context here
        String body = context.getEnvelope().getBody().toString();
        System.out.println("Body: " + body);
        context.setProperty("CUSTOM_PROPERTY", "+++++++ custom property +++++");

        // Return true to indicate successful mediation
        return true;
    }
}