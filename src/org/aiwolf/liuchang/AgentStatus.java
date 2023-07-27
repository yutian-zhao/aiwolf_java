/**
 * 
 */
package org.aiwolf.liuchang;

/**
 * @author liuch
 *
 */
public class AgentStatus {

	int corole = -1;
	int role = -1;
	int target = -1;
	int voteby = -1;
	int votefor = -1;
	int will_vote = -1;
	int wincnt = 0;
	
	boolean Alive = true;
	
	void game_init() {
		corole = -1;
		target = -1;
		voteby = -1;
		votefor = -1;
		will_vote = -1;
		Alive = true;
	}
	
	void day_init() {
		target = -1;
		voteby = -1;
		votefor = -1;
		will_vote = -1;
	}
	
}
