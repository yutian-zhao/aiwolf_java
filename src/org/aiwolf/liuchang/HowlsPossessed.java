/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.NotContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author liuch
 *
 */
public class HowlsPossessed extends HowlsBasePlayer {

	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	StateHolder sh2;
	boolean f = true;
	Parameters params;
	int whatCO;
	double COnum;
	boolean doCO = false;
	boolean doFO = false;
	boolean houkoku = true;
	boolean[] divined;
	boolean pos = false;
	boolean update_sh = true;
	int black_count;
	GameData lastGameData;
	boolean guarded;
	int target;
	double ContentRand;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);
			f = false;
		}

		update_sh = true;
		doCO = false;
		doFO = false;
		houkoku = true;
		pos = false;
		divined = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++)
			divined[i] = false;

		if (numAgents == 5) {
			whatCO = 0;
		}

		else {
			COnum = Math.random();

			if(COnum <= 0.65) {whatCO = 0;}
			else {whatCO = 1;}
		}

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		gamedata.clear();

		sh.head = 0;
		sh2.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.POSSESSED, params);
		sh2.game_init(fixed, meint, numAgents, Util.SEER, params);

		guarded = false;
		target = -1;
	}

	public void dayStart() {
		super.dayStart();
		if(whatCO != 2) {houkoku = false;}
		if (whatCO == 2 &&  !gamedata.isEmpty()) {
			lastGameData = gamedata.get(gamedata.size() - 1);
			if (lastGameData.type != DataType.KILLED && day > 1 ) {
				houkoku = false;
			}
		}
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata);

		double mn = -1;
		int c = 0;
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			for (int i = 0; i < numAgents; i++) {
				if (i != meint && sh.gamestate.agents[i].Alive) {
					double score = 1 - getPred(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		else {
			mn = -100;

			if (numAgents == 5) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && sh.gamestate.agents[i].Alive) {
						double score = getPred(i, Util.WEREWOLF) - getPred(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			} 

			else {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && sh.gamestate.agents[i].Alive) {
						double score = getPred(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
				int t = sh.gamestate.agents[c].will_vote;

				mn = -100;
				if (t == -1) {
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive) {
							double score = 1 - getPred(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								t = i;
							}
						}
					}
				}
				c = t;
			}
		}
		if (!isValidIdx(c)) {return null;}
		return currentGameInfo.getAgentList().get(c);
	}

	protected String chooseTalk() {
		if (lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false));
			lastTurn++;
		}

		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		lastTalkTurn = lastTurn;
		updateState(sh);
		updateState(sh2);
		if (update_sh) {
			update_sh = false;
			sh.search(1000);
			sh2.search(1000);
		}
		double mn = -1;
		int c = 0;
		
		if(!doFO) {
			doFO = true;
			return (new Content(new RequestContentBuilder(Content.ANY, new Content(new ComingoutContentBuilder(Content.ANY,Role.ANY))))).getText();
		}

		switch (whatCO) {
			case 0: 
				if (!doCO) {
					int num_seer = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.SEER)) {
								num_seer += 1;
						}
					}
					if(num_seer >= 2) {
						whatCO = 2;
						houkoku = true;
						break;
					}
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
				}
				if (!houkoku) {
					houkoku = true;
					if (numAgents == 5) {
						c = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i] && sh.gamestate.agents[i].corole == Util.SEER) {c = i;}
								}
						}
						if (c != -1) {
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))).getText();
						} 
						else {
							mn = -1;
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i] && mn < getPred(i, Util.WEREWOLF)) {
										mn = getPred(i, Util.WEREWOLF);
										c = i;
									}
								}
							}
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target, Species.HUMAN))).getText();
						}
					}
					else {
						if (day != 2) {
							mn = -100;
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i]) {
										double score = 1 - getPred(i, Util.WEREWOLF);
										if (sh.gamestate.agents[i].corole != -1) {
											score -= 1.0;
										}
										if (mn < score) {
											mn = score;
											c = i;
										}
									}
								}
							}
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target,Species.WEREWOLF))).getText();
						} 
						else {
							mn = -100;
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i]) {
										double score = getPred(i, Util.WEREWOLF);
										if (sh.gamestate.agents[i].corole != -1) {
											score -= 1.0;
										}
										if (mn < score) {
											mn = score;
											c = i;
										}
									}
								}
							}
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target,Species.WEREWOLF))).getText();
						}
					}
				}
				break;

			case 1: 
				if (!doCO) {
					int num_medium = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.MEDIUM)) {
								num_medium += 1;
						}
					}
					if(num_medium >= 2) {
						whatCO = 2;
						houkoku = true;
						break;
					}
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
				if(!houkoku) {
					houkoku = true;
					if (currentGameInfo.getExecutedAgent() != null) {
						Agent target = currentGameInfo.getExecutedAgent();
						c = target.getAgentIdx() - 1;
						if (agents[c].COrole == Role.SEER) {
							return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
						}
						else if (black_count <= 1) {
							black_count ++;
							return (new Content(new IdentContentBuilder(target, Species.WEREWOLF))).getText();
						}
						return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
					}
				}
				break;

			case 2: 
				int hostility = 0;
				if (sh.gamestate.cnt_vote(meint) >= 3) {
					hostility = 1;
				}
	
				if (!doCO) {
					if( hostility == 1 || !houkoku) {
						doCO = true;
						return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();	
					}
				}
				if(doCO && !houkoku && day >= 2) {

					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive) {
							double score = getPred(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}

					if (!isValidIdx(c)) {return null;}
					houkoku = true;
					target = c;
					voteCandidate = currentGameInfo.getAgentList().get(c);
					return (new Content(new AndContentBuilder(new Content(new GuardedAgentContentBuilder(currentGameInfo.getAgentList().get(c))),
							new Content(new NotContentBuilder(new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c), Role.WEREWOLF))))))).getText();
				}
				break;
		}

		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			if (!pos) {
				pos = true;
				return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
			}
		}
		

		sh2.update();

		for (int i = 0; i < numAgents; i++) {
			if (i != meint && sh.gamestate.agents[i].Alive) {
				double score = getPred(i, Util.WEREWOLF);
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}

		if (target != c) {
			if (!isValidIdx(c)) {return null;}
			voteCandidate = currentGameInfo.getAgentList().get(c);
			 {
					ContentRand = Math.random();
					if(ContentRand <= 0.3) {return (new Content(new VoteContentBuilder(voteCandidate))).getText();}
					else if( ContentRand > 0.3 && ContentRand <= 0.7){return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();}
					else {return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();}
			 }
		}

		if (numAgents == 5) {
			if (sh.gamestate.cnt_vote(c) * 2 <= currentGameInfo.getAliveAgentList().size())
				;
		}
		return Talk.SKIP;
	}
}
