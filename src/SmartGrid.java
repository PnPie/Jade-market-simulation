import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartGrid {
	
	private static final int nomFournisseur = 4;
	private static final int nomConsommateur = 10;
	
	SmartGrid() throws StaleProxyException {
		// a runtime object is an environment where JADE agents can "live"
		Runtime rt = Runtime.instance();
		rt.setCloseVM(true);
		
		// give runtime object a host and let it to be an agent container
		Profile pMain = new ProfileImpl("localhost",1111,null);
		AgentContainer mc = rt.createMainContainer(pMain);
		
		// create agents fournisseur in the container
		for (int i = 1; i <= nomFournisseur; i++) {
			mc.createNewAgent("F" + i, Fournisseur.class.getName(), new Object[0]).start();
		}
		
		// create agents consommateur in the container
		for (int i = 1; i <= nomConsommateur; i++) {
			mc.createNewAgent("C" + i, Consommateur.class.getName(), new Object[0]).start();
		}
		
		// create agent observateur in the container
		AgentController observateur = mc.createNewAgent("Observateur", Observateur.class.getName(), new Object[0]);
		observateur.start();
	}
	
	public static void main(String[] args) throws StaleProxyException {
		// launch the smart grid
		new SmartGrid();
	}
}