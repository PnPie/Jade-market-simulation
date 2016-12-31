package role;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import util.Registration;

public class Consumer extends Agent {

    private AID supplier;
    private float consumptionBase;
    private DFAgentDescription[] suppliersDFD;
    private int cnt;

    /*
     * invoked while an agent is created
     *
     * @see jade.core.Agent#setup()
     */
    protected void setup() {
        if (Math.random() < 0.2)
            consumptionBase = (float) (800 + Math.random() * 400);
        else
            consumptionBase = (float) (80 + Math.random() * 400);
        try {
            Registration.register(getAID(), "Consumer", getName(), this);

            // Create a DF Description template
            DFAgentDescription dfdTemplate = Registration.DFDTemplate("Supplier");

            // Searching for agents matching the DF Description template
            suppliersDFD = DFService.search(this, dfdTemplate);

            if (suppliersDFD != null && suppliersDFD.length > 0) {
                addBehaviour(new Subscribe(suppliersDFD));
                addBehaviour(new SendConsumption(this, 3000));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
            doDelete();
        }
    }

    /*
     * invoked just before an agent terminates and
     * intended to include agent clean-up operations.
     *
     * @see jade.core.Agent#takeDown()
     */
    protected void takeDown() {
        try {
            Registration.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to a supplier with the lowest provided price
     * and tell him how much electricity this consumer consumes every day
     *
     * @author yu
     */
    private class Subscribe extends Behaviour {

        private int step;
        private float bestPrice;
        private AID bestSupplier;
        private int proposalReceived = 0;
        private DFAgentDescription[] suppliersDFD;
        private MessageTemplate msgTemplateCFP;

        private Subscribe(DFAgentDescription[] dfdTab) {
            step = 0;
            suppliersDFD = dfdTab;
        }

        /*
         * The operations to be performed when the behavior is in execution
         *
         * @see jade.core.behaviours.Behaviour#action()
         */
        @Override
        public void action() {
            switch (step) {
                case 0:
                /*
                 *  Send messages to all suppliers to ask for the price
                 *  CFP: Call For Proposal
				 */
                    ACLMessage msgCFP = new ACLMessage(ACLMessage.CFP);
                    msgCFP.setContent("PriceRequest");
                    msgCFP.setConversationId("subscription");
                    for (int i = 0; i < suppliersDFD.length; i++) {
                        msgCFP.addReceiver(suppliersDFD[i].getName());
                    }
                    msgCFP.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(msgCFP);
                    msgTemplateCFP = MessageTemplate.MatchInReplyTo(msgCFP.getReplyWith());
                    step = 1;
                    break;
                case 1:
                /*
                 * Choose a supplier which providing the lowest price
                 * and other than the current one
				 */
                    ACLMessage msgProposal = myAgent.receive(msgTemplateCFP);
                    if (msgProposal != null) {
                        if (msgProposal.getPerformative() == ACLMessage.PROPOSE) {
                            float priceReceived = Float.parseFloat(msgProposal.getContent());
                            if (bestSupplier == null ||
                                    (priceReceived < bestPrice && supplier != msgProposal.getSender())) {
                                bestPrice = priceReceived;
                                bestSupplier = msgProposal.getSender();
                            }
                            proposalReceived++;
                            if (proposalReceived >= suppliersDFD.length) {
                                supplier = bestSupplier;
                                step = 2;
                            }
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                /*
                 * Tell the supplier to subscribe
				 */
                    ACLMessage subscription = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    subscription.addReceiver(supplier);
                    subscription.setContent(Float.toString(consumptionBase));
                    subscription.setConversationId("subscription");
                    myAgent.send(subscription);
                    step = 3;
                    break;
            }
        }

        /*
         * the done() method specifies whether or not a behavior has completed
         * and have to be removed from the pool of behaviors
         *
         * @see jade.core.behaviours.Behaviour#done()
         */
        @Override
        public boolean done() {
            return step == 3;
        }

    }

    /**
     * Send the consumed electricity to the supplier periodically,
     * and change the supplier after a certain period.
     *
     * @author yu
     */
    private class SendConsumption extends TickerBehaviour {

        public SendConsumption(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (supplier != null) {
                float consumption = (float) (consumptionBase * Math.random() * 0.2 + 0.9);
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent(Float.toString(consumption));
                msg.setConversationId("consumption");
                msg.addReceiver(supplier);
                myAgent.send(msg);
                cnt++;
                if (cnt >= 4) {
                    ACLMessage msgQuit = new ACLMessage(ACLMessage.CANCEL);
                    msgQuit.setConversationId("quit");
                    msgQuit.addReceiver(supplier);
                    msgQuit.setReplyWith("Cancel" + System.currentTimeMillis());
                    myAgent.send(msgQuit);
                    myAgent.addBehaviour(new Subscribe(suppliersDFD));
                    cnt = 0;
                }
            }
        }

    }
}
