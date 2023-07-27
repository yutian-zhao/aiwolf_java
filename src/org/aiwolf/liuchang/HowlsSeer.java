/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class HowlsSeer extends HowlsBasePlayer {

	int target;
	
	boolean black;
	boolean[] divined;
	boolean doCO = false;
	boolean f = true;
	boolean houkoku = true;
	boolean pos;
	boolean update_sh = true;
	
	Judge divination;
	Parameters params;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			f = false;
		}
		update_sh = true;
		pos = false;
		doCO = false;
		houkoku = true;
		divined = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++) {
			divined[i] = false;
		}
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.SEER, params);
	}
	
	@Override
	public void dayStart() {
		super.dayStart();
		divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			divined[divination.getTarget().getAgentIdx() - 1] = true;
			houkoku = false;
			gamedata.add(new GameData(DataType.DIVINED, day, meint, divination.getTarget().getAgentIdx() - 1, divination.getResult() == Species.HUMAN));
			target = divination.getTarget().getAgentIdx() - 1;
			black = (divination.getResult() == Species.WEREWOLF);
		}
		sh.process(params, gamedata);
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
	
	@Override
	public Agent divine() {
		sh.process(params, gamedata);
		sh.update();
		int c = -1;
		double mn = -1;
		for (int i = 0; i < numAgents; i++) {
			if (i != meint) {
				if (sh.gamestate.agents[i].Alive) {
					if (!divined[i]) {
						double score = sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
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
		
		if (getAliveAgentsCount() <= 3) {
			if (!pos) {
				pos = true;
				double all = 0;
				double alive = 0;
				for (int i = 0; i < numAgents; i++) {
					all += sh.rp.getProb(i, Util.POSSESSED);
					if (sh.gamestate.agents[i].Alive) {
						alive += sh.rp.getProb(i, Util.POSSESSED);
					}
				}
				if (alive > 0.5 * all) {
					doCO = true;
					houkoku = true;
					return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
				}
			}
		}
		
		if (!doCO) {
			if (numAgents == 5) {
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
			else {
				if (black) {
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
				}
			}
		}
		
		if (!houkoku) {
			if (numAgents == 5) {
				houkoku = true;
				return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(target), black ? Species.WEREWOLF : Species.HUMAN))).getText();
			}
			else {
				if (black) {
					houkoku = true;
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(target), black ? Species.WEREWOLF : Species.HUMAN))).getText();
				}
			}
		}
		else {
			return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(target), black ? Species.WEREWOLF : Species.HUMAN))).getText();
		}
		
		if (!isValidIdx(c)) {
			return null;
		}
		else {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		
	}
	
}
