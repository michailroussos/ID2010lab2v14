
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
import java.util.Random;
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
  private Map<String, BailiffInterface> goodNames;

  // isTagged flag
  private boolean isIt;

  // Migration flag
  private boolean migrating;

  // Current Bailiff
  private BailiffInterface currentBailiff;

  // =============== Tag Setter
  public void setTag() {
    this.isIt = true;
  }

  // =============== Interfaces implementation

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

  // Implementing isMigrating method
  public boolean isMigrating() throws java.rmi.RemoteException {
    return this.migrating;
  }

  // ================== Constructor
  public Player() {
    super();
    this.uuid = UUID.randomUUID();
    this.goodNames = new HashMap<>();
    this.isIt = false;
    this.migrating = false;
    this.currentBailiff = null;
  }

  // ================== Helpers Methods

  /**
   * Scan for Baliff services.
   */
  protected void scanForBailiffs() {

    boolean badName = false;

    try {

      // Get the default RMI registry

      Registry registry = LocateRegistry.getRegistry(null);

      // Ask for all registered services

      String[] serviceNames = registry.list();

      // Inspect the list of service names

      for (String name : serviceNames) {

        if (name.startsWith("Bailiff")) {

          // If the name already is on the bad list, ignore it

          if (this.goodNames.containsKey(name))
            continue;

          // Else, optimistically add it to the good names

          // Lookup the service name we selected

          try {
            Remote service = registry.lookup(name);

            // Verify it is what we want

            if (service instanceof BailiffInterface) {

              BailiffInterface bfi = (BailiffInterface) service;

              // Add the name to the good list
              this.goodNames.put(name, bfi);
            }
          } catch (Exception e) {
            badName = true;
            debugMsg(String.format("Bad service name found: %s", name));
            badNames.add(name);
          }       
        }
      }
    } catch (Exception e) {
      debugMsg("Scanning for Bailiffs failed: " + e.toString());
    }
  } 

  // ================== Behavior Methods

  public void topLevel()
      throws java.io.IOException {
    jumpCount++;
    // Reset migrating flag
    this.migrating = false;

    // Debug tag flag
    // debugMsg("Is it: " + this.isIt);
    // Debug current Bailiff
    // debugMsg("Current Bailiff: " + this.currentBailiff);

    // Tag behavior
    if (this.currentBailiff != null) {
      try {
        if (this.isIt) {
          debugMsg(id + " is it!");
          // Get the players from the current Bailiff
          Map<UUID, PlayerInterface> players = this.currentBailiff.getPlayers();
          // Get the first player eligible to be tagged!
          for (UUID uuid : players.keySet()) {
            // If the player is not tagged and not migrating
            if (!players.get(uuid).isTagged() && !players.get(uuid).isMigrating()) {
              // Tag the player
              this.currentBailiff.tagPlayer(uuid);
              debugMsg(id + " tagged " + players.get(uuid).getName());
              this.isIt = false;
              break;
            }
          }
        }
      } catch (RemoteException e) {
        debugMsg("Failed to tag: " + e.toString());
      }
    }

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

        // Service to migrate
        BailiffInterface service = null;

        // If the player is not tagged, we will select a Bailiff with less players
        if (!this.isIt) {
          // Get the number of players from each Bailiff
          Map<String, BailiffInterface> tempGoodNames = new HashMap<>(this.goodNames);
          for (String name : this.goodNames.keySet()) {
            try {
              if (this.goodNames.get(name).getNumberOfPlayers() > 0) {
                tempGoodNames.remove(name);
              }
            } catch (RemoteException e) {
              debugMsg("Failed to get number of players: " + e.toString());
            }
          }
          // If there are no Bailiffs with less players, we will select a random one
          if (tempGoodNames.isEmpty()) {
            List<String> keys = new ArrayList<>(this.goodNames.keySet());
            Random rand = new Random();
            String randomKey = keys.get(rand.nextInt(keys.size()));
            service = this.goodNames.get(randomKey);
          } else {
            List<String> keys = new ArrayList<>(tempGoodNames.keySet());
            Random rand = new Random();
            String randomKey = keys.get(rand.nextInt(keys.size()));
            service = tempGoodNames.get(randomKey);
          }
        } else {
          // If the player is tagged, we will select a random Bailiff
          List<String> keys = new ArrayList<>(this.goodNames.keySet());
          Random rand = new Random();
          String randomKey = keys.get(rand.nextInt(keys.size()));
          service = this.goodNames.get(randomKey);
        }

        // Prepare some state flags

        boolean noRegistry = false;
        boolean badName = false;

        // Attempt to migrate

        try {
          debugMsg("Trying to migrate");

          if (service != null) {
            // Set migrating flag
            this.migrating = true;
            this.currentBailiff = service;
            service.migrate(this, "topLevel", new Object[] {});
            debugMsg("Has migrated");
            return; // SUCCESS, we are done here
          } else {
            debugMsg("Service is null, migration failed");
            throw new RemoteException("Service is null");
          }
        } catch (RemoteException rex) {
          debugMsg(rex.toString());
          badName = true;
        } catch (NoSuchMethodException e) {
          debugMsg(e.toString());
          badName = true;
        }

        // If we come here the migrate failed. Check the state flags
        // and take appropriate action.

        if (noRegistry) {
          debugMsg("No registry found - resetting name lists");
          goodNames.clear();
          badNames.clear();
        } else if (badName) {
          debugMsg(String.format("Bad service name found: %s", this.currentBailiff.toString()));
          goodNames.remove(this.currentBailiff.toString());
          badNames.add(this.currentBailiff.toString());
        }

      } // while candidates remain

      debugMsg("All Bailiffs failed.");
    } // for ever
  } // topLevel

  // ================== Main Method

  /**
   * Prints commandline help.
   */
  protected static void showUsage() {
    String[] msg = {
        "Usage: {?,-h,-help}|[-debug][-id string][-rs ms][-qs ms]",
        "? -h -help   Show this text",
        "-debug       Enable trace and diagnostic messages",
        "-id  string  Set the id string printed by debug messages",
        "-rs  ms      Set the restraint sleep in milliseconds",
        "-qs  ms      Set the lookup query retry delay",
        "-tag         Tag the player",
    };
    for (String s : msg)
      System.out.println(s);
  }

  public static void main(String[] argv)
      throws java.io.IOException, java.lang.ClassNotFoundException {

    // Make a new Dexter and configure it from commandline arguments.

    Player dx = new Player();

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
          else if (av.equals("-tag")) 
            dx.setTag();
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

    dx.topLevel(); // Start the Player

  } // main

}
