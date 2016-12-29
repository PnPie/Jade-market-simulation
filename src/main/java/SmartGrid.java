import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartGrid {

    private static final int supplierNumber = 4;
    private static final int consumerNumber = 10;

    SmartGrid() {
        /**
         * Runtime is an singleton instance which represents the JADE runtime system
         */
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile profile = new ProfileImpl("localhost", 1111, "my jade main container", true);
        AgentContainer mc = rt.createMainContainer(profile);

        // create different agents in the main container
        try {
            for (int i = 1; i <= supplierNumber; i++)
                mc.createNewAgent("Supplier_" + i, Supplier.class.getName(), new Object[0]).start();
            for (int i = 1; i <= consumerNumber; i++)
                mc.createNewAgent("Consumer_" + i, Consommateur.class.getName(), new Object[0]).start();
            AgentController observer = mc.createNewAgent("Observateur", Observateur.class.getName(), new Object[0]);
            observer.start();
        } catch (StaleProxyException exception) {
            System.err.println("main container proxy unavailable.");
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws StaleProxyException {
        new SmartGrid();
    }
}