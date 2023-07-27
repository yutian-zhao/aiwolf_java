/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class HowlsBodyguard extends HowlsBasePlayer {
	
	int target;
	
	boolean f = true;
	boolean update_sh = true;
	
	Parameters params;
	Agent guardedAgent;
	GameData lastGameData;
	
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
		sh.process(params,  gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.BODYGUARD, params);
		update_sh = true;
	}
	
	@Override
	public Agent guard() {
		
		sh.process(params, gamedata);
		sh.update();
		
		double mn = -1;
		int c = 0;
		
		for (int i = 0; i < numAgents; i++) {
			if (i != meint) {
				if (sh.gamestate.agents[i].Alive) {
					double score = sh.rp.getProb(i,  Util.VILLAGER) + 2 * sh.rp.getProb(i, Util.MEDIUM) + 3 * sh.rp.getProb(i, Util.SEER);
					score += 1.0 * wincnt[i] / (gamecount + 0.01);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		
		guardedAgent = currentGameInfo.getAgentList().get(c);
		target = c;
		return guardedAgent;
		
	}
	
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));
		sh.process(params, gamedata);
		int c = 0;
		c = chooseMostLikelyWerewolf();
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
		
		updateState(sh);
		if (update_sh) {
			update_sh = false;
			sh.search(1000);
		}
		
		int c = 0;
		c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
		if (c == -1) {
			c = chooseMostLikelyWerewolf();
		}
		
		if (!isValidIdx(c)) {
			return null;
		}
		else {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
		}
		
	}
	
}
