import java.util.UUID;

public interface PlayerInterface extends java.rmi.Remote {

    public Player.State getState() throws java.rmi.RemoteException;

    public UUID getUniqueId();

    public boolean getTagged();
}
