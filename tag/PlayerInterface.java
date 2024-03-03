import java.util.UUID;

public interface PlayerInterface extends
        java.rmi.Remote {

    // Get Player UUID
    public UUID getUUID()
            throws java.rmi.RemoteException;

    // Get Player Name
    public String getName()
            throws java.rmi.RemoteException;

    // Tag Player
    public boolean tag()
            throws java.rmi.RemoteException;
   
    // Is Player Tagged
    public boolean isTagged()
        throws java.rmi.RemoteException;
}
