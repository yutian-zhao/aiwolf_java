/**
 * 
 */
package org.aiwolf.liuchang;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuch
 *
 */
public class StateHolder {
	
	int head;
	int N;
	int times = 50;
	int v = 0;
	
	GameState gamestate;
	RolePrediction rp;
	ScoreMatrix scorematrix;
	
	StateHolder() {
		
	}
	
	StateHolder(int _N) {
		N = _N;
		rp = new RolePrediction();
		scorematrix = new ScoreMatrix();
		gamestate = new GameState(N);
		head = 0;
		v = 0;
		times = 50;
		if (N == 15) {
			times = 500;
		}
	}
	
	void game_init(List<Integer> fixed, int me, int N, int role, Parameters params) {
		v = 0;
		rp = new RolePrediction(N, fixed, role);
		gamestate.game_init(N);
		gamestate.me = me;
		scorematrix = new ScoreMatrix();
		scorematrix.init(N);
		scorematrix.params = params;
	}
	
	void update() {
		rp.recalc(scorematrix, gamestate);
		rp.search(scorematrix, gamestate, times);
	}
	
	void search(int t) {
		rp.search(scorematrix, gamestate, t);
	}
	
	void process(Parameters params, ArrayList<GameData> logs) {
		for (; head < logs.size(); head++) {
			GameData g = logs.get(head);
			switch (g.type) {
			case TURNSTART: {
				if (N == 5) {
					System.out.println(gamestate.day + " " + gamestate.turn);
					if (gamestate.day == 1 && gamestate.turn == 1) {
						scorematrix.firstTurnEnd(gamestate);
					}
				}
				else {
					if (gamestate.day == 1 && gamestate.turn == 1) {
						scorematrix.firstTurnEnd(gamestate);
					}
				}
				gamestate.turn++;
				rp.recalc(scorematrix, gamestate);
				rp.search(scorematrix, gamestate, times);
			}
			break;
			case ROLE: {
				gamestate.agents[g.talker].role = g.object;
			}
			break;
			case DAYCHANGE: {
				gamestate.day++;
				gamestate.day_init(N);
			}
			break;
			case VOTESTART: {
				rp.recalc(scorematrix, gamestate);
				rp.search(scorematrix, gamestate, times);
			}
			break;
			case WILLVOTE: {
				scorematrix.will_vote(gamestate, g.talker, g.object);
			}
			break;
			case TALKDIVINED: {
				scorematrix.talk_divined(gamestate, g.talker, g.object, g.white);
			}
			break;
			case ID: {
				scorematrix.ident(gamestate, g.talker, g.object, g.white);
			}
			break;
			case CO: {
				scorematrix.talk_co(gamestate, g.talker, g.object);
				gamestate.agents[g.talker].corole = g.object;
			}
			break;
			case VOTE: {
				scorematrix.vote(gamestate, g.talker, g.object);
			}
			break;
			case DIVINED: {
				scorematrix.divined(gamestate, g.talker, g.object, g.white);
			}
			break;
			case EXECUTED: {
				gamestate.agents[g.object].Alive = false;
			}
			break;
			case KILLED: {
				if (g.white) {
					if (gamestate.agents[g.object].Alive) {
						scorematrix.killed(gamestate, g.object);
						gamestate.agents[g.object].Alive = false;
					}
				}
			}
			break;
			case WINNER: {
				gamestate.agents[g.talker].wincnt++;
			}
			break;
			case MATCHSTART: {
				gamestate = new GameState(N);
			}
			break;
			case GAMEEND: {
				gamestate.game++;
			}
			break;
			default:
				break;
			}
		}
	}

}
