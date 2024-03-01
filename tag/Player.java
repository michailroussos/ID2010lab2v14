
//Player.hava
import java.io.Serializable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.ArrayList;

import java.util.UUID;

/**
 * Player extends Dexter. The Player objects will represent the players in the
 * game of Tag.
 */
public class Player extends Dexter {
    // The variables of the Player class
    private UUID unique_id;
    private State state;

    // The bailiff that the player is currently on
    private BailiffInterface my_bailiff;

    // Enum to represent the state of the player
    public enum State {
        IT,
        NOT_IT
    }

    // The constructor of the Player class
    public Player() {
        super();
        this.unique_id = UUID.randomUUID();
        this.state = State.NOT_IT;

    }

    // Getters and setters for the variables of the Player class
    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /**
     * This method returns the unique id of the player.
     */
    public UUID getUniqueId() {
        return this.unique_id;
    }

    /**
     * This method tags the player and returns true if the player was successfully
     * tagged and false when not.
     */
    public boolean getTagged() {
        this.state = State.IT;
        if (this.state == State.IT)
            return true;
        else
            return false;
    }

    /**
     * This method is based on the Dexter's topLevel method. It will be enhanced so
     * that we can order the Bailiffs.
     */

    public void topLevelForPlayer()
            throws java.io.IOException {
        jumpCount++;

        // Loop forever until we have successfully jumped to a Bailiff.

        for (;;) {

            long retryInterval = 0; // incremented when no Bailiffs are found

            // Sleep a bit so that humans can keep up.

            debugMsg("Is here - entering restraint sleep.");
            snooze(restraintSleepMs);
            debugMsg("Leaving restraint sleep.");

            // Try to find Bailiffs.
            // The loop keeps going until we get a non-empty list of good names.
            // If no results are found, we sleep a bit between attempts.

            do {

                if (0 < retryInterval) {
                    debugMsg("No Bailiffs detected - sleeping.");
                    snooze(retryInterval);
                    debugMsg("Waking up, looking for Bailiffs.");
                }

                scanForBailiffs();

                retryInterval = retrySleep;

                // If no lookup servers or bailiffs are found, go back up to
                // the beginning of the loop, sleep a bit, and then try again.

            } while (goodNames.isEmpty());

            // Now, at least one possibly good Bailiff has been found.

            debugMsg("Found " + goodNames.size() + " Bailiffs");

            // Now we will order the Bailiffs based on the number of players they have.

                orderBailiffs();
            }else{
                // If the player is not it, we will order the Bailiffs based on the number of
                // players and it will put the one with the player that is 'it' last.
                orderBailiffsForNotIt();
            }
            


            // Enter a loop in which we:
            // - randomly pick one Bailiff
            // - migrate to it, or if that fail, try another one

            while (!goodNames.isEmpty()) {

                // Randomly pick one of the good names

                String name = goodNames.get((int) (goodNames.size() * Math.random()));

                // Prepare some state flags

                boolean noRegistry = false;
                boolean badName = false;

                try {

                    // Obtain the default RMI registry

                    Registry registry = LocateRegistry.getRegistry(null);

                    try {

                        // Lookup the service name we selected

                        Remote service = registry.lookup(name);

                        // Verify it is what we want

                        if (service instanceof BailiffInterface) {

                            BailiffInterface bfi = (BailiffInterface) service;

                            // Attempt to migrate

                            try {
                                debugMsg("Trying to migrate");

                                bfi.migrate(this, "topLevel", new Object[] {});

                                debugMsg("Has migrated");

                                return; // SUCCESS, we are done here
                            } catch (RemoteException rex) {
                                debugMsg(rex.toString());
                                badName = true;
                            }
                        } else
                            badName = true;
                    } catch (Exception e) {
                        badName = true;
                    }
                } catch (Exception e) {
                    noRegistry = true;
                }

                // If we come here the migrate failed. Check the state flags
                // and take appropriate action.

                if (noRegistry) {
                    debugMsg("No registry found - resetting name lists");
                    goodNames.clear();
                    badNames.clear();
                } else if (badName) {
                 

    
            } // while candidates remain

            debugMsg("All Bailiffs failed.");
        } // for ever
    } // topLevel

    private void orderBailiffs() {

        for (String name : goodNames) {
            // Prepare some state flags

            boolean noRegistry = false;
            boolean badName = false;
            try {
                // Obtain the default RMI registry
                Registry registry = LocateRegistry.getRegistry(null);

                try {

                    // Lookup the service name we selected

                    Remote service = registry.lookup(name);

                    // Verify it is what we want

                    if (service instanceof BailiffInterface) {

                        BailiffInterface bfi = (BailiffInterface) service;

                        // Attempt to migrate

                        //check if we are in the Bailiff already
                        if (bfi.isPlayerInBailiff(this)) {
                            my_bailiff = bfi;
                            
                        }


                    } else
                        badName = true;
                } catch (Exception e) {
                    badName = true;
                }
            } catch (Exception e) {
                noRegistry = true;
            }

            // If we come here the migrate failed. Check the state flags
            // and take appropriate action.

            if (noRegistry) {
                debugMsg("No registry found - resetting name lists");
                goodNames.clear();
                badNames.clear();
            } else if (badName) {
                debugMsg(String.format("Bad service name found: %s", name));
                goodNames.remove(name);
                badNames.add(name);
            }

        } // while candidates remain

        debugMsg("All Bailiffs failed.");
        }   

    }


    

     

                "Usage: {?,-h,-help}|[-debug][-id string][-rs ms][-qs ms]",
                "? -h -help   Show this text",
                "-debug       Enable trace and diagnostic messages",
                /* Removed since we do not want the user to set the id */
                // "-id string Set the id string printed by debug messages",
                "-rs  ms      Set the restraint sleep in milliseconds",
                "-s  ms      Set the lookup query retry delay",
                "-it         Set the player to be it"
        };
        for (String s : msg)
            System.out.println(s);
    }

    public static void main(String[] argv)
            throws java.io.IOException, java.lang.ClassNotFoundException {

        // Make a new Dexter and configure it from commandline arguments.
        Player pl = new Player();

        // Parse and act on the commandline arguments.

        int state = 0;

        for (String av : argv) {

            switch (state) {

                case 0:
                    if (av.equals("?") || av.equals("-h") || av.equals("-help")) {
                        showUsage();
                        return;
                    } else if (av.equals("-debug"))
                        pl.setDebug(true);

                    // else if (a

                    else if (av.equals("-rs"))
                        state = 2;
                    else if (av.equals("-qs"))
                        state = 3;
                    else if (av.equals("-it"))
                        state = 4;
                    else {
                        System.err.println("Unknown commandline argument: " + av);
                        return;
                    }
                    break;

                /*
                 * //user will not be able to set the id anymore
                 * case 1:
                 * // pl.setId(av);
                 * // state = 0;
                 * // break;
                 */

                case 2:
                    pl.setRestraintSleep(Long.parseLong(av));
                    state = 0;
                    break;

                case 3:
                    pl.setRetrySleep(Long.parseLong(av));
                    state = 0;
                    break;
                case 4:
                    pl.setState(State.IT);
                    state = 0;
                    break;
            } // switch
        } // for all commandline arguments

        pl.topLevel(); // Start the Dexter

    } // main
}
