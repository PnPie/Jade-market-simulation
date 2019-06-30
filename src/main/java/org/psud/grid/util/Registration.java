package org.psud.grid.util;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public final class Registration {
    public static DFAgentDescription DFDTemplate(String type) {
        // Create a DF Description template
        DFAgentDescription dfdTemplate = new DFAgentDescription();
        ServiceDescription sdTemplate = new ServiceDescription();
        sdTemplate.setType(type);
        dfdTemplate.addServices(sdTemplate);
        return dfdTemplate;
    }

    public static void register(AID aid, String type, String name, Agent agent) throws FIPAException {
        // Create DF(Directory Facilitator) Description for this agent
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(aid);
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(name);
        dfd.addServices(sd);
        // Register this agent in Directory Facilitator Service
        DFService.register(agent, dfd);
    }

    public static void deregister(Agent agent) throws FIPAException {
        DFService.deregister(agent);
    }
}
