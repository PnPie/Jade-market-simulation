	import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * An interface to show all the related information of every supplier
 * 
 * @author yu
 *
 */
public class Observateur extends Agent {

	private static final long serialVersionUID = 1L;
	private static final int mois = 6000;
	private DFAgentDescription[] resTab;
	private ObservateurGUI gui;
	
	protected void setup() {
		// Create DF Description
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Observateur");
		sd.setName(getName());
		dfd.addServices(sd);
		try {
			// Register in DF
			DFService.register(this, dfd);
			DFAgentDescription templatedfd = new DFAgentDescription();
			ServiceDescription templatesd = new ServiceDescription();
			templatesd.setType("Fournisseur");
			templatedfd.addServices(templatesd);
			// Searching for agents match the DF Description template
			resTab = DFService.search(this, templatedfd);
			gui = new ObservateurGUI();
			addBehaviour(new SendReq(this,mois));
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

		private static final long serialVersionUID = 1L;

		public SendReq(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setConversationId("observe");
			for (int i = 0; i < resTab.length; i++) {
				msg.addReceiver(resTab[i].getName());
			}
			msg.setReplyWith("request" + System.currentTimeMillis());
			myAgent.send(msg);
		}
		
	}
	
	private class GetInfo extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt;

		@Override
		public void action() {
			mt = MessageTemplate.MatchConversationId("observe");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
				String content = msg.getContent();
				String[] res = content.split(",");
				System.out.println(content);
				gui.refresh(res);
			} else {
				block();
			}
		}
		
	}

}

class ObservateurGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final int nomFournisseur = 4;
	private static String[] title = {"Supplier", "Number of clients", "Amount sold", "Number of private transport(in service/total)", "Private transport construction cost", "Public transport cost", "Turnover", "Profit"};
	private DefaultTableModel model;
	private JScrollPane scrollPane;
	private JTable table;
	private Vector<String> titleVector;
	private Vector<Vector<String> > dataVector;
	
	public ObservateurGUI() {
		super("Observateur");
		titleVector = new Vector<>();
		for (int i = 0; i < title.length; i++) {
			titleVector.add(title[i]);
		}
		dataVector = new Vector<>();
		for (int i = 0; i < nomFournisseur; i++) {
			dataVector.add(new Vector<String>());
		}
		model = new DefaultTableModel(dataVector,titleVector);
		table = new JTable(model);
		table.setPreferredScrollableViewportSize(new Dimension(1000, 400));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		scrollPane = new JScrollPane(table);
		this.getContentPane().add(scrollPane, BorderLayout.CENTER);
		this.pack();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}
	
	public void refresh(String[] tab) {
		if (tab[0].equals("F1")) {
			dataVector.elementAt(0).clear();
			for (int i = 0; i < tab.length; i++) {
				dataVector.elementAt(0).add(tab[i]);
			}
			table.repaint();
			table.updateUI();
		}
		if (tab[0].equals("F2")) {
			dataVector.elementAt(1).clear();
			for (int i = 0; i < tab.length; i++) {
				dataVector.elementAt(1).add(tab[i]);
			}
			table.repaint();
			table.updateUI();
		}
		if (tab[0].equals("F3")) {
			dataVector.elementAt(2).clear();
			for (int i = 0; i < tab.length; i++) {
				dataVector.elementAt(2).add(tab[i]);
			}
			table.repaint();
			table.updateUI();
		}
		if (tab[0].equals("F4")) {
			dataVector.elementAt(3).clear();
			for (int i = 0; i < tab.length; i++) {
				dataVector.elementAt(3).add(tab[i]);
			}
			table.repaint();
			table.updateUI();
		}
	}

}