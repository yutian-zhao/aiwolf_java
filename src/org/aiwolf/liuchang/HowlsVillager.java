/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class HowlsVillager extends HowlsBasePlayer {

	boolean f = true;
	boolean update_sh = true;
	double ContentRand;
	Parameters params;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			f = false;
		}
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.VILLAGER, params);
		update_sh = true;
	}
	
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));
		sh.process(params, gamedata);
		int c = 0;
		if (numAgents == 5) {
			c = chooseMostLikelyWerewolf();
		}
		else {
			c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
			if (c == -1) {
				c = chooseMostLikelyWerewolf();
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
		lastTalkTurn = lastTurn;
		boolean condition = false;
		
		if (numAgents == 5) {
			condition = ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2);
		}
		else {
			condition = (day < max_day && sh.gamestate.turn <= 4 && sh.gamestate.turn >= 2);
		}
		
		if (condition) {
			int tu = sh.gamestate.turn - 2;
			if (day == 1 && sh.gamestate.turn == 2) {
				pred = 0;
				double mm = 0;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && sh.gamestate.agents[i].Alive) {
						if (mm < agentScore[day][tu][i][Util.WEREWOLF]) {
							mm = agentScore[day][tu][i][Util.WEREWOLF];
							pred = i;
						}
					}
				}
			}
		}
		
		updateState(sh);
		if (update_sh) {
			update_sh = false;
			sh.search(1000);
		}
		
		int c = 0;
		if (numAgents == 5) {
			c = chooseMostLikelyWerewolf();
		} else {
			c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
			if (c == -1) {
				c = chooseMostLikelyWerewolf();
			}
		}
		
		if (numAgents == 5) {
			if (day == 1 && sh.gamestate.turn == 1) {
				return Talk.SKIP;
			}
			else {
				if (!isValidIdx(c)) {
					return Talk.SKIP;
				}
				else {
					voteCandidate = currentGameInfo.getAgentList().get(c);
					return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
				}
			}
		}
		else {
			ContentRand = Math.random();
			if (day == 1 && sh.gamestate.turn == 1 && ContentRand > 0.5) {
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
			else {
				if (!isValidIdx(c)) {
					return Talk.SKIP;
				}
				else {
					voteCandidate = currentGameInfo.getAgentList().get(c);
					ContentRand = Math.random();
					if (ContentRand <= 0.3) {
						return (new Content(new VoteContentBuilder(voteCandidate))).getText();
					}
					else if (ContentRand > 0.3 && ContentRand <= 0.7) {
						return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
					}
					else {
						return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();
					}
				}
			}
		}
		
	}
	
}
