package lift_management;

import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Agent;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;

/**
 * Created by Gustavo on 06/10/2016.
 */
public class LiftManagementLauncher extends Repast3Launcher {
    private ContainerController mainContainer;

    public static void main(String[] args) {
        return;
    }

    public String getName() {
        return null;
    }

    protected void launchJADE() {
        Runtime rt = Runtime.instance();
        Profile p1 = new ProfileImpl();
        mainContainer = rt.createMainContainer(p1);
    }

	@Override
	public String[] getInitParam() {
		// TODO Auto-generated method stub
		return null;
	}
}