// BailiffInterface.java
// 2024-01-25/fki Reviewed for v14
// 2018-08-16/fki Refactored for v13.

import java.util.UUID;

/**
 * This interface is for the Bailiff's clients. The clients are mobile
 * code which move into the Bailiff's JVM for execution.
 */
public interface BailiffInterface
    extends
    java.rmi.Remote {
  /**
   * Returns a string which confirms communication with the Bailiff
   * service instance.
   */
  public String ping()
      throws java.rmi.RemoteException;

  /**
   * Returns a property of the Bailiff.
   * 
   * @param key The case-insensitive property key to retrieve.
   * @return The property string or null.
   */
  public String getProperty(String key)
      throws java.rmi.RemoteException;

  /**
   * The entry point for mobile code.
   * The client sends and object (itself perhaps), a string
   * naming the callback method and an array of arguments which must
   * map against the parameters of the callback method.
   *
   * @param obj  The object (to execute).
   * @param cb   The name of the method to call as the program of obj.
   * @param args The parameters for the callback method. Note that if
   *             the method has a signature without arguments the value of args
   *             should be an empty array. Setting args to null will not work.
   * @exception java.rmi.RemoteException        Thrown if there is an RMI problem.
   * @exception java.lang.NoSuchMethodException Thrown if the proposed
   *                                            callback is not found (which
   *                                            happen if the name is spelled
   *                                            wrong,
   *                                            the number of arguments is wrong
   *                                            or are of the wrong type).
   * 
   */
  public void migrate(Object obj, String cb, Object[] args)
      throws java.rmi.RemoteException,
      java.lang.NoSuchMethodException;

  // =============== New Methods ==========================================================

  /** Tag Player */
  public boolean tagPlayer(UUID uuid)
      throws java.rmi.RemoteException;

  /** Return players' Map */
  public java.util.Map<UUID, PlayerInterface> getPlayers()
      throws java.rmi.RemoteException;

  /** Return players' names */
  public java.util.Map<UUID, String> getPlayersNames()
      throws java.rmi.RemoteException;

  /** Return the number of players */
  public int getNumberOfPlayers()
      throws java.rmi.RemoteException;

  /** Return the tagged players */
  public java.util.Map<UUID, Boolean> getTaggedPlayers()
      throws java.rmi.RemoteException;
}
