package commands;

import java.io.IOException;

import server.ClientInfo;
import server.Connection;
import server.ServerInfo;

/**
 * Authenticate changes a user to an authenticated user, optionally
 * changing their username.
 * 
 * It takes a password to be (hashed and) kept for reference by the server.
 * It also stores a local copy of the username and password so the client
 * can automatically reconnect as an authenticated user.
 * 
 * @author rob
 *
 */
public class Authenticate extends IdentityChange
{
	final static int NUM_ARGS = 2;
	private String hash;
	
	/**
	 * One argument constructor method for when user keeps current identity
	 */
	/*
	public Authenticate(String hash)
	{
		super("");
		this.type = "authenticate";
		this.hash = hash;
		this.identity = "";
	}
	*/
	
	/**
	 * Two argument constructor method for when new name is specified
	 */
	public Authenticate(String hash, String identity)
	{
		super(identity);
		this.type = "authenticate";
		this.hash = hash;
		this.identity = identity;
	}

	/**
	 * Change the client to an authenticated client.
	 * Change their name if they entered one.
	 */
	@Override
	public void execute(Connection c) throws IOException
	{
		ClientInfo cInfo = c.getClientInfo();
		ServerInfo sInfo = c.getServerInfo();
		
		// Fail if new name doesn't match rules or if another user is logged in already
		if (!validRegexName(identity) || isConnectedName(identity, sInfo))
		{
			(new AuthResponse("Invalid username.", false)).sendJSON(c);;
			return;
		}
		
		if (isGuestName(identity)) {
			(new AuthResponse("You may not authenticate as a guest.\nPick another name.", false)).sendJSON(c);
			return;
		}

		// If the name is recorded in the authentication index,
		// test the hash and log the user in if it matches
		if (isAuthName(identity, sInfo)) {
			if (!sInfo.tryExistingAuth(identity, hash)) {
				(new AuthResponse("Incorrect username or password.", false)).sendJSON(c);;
				return;
			}
			else {
				(new AuthResponse("", true)).sendJSON(c);;
				super.execute(c);
				return;
			}
		}
		// Otherwise the name doesn't exist,
		// so change the client's ID
		super.execute(c);
		
		/*}
		// If the user provides no new name but is already authenticated,
		// then they are dumb
		else if (cInfo.isAuthenticated()) {
			// TODO send the client a message that they are dumb
			return;
		}*/
		
		// Add the user to the authentication index
		sInfo.addAuthUser(identity, hash);
		// Mark the user's auth flag as true
		cInfo.makeAuth(hash);
		
		// Tell the client the new identity is authenticated
		(new AuthResponse(identity, true)).sendJSON(c);

		return;
	}
	
	private boolean isGuestName(String name)
	{
		return name.matches("guest\\d+");
	}
}
