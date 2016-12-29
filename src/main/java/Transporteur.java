import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class Transporteur extends Agent {

	private static final long serialVersionUID = 1L;
	
	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Transporteur");
		sd.setName(getName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
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
	
	private class Delivery extends CyclicBehaviour {

		private MessageTemplate mt;
		
		public Delivery(Agent a) {
			super(a);
			mt = MessageTemplate.MatchConversationId("delivery");
		}

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					String content = msg.getContent();
					int cons = Integer.parseInt(content);
					ACLMessage reply = msg.createReply();
					myAgent.send(reply);
				}
			}
			
		}
		
	}

}
