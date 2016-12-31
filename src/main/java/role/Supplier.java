package role;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import util.Registration;

import java.util.*;

public class Supplier extends Agent {

    private static final float eletricityCost = 8.0f;
    private float electricityPrice;  // per kWh
    private static float transportCost = 0.2f;  // per kWh
    private float profit;
    private float profitSeason;
    private int seasonCnt;
    private float balance;
    private Set<AID> clientSet = new HashSet<AID>();

    /**
     * invoked when an agent starts
     */
    protected void setup() {
        electricityPrice = (float) (Math.random() * 10 + 10);
        try {
            Registration.register(getAID(), "Supplier", getName(), this);

            addBehaviour(new WaitForSubscription(this));
            addBehaviour(new ReceiveConsumption(this));
            addBehaviour(new sentToObserver(this));
        } catch (FIPAException exception) {
            exception.printStackTrace();
            doDelete();
        }
    }

    /**
     * invoked when an agent terminates
     *
     * @see jade.core.Agent#takeDown()
     */
    protected void takeDown() {
        try {
            Registration.deregister(this);
        } catch (FIPAException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Waiting for agents to subscribe, and decide to use
     * public transport or to construct a private one,
     * and also deal with the cancel demand of a subscriber.
     *
     * @author yu
     */
    private class WaitForSubscription extends CyclicBehaviour {

        private MessageTemplate msgTemplate;

        private WaitForSubscription(Agent a) {
            super(a);
            msgTemplate = MessageTemplate.or(MessageTemplate.MatchConversationId("subscription"),
                    MessageTemplate.MatchConversationId("quit"));
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(msgTemplate);
            if (msg != null) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();
                if (msg.getPerformative() == ACLMessage.CFP) {
                    if (content.indexOf("PriceRequest") != -1) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(Float.toString(electricityPrice));
                        myAgent.send(reply);
                    }
                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    clientSet.add(msg.getSender());
                    System.out.println(msg.getSender().getLocalName() + " subscribe to " + getLocalName());
                } else if (msg.getPerformative() == ACLMessage.CANCEL) {
                    if (clientSet.contains(msg.getSender()))
                        clientSet.remove(msg.getSender());
                } else {
                    block();
                }
            }
        }
    }

    /**
     * Receive the consumption volume of a subscriber and update the Set
     *
     * @author yu
     */
    private class ReceiveConsumption extends CyclicBehaviour {

        private MessageTemplate mt;

        public ReceiveConsumption(Agent a) {
            super(a);
            mt = MessageTemplate.MatchConversationId("consumption");
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    String content = msg.getContent();
                    float cons = Float.parseFloat(content);
                    if (clientSet.contains(msg.getSender())) {
                        profit += cons - eletricityCost - transportCost;
                        balance += cons - eletricityCost - transportCost;
                    }
                }
            } else {
                block();
            }
        }

    }

    /**
     * Send all the information to the observer
     *
     * @author yu
     */
    private class sentToObserver extends CyclicBehaviour {

        private MessageTemplate mt;
        private int clientSize;

        public sentToObserver(Agent a) {
            super(a);
            mt = MessageTemplate.MatchConversationId("observe");
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                ACLMessage reply = msg.createReply();
                clientSize = clientSet.size();
                String content = getLocalName() + ": (Clients number: " + clientSize
                        + ", Profit: " + profit + ", Balance:" + balance + ")";
                reply.setContent(content);
                reply.setPerformative(ACLMessage.INFORM);
                myAgent.send(reply);
                if (seasonCnt < 4) {
                    profitSeason += profit;
                    seasonCnt++;
                } else {
                    if (profitSeason > 0)
                        electricityPrice *= 1.1;
                    else
                        electricityPrice *= 0.9;
                    profitSeason = 0;
                    seasonCnt = 0;
                }
                profit = 0f;
            }
        }

    }

}