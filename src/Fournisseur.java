import java.util.Hashtable;
import java.util.Iterator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Fournisseur extends Agent {

	private static final long serialVersionUID = 1L;
	private static final int prixTransPrivConstruit = 240;
	private Hashtable<AID, Integer> clientTransCom;
	private Hashtable<AID, Integer> clientTransPriv;
	private int prixParKWh;
	private int prixTransComParKWh;
	private int fraisTransPrivParMois;
	private int nomTransPriv;
	private int nomTransPrivEnService;

	/*
	 * invoked when an agent starts
	 * 
	 * @see jade.core.Agent#setup()
	 */
	protected void setup() {
		prixTransComParKWh = 2;
		fraisTransPrivParMois = 0;
		nomTransPriv = 0;
		nomTransPrivEnService = 0;
		prixParKWh = (int)(Math.random() * 10 + 10);
		clientTransCom = new Hashtable<>();
		clientTransPriv = new Hashtable<>();
		// Create DF(Directory Facilitator) Description for him
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Fournisseur");
		sd.setName(getName());
		dfd.addServices(sd);
		try {
			// Register this agent in Directory Facilitator Service
			DFService.register(this, dfd);
			//System.out.println(getLocalName() + ": register in DF");
			addBehaviour(new WaitingSubscription(this));
			addBehaviour(new ReceiveConsumption(this));
			addBehaviour(new Observation(this));
		} catch (FIPAException e) {
			e.printStackTrace();
			doDelete();
		}
	}
	
	/*
	 * invoked when an agent terminates
	 * 
	 * @see jade.core.Agent#takeDown()
	 */
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waiting for agents to subscribe, and decide to use public transport or to construct a private one,
	 * and also deal with the cancel demand of a subscriber.
	 * 
	 * @author yu
	 *
	 */
	private class WaitingSubscription extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt;

		private WaitingSubscription(Agent a) {
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
						reply.setContent(Integer.toString(prixParKWh));
						myAgent.send(reply);
						//System.out.println(getLocalName() + ": send the price to " + msg.getSender().getLocalName() + ".");
					}
				} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					int moyenne = Integer.parseInt(content);
					if(prixTransComParKWh * moyenne * 12 <= prixTransPrivConstruit)
						clientTransCom.put(msg.getSender(), 0);
					else {
						clientTransPriv.put(msg.getSender(), 0);
						if (nomTransPrivEnService < nomTransPriv) {
							nomTransPrivEnService++;
						} else {
							fraisTransPrivParMois += prixTransPrivConstruit;
							nomTransPriv++;
							nomTransPrivEnService++;
						}
					}
					
					System.out.print(getLocalName() + ": La liste actuelle des consommateurs: ");
					Iterator<AID> it = clientTransCom.keySet().iterator();
					while(it.hasNext()){
						System.out.print(it.next().getLocalName() + " ");
					}
					it = clientTransPriv.keySet().iterator();
					while(it.hasNext()){
						System.out.print(it.next().getLocalName() + " ");
					}
					System.out.println();
					
				} else if (msg.getPerformative() == ACLMessage.CANCEL) {
					if(clientTransCom.containsKey(msg.getSender()))
						clientTransCom.remove(msg.getSender());
					if(clientTransPriv.containsKey(msg.getSender())) {
						clientTransPriv.remove(msg.getSender());
						nomTransPrivEnService--;
					}
				}else {
					block();
				}
			}
		}
	}

	/**
	 * Receive the consumption volume of a subscriber and update the Hashtable
	 * 
	 * @author yu
	 *
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
					if (clientTransCom.containsKey(msg.getSender())) {
						int current = clientTransCom.remove(msg.getSender());
						clientTransCom.put(msg.getSender(), current + cons);
						System.out.println(getLocalName() + ": " + msg.getSender().getLocalName() + " " + clientTransCom.get(msg.getSender()) + "KWh");
					}
					if (clientTransPriv.containsKey(msg.getSender())) {
						int current = clientTransPriv.remove(msg.getSender());
						clientTransPriv.put(msg.getSender(), current + cons);
						System.out.println(getLocalName() + ": " + msg.getSender().getLocalName() + " " + clientTransPriv.get(msg.getSender()) + "KWh");
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
	 *
	 */
	private class Observation extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt;
		private int nomClient;
		private int qteVendue;
		private int fraisTransCom;

		public Observation(Agent a) {
			super(a);
			mt = MessageTemplate.MatchConversationId("observe");
		}

		private int qteVendueCalcul(Hashtable<AID, Integer> ht1) {
			int count = 0;
			Iterator<AID> it = ht1.keySet().iterator();
			while(it.hasNext()) {
				count += ht1.get(it.next());
			}
			return count;
		}

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null && msg.getPerformative() == ACLMessage.REQUEST){
				ACLMessage reply = msg.createReply();
				nomClient = clientTransCom.size() + clientTransPriv.size();
				qteVendue = qteVendueCalcul(clientTransCom) + qteVendueCalcul(clientTransPriv);
				fraisTransCom = prixTransComParKWh * qteVendueCalcul(clientTransCom);
				reply.setContent(getLocalName() + "," + nomClient + "," + qteVendue + "," + nomTransPrivEnService + "/" + nomTransPriv + "," + fraisTransPrivParMois + "," + fraisTransCom + "," + prixParKWh * qteVendue + "," + (prixParKWh * qteVendue - fraisTransPrivParMois - fraisTransCom));
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
				fraisTransPrivParMois = 0;
			}
		}

	}

}