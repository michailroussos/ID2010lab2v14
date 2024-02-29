
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
    private UUID unique_id;

    private State state;

    private enum State {
        IT,
        NOT_IT
    }

    public Player() {
        super();
        this.unique_id = UUID.randomUUID();
        this.state = State.NOT_IT;

    }

}
