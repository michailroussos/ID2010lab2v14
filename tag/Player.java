// Player.java
import java.io.Serializable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.ArrayList;
import java.util.UUID;

public class Player extends Dexter implements PlayerInterface {
 
    // =============== New Attributes ==========================================================

    // UUID for the player
    private UUID uuid = UUID.randomUUID();

    // isTagged flag
    private boolean isIt = false;

    // =============== Interfaces implementation ==========================================================

    // Implementing getUUID method
    public UUID getUUID() throws java.rmi.RemoteException {
        return uuid;
    }

    // Implementing getName method
    public String getName() throws java.rmi.RemoteException {
        return id;
    }

    // Implementing tag method
    public boolean tag() throws java.rmi.RemoteException {
        isIt = true;
        return true;
    }

    // =============== Main overwrite ==========================================================
    public static void main(String[] argv)
            throws java.io.IOException, java.lang.ClassNotFoundException {

        // Make a new Dexter and configure it from commandline arguments.

        Dexter dx = new Dexter();

        // Parse and act on the commandline arguments.

        int state = 0;

        for (String av : argv) {

            switch (state) {

                case 0:
                    if (av.equals("?") || av.equals("-h") || av.equals("-help")) {
                        showUsage();
                        return;
                    } else if (av.equals("-debug"))
                        dx.setDebug(true);
                    else if (av.equals("-id"))
                        state = 1;
                    else if (av.equals("-rs"))
                        state = 2;
                    else if (av.equals("-qs"))
                        state = 3;
                    else {
                        System.err.println("Unknown commandline argument: " + av);
                        return;
                    }
                    break;

                case 1:
                    dx.setId(av);
                    state = 0;
                    break;

                case 2:
                    dx.setRestraintSleep(Long.parseLong(av));
                    state = 0;
                    break;

                case 3:
                    dx.setRetrySleep(Long.parseLong(av));
                    state = 0;
                    break;
            } // switch
        } // for all commandline arguments

        dx.topLevel(); // Start the Dexter

    } // main
}
