// Bailiff.java
// 2024-01-25/fki Refactored for v14 - No Jini, just rmiregistry
// 2018-08-16/fki Refactored for v13

import java.io.IOException;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * The Bailiff is an RMI service that provides an execution
 * environment for agents. The service it provides is this:
 *
 * A serializable class may call the Bailiff's migrate() method to
 * transfer itself to the JVM of the Bailiff and there have its own
 * thread of execution in a method of its own choice. When the
 * specified method returns the thread ends and the object instance is
 * garbage-collected.
 *
 * The Bailiff is not mobile.
 *
 * [bail-iff n 1 law officer who helps a sheriff in issuing writs and
 * making arrests. 2 (Brit.) landlord's agent or steward; manager of
 * an estate or farm. 3 (US) official in a lawcourt, esp one who takes
 * people to their seats and announces the arrival of the judge.]
 *
 * @author Fredrik Kilander, fki@kth.se
 */
public class Bailiff
    extends
    java.rmi.server.UnicastRemoteObject // for RMI
    implements
    BailiffInterface // for clients
{
  // When debug is true, trace and diagnostic messages are printed.
  protected boolean debug = false;

  // The java.util.logging.Logger is provided for tracking and
  // forensic analysis.
  protected Logger log;

  // The id string identifies this Bailiff instance in messages.
  protected String id = "";

  // The info string provides information particular to this Bailiff,
  // if any.
  protected String info = "";

  // The property map can optionally be used to get and set runtime
  // configuration properties of the Bailiff, for example, the maximum
  // nof clients.
  protected Map<String, String> propertyMap;

  // Set to the name of the host the JVM is executing on.
  protected String myHostName = "";

  // Set to the internet address of this Bailiff.
  protected InetAddress myInetAddress;

  // Registration name with the rmiregistry
  protected String serviceName = null;

  // Map all players to their respective uuids
  protected Map<UUID, PlayerInterface> playerMap = new HashMap<>();

  /**
   * If debug is enabled, prints a message on stdout.
   * 
   * @s The message string
   */
  protected void debugMsg(String s) {
    if (debug) {
      System.out.println(s);
    }
  }

  /* ================ A g i t a t o r ================ */

  /**
   * Class Agitator wraps and encapsulates the remote object to which the
   * Bailiff lends a thread of execution.
   */
  private class Agitator extends Thread {

    protected Object myObj; // The client object
    protected String myCb; // The name of the entry point method
    protected Object[] myArgs; // Arguments to the entry point method
    protected java.lang.reflect.Method myMethod; // Ref. to entry point method
    protected Class[] myParms; // Class reflection of arguments

    /**
     * Creates a new Agitator by copying th references to the client
     * object, the name of the entry method and the arguments to
     * the entry method.
     * 
     * @param obj  The client object, holding the method to execute
     * @param cb   The name of the entry point method (callback)
     * @param args Arguments to the entry point method
     */
    public Agitator(Object obj, String cb, Object[] args) {
      myObj = obj;
      myCb = cb;
      myArgs = args;

      // If the array of arguments are non-zero we must create an array
      // of Class so that we can match the entry point method's name with
      // the parameter signature. So, the myParms[] array is loaded with
      // the class of each entry point parameter.

      if (0 < args.length) {
        myParms = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
          myParms[i] = args[i].getClass();
        }
      } else {
        myParms = null;
      }
    }

    /**
     * This method locates the method that is the client object's requested
     * entry point. It also sets the classloader of the current instance
     * to follow the client's classloader.
     * 
     * @throws NoSuchMethodException Thrown if the entry point specified
     *                               in the constructor can not be found.
     */
    public void initialize() throws java.lang.NoSuchMethodException {
      myMethod = myObj.getClass().getMethod(myCb, myParms);
      setContextClassLoader(myObj.getClass().getClassLoader());
    }

    /**
     * Overrides the default run() method in class Thread (a superclass to
     * us). Then we invoke the requested entry point on the client object.
     */
    public void run() {
      try {
        myMethod.invoke(myObj, myArgs);
      } catch (Throwable t) {
        log.severe(t.getMessage());
      }
    }
  } // class Agitator

  /* ================ B a i l i f f I n t e r f a c e ================ */

  /**
   * Returns a string acknowledging the host, IP address, room and user
   * fields of this Bailiff instance. This method can be used to debug
   * the identity of the Bailiff from a client and to verify that the
   * connection is still operational.
   * 
   * @returns The ping response.
   * @throws RemoteException
   */
  public String ping() throws java.rmi.RemoteException {
    log.fine("ping");

    return String.format("Ping response from Bailiff %s on host %s [%s]",
        id,
        myHostName,
        myInetAddress.getHostAddress());
  }

  /**
   * Returns the string property stored under key.
   * 
   * @param key The key to look up.
   * @returns The property value.
   */
  public String getProperty(String key) {
    log.fine(String.format("getProperty key=%s", key));

    return propertyMap.get(key.toLowerCase());
  }

  /**
   * Sets the property value to be stored under key.
   * 
   * @param key   The name of the property.
   * @param value The value of the property.
   */
  public void setProperty(String key, String value) {
    log.fine(String.format("setProperty key=%s value=%s", key, value));

    propertyMap.put(key.toLowerCase(), value);
  }

  /**
   * Entry point for remote clients who want to pass an object to be
   * executed by the Bailiff. The Bailiff starts a new thread for the
   * object and calls the specified entry (callback) method. When that
   * method returns, the thread exits and the object becomes inert.
   * 
   * @param obj  The object to execute.
   * @param cb   The name of the entry (callback) method to call.
   * @param args Array of arguments to the entry method. The elements in
   *             the array must match the entry method's signature.
   * @throws NoSuchMethodException Thrown if the specified entry method
   *                               does not exist with the expected signature.
   */
  public void migrate (Object obj, String cb, Object [] args)
    throws
      java.rmi.RemoteException, NoSuchMethodException
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(String.format("migrate obj=%s cb=%s args=%s",
          obj.toString(),
          cb,
          Arrays.toString(args))
      );
    }

    // Add a new Player to the map
    if (obj instanceof Player) {
      PlayerInterface p = (PlayerInterface) obj;
      playerMap.put(p.getUUID(), p);
    }

    Agitator agt = new Agitator(obj, cb, args);
    agt.initialize();
    agt.start();
  }

  // ================ New Interface Methods Implementation  ================
  
  /**
   * Tag a player
   * @param id
   * @throws java.rmi.RemoteException
   */
  public boolean tagPlayer(UUID id) throws java.rmi.RemoteException {
    if (playerMap.containsKey(id)) {
      // get the player
      PlayerInterface p = playerMap.get(id);
      // tag the player
      return p.tag();
    }
    return false;
  }

  /**
   * Return players' Map
   * @return
   * @throws java.rmi.RemoteException
   */
  public Map<UUID, PlayerInterface> getPlayers() throws java.rmi.RemoteException {
    return playerMap;
  }

  /**
   * Return players' names
   * @return
   * @throws java.rmi.RemoteException
   */
  public Map<UUID, String> getPlayersNames() throws java.rmi.RemoteException {
    Map<UUID, String> names = new HashMap<>();
    for (Map.Entry<UUID, PlayerInterface> entry : playerMap.entrySet()) {
      names.put(entry.getKey(), entry.getValue().getName());
    }
    return names;
  }

  /**
   * Return the number of players
   * @return
   * @throws java.rmi.RemoteException
   */
  public int getNumberOfPlayers() throws java.rmi.RemoteException {
    return playerMap.size();
  }

  /**
   * Return the tagged players
   * @return
   * @throws java.rmi.RemoteException
   */
  public Map<UUID, Boolean> getTaggedPlayers() throws java.rmi.RemoteException {
    Map<UUID, Boolean> tagged = new HashMap<>();
    for (Map.Entry<UUID, PlayerInterface> entry : playerMap.entrySet()) {
      tagged.put(entry.getKey(), entry.getValue().isTagged());
    }
    return tagged;
  }

  /* ================ C o n s t r u c t o r ================ */

  /**
   * Creates a new Bailiff service instance.
   * 
   * @param room  Informational text field used to designate the 'room'
   *              (physical or virtual) the Bailiff is running in.
   * @param user  Information text field used to designate the 'user'
   *              who is associated with the Bailiff instance.
   * @param debug If true, diagnostic messages will be logged to the
   *              provided Logger instance. This parameter is overridden if the
   *              class local debug variable is set to true in the source code.
   *
   * @param log   If debug is true, this parameter can be a Logger instance
   *              configured to accept entries. If log is null a default Logger
   *              instance
   *              is created.
   * @throws RemoteException
   * @throws UnknownHostException Thrown if the local host address can not
   *                              be determined.
   * @throws IOException          Thrown if there is an I/O problem.
   */
  public Bailiff(String id, String info, Logger log)
      throws java.rmi.RemoteException,
      java.net.UnknownHostException,
      java.io.IOException {
    // Process constructor parameters

    if (log != null)
      this.log = log;
    else
      throw new IllegalArgumentException("Logger is null");

    this.id = (id != null) ? id : this.id;
    this.info = (info != null) ? info : this.info;

    // Retrieve host and network information

    myInetAddress = java.net.InetAddress.getLocalHost();
    myHostName = myInetAddress.getHostName().toLowerCase();

    // Create and initialize the property map

    propertyMap = Collections.synchronizedMap(new HashMap<String, String>());

    propertyMap.put("id", id);
    propertyMap.put("info", info);
    propertyMap.put("hostname", myHostName);
    propertyMap.put("hostaddress", myInetAddress.getHostAddress());

    // Make a log entry that we are starting.

    log.info(String.format("STARTING id=%s info=%s host=%s debug=%b",
        id, info, myHostName, debug));

    // Compose the service name under which to register

    serviceName = getClass().getName()
        + "." + id
        + "." + Integer.toString((int) (Math.random() * (float) 0x7FFF_FFFF));

    // Register with the default rmiregistry

    Naming.rebind("///" + serviceName, this);

    log.info(String.format("Registered as %s", serviceName));
  }

  /**
   * Unbind (remove) this Bailiff from the rmiregistry
   */
  protected void unbind() {
    try {
      Naming.unbind("///" + serviceName);
    } catch (Exception e) {
      System.out.printf("When unbinding from rmiregistry: %s\n",
          e.toString());
    }
  }

  /**
   * Returns a string representation of this service instance.
   * 
   * @returns A string representing this Bailiff instance.
   */
  public String toString() {
    return String.format("Bailiff %s (%s) on host %s [%s]",
        id, info, myHostName,
        myInetAddress.getHostAddress());
  }

  /* ================ m a i n s u p p o r t ================ */

  private static void showUsage() {
    String[] msg = {
        "Usage: {'?',-h,-help}|[-id string][-info string][-log n]",
        "? -h help     This message",
        "-id   string  Sets the identification string of this Bailiff",
        "-info string  Sets the information message of this Bailiff",
        "-log  n       Sets the logging level, higher is more:",
        "  -log 0        Level.OFF",
        "  -log 3        Level.INFO",
        "  -log 7        Level.ALL"
    };
    for (String s : msg)
      System.out.println(s);
  }

  private static Level setLoglevelFromCmdLine(String s) {
    switch (Integer.parseInt(s)) {
      case 0:
        return Level.OFF;
      case 1:
        return Level.SEVERE;
      case 2:
        return Level.WARNING;
      case 3:
        return Level.INFO;
      case 4:
        return Level.CONFIG;
      case 5:
        return Level.FINE;
      case 6:
        return Level.FINER;
      case 7:
        return Level.FINEST;
      default:
        return Level.ALL;
    }
  }

  /* ================ m a i n ================ */

  public static void main(String[] argv)
      throws java.net.UnknownHostException,
      java.rmi.RemoteException,
      java.io.IOException {
    String id = null;
    String info = null;
    Level logLevel = Level.ALL;

    int state = 0;

    for (String av : argv) {

      switch (state) {

        case 0:
          if (av.equals("?") || av.equals("-h") || av.equals("-help")) {
            showUsage();
            return;
          } else if (av.equals("-id"))
            state = 1;
          else if (av.equals("-info"))
            state = 2;
          else if (av.equals("-log"))
            state = 3;
          else {
            System.err.println("Unknown commandline argument: " + av);
            return;
          }
          break;

        case 1:
          id = av;
          state = 0;
          break;

        case 2:
          info = av;
          state = 0;
          break;

        case 3:
          logLevel = setLoglevelFromCmdLine(av);
          state = 0;
          break;
      } // switch
    } // for

    Logger log = Logger.getAnonymousLogger();
    log.setLevel(logLevel);

    new Bailiff(id, info, log);

  } // main

} // class Bailiff
