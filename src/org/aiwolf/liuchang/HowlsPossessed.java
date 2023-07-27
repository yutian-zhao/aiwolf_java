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

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;

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
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	
	boolean f = true;
	boolean seer = true;
	boolean doCO = false;
	boolean houkoku = true;
	boolean[] divined;
	boolean pos = false;
	boolean update_sh = true;
	
	Parameters params;
	StateHolder sh;
	StateHolder sh2;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);
			f = false;
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
		update_sh = true;
		doCO = false;
		houkoku = false;
		pos = false;
	}
	
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));
		sh.process(params, gamedata);
		int c = 0;
		double mn = -1;
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			for (int i = 0; i < numAgents; i++) {
				if (i != meint) {
					if (sh.gamestate.agents[i].Alive) {
						double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
		}
		else {
			mn = -100;
			if (numAgents == 5) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = sh2.rp.getProb(i, Util.WEREWOLF) - sh.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
			else {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = sh.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
				int t = sh.gamestate.agents[c].will_vote;
				mn = -100;
				if (t == -1) {
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
								if (mn < score) {
									mn = score;
									t = i;
								}
							}
						}
					}
				}
				c = t;
			}
		}
		if (!isValidIdx(c)) {
			return null;
		}
		else {
			return currentGameInfo.getAgentList().get(c);
		}
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
		
		int c = 0;
		double mn = -1;
		
		if (numAgents == 5) {
			if (!doCO) {
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
				if (getAliveAgentsCount() <= 3) {
					if (!pos) {
						pos = true;
						return (new Content(new ComingoutContentBuilder(me, Role.POSSESSED))).getText();
					}
				}
				else {
					c = -1;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								if (!divined[i]) {
									if (sh.gamestate.agents[i].corole == Util.SEER) {
										c = i;
									}
								}
							}
						}
					}
					c = -1;
					if (c != -1) {
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
						return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.WEREWOLF))).getText();
					}
					else {
						mn = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint) {
								if (sh.gamestate.agents[i].Alive) {
									if (!divined[i]) {
										if (mn < sh.rp.getProb(i, Util.WEREWOLF)) {
											mn = sh.rp.getProb(i, Util.WEREWOLF);
											c = i;
										}
									}
								}
							}
						}
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
						return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.HUMAN))).getText();
					}
				}
			}
		else {
			if (getAliveAgentsCount() <= 3) {
				if (!pos) {
					pos = true;
					return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
				}
			}
			else {
				c = chooseMostLikelyWerewolf();
				if (!isValidIdx(c)) {
					return Talk.SKIP;
				}
				else {	
					voteCandidate = currentGameInfo.getAgentList().get(c);
					return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
				}
			}
		}
		return Talk.SKIP;
		
	}
	
}
