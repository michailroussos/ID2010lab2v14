
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

  // The current Bailliff
  private String currentBailiff;

  // isTagged flag
  private boolean isIt;

  // Migration flag
  private boolean migrating;

  // the starting delay for the players
  protected long initialDelay = 4000;

  // =============== Interfaces implementation
  // ==========================================================

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

  // Implementing isTagged method
  public boolean isTagged() throws java.rmi.RemoteException {
    return isIt;
  }

  // ================== Constructor
  // ==========================================================
  public Player() {
    super();
    this.uuid = UUID.randomUUID();
    this.goodNames = new HashMap<>();
    this.isIt = false;
    this.migrating = false;
    this.currentBailiff = null;
  }

  // ================== Helpers Methods
  // ==========================================================

  /**
   * This method tags the player and returns true i f the player was successfully
   * tagged and false when not.
   */
  public boolean getTaggedBySomebody() {
    this.isIt = true;
    if (this.isIt && this.migrating == false) {
      debugMsg("I have been tagged, I am it now.");
      return true;
    } else {
      return false;
    }
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

    migrating = true; // we reset the migrating flag

    debugMsg("Initial delay snooze..");
    // let's start by adding a small delay to the player's migration
    if (isIt) {// we award the player who is it with a shorter delay
      snooze(initialDelay - initialDelay / 5);
    } else {
      snooze(initialDelay);
    }
    /*
     * //here we will check for any players in the current bailiff
     * try {
     * // Obtain the default RMI registry
     * Registry registry = LocateRegistry.getRegistry(null);
     * // Lookup the service name we selected
     * Remote service = registry.lookup();
     * // Verify it is what we want
     * if (service instanceof BailiffInterface) {
     * BailiffInterface bfi = (BailiffInterface) service;
     * bfi.addPlayer(this);
     * }
     * } catch (Exception e) {
     * debugMsg("Error adding player to bailiff: " + e.toString());
     * }
     */

    // if we have moved to a Bailliff, we try to see if we have to do anything
    // depending on the player's state ( it or not it)
    if (currentBailiff != null) {
      debugMsg("I am in a bailiff, let's see if I have to do anything.");
      // This is the functionality for the 'it' and 'not it' players
      try {
        // Obtain the default RMI registry
        Registry registry = LocateRegistry.getRegistry(null);
        // Lookup the service name we selected
        Remote service = registry.lookup(currentBailiff);
        // Verify it is what we want
        if (service instanceof BailiffInterface) {
          BailiffInterface bfi = (BailiffInterface) service;
          if (isIt) {// if we are it
            debugMsg("I am it, let's see if I can tag someone.");
            HashMap<UUID, PlayerInterface> playersHashMap = (HashMap<UUID, PlayerInterface>) bfi.getPlayers();
            for (PlayerInterface player : playersHashMap.values()) {
              if (!(player.getUUID().equals(this.getUUID())) && !player.isTagged()) {
                debugMsg("I have found a player who is not tagged, I will try to tag him.");
                // if we successfully tag a player
                if (player.getTaggedBySomebody()) {
                  debugMsg("I have tagged the player " + player.toString());
                  // we are not it anymore
                  this.isIt = false;
                  // we break the loop
                  break;
                }
              }
            }
          } else {// if we are not it
            debugMsg("I am not it, let's see what I will do now.");
            if (bfi.checkIfContainsIt()) {// this means that the player who is it is in the same bailiff
              debugMsg("I have to try to find a new Bailiff, because the 'it' is in the same Bailiff.");
            } else {
              debugMsg("I will sleep a bit more, since I am not in the same Bailiff as the 'it'.");
              snooze(initialDelay / 2);
            }
          }
        }
      } catch (Exception e) {
        debugMsg("Error adding player to bailiff: " + e.toString());
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

        // Randomly pick one of the good names

        String name;

        if (isIt) {// if the player is it, we want to migrate to the bailiff with the most players
          name = getBailiffWithMaxPlayers();
        } else {// if the player is not it, we want to migrate to the bailiff with the least
                // players
          name = getBailiffWithMinPlayers();
        }

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
                currentBailiff = name;// we set the current bailiff to the one we are migrating to
                migrating = true;// we set the migrating flag to true
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

  public String getBailiffWithMinPlayers() {
    int min = 0;
    String bailiffName = null;
    for (String name : goodNames.keySet()) {
      List<Object> tuple = goodNames.get(name);
      if ((int) tuple.get(1) < min) {
        min = (int) tuple.get(1);
        bailiffName = name;
      }
    }
    return bailiffName;
  }

  public static void main(String[] argv)
      throws java.io.IOException, java.lang.ClassNotFoundException {

    // Make a new Player and configure it from commandline arguments.

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
          pl.setId(av);
          state = 0;
          break;

        case 2:
          pl.setRestraintSleep(Long.parseLong(av));
          state = 0;
          break;

        case 3:
          pl.setRetrySleep(Long.parseLong(av));
          state = 0;
          break;
      } // switch
    } // for all commandline arguments

    pl.topLevel(); // Start the Player

  } // main

}
