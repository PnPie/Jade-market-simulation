package role;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import util.Registration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

/**
 * An interface to show all the related information of every supplier
 *
 * @author yu
 */
public class Observer extends Agent {

    private static final long serialVersionUID = 1L;
    private DFAgentDescription[] suppliers;

    protected void setup() {
        try {
            Registration.register(getAID(), "Observer", getName(), this);

            DFAgentDescription dfdTemplate = Registration.DFDTemplate("Supplier");

            // Searching for agents match the DF Description template
            suppliers = DFService.search(this, dfdTemplate);
            addBehaviour(new SendReq(this, 3000));
            addBehaviour(new GetInfo());
        } catch (FIPAException e) {
            e.printStackTrace();
            doDelete();
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class SendReq extends TickerBehaviour {

        public SendReq(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setConversationId("observe");
            for (int i = 0; i < suppliers.length; i++) {
                msg.addReceiver(suppliers[i].getName());
            }
            msg.setReplyWith("request" + System.currentTimeMillis());
            myAgent.send(msg);
        }

    }

    private class GetInfo extends CyclicBehaviour {

        private MessageTemplate mt;

        @Override
        public void action() {
            mt = MessageTemplate.MatchConversationId("observe");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                String content = msg.getContent();
                String[] res = content.split(",");
                System.out.println(content);
            } else {
                block();
            }
        }

    }

}
