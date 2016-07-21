import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Supplier extends Agent {
    private static final long serialVersionUID = 1L;
    private static final int privateLineCost = 240;
    private int electricityPrice = (int) Math.random() * 10 + 10;  // per kWh
    private int transportCost = 2;  // per kWh
    private int otherCost;
    private int privateLineNumber;
    private int privateLineNumberInUse;
    private Map<AID, Integer> clientTransport = new HashMap<>();
    private Map<AID, Integer> clientPrivateLine = new HashMap<>();

    /**
     * invoked when an agent starts
     */
    protected void setup() {
        // Create DF(Directory Facilitator) Description for this agent
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Supplier");
        sd.setName(getName());
        dfd.addServices(sd);
        try {
            // Register this agent in Directory Facilitator Service
            DFService.register(this, dfd);
            //System.out.println(getLocalName() + ": register in DF");
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
            DFService.deregister(this);
        } catch (FIPAException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Waiting for agents to subscribe, and decide to use public transport or to construct a private one,
     * and also deal with the cancel demand of a subscriber.
     *
     * @author yu
     */
    private class WaitForSubscription extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;
        private MessageTemplate mt;

        private WaitForSubscription(Agent a) {
            super(a);
            mt = MessageTemplate.or(MessageTemplate.MatchConversationId("sub"), MessageTemplate.MatchConversationId("quit"));
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent(); // the KWhPerDay of the consumer
                ACLMessage reply = msg.createReply();
                if (msg.getPerformative() == ACLMessage.CFP) {
                    if (content.indexOf("PriceRequest") != -1) {
                        //System.out.println(getLocalName() + ": receive a price request from " + msg.getSender().getLocalName() + ".");
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(Integer.toString(electricityPrice));
                        myAgent.send(reply);
                        //System.out.println(getLocalName() + ": send the price to " + msg.getSender().getLocalName() + ".");
                    }
                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    int moyenne = Integer.parseInt(content);
                    if (transportCost * moyenne * 12 <= privateLineCost)
                        clientTransport.put(msg.getSender(), 0);
                    else {
                        clientPrivateLine.put(msg.getSender(), 0);
                        if (privateLineNumberInUse < privateLineNumber) {
                            privateLineNumberInUse++;
                        } else {
                            otherCost += privateLineCost;
                            privateLineNumber++;
                            privateLineNumberInUse++;
                        }
                    }

                    System.out.print(getLocalName() + ": La liste actuelle des consommateurs: ");
                    Iterator<AID> it = clientTransport.keySet().iterator();
                    while (it.hasNext()) {
                        System.out.print(it.next().getLocalName() + " ");
                    }
                    it = clientPrivateLine.keySet().iterator();
                    while (it.hasNext()) {
                        System.out.print(it.next().getLocalName() + " ");
                    }
                    System.out.println();

                } else if (msg.getPerformative() == ACLMessage.CANCEL) {
                    if (clientTransport.containsKey(msg.getSender()))
                        clientTransport.remove(msg.getSender());
                    if (clientPrivateLine.containsKey(msg.getSender())) {
                        clientPrivateLine.remove(msg.getSender());
                        privateLineNumberInUse--;
                    }
                } else {
                    block();
                }
            }
        }
    }

    /**
     * Receive the consumption volume of a subscriber and update the Hashtable
     *
     * @author yu
     */
    private class ReceiveConsumption extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;
        private MessageTemplate mt;

        public ReceiveConsumption(Agent a) {
            super(a);
            mt = MessageTemplate.MatchConversationId("cons");
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    String content = msg.getContent();
                    int cons = Integer.parseInt(content);
                    if (clientTransport.containsKey(msg.getSender())) {
                        int current = clientTransport.remove(msg.getSender());
                        clientTransport.put(msg.getSender(), current + cons);
                        System.out.println(getLocalName() + ": " + msg.getSender().getLocalName() + " " + clientTransport.get(msg.getSender()) + "KWh");
                    }
                    if (clientPrivateLine.containsKey(msg.getSender())) {
                        int current = clientPrivateLine.remove(msg.getSender());
                        clientPrivateLine.put(msg.getSender(), current + cons);
                        System.out.println(getLocalName() + ": " + msg.getSender().getLocalName() + " " + clientPrivateLine.get(msg.getSender()) + "KWh");
                    }
                }
            } else {
                block();
            }
        }

    }

    /**
     * Send all the information to the observator
     *
     * @author yu
     */
    private class sentToObserver extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;
        private MessageTemplate mt;
        private int nomClient;
        private int qteVendue;
        private int fraisTransCom;

        public sentToObserver(Agent a) {
            super(a);
            mt = MessageTemplate.MatchConversationId("observe");
        }

        private int qteVendueCalcul(Map<AID, Integer> ht1) {
            int count = 0;
            Iterator<AID> it = ht1.keySet().iterator();
            while (it.hasNext()) {
                count += ht1.get(it.next());
            }
            return count;
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                ACLMessage reply = msg.createReply();
                nomClient = clientTransport.size() + clientPrivateLine.size();
                qteVendue = qteVendueCalcul(clientTransport) + qteVendueCalcul(clientPrivateLine);
                fraisTransCom = transportCost * qteVendueCalcul(clientTransport);
                reply.setContent(getLocalName() + "," + nomClient + "," + qteVendue + "," + privateLineNumberInUse + "/" + privateLineNumber + "," + otherCost + "," + fraisTransCom + "," + electricityPrice * qteVendue + "," + (electricityPrice * qteVendue - otherCost - fraisTransCom));
                reply.setPerformative(ACLMessage.INFORM);
                myAgent.send(reply);
                otherCost = 0;
            }
        }

    }

}