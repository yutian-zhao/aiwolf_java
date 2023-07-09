package org.aiwolf.liuchang;

// Agentの今の状態を整理するクラスっぽい
// sh.gamestate.agents[]として用いられているのはこちら(単なるagents[]とは異なる)
public class AgentStatus {
	int wincnt = 0;
	int role = -1;
	int votefor = -1;
	int voteby = -1;
	int corole = -1;
	int will_vote = -1;
	int target = -1;
	boolean Alive = true;

	AgentStatus() {

	}

	// gameごとに行われてそう
	void game_init() {
		votefor = -1;
		voteby = -1;
		corole = -1;
		will_vote = -1;
		Alive = true;
		target = -1;
	}

	// dayごとに行われてそう
	void day_init() {
		votefor = -1;
		voteby = -1;
		will_vote = -1;
		target = -1;
	}
}
