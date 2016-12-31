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
     * @param className
     * @throws StaleProxyException
     */
    private void addAgent(AgentContainer mc, String agentName, String className)
            throws StaleProxyException {
        mc.createNewAgent(agentName, className, new Object[0]).start();
    }


    public static void main(String[] args) throws StaleProxyException {
        final int supplierNum = 2;
        final int consumerNum = 4;

        SmartGrid smartGrid = new SmartGrid();

        AgentContainer mc = smartGrid.createMC();

        // create different agents in the main container
        for (int i = 0; i < supplierNum; i++)
            smartGrid.addAgent(mc, "Supplier-" + i, Supplier.class.getName());
        for (int i = 0; i < consumerNum; i++)
            smartGrid.addAgent(mc, "Consumer-" + i, Consumer.class.getName());
        smartGrid.addAgent(mc, "Observer", Observer.class.getName());
    }
}