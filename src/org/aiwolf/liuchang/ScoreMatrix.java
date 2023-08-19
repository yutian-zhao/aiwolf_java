/**
 * 
 */
package org.aiwolf.liuchang;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class ScoreMatrix {

	int seer_number;
	
	double[][][][] scores;
	
	ArrayList<Integer> seer_id;
	Parameters params;
	
	void init(int N) {
		int M = 0;
		if (N == 5) {
			M = 4;
		}
		else if (N == 15) {
			M = 6;
		}
		seer_number = 0;
		seer_id = new ArrayList<Integer>();
		scores = new double[N][M][N][M];
		for (int i1 = 0; i1 < N; i1++) {
			for (int r1 = 0; r1 < M; r1++) {
				for (int i2 = 0; i2 < N; i2++) {
					for (int r2 = 0; r2 < M; r2++) {
						scores[i1][r1][i2][r2] = 0;
					}
				}
			}
		}
	}
	
	void killed(GameState gamestate, int a) {
		scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 30;
	}
	
	void talk_co(GameState gamestate, int a, int role) {
		if (a < 0 || a >= gamestate.N)
			return;
		if (gamestate.N == 15) {
			if (role == Util.SEER) {
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.SEER) {
					scores[a][Util.SEER][a][Util.SEER] += 50;
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
				}
				else {
					if (!seer_id.contains(a)) {
						seer_number += 1;
						seer_id.add(a);
						if (seer_number == 1) {
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.POSSESSED][a][Util.POSSESSED] += 5;
							scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 5;
						}
						else if (seer_number == 2) {
							scores[seer_id.get(0)][Util.POSSESSED][seer_id.get(0)][Util.POSSESSED] -= 5;
							scores[seer_id.get(0)][Util.WEREWOLF][seer_id.get(0)][Util.WEREWOLF] -= 4.6;
							
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 0.4;
						}
						else if (seer_number == 3) {
							scores[seer_id.get(0)][Util.WEREWOLF][seer_id.get(0)][Util.WEREWOLF] -= 0.4;
							scores[seer_id.get(1)][Util.WEREWOLF][seer_id.get(1)][Util.WEREWOLF] -= 0.4;
							
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
						}
						else {
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							for (int id = 0; id < seer_number; id++) {
								scores[seer_id.get(id)][Util.WEREWOLF][seer_id.get(id)][Util.WEREWOLF] -= 0.4;
							}
						}
					}
				}
			}
			else if (role == Util.MEDIUM) {
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.MEDIUM) {
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 50;
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 30;
					scores[a][Util.SEER][a][Util.SEER] += 30;
				}
				else {
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 30;
					scores[a][Util.SEER][a][Util.SEER] += 30;
				}
				
			}
			else if(role == Util.BODYGUARD) {
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.BODYGUARD) {
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 5;
					scores[a][Util.SEER][a][Util.SEER] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 50;
				}
				else {
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 5;
					scores[a][Util.SEER][a][Util.SEER] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] -= 0.01;
				}
			}
		}
	}
	
	void firstTurnEnd(GameState gamestate) {
		if (gamestate.N == 5) {
			for (int i = 0; i < gamestate.N; i++) {
				if (gamestate.agents[i].corole == -1) {
					scores[i][Util.SEER][i][Util.SEER] += 10;
					scores[i][Util.WEREWOLF][i][Util.WEREWOLF] += Util.nlog(1 - params.werewolf_co_rate.value);
					scores[i][Util.POSSESSED][i][Util.POSSESSED] += Util.nlog(1 - params.possessed_co_rate.value);
				}
				else if (gamestate.agents[i].corole == Util.SEER) {
					if (gamestate.me != i && gamestate.agents[gamestate.me].role == Util.SEER) {
						scores[i][Util.SEER][i][Util.SEER] += 50;
					}					
					scores[i][Util.VILLAGER][i][Util.VILLAGER] += 10;
					scores[i][Util.WEREWOLF][i][Util.WEREWOLF] += Util.nlog(params.werewolf_co_rate.value);
					scores[i][Util.POSSESSED][i][Util.POSSESSED] += Util.nlog(params.possessed_co_rate.value);
				}
			}
		}
	}
	
	void divined(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N) {
			return;
		}
		if (white) {
			scores[b][Util.WEREWOLF][b][Util.WEREWOLF] += 50;
		}
		else {
			scores[b][Util.WEREWOLF][b][Util.WEREWOLF] -= 50;
		}
	}
	
	void ident(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N) {
			return;
		}
		if (gamestate.N == 15) {
			if (white) {
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] += 50;
			}
			else {
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] -= 50;
			}
		}
	}
	
	void talk_divined(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N) {
			return;
		}
		if (gamestate.agents[a].target == -1) {
			gamestate.agents[a].target = b;
		}
		else {
			return;
		}
		if (white) {
			if (gamestate.N == 5) {
				if (gamestate.me == b && gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				}
				else {
					scores[a][Util.SEER][b][Util.WEREWOLF] += 10;
					scores[a][Util.POSSESSED][a][Util.POSSESSED] += Util.nlog(1 - params.possessed_divined_black.value);
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += Util.nlog(1 - params.werewolf_divined_black.value);
				}
			}
			else {
				if (gamestate.me == b && gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				}
				else {
					scores[a][Util.SEER][b][Util.WEREWOLF] += 30;
					if (gamestate.agents[gamestate.me].role == Util.WEREWOLF || gamestate.agents[gamestate.me].role == Util.POSSESSED || gamestate.agents[gamestate.me].role == Util.MEDIUM) {
						scores[a][Util.WEREWOLF][b][Util.WEREWOLF] -= 2;
					}
					else {
						scores[a][Util.WEREWOLF][b][Util.WEREWOLF] -= 2;
					}
				}
			}
		}
		else {
			if (gamestate.N == 5) {
				if (gamestate.me == b && gamestate.agents[gamestate.me].role != Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 0.1;
				}
				else {
					scores[a][Util.POSSESSED][a][Util.POSSESSED] += Util.nlog(params.possessed_divined_black.value);
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += Util.nlog(params.werewolf_divined_black.value);
					scores[a][Util.SEER][b][Util.VILLAGER] += 0.8;
					scores[a][Util.SEER][b][Util.POSSESSED] += 1.0;
				}
			}
			else {
				if (gamestate.me == b && gamestate.agents[gamestate.me].role != Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				}
				else {
					scores[a][Util.SEER][b][Util.VILLAGER] += 0.2;
					scores[a][Util.SEER][b][Util.POSSESSED] += 0.5;
					scores[a][Util.SEER][b][Util.BODYGUARD] += 0.5;
					scores[a][Util.SEER][b][Util.MEDIUM] += 0.5;
					scores[a][Util.WEREWOLF][b][Util.WEREWOLF] += 5;
				}
			}
		}
	}

	void vote(GameState gamestate, int a, int b) {
		if (b < 0 || b >= gamestate.N) {
			return;
		}
		gamestate.agents[a].votefor = b;
		if (gamestate.N == 5) {
			if (gamestate.me != a) {
				if (gamestate.agents[gamestate.me].role == Util.VILLAGER) {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] += 0.1;
				}
				else if (gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= 0;
				}
				else {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= 0.1;
				}
				scores[a][Util.SEER][b][Util.WEREWOLF] -= 0.3;
			}
		}
		else {
			if (gamestate.me != a) {
				for (int i = 0; i < 2; i++) {
					scores[a][Util.SEER][b][Util.nothumans[i]] -= 0.02;
					scores[a][Util.BODYGUARD][b][Util.nothumans[i]] -= 0.01;
					scores[a][Util.MEDIUM][b][Util.nothumans[i]] -= 0.01;
				}
				scores[a][Util.WEREWOLF][b][Util.WEREWOLF] += 0.05;
			}
		}
	}

	void will_vote(GameState gamestate, int a, int b) {
		if (b < 0 || b >= gamestate.N) {
			return;
		}
		if (gamestate.N == 5) {
			if (gamestate.agents[a].will_vote == b)
				return;
			if (gamestate.day != 1 || gamestate.turn != 1) {
				if (gamestate.me != a) {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= params.trust.value * 0.1;
					scores[a][Util.SEER][b][Util.WEREWOLF] -= params.trust.value;
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] -= params.werewolf_taben.value;
				}
				gamestate.agents[a].will_vote = b;
			}
		}
		else {
			if (gamestate.agents[a].will_vote == b) {
				return;
			}
			gamestate.agents[a].will_vote = b;
			if (gamestate.me != a) {
				scores[a][Util.BODYGUARD][b][Util.WEREWOLF] -= 0.01;
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] -= 0.01;
				scores[a][Util.SEER][b][Util.WEREWOLF] -= 0.02;
				scores[a][Util.WEREWOLF][a][Util.WEREWOLF] -= params.werewolf_taben.value;
			}
		}
	}
	
}
