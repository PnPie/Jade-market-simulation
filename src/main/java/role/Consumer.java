package role;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import util.Registration;

public class Consumer extends Agent {

	private static final int Jour = 2000;
	private static final int ChangementJour = 10;
	private AID supplierAID;
	private int KWhPerDay;
	private int countJour;
	private DFAgentDescription[] suppliersDFD;
	
	/*
	 * invoked while an agent is created
	 * 
	 * @see jade.core.Agent#setup()
	 */
	protected void setup() {
		countJour = 0;
		KWhPerDay = (int)(Math.random() * 10 + 5);
		supplierAID = new AID();
		try {
			Registration.register(getAID(), "Consumer", getName(), this);

			// Create a DF Description template
			DFAgentDescription dfdTemplate = Registration.DFDTemplate("Supplier");
			
			// Searching for agents matching the DF Description template
			suppliersDFD = DFService.search(this, dfdTemplate);
			
			if (suppliersDFD != null && suppliersDFD.length > 0) {
				//System.out.println(getLocalName() + ": Fournisseurs  found.");
				addBehaviour(new Subscribe(suppliersDFD));
				addBehaviour(new SendConsumption(this, Jour));
				addBehaviour(new ReceivingBill());
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
	 *
	 */
	private class Subscribe extends Behaviour {

		private int step = 0;
		private int bestPrice;
		private AID bestSupplier;
		private int replyCounts = 0;
		private DFAgentDescription[] suppliers;
		private MessageTemplate mt;
		
		private Subscribe(DFAgentDescription[] dfdTab) {
			suppliers = dfdTab;
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
				 *  Send messages to every supplier to ask for the price
				 */
				ACLMessage msg1 = new ACLMessage(ACLMessage.CFP);
				msg1.setContent("PriceRequest");
				msg1.setConversationId("sub");
				for (int i = 0; i < suppliers.length; i++) {
					msg1.addReceiver(suppliers[i].getName());
				}
				msg1.setReplyWith("cfp" + System.currentTimeMillis());
				myAgent.send(msg1);
				mt = MessageTemplate.MatchInReplyTo(msg1.getReplyWith());
				//System.out.println(getLocalName() + ": send a price request to fournisseurs");
				step = 1;
				break;
			case 1:
				/*
				 * TODO: Choose the supplier which providing the lowest price
				 * In this moment, we do it randomly :)
				 */
				ACLMessage msg2 = myAgent.receive(mt);
				if (msg2 != null) {
					if (msg2.getPerformative() == ACLMessage.PROPOSE) {
						int priceReceived = Integer.parseInt(msg2.getContent());
						if(bestSupplier == null || priceReceived < bestPrice){
							bestPrice = priceReceived;
							bestSupplier = msg2.getSender();
						}
						replyCounts++;
						if(replyCounts >= suppliers.length){
							int i = (int) (Math.random() * suppliers.length);
							supplierAID = suppliers[i].getName();
							System.out.println(getLocalName() + ": subscribe to " + supplierAID.getLocalName());
							step = 2;
						}
					}
				} else {
					block();
				}
				break;
			case 2:
				/*
				 * Tell the supplier how much electricity he consumes every day
				 */
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(supplierAID);
				order.setContent(Integer.toString(KWhPerDay));
				order.setConversationId("sub");
				myAgent.send(order);
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
	 *
	 */
	private class SendConsumption extends TickerBehaviour {
		
		private static final long serialVersionUID = 1L;

		public SendConsumption(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			if(supplierAID != null){
				int cons = KWhPerDay - 3 + (int)(Math.random() * 6);
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent(Integer.toString(cons));
				msg.setConversationId("cons");
				msg.addReceiver(supplierAID);
				myAgent.send(msg);
				countJour++;
				if(countJour >= ChangementJour){
					ACLMessage msgQuit = new ACLMessage(ACLMessage.CANCEL);
					msgQuit.setConversationId("quit");
					msgQuit.addReceiver(supplierAID);
					msgQuit.setReplyWith("Cancel" + System.currentTimeMillis());
					myAgent.send(msgQuit);
					myAgent.addBehaviour(new Subscribe(suppliersDFD));
					countJour = 0;
				}
			}
		}
		
	}

	/**
	 * The CyclicBehaviour never terminate and its action() method
	 * executes each time when it is called, and its done() method
	 * returns always false.
	 * 
	 * @author yu
	 *
	 */
	private class ReceivingBill extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("facturation");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
				String content = msg.getContent();
				System.out.println(getLocalName() + ": receive " + content);
			} else {
				block();
			}
		}
	}
	
}
