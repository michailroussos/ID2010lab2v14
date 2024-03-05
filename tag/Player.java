
//Player.java
import java.io.Serializable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player extends Dexter. The Player objects will represent the players in the
 * game of Tag.
 */
public class Player extends Dexter implements PlayerInterface {

  // =============== New Attributes
  // ==========================================================

  // UUID for the player
  private UUID uuid;

  // The good Bailiffs
  private Map<String, List<Object>> goodNames;

  // isTagged flag
  private boolean isIt;

  // Migration flag
  private boolean migrating;

  // =============== Interfaces implementation
  // ==========================================================

  // Implementing getUUID method
  public UUID getUUID() throws java.rmi.RemoteException {
    return this.uuid;
  }

  // Implementing getName method
  public String getName() throws java.rmi.RemoteException {
    return this.id;
  }

  // Implementing tag method
  public boolean tag() throws java.rmi.RemoteException {
    this.isIt = true;
    return true;
  }

  // Implementing isTagged method
  public boolean isTagged() throws java.rmi.RemoteException {
    return this.isIt;
  }

  // ================== Constructor
  // ==========================================================
  public Player() {
    super();
    this.uuid = UUID.randomUUID();
    this.goodNames = new HashMap<>();
    this.isIt = false;
    this.migrating = false;
  }

  // ================== Helpers Methods
  // ==========================================================

  /**
   * This method tags the player and returns true i f the player was successfully
   * tagged and false when not.
   */
  public boolean getTagged() {
    this.isIt = true;
    if (this.isIt && this.migrating == false)
      return true;
    else
      return false;
  }

  /**
   * Scan for Baliff services and prepare the contents of the Hashmap goodnames.
   */
  protected void scanForBailiffs() {

    try {

      // Get the default RMI registry

      Registry registry = LocateRegistry.getRegistry(null);

      // Ask for all registered services

      String[] serviceNames = registry.list();

      // Inspect the list of service names

      for (String name : serviceNames) {

        if (name.startsWith("Bailiff")) {

          // If the name already is on the bad list, ignore it

          if (badNames.contains(name))
            continue;

          // If the name already is on the good list, ignore it

          if (goodNames.get(name) != null)
            continue;

          // Else, optimistically add it to the good names

          // goodNames.add(name);
          ////////////////////////////
          boolean noRegistry = false;
          boolean badName = false;
          try {

            // Obtain the default RMI registry

            registry = LocateRegistry.getRegistry(null);

            try {

              // Lookup the service name we selected

              Remote service = registry.lookup(name);

              // Verify it is what we want

              if (service instanceof BailiffInterface) {

                BailiffInterface bfi = (BailiffInterface) service;
                int playerCount = bfi.getPlayers().size();
                // add it to the good names
                List<Object> tuple = new ArrayList<>();
                tuple.add(bfi);
                tuple.add(playerCount);

                // here we create this strcture: {key: name, value: [interface, # of players]}
                goodNames.put(name, tuple);

              } else {
                badName = true;
              }
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
            // the name is never added, so no reason to remove it
            // goodNames.remove(name);
            badNames.add(name);
          }

          /////////////////

        }

      }

    } catch (Exception e) {
      debugMsg("Scanning for Bailiffs failed: " + e.toString());
    }
  }

  /**
   * This is Dexter's main program once he is on his way. In short, he
   * goes into an infinite loop in which the only exit is a
   * successfull migrate to a Bailiff.
   *
   * for (;;) {
   *
   * while no good Bailiffs are found
   * look for Bailiffs
   *
   * while good Bailiffs are known
   * jump to a random Bailiff
   * or
   * update the lists of good and bad bailiffs
   * }
   *
   * Dexter has no concept of where he is, and may happily migrate to
   * the Bailiff he is already in.
   */
  public void topLevel()
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

      // Enter a loop in which we:
      // - randomly pick one Bailiff
      // - migrate to it, or if that fail, try another one

      while (!goodNames.isEmpty()) {

        // Randomly pick one of the good names

        String name = getBailiffWithMaxPlayers();

        ////////////////////////////
        // Prepare some state flags
        boolean noRegistry = false;
        boolean badName = false;
        // maybe we could remove some parts here since we do them in the scanForBailiffs
        // method
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
          debugMsg(String.format("Bad service name found: %s", name));
          goodNames.remove(name);
          badNames.add(name);
        }
        ////////////////////////////
      } // while candidates remain

      debugMsg("All Bailiffs failed.");
    } // for ever
  } // topLevel

  public String getBailiffWithMaxPlayers() {
    int max = 0;
    String bailiffName = null;
    for (String name : goodNames.keySet()) {
      List<Object> tuple = goodNames.get(name);
      if ((int) tuple.get(1) > max) {
        max = (int) tuple.get(1);
        bailiffName = name;
      }
    }
    return bailiffName;
  }

}
