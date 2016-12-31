import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import role.Consumer;
import role.Observer;
import role.Supplier;

public class SmartGrid {

    /**
     * create a {@link AgentContainer} main container
     * which contains the other agents
     *
     * @return
     */
    private AgentContainer createMC() {
        // Runtime is an singleton instance which represents the JADE runtime system
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile profile = new ProfileImpl("localhost", 1111, "my jade main container", true);
        AgentContainer mc = rt.createMainContainer(profile);
        return mc;
    }


    /**
     * add agent to the main container
     * @param mc
     * @param agentName
     * @param classObject
     * @throws StaleProxyException
     */
    private void addAgent(AgentContainer mc, String agentName, Class classObject)
            throws StaleProxyException {
        mc.createNewAgent(agentName, classObject.getName(), new Object[0]).start();
    }


    public static void main(String[] args) throws StaleProxyException {
        final int supplierNum = 4;
        final int consumerNum = 10;

        SmartGrid smartGrid = new SmartGrid();

        AgentContainer mc = smartGrid.createMC();

        // create different agents in the main container
        for (int i = 0; i < supplierNum; i++)
            smartGrid.addAgent(mc, "Supplier-" + i, Supplier.class);
        for (int i = 0; i < consumerNum; i++)
            smartGrid.addAgent(mc, "Consumer-" + i, Consumer.class);
        smartGrid.addAgent(mc, "Observer", Observer.class);
    }
}